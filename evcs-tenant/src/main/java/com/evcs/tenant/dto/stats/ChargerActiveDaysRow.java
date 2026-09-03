package com.evcs.tenant.dto.stats;

/**
 * 仪表盘统计投影（来自归属服务内部 API 的数据）。
 */
public record ChargerActiveDaysRow(
        Long chargerId, Integer activeDays
) {
}
