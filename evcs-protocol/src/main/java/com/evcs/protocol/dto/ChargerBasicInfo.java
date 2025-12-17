package com.evcs.protocol.dto;

import lombok.Data;

/**
 * 充电桩基础信息（用于跨服务查询时的最小字段集）
 */
@Data
public class ChargerBasicInfo {
    private Long id;
    private Long tenantId;
    private Long stationId;
    private String chargerCode;
    private String chargerName;
}
