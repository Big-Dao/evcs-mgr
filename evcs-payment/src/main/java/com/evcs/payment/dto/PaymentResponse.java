package com.evcs.payment.dto;

import com.evcs.payment.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付响应
 */
@Data
public class PaymentResponse {
    /**
     * 支付订单ID
     */
    private Long paymentId;

    /**
     * 交易流水号
     */
    private String tradeNo;

    /**
     * 支付参数（用于客户端调起支付）
     */
    private String payParams;

    /**
     * 支付URL（扫码支付使用）
     */
    private String payUrl;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付状态
     */
    private PaymentStatus status;
}
