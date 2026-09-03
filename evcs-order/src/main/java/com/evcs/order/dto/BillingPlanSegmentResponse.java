package com.evcs.order.dto;

import com.evcs.order.entity.BillingPlanSegment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 计费计划分段响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class BillingPlanSegmentResponse {
    Long id;
    Long tenantId;
    Long planId;
    Integer segmentIndex;
    String startTime;
    String endTime;
    BigDecimal energyPrice;
    BigDecimal serviceFee;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static BillingPlanSegmentResponse from(com.evcs.order.entity.BillingPlanSegment e) {
        if (e == null) {
            return null;
        }
        return BillingPlanSegmentResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .planId(e.getPlanId())
                .segmentIndex(e.getSegmentIndex())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .energyPrice(e.getEnergyPrice())
                .serviceFee(e.getServiceFee())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
