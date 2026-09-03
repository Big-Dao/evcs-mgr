package com.evcs.order.dto;

import com.evcs.order.entity.ChargingOrder;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电订单响应 DTO。
 *
 * <p>对外暴露订单业务字段；{@code paymentTradeId}（第三方交易号）、
 * {@code version}/{@code deleted} 与审计人字段属于 Entity 内部结构，
 * 不得通过 API 泄漏。
 */
@Value
@Builder
public class OrderResponse {
    Long id;
    Long tenantId;
    Long stationId;
    Long chargerId;
    String sessionId;
    Long userId;
    Long billingPlanId;
    Long couponId;
    Integer status;
    LocalDateTime startTime;
    LocalDateTime endTime;
    Double energy;
    Long duration;
    BigDecimal amount;
    BigDecimal discountAmount;
    BigDecimal payAmount;
    LocalDateTime paidTime;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static OrderResponse from(ChargingOrder order) {
        if (order == null) {
            return null;
        }
        return OrderResponse.builder()
                .id(order.getId())
                .tenantId(order.getTenantId())
                .stationId(order.getStationId())
                .chargerId(order.getChargerId())
                .sessionId(order.getSessionId())
                .userId(order.getUserId())
                .billingPlanId(order.getBillingPlanId())
                .couponId(order.getCouponId())
                .status(order.getStatus())
                .startTime(order.getStartTime())
                .endTime(order.getEndTime())
                .energy(order.getEnergy())
                .duration(order.getDuration())
                .amount(order.getAmount())
                .discountAmount(order.getDiscountAmount())
                .payAmount(order.getPayAmount())
                .paidTime(order.getPaidTime())
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime())
                .build();
    }
}
