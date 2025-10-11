package com.evcs.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.entity.PaymentOrder;

/**
 * 支付服务接口
 */
public interface IPaymentService extends IService<PaymentOrder> {

    /**
     * 创建支付订单
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * 查询支付状态
     */
    PaymentResponse queryPayment(String tradeNo);

    /**
     * 处理支付回调
     */
    boolean handlePaymentCallback(String tradeNo, boolean success);

    /**
     * 退款
     */
    RefundResponse refund(RefundRequest request);

    /**
     * 根据业务订单ID查询支付订单
     */
    PaymentOrder getByOrderId(Long orderId);
}
