package com.evcs.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.order.dto.OrderUsageCount;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.mapper.ChargingOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户订单用量统计服务。
 *
 * <p>供内部 API 使用：tenant 服务的删除预检需要订单计数，
 * 数据归属 order 服务，按调用方给定的租户ID集合显式统计
 * （受控禁用租户过滤，等价于跨租户管理查询）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderUsageService {

    private final ChargingOrderMapper chargingOrderMapper;

    public List<OrderUsageCount> getUsageCounts(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return List.of();
        }

        try {
            CustomTenantLineHandler.disableTenantFilter();
            QueryWrapper<ChargingOrder> query = new QueryWrapper<ChargingOrder>()
                    .select("tenant_id", "COUNT(*) AS cnt")
                    .in("tenant_id", tenantIds)
                    .groupBy("tenant_id");

            Map<Long, Long> counts = new HashMap<>();
            for (Map<String, Object> row : chargingOrderMapper.selectMaps(query)) {
                Long tenantId = readLong(row, "tenant_id");
                Long cnt = readLong(row, "cnt");
                if (tenantId != null) {
                    counts.put(tenantId, cnt == null ? Long.valueOf(0L) : cnt);
                }
            }

            List<OrderUsageCount> result = new ArrayList<>(tenantIds.size());
            for (Long tenantId : tenantIds.stream().distinct().sorted().toList()) {
                result.add(new OrderUsageCount(tenantId, counts.getOrDefault(tenantId, 0L)));
            }
            return result;
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }

    private Long readLong(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key) && entry.getValue() != null) {
                return Long.parseLong(String.valueOf(entry.getValue()));
            }
        }
        return null;
    }
}
