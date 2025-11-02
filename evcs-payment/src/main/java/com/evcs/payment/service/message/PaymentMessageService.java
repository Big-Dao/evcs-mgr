package com.evcs.payment.service.message;

import com.evcs.payment.entity.PaymentOrder;

/**
 * 支付消息服务接口
 *
 * 负责发送支付相关的业务消息
 */
public interface PaymentMessageService {

    /**
     * 发送支付成功消息
     *
     * @param paymentOrder 支付订单
     */
    void sendPaymentSuccessMessage(PaymentOrder paymentOrder);

    /**
     * 发送支付失败消息
     *
     * @param paymentOrder 支付订单
     */
    void sendPaymentFailureMessage(PaymentOrder paymentOrder);

    /**
     * 发送退款成功消息
     *
     * @param paymentOrder 支付订单
     */
    void sendRefundSuccessMessage(PaymentOrder paymentOrder);
}