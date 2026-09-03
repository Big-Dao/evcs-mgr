package com.evcs.station.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import com.evcs.station.dto.TenantUsageCount;
import com.evcs.station.mapper.ChargerMapper;
import com.evcs.station.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户资源用量统计服务。
 *
 * <p>供内部 API 使用：tenant 服务的配额校验需要站点/充电桩计数，
 * 数据归属 station 服务，按调用方给定的租户ID集合显式统计
 * （受控禁用租户过滤，等价于跨租户管理查询）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationUsageService {

    private final StationMapper stationMapper;
    private final ChargerMapper chargerMapper;

    public List<TenantUsageCount> getUsageCounts(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return List.of();
        }

        try {
            CustomTenantLineHandler.disableTenantFilter();
            Map<Long, Long> stationCounts = countByTenant(stationMapper.selectMaps(stationQuery(tenantIds)));
            Map<Long, Long> chargerCounts = countByTenant(chargerMapper.selectMaps(chargerQuery(tenantIds)));

            List<TenantUsageCount> result = new ArrayList<>(tenantIds.size());
            for (Long tenantId : tenantIds.stream().distinct().sorted().toList()) {
                result.add(new TenantUsageCount(
                        tenantId,
                        stationCounts.getOrDefault(tenantId, 0L),
                        chargerCounts.getOrDefault(tenantId, 0L)));
            }
            return result;
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }

    private QueryWrapper<Station> stationQuery(List<Long> tenantIds) {
        return new QueryWrapper<Station>()
                .select("tenant_id", "COUNT(*) AS cnt")
                .in("tenant_id", tenantIds)
                .groupBy("tenant_id");
    }

    private QueryWrapper<Charger> chargerQuery(List<Long> tenantIds) {
        return new QueryWrapper<Charger>()
                .select("tenant_id", "COUNT(*) AS cnt")
                .in("tenant_id", tenantIds)
                .groupBy("tenant_id");
    }

    private Map<Long, Long> countByTenant(List<Map<String, Object>> rows) {
        Map<Long, Long> counts = new HashMap<>();
        if (rows == null) {
            return counts;
        }
        for (Map<String, Object> row : rows) {
            Long tenantId = readLong(row, "tenant_id");
            Long cnt = readLong(row, "cnt");
            if (tenantId != null) {
                counts.put(tenantId, cnt == null ? 0L : cnt);
            }
        }
        return counts;
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
