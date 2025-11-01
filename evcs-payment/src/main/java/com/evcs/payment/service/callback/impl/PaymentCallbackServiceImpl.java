package com.evcs.payment.service.callback.impl;

import com.evcs.common.tenant.TenantContext;
import com.evcs.payment.dto.CallbackRequest;
import com.evcs.payment.dto.CallbackResponse;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.IPaymentService;
import com.evcs.payment.service.callback.PaymentCallbackService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.evcs.payment.service.message.PaymentMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付回调服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private final IPaymentService paymentService;
    private final PaymentMetrics paymentMetrics;
    private final PaymentMessageService paymentMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallbackResponse handleCallback(String channel, CallbackRequest request) {
        log.info("处理支付回调: channel={}, tradeNo={}, tradeStatus={}",
                channel, request.getTradeNo(), request.getTradeStatus());

        try {
            // 1. 验证签名
            if (!verifySignature(channel, request)) {
                log.warn("支付回调签名验证失败: channel={}, tradeNo={}", channel, request.getTradeNo());
                paymentMetrics.recordCallbackFailure();
                return CallbackResponse.failure("签名验证失败");
            }

            // 2. 记录回调接收
            paymentMetrics.recordCallbackReceived();

            // 3. 查询支付订单
            PaymentOrder paymentOrder = paymentService.getByTradeNo(request.getTradeNo());
            if (paymentOrder == null) {
                log.warn("支付订单不存在: tradeNo={}", request.getTradeNo());
                paymentMetrics.recordCallbackFailure();
                return CallbackResponse.failure("支付订单不存在");
            }

            // 4. 幂等性检查
            if (PaymentStatus.SUCCESS.equals(paymentOrder.getStatusEnum())) {
                log.info("支付订单已经是成功状态，跳过处理: tradeNo={}", request.getTradeNo());
                paymentMetrics.recordCallbackSuccess();
                return createSuccessResponse(channel);
            }

            // 5. 解析支付状态
            boolean isPaymentSuccess = parsePaymentStatus(channel, request.getTradeStatus());

            // 6. 更新支付订单状态
            boolean updated = updatePaymentOrder(paymentOrder, request, isPaymentSuccess);

            if (updated) {
                paymentMetrics.recordCallbackSuccess();

                if (isPaymentSuccess) {
                    // 7. 发送业务消息通知订单服务
                    sendPaymentSuccessNotification(paymentOrder);
                }

                log.info("支付回调处理成功: channel={}, tradeNo={}, success={}",
                        channel, request.getTradeNo(), isPaymentSuccess);

                return createSuccessResponse(channel);
            } else {
                paymentMetrics.recordCallbackFailure();
                return CallbackResponse.failure("更新支付订单失败");
            }

        } catch (Exception e) {
            log.error("处理支付回调异常: channel={}, tradeNo={}", channel, request.getTradeNo(), e);
            paymentMetrics.recordCallbackFailure();
            return CallbackResponse.failure("内部处理异常");
        }
    }

    @Override
    public boolean verifySignature(String channel, CallbackRequest request) {
        try {
            // 选择对应的支付渠道验证签名
            PaymentMethod method = PaymentMethod.valueOf(channel.toUpperCase() + "_APP");
            IPaymentChannel paymentChannel = paymentService.selectChannel(method);

            if (paymentChannel != null) {
                return paymentChannel.verifySignature(request.getRawData(), request.getSign());
            }

            log.warn("未找到支付渠道: {}", channel);
            return false;
        } catch (Exception e) {
            log.error("验证签名失败: channel={}", channel, e);
            return false;
        }
    }

    /**
     * 解析支付状态
     */
    private boolean parsePaymentStatus(String channel, String tradeStatus) {
        // 支付宝状态码
        if ("alipay".equals(channel)) {
            return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
        }

        // 微信支付状态码
        if ("wechat".equals(channel)) {
            return "SUCCESS".equals(tradeStatus);
        }

        // 默认处理
        return "SUCCESS".equals(tradeStatus);
    }

    /**
     * 更新支付订单状态
     */
    private boolean updatePaymentOrder(PaymentOrder paymentOrder, CallbackRequest request, boolean isPaymentSuccess) {
        try {
            paymentOrder.setStatusEnum(isPaymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

            if (isPaymentSuccess) {
                paymentOrder.setPaidTime(LocalDateTime.now());
                paymentOrder.setOutTradeNo(request.getOutTradeNo());
            }

            paymentOrder.setUpdateBy(TenantContext.getCurrentUserId());

            return paymentService.updatePaymentOrder(paymentOrder);
        } catch (Exception e) {
            log.error("更新支付订单状态失败: tradeNo={}", request.getTradeNo(), e);
            return false;
        }
    }

    /**
     * 发送支付成功通知
     */
    private void sendPaymentSuccessNotification(PaymentOrder paymentOrder) {
        try {
            // 使用消息服务发送支付成功通知
            paymentMessageService.sendPaymentSuccessMessage(paymentOrder);

            log.info("支付成功通知已发送: orderId={}, tradeNo={}, amount={}",
                    paymentOrder.getOrderId(), paymentOrder.getTradeNo(), paymentOrder.getAmount());

        } catch (Exception e) {
            log.error("发送支付成功通知失败: orderId={}", paymentOrder.getOrderId(), e);
            // 不影响回调处理结果，只记录日志
        }
    }

    /**
     * 创建成功响应
     */
    private CallbackResponse createSuccessResponse(String channel) {
        if ("alipay".equals(channel)) {
            // 支付宝要求返回 "success"
            return CallbackResponse.success("success");
        } else if ("wechat".equals(channel)) {
            // 微信支付要求返回XML格式的成功响应
            String xmlResponse = "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
            return CallbackResponse.success(xmlResponse);
        }

        return CallbackResponse.success("OK");
    }
}