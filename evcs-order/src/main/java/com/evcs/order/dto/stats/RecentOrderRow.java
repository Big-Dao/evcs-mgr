package com.evcs.order.dto.stats;

/**
 * 仪表盘统计投影（内部 API 返回，字段与归属服务数据对齐）。
 */
public record RecentOrderRow(
        String sessionId, Long stationId, Long chargerId, Long userId, java.math.BigDecimal amount, Integer status, java.time.LocalDateTime createTime
) {
}
