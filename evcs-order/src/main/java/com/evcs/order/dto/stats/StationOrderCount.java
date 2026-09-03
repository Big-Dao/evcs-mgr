package com.evcs.order.dto.stats;

/**
 * 仪表盘统计投影（内部 API 返回，字段与归属服务数据对齐）。
 */
public record StationOrderCount(
        Long stationId, Long orderCount
) {
}
