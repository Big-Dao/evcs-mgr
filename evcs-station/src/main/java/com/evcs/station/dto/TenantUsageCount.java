package com.evcs.station.dto;

import lombok.Value;

/**
 * 租户资源用量计数（内部 API 返回的最小字段集）。
 */
@Value
public class TenantUsageCount {
    Long tenantId;
    long stationCount;
    long chargerCount;
}
