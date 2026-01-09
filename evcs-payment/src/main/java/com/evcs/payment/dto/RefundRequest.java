package com.evcs.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款请求
 */
@Data
public class RefundRequest {
    /**
     * 支付订单ID
     */
    private Long paymentId;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 原支付金额
     */
    private BigDecimal totalAmount;

    /**
     * 商户支付订单号（out_trade_no）
     */
    private String tradeNo;

    /**
     * 微信/第三方交易流水号（transaction_id）
     */
    private String transactionId;

    /**
     * 退款请求号（微信 out_refund_no / 支付宝 out_request_no）
     */
    private String refundRequestNo;
}
