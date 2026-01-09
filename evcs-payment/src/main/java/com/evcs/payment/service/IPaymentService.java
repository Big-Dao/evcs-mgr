package com.evcs.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;

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
     * 处理支付最终态收敛（用于对账/轮询等后台任务）
     *
     * @return true 表示已处理或已是最终态；false 表示未能处理（例如锁竞争）
     */
    boolean handlePaymentFinalStatus(String tradeNo, PaymentStatus finalStatus);

    /**
     * 处理退款最终态收敛（用于退款中订单的后台轮询补偿）
     *
     * @param paymentId       支付订单ID
     * @param refundRequestNo 退款请求号（微信 out_refund_no / 支付宝 out_request_no）
     * @param refundStatus    渠道退款状态（SUCCESS/PROCESSING/CLOSED/ABNORMAL 等）
     * @param refundAmount    本次退款确认金额（可为空，空则回退到订单上的 refundRequestAmount）
     */
    boolean handleRefundFinalStatus(Long paymentId, String refundRequestNo, String refundStatus, java.math.BigDecimal refundAmount);

    /**
     * 退款
     */
    RefundResponse refund(RefundRequest request);

    /**
     * 根据业务订单ID查询支付订单
     */
    PaymentOrder getByOrderId(Long orderId);

    /**
     * 根据交易流水号查询支付订单
     */
    PaymentOrder getByTradeNo(String tradeNo);

    /**
     * 更新支付订单
     */
    boolean updatePaymentOrder(PaymentOrder paymentOrder);

    /**
     * 选择支付渠道
     */
    com.evcs.payment.service.channel.IPaymentChannel selectChannel(PaymentMethod method);
}
