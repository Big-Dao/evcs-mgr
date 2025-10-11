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
}
