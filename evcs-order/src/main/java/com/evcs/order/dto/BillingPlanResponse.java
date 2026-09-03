package com.evcs.order.dto;

import com.evcs.order.entity.BillingPlan;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 计费计划响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class BillingPlanResponse {
    Long id;
    Long tenantId;
    Long stationId;
    String name;
    String code;
    Integer status;
    Integer isDefault;
    Integer priority;
    java.time.LocalDate effectiveStartDate;
    java.time.LocalDate effectiveEndDate;
    Integer segmentCount;
    Integer stationCount;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static BillingPlanResponse from(com.evcs.order.entity.BillingPlan e) {
        if (e == null) {
            return null;
        }
        return BillingPlanResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .stationId(e.getStationId())
                .name(e.getName())
                .code(e.getCode())
                .status(e.getStatus())
                .isDefault(e.getIsDefault())
                .priority(e.getPriority())
                .effectiveStartDate(e.getEffectiveStartDate())
                .effectiveEndDate(e.getEffectiveEndDate())
                .segmentCount(e.getSegmentCount())
                .stationCount(e.getStationCount())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
