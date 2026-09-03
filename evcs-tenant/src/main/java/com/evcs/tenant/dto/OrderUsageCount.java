package com.evcs.tenant.dto;

/**
 * 租户订单用量计数（来自 order 服务内部端点的数据）。
 */
public record OrderUsageCount(
        Long tenantId,
        int orderCount
) {
}
