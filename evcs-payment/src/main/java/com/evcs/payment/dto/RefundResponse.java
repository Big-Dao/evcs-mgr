package com.evcs.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款响应
 */
@Data
public class RefundResponse {
    /**
     * 退款流水号
     */
    private String refundNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款状态
     */
    private String refundStatus;
}
