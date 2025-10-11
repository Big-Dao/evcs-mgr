package com.evcs.payment.dto;

import com.evcs.payment.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付请求
 */
@Data
public class PaymentRequest {
    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式
     */
    private PaymentMethod paymentMethod;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 幂等键（用于防止重复支付）
     */
    private String idempotentKey;

    /**
     * 支付描述
     */
    private String description;
}
