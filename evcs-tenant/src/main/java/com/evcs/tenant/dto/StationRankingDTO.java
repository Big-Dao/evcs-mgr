package com.evcs.tenant.dto;

import lombok.Data;

/**
 * 充电站排名DTO
 */
@Data
public class StationRankingDTO {
    
    /**
     * 充电站ID
     */
    private Long id;
    
    /**
     * 充电站名称
     */
    private String name;
    
    /**
     * 订单数量
     */
    private Integer orders;
    
    /**
     * 百分比（相对于最高的站点）
     */
    private Integer percentage;
}
