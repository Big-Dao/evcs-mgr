package com.evcs.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Dashboard统计数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "租户数量")
    private Long tenantCount;
    
    @Schema(description = "用户数量")
    private Long userCount;
    
    @Schema(description = "充电站数量")
    private Long stationCount;
    
    @Schema(description = "充电桩数量")
    private Long chargerCount;
    
    @Schema(description = "今日订单数")
    private Long todayOrderCount;
    
    @Schema(description = "今日充电量(kWh)")
    private BigDecimal todayChargingAmount;
    
    @Schema(description = "今日收入(元)")
    private BigDecimal todayRevenue;
}
