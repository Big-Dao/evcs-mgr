package com.evcs.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_plan")
public class BillingPlan extends BaseEntity {
    private Long stationId;     // 站点ID，null 表示租户默认
    private String name;
    private String code;
    private Integer status;     // 1-启用 0-禁用
    private Integer isDefault;  // 1-站点默认
    private Integer priority;   // 优先级，越大越优先
    private java.time.LocalDate effectiveStartDate; // 生效开始日期，可为空
    private java.time.LocalDate effectiveEndDate;   // 生效结束日期，可为空
}
