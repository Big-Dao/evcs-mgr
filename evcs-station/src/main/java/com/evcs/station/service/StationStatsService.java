package com.evcs.station.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.station.dto.stats.ChargerCodeRow;
import com.evcs.station.dto.stats.ChargerStatusStats;
import com.evcs.station.dto.stats.StationBriefRow;
import com.evcs.station.dto.stats.StationNameRow;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import com.evcs.station.mapper.ChargerMapper;
import com.evcs.station.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 站点/充电桩统计与批量查询服务（供内部 API 使用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationStatsService {

    private final StationMapper stationMapper;
    private final ChargerMapper chargerMapper;

    /**
     * 按主键查询站点简要信息（跨服务写单反范式化用，唯一键查询免租户上下文）。
     */
    public StationBriefRow getStationBrief(Long stationId) {
        try {
            CustomTenantLineHandler.disableTenantFilter();
            Station station = stationMapper.selectById(stationId);
            if (station == null) {
                return null;
            }
            return new StationBriefRow(station.getStationId(), station.getStationName(),
                    station.getProvince(), station.getCity());
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }

    public List<StationNameRow> getStationNames(List<Long> tenantIds) {
        try {
            CustomTenantLineHandler.disableTenantFilter();
            QueryWrapper<Station> query = new QueryWrapper<Station>()
                    .select("station_id", "station_name")
                    .in("tenant_id", tenantIds)
                    .eq("deleted", 0);
            List<StationNameRow> rows = new ArrayList<>();
            for (Map<String, Object> row : stationMapper.selectMaps(query)) {
                Long id = readLong(row, "station_id");
                if (id != null) {
                    rows.add(new StationNameRow(id, readString(row, "station_name")));
                }
            }
            return rows;
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }

    public List<ChargerCodeRow> getChargerCodes(List<Long> tenantIds) {
        try {
            CustomTenantLineHandler.disableTenantFilter();
            QueryWrapper<Charger> query = new QueryWrapper<Charger>()
                    .select("charger_id", "charger_code")
                    .in("tenant_id", tenantIds)
                    .eq("deleted", 0);
            List<ChargerCodeRow> rows = new ArrayList<>();
            for (Map<String, Object> row : chargerMapper.selectMaps(query)) {
                Long id = readLong(row, "charger_id");
                if (id != null) {
                    rows.add(new ChargerCodeRow(id, readString(row, "charger_code")));
                }
            }
            return rows;
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }

    public ChargerStatusStats getChargerStatusStats(List<Long> tenantIds) {
        try {
            CustomTenantLineHandler.disableTenantFilter();
            QueryWrapper<Charger> query = new QueryWrapper<Charger>()
                    .select(
                            "COALESCE(SUM(CASE WHEN status <> 0 THEN 1 ELSE 0 END), 0) AS online",
                            "COALESCE(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS offline",
                            "COALESCE(SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END), 0) AS charging",
                            "COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS idle")
                    .in("tenant_id", tenantIds)
                    .eq("deleted", 0);
            List<Map<String, Object>> rows = chargerMapper.selectMaps(query);
            Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
            return new ChargerStatusStats(
                    readLong(row, "online"),
                    readLong(row, "offline"),
                    readLong(row, "charging"),
                    readLong(row, "idle"));
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }

    private Long readLong(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key) && entry.getValue() != null) {
                if (entry.getValue() instanceof Number number) {
                    return number.longValue();
                }
                try {
                    return Long.parseLong(String.valueOf(entry.getValue()));
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private String readString(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue() == null ? null : String.valueOf(entry.getValue());
            }
        }
        return null;
    }
}
