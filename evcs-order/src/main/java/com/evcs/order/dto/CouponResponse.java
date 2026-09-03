package com.evcs.order.dto;

import com.evcs.order.entity.Coupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 优惠券响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class CouponResponse {
    Long id;
    Long tenantId;
    Long userId;
    String name;
    Integer type;
    BigDecimal value;
    BigDecimal minAmount;
    LocalDateTime startTime;
    LocalDateTime endTime;
    Integer status;
    Long orderId;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static CouponResponse from(com.evcs.order.entity.Coupon e) {
        if (e == null) {
            return null;
        }
        return CouponResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .userId(e.getUserId())
                .name(e.getName())
                .type(e.getType())
                .value(e.getValue())
                .minAmount(e.getMinAmount())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .status(e.getStatus())
                .orderId(e.getOrderId())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
