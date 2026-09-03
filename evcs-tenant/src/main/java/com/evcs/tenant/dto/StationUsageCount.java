package com.evcs.tenant.dto;

/**
 * 租户资源用量计数（来自 station 服务内部端点的数据）。
 */
public record StationUsageCount(
        Long tenantId,
        int stationCount,
        int chargerCount
) {
}
