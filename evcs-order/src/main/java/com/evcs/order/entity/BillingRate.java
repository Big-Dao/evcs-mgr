package com.evcs.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_rate")
public class BillingRate extends BaseEntity {
    private Long stationId;              // null 表示租户默认
    private Integer touEnabled;          // 0/1
    private String peakStart;            // HH:mm
    private String peakEnd;              // HH:mm
    private BigDecimal peakPrice;        // 高峰价
    private BigDecimal offpeakPrice;     // 低谷价
    private BigDecimal flatPrice;        // 平价
    private BigDecimal serviceFee;       // 服务费
    private Integer status;              // 1-启用 0-禁用
}
