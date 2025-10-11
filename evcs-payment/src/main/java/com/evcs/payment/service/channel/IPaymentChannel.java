package com.evcs.payment.service.channel;

import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;

/**
 * 支付渠道接口
 */
public interface IPaymentChannel {

    /**
     * 创建支付
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * 查询支付状态
     */
    PaymentResponse queryPayment(String tradeNo);

    /**
     * 退款
     */
    RefundResponse refund(RefundRequest request);

    /**
     * 验证签名（用于异步通知）
     */
    boolean verifySignature(String data, String signature);

    /**
     * 获取支付渠道名称
     */
    String getChannelName();
}
