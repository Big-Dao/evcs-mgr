package com.evcs.payment.service;

import com.evcs.payment.entity.PaymentOrder;

/**
 * 订单同步服务接口
 *
 * 负责支付成功后同步更新订单状态
 */
public interface OrderSyncService {

    /**
     * 同步支付成功状态到订单服务
     *
     * @param paymentOrder 支付订单
     * @return 是否同步成功
     */
    boolean syncPaymentSuccess(PaymentOrder paymentOrder);

    /**
     * 同步支付失败状态到订单服务
     *
     * @param paymentOrder 支付订单
     * @param reason 失败原因
     * @return 是否同步成功
     */
    boolean syncPaymentFailure(PaymentOrder paymentOrder, String reason);

    /**
     * 检查订单同步状态
     *
     * @param paymentOrderId 支付订单ID
     * @return 是否已同步
     */
    boolean isOrderSynced(Long paymentOrderId);
}