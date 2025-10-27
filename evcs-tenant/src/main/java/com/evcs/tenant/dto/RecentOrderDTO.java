package com.evcs.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 最近订单DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "最近订单信息")
public class RecentOrderDTO {
    
    @Schema(description = "订单ID")
    private String orderId;
    
    @Schema(description = "充电站名称")
    private String stationName;
    
    @Schema(description = "充电桩编码")
    private String chargerCode;
    
    @Schema(description = "用户名称")
    private String userName;
    
    @Schema(description = "订单金额(元)")
    private BigDecimal amount;
    
    @Schema(description = "订单状态")
    private String status;
    
    @Schema(description = "创建时间")
    private String createTime;
}
