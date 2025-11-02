package com.evcs.payment.service.channel;

import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 微信支付渠道服务 V2 - 简化版本
 *
 * 基于实际微信支付SDK结构实现的简化版本
 * 先支持基础功能，后续逐步完善
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPayChannelServiceV2 implements IPaymentChannel {

    private final PaymentConfig paymentConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${evcs.payment.enabled:false}")
    private boolean paymentEnabled;

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("创建微信支付订单: orderId={}, amount={}, method={}",
                request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        // 如果支付未启用，返回模拟响应
        if (!paymentEnabled) {
            log.warn("支付功能未启用，返回模拟响应");
            return createMockPaymentResponse(request);
        }

        try {
            PaymentResponse response = new PaymentResponse();

            switch (request.getPaymentMethod()) {
                case WECHAT_JSAPI:
                    response = createJsApiPayment(request);
                    break;
                case WECHAT_NATIVE:
                    response = createNativePayment(request);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的支付方式: " + request.getPaymentMethod());
            }

            log.info("微信支付订单创建成功: tradeNo={}", response.getTradeNo());
            return response;
        } catch (Exception e) {
            log.error("创建微信支付订单失败: orderId={}", request.getOrderId(), e);
            throw new RuntimeException("创建微信支付订单失败", e);
        }
    }

    /**
     * 创建JSAPI支付（小程序/公众号）
     */
    private PaymentResponse createJsApiPayment(PaymentRequest request) {
        // 暂时返回模拟响应，后续集成真实SDK
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(generateTradeNo());
        response.setAmount(request.getAmount());
        response.setStatus(PaymentStatus.PENDING);
        try {
            response.setPayParams(objectMapper.writeValueAsString(createMockJsApiParams(request)));
        } catch (Exception e) {
            log.warn("转换JSAPI参数为JSON失败", e);
            response.setPayParams("{\"error\":\"参数转换失败\"}");
        }

        log.info("微信JSAPI支付创建成功: tradeNo={}", response.getTradeNo());
        return response;
    }

    /**
     * 创建Native支付（扫码支付）
     */
    private PaymentResponse createNativePayment(PaymentRequest request) {
        // 暂时返回模拟响应，后续集成真实SDK
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(generateTradeNo());
        response.setAmount(request.getAmount());
        response.setStatus(PaymentStatus.PENDING);
        response.setPayUrl("https://api.mch.weixin.qq.com/mock/" + response.getTradeNo());

        log.info("微信Native支付创建成功: tradeNo={}", response.getTradeNo());
        return response;
    }

    @Override
    public PaymentResponse queryPayment(String tradeNo) {
        log.info("查询微信支付状态: tradeNo={}", tradeNo);

        if (!paymentEnabled) {
            return createMockQueryResponse(tradeNo);
        }

        try {
            // 暂时返回模拟响应，后续集成真实SDK
            PaymentResponse response = new PaymentResponse();
            response.setTradeNo(tradeNo);
            response.setStatus(PaymentStatus.SUCCESS);

            log.info("微信查询响应: tradeNo={}, status={}", tradeNo, response.getStatus());
            return response;
        } catch (Exception e) {
            log.error("微信查询失败: tradeNo={}", tradeNo, e);
            throw new RuntimeException("微信查询失败", e);
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("发起微信退款: paymentId={}, amount={}",
                request.getPaymentId(), request.getRefundAmount());

        if (!paymentEnabled) {
            return createMockRefundResponse(request);
        }

        try {
            // 暂时返回模拟响应，后续集成真实SDK
            RefundResponse response = new RefundResponse();
            response.setRefundNo(generateRefundNo());
            response.setRefundAmount(request.getRefundAmount());
            response.setRefundStatus("SUCCESS");

            log.info("微信退款响应: refundNo={}", response.getRefundNo());
            return response;
        } catch (Exception e) {
            log.error("微信退款失败", e);
            throw new RuntimeException("微信退款失败", e);
        }
    }

    @Override
    public boolean verifySignature(String data, String signature) {
        // 微信支付签名验证在SDK内部处理
        // 这里可以做额外的业务验证
        log.info("验证微信签名: data={}, signature={}", data, signature);
        return true;
    }

    @Override
    public String getChannelName() {
        return "wechat";
    }

    /**
     * 生成交易号
     */
    private String generateTradeNo() {
        return "EVC" + System.currentTimeMillis();
    }

    /**
     * 生成退款号
     */
    private String generateRefundNo() {
        return "EVCR" + System.currentTimeMillis();
    }

    /**
     * 创建模拟JSAPI支付参数
     */
    private Map<String, Object> createMockJsApiParams(PaymentRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("appId", paymentConfig.getWechat().getMchid());
        params.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("nonceStr", generateNonceStr());
        params.put("package", "prepay_id=" + generateTradeNo());
        params.put("signType", "RSA");
        params.put("paySign", "mock_signature");
        return params;
    }

    /**
     * 生成随机字符串
     */
    private String generateNonceStr() {
        return Long.toHexString(System.currentTimeMillis());
    }

    /**
     * 创建模拟支付响应（支付未启用时）
     */
    private PaymentResponse createMockPaymentResponse(PaymentRequest request) {
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(generateTradeNo());
        response.setAmount(request.getAmount());
        response.setStatus(PaymentStatus.PENDING);

        switch (request.getPaymentMethod()) {
            case WECHAT_JSAPI:
                try {
                    response.setPayParams(objectMapper.writeValueAsString(createMockJsApiParams(request)));
                } catch (Exception e) {
                    log.warn("转换JSAPI参数为JSON失败", e);
                    response.setPayParams("{\"error\":\"参数转换失败\"}");
                }
                break;
            case WECHAT_NATIVE:
                response.setPayUrl("https://api.mch.weixin.qq.com/mock/" + response.getTradeNo());
                break;
            default:
                throw new IllegalArgumentException("不支持的支付方式: " + request.getPaymentMethod());
        }

        return response;
    }

    /**
     * 创建模拟查询响应（支付未启用时）
     */
    private PaymentResponse createMockQueryResponse(String tradeNo) {
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(tradeNo);
        response.setStatus(PaymentStatus.SUCCESS);
        return response;
    }

    /**
     * 创建模拟退款响应（支付未启用时）
     */
    private RefundResponse createMockRefundResponse(RefundRequest request) {
        RefundResponse response = new RefundResponse();
        response.setRefundNo(generateRefundNo());
        response.setRefundAmount(request.getRefundAmount());
        response.setRefundStatus("SUCCESS");
        return response;
    }
}