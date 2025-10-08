package com.evcs.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("charging_order")
public class ChargingOrder extends BaseEntity {
    // 状态常量
    public static final int STATUS_CREATED = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_CANCELLED = 2;
    public static final int STATUS_TO_PAY = 10;
    public static final int STATUS_PAID = 11;
    public static final int STATUS_REFUNDING = 12;
    public static final int STATUS_REFUNDED = 13;
    private Long stationId;
    private Long chargerId;
    private String sessionId;
    private Long userId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;


    private Double energy;      // kWh
    private Long duration;      // minutes
    private BigDecimal amount;  // total amount

    private Long billingPlanId; // assigned at start if charger has plan

    // 简易支付字段（占位）
    private String paymentTradeId;  // 第三方支付单号/交易ID
    private LocalDateTime paidTime; // 支付完成时间

    private Integer status;     // 0-created, 1-completed, 2-cancelled, 10-to_pay, 11-paid
}
