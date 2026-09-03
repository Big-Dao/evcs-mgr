package com.evcs.order.dto;

/**
 * 租户订单用量计数（内部 API 返回的最小字段集）。
 */
public record OrderUsageCount(
        Long tenantId,
        long orderCount
) {
}
