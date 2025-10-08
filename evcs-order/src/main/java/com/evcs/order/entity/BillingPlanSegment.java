package com.evcs.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_plan_segment")
public class BillingPlanSegment extends BaseEntity {
    private Long planId;
    private Integer segmentIndex;       // 1..96
    private String startTime;           // HH:mm
    private String endTime;             // HH:mm
    private BigDecimal energyPrice;     // 电价
    private BigDecimal serviceFee;      // 服务费(按kWh)
}
