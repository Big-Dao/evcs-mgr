package com.evcs.order.dto;

import java.time.LocalDate;
import lombok.Data;

/**
 * 输入请求 DTO：仅暴露调用方可写业务字段。
 * 租户归属、逻辑删除、审计人与设备运行时字段不在绑定面（批量赋值防护）。
 */
@Data
public class BillingPlanUpsertRequest {
    private String name;
    private String code;
    private Long stationId;              // null 表示租户默认
    private Integer status;              // 1-启用 0-禁用
    private Integer isDefault;           // 1-站点默认
    private Integer priority;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;

    public com.evcs.order.entity.BillingPlan toEntity() {
        com.evcs.order.entity.BillingPlan e = new com.evcs.order.entity.BillingPlan();
        applyTo(e);
        return e;
    }

    public void applyTo(com.evcs.order.entity.BillingPlan e) {
        e.setName(name);
        e.setCode(code);
        e.setStationId(stationId);
        e.setStatus(status);
        e.setIsDefault(isDefault);
        e.setPriority(priority);
        e.setEffectiveStartDate(effectiveStartDate);
        e.setEffectiveEndDate(effectiveEndDate);
    }
}
