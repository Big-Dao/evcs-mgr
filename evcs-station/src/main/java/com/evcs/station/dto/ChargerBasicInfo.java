package com.evcs.station.dto;

import lombok.Data;

/**
 * 充电器基础信息（跨服务内部查询返回的最小字段集）。
 */
@Data
public class ChargerBasicInfo {
    private Long id;
    private Long tenantId;
    private Long stationId;
    private String chargerCode;
    private String chargerName;
}
