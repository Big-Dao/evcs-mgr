package com.evcs.tenant.dto;

import lombok.Data;

/**
 * 充电桩利用率DTO
 */
@Data
public class ChargerUtilizationDTO {
    
    /**
     * 充电桩ID
     */
    private Long id;
    
    /**
     * 充电桩编码
     */
    private String code;
    
    /**
     * 利用率（百分比）
     */
    private Integer utilization;
}
