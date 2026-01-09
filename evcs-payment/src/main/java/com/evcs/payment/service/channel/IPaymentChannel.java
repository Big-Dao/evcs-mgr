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
     * 查询退款状态（用于退款中订单的后台轮询补偿）。
     *
     * @param refundRequestNo 退款请求号（微信 out_refund_no / 支付宝 out_request_no）
     */
    default RefundResponse queryRefund(String refundRequestNo) {
        throw new UnsupportedOperationException("当前渠道不支持退款状态查询");
    }

    /**
     * 验证签名（用于异步通知）
     */
    boolean verifySignature(String data, String signature);

    /**
     * 获取支付渠道名称
     */
    String getChannelName();
}
