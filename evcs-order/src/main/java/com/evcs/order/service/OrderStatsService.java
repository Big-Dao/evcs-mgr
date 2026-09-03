package com.evcs.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.order.dto.stats.ChargerActiveDays;
import com.evcs.order.dto.stats.DailySummary;
import com.evcs.order.dto.stats.HourlyCount;
import com.evcs.order.dto.stats.RecentOrderRow;
import com.evcs.order.dto.stats.StationOrderCount;
import com.evcs.order.dto.stats.TrendPoint;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.mapper.ChargingOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单统计服务（tenant 仪表盘数据来源，跨服务 SQL 拆解后的归属端）。
 *
 * <p>语义对齐原 tenant DashboardMapper：订单数不过滤状态；电量/收入只统计
 * status IN (1, 11)；按调用方给定的租户ID集合显式统计（受控禁用租户过滤）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatsService {

    private static final String COMPLETED_OR_PAID = "status IN (1, 11)";

    private final ChargingOrderMapper chargingOrderMapper;

    public DailySummary getDailySummary(LocalDate date, List<Long> tenantIds) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        QueryWrapper<ChargingOrder> query = baseQuery(tenantIds)
                .select(
                        "COUNT(*) AS order_count",
                        "COALESCE(SUM(CASE WHEN " + COMPLETED_OR_PAID + " THEN energy ELSE 0 END), 0) AS energy",
                        "COALESCE(SUM(CASE WHEN " + COMPLETED_OR_PAID + " THEN amount ELSE 0 END), 0) AS revenue")
                .ge("start_time", start)
                .lt("start_time", end);

        List<Map<String, Object>> rows = selectMaps(query);
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        return new DailySummary(
                readLong(row, "order_count"),
                readDecimal(row, "energy"),
                readDecimal(row, "revenue"));
    }

    public List<TrendPoint> getDailyTrends(LocalDate startDate, List<Long> tenantIds) {
        QueryWrapper<ChargingOrder> query = baseQuery(tenantIds)
                .select(
                        "CAST(start_time AS DATE) AS d",
                        "COALESCE(SUM(energy), 0) AS energy",
                        "COALESCE(SUM(CASE WHEN " + COMPLETED_OR_PAID + " THEN amount ELSE 0 END), 0) AS revenue")
                .ge("start_time", startDate.atStartOfDay())
                .groupBy("CAST(start_time AS DATE)")
                .orderByAsc("CAST(start_time AS DATE)");

        List<TrendPoint> points = new ArrayList<>();
        for (Map<String, Object> row : selectMaps(query)) {
            Object d = readValue(row, "d");
            LocalDate date = d == null ? null : LocalDate.parse(String.valueOf(d));
            if (date != null) {
                points.add(new TrendPoint(date, readDecimal(row, "energy"), readDecimal(row, "revenue")));
            }
        }
        return points;
    }

    public List<HourlyCount> getHourlyHistogram(LocalDate date, List<Long> tenantIds, Long stationId) {
        QueryWrapper<ChargingOrder> query = baseQuery(tenantIds)
                .select("EXTRACT(HOUR FROM start_time) AS h", "COUNT(*) AS cnt")
                .ge("start_time", date.atStartOfDay())
                .lt("start_time", date.plusDays(1).atStartOfDay())
                .groupBy("EXTRACT(HOUR FROM start_time)")
                .orderByAsc("EXTRACT(HOUR FROM start_time)");
        if (stationId != null) {
            query.eq("station_id", stationId);
        }

        List<HourlyCount> histogram = new ArrayList<>();
        for (Map<String, Object> row : selectMaps(query)) {
            Integer hour = readLong(row, "h") == null ? null : readLong(row, "h").intValue();
            if (hour != null) {
                histogram.add(new HourlyCount(hour, readLong(row, "cnt")));
            }
        }
        return histogram;
    }

    public List<StationOrderCount> getStationOrderCounts(List<Long> tenantIds) {
        QueryWrapper<ChargingOrder> query = baseQuery(tenantIds)
                .select("station_id", "COUNT(*) AS cnt")
                .groupBy("station_id");

        List<StationOrderCount> counts = new ArrayList<>();
        for (Map<String, Object> row : selectMaps(query)) {
            Long stationId = readLong(row, "station_id");
            if (stationId != null) {
                counts.add(new StationOrderCount(stationId, readLong(row, "cnt")));
            }
        }
        return counts;
    }

    public List<ChargerActiveDays> getChargerActiveDays(LocalDate sinceDate, List<Long> tenantIds) {
        QueryWrapper<ChargingOrder> query = baseQuery(tenantIds)
                .select("charger_id", "COUNT(DISTINCT CAST(start_time AS DATE)) AS active_days")
                .ge("start_time", sinceDate.atStartOfDay())
                .groupBy("charger_id");

        List<ChargerActiveDays> rows = new ArrayList<>();
        for (Map<String, Object> row : selectMaps(query)) {
            Long chargerId = readLong(row, "charger_id");
            if (chargerId != null) {
                rows.add(new ChargerActiveDays(chargerId, readLong(row, "active_days") == null ? 0
                        : readLong(row, "active_days").intValue()));
            }
        }
        return rows;
    }

    public List<RecentOrderRow> getRecentOrders(List<Long> tenantIds, int limit) {
        QueryWrapper<ChargingOrder> query = baseQuery(tenantIds)
                .select("session_id", "station_id", "charger_id", "user_id", "amount", "status", "create_time")
                .orderByDesc("create_time")
                .last("LIMIT " + Math.max(1, limit));

        List<RecentOrderRow> rows = new ArrayList<>();
        for (Map<String, Object> row : selectMaps(query)) {
            rows.add(new RecentOrderRow(
                    readString(row, "session_id"),
                    readLong(row, "station_id"),
                    readLong(row, "charger_id"),
                    readLong(row, "user_id"),
                    readDecimal(row, "amount"),
                    readLong(row, "status") == null ? null : readLong(row, "status").intValue(),
                    readDateTime(row, "create_time")));
        }
        return rows;
    }

    private QueryWrapper<ChargingOrder> baseQuery(List<Long> tenantIds) {
        return new QueryWrapper<ChargingOrder>()
                .in("tenant_id", tenantIds)
                .eq("deleted", 0);
    }

    private List<Map<String, Object>> selectMaps(QueryWrapper<ChargingOrder> query) {
        try {
            CustomTenantLineHandler.disableTenantFilter();
            return chargingOrderMapper.selectMaps(query);
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }

    private Long readLong(Map<String, Object> row, String key) {
        Object v = readValue(row, key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal readDecimal(Map<String, Object> row, String key) {
        Object v = readValue(row, key);
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String readString(Map<String, Object> row, String key) {
        Object v = readValue(row, key);
        return v == null ? null : String.valueOf(v);
    }

    private LocalDateTime readDateTime(Map<String, Object> row, String key) {
        Object v = readValue(row, key);
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDateTime dt) {
            return dt;
        }
        if (v instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return null;
    }

    private Object readValue(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
