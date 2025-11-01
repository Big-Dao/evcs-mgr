package com.evcs.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.evcs.payment.config.AlipayConfig;
import com.evcs.payment.dto.RefundCallbackRequest;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.service.IRefundCallbackService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 退款回调服务实现
 */
@Slf4j
@Service
public class RefundCallbackServiceImpl implements IRefundCallbackService {

    @Resource
    private AlipayConfig alipayConfig;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private Map<String, IPaymentChannel> paymentChannelMap;

    @Override
    @Transactional
    public boolean handleRefundCallback(RefundCallbackRequest callbackRequest) {
        log.info("处理退款回调: channel={}, outTradeNo={}, outRequestNo={}, refundStatus={}",
            callbackRequest.getChannel(), callbackRequest.getOutTradeNo(),
            callbackRequest.getOutRequestNo(), callbackRequest.getRefundStatus());

        try {
            // 1. 验证签名
            if (!verifyRefundCallbackSignature(callbackRequest)) {
                log.error("退款回调签名验证失败: channel={}, outTradeNo={}",
                    callbackRequest.getChannel(), callbackRequest.getOutTradeNo());
                return false;
            }

            // 2. 查找支付订单
            QueryWrapper<PaymentOrder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("trade_no", callbackRequest.getOutTradeNo())
                       .eq("deleted", 0);
            PaymentOrder order = paymentOrderMapper.selectOne(queryWrapper);

            if (order == null) {
                log.warn("退款回调对应的订单不存在: outTradeNo={}", callbackRequest.getOutTradeNo());
                return false;
            }

            // 3. 检查订单状态
            if (order.getStatusEnum() != PaymentStatus.SUCCESS) {
                log.warn("退款回调对应订单状态不正确: orderId={}, status={}",
                    order.getId(), order.getStatus());
                return false;
            }

            // 4. 处理退款状态
            boolean refundSuccess = processRefundStatus(callbackRequest, order);

            log.info("退款回调处理完成: channel={}, outTradeNo={}, success={}",
                callbackRequest.getChannel(), callbackRequest.getOutTradeNo(), refundSuccess);

            return refundSuccess;

        } catch (Exception e) {
            log.error("处理退款回调失败: channel={}, outTradeNo={}",
                callbackRequest.getChannel(), callbackRequest.getOutTradeNo(), e);
            return false;
        }
    }

    @Override
    public boolean verifyRefundCallbackSignature(RefundCallbackRequest callbackRequest) {
        try {
            switch (callbackRequest.getChannel()) {
                case "alipay":
                    return verifyAlipayRefundSignature(callbackRequest);
                case "wechat":
                    return verifyWechatRefundSignature(callbackRequest);
                default:
                    log.warn("不支持的渠道: {}", callbackRequest.getChannel());
                    return false;
            }
        } catch (Exception e) {
            log.error("验证退款回调签名失败: channel={}", callbackRequest.getChannel(), e);
            return false;
        }
    }

    @Override
    public RefundCallbackRequest parseAlipayRefundCallback(Map<String, String> params) {
        RefundCallbackRequest request = new RefundCallbackRequest();
        request.setChannel("alipay");
        request.setOutTradeNo(params.get("out_trade_no"));
        request.setOutRequestNo(params.get("out_request_no"));
        request.setTradeNo(params.get("trade_no"));
        request.setRefundFee(new BigDecimal(params.get("refund_fee")));
        request.setRefundStatus(params.get("refund_status"));
        request.setReason(params.get("refund_reason"));
        request.setGmtRefundPay(params.get("gmt_refund_pay"));
        request.setRawParams(params);
        request.setSign(params.get("sign"));
        request.setSignType(params.get("sign_type"));

        return request;
    }

    @Override
    public RefundCallbackRequest parseWechatRefundCallback(String xmlData) {
        // TODO: 实现微信退款回调解析
        log.warn("微信退款回调解析待实现");
        return null;
    }

    /**
     * 处理退款状态
     */
    private boolean processRefundStatus(RefundCallbackRequest callbackRequest, PaymentOrder order) {
        String refundStatus = callbackRequest.getRefundStatus();
        BigDecimal refundAmount = callbackRequest.getRefundFee();

        switch (refundStatus) {
            case "REFUND_SUCCESS":
                return handleRefundSuccess(order, refundAmount, callbackRequest);
            case "REFUND_FAILED":
                return handleRefundFailed(order, refundAmount, callbackRequest);
            default:
                log.warn("未知的退款状态: {}", refundStatus);
                return false;
        }
    }

    /**
     * 处理退款成功
     */
    private boolean handleRefundSuccess(PaymentOrder order, BigDecimal refundAmount, RefundCallbackRequest callbackRequest) {
        try {
            // 更新订单状态为已退款
            order.setStatus(PaymentStatus.REFUNDED.getCode());
            order.setRefundAmount(refundAmount);
            order.setRefundTime(java.time.LocalDateTime.now());

            int updateCount = paymentOrderMapper.updateById(order);
            if (updateCount > 0) {
                log.info("订单退款状态更新成功: orderId={}, refundAmount={}",
                    order.getId(), refundAmount);
                return true;
            } else {
                log.error("订单退款状态更新失败: orderId={}", order.getId());
                return false;
            }
        } catch (Exception e) {
            log.error("处理退款成功状态失败: orderId={}", order.getId(), e);
            return false;
        }
    }

    /**
     * 处理退款失败
     */
    private boolean handleRefundFailed(PaymentOrder order, BigDecimal refundAmount, RefundCallbackRequest callbackRequest) {
        log.warn("退款失败: orderId={}, refundAmount={}, reason={}",
            order.getId(), refundAmount, callbackRequest.getReason());
        // 这里可以记录退款失败信息，或者发送通知
        return true; // 回调处理成功，虽然退款失败
    }

    /**
     * 验证支付宝退款回调签名
     */
    private boolean verifyAlipayRefundSignature(RefundCallbackRequest callbackRequest) {
        try {
            // 获取对应的支付渠道服务
            IPaymentChannel channel = paymentChannelMap.get("alipay");
            if (channel == null) {
                log.error("找不到支付宝支付渠道服务");
                return false;
            }

            // 使用支付渠道服务验证签名
            return channel.verifySignature(
                callbackRequest.getRawParams().toString(),
                callbackRequest.getSign()
            );
        } catch (Exception e) {
            log.error("验证支付宝退款回调签名失败", e);
            return false;
        }
    }

    /**
     * 验证微信退款回调签名
     */
    private boolean verifyWechatRefundSignature(RefundCallbackRequest callbackRequest) {
        // TODO: 实现微信退款回调签名验证
        log.warn("微信退款回调签名验证待实现");
        return true; // 暂时返回true
    }
}