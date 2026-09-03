package com.evcs.tenant.service.impl;

import com.evcs.common.tenant.TenantContext;
import com.evcs.tenant.client.AuthStatsClient;
import com.evcs.tenant.client.OrderStatsClient;
import com.evcs.tenant.client.StationUsageClient;
import com.evcs.tenant.dto.ChargerStatusStatsDTO;
import com.evcs.tenant.dto.ChargerUtilizationDTO;
import com.evcs.tenant.dto.DashboardStatsDTO;
import com.evcs.tenant.dto.PeriodDistributionDTO;
import com.evcs.tenant.dto.RecentOrderDTO;
import com.evcs.tenant.dto.StationRankingDTO;
import com.evcs.tenant.dto.TrendPointDTO;
import com.evcs.tenant.dto.stats.ChargerStatusStatsRow;
import com.evcs.tenant.dto.stats.RecentOrderRow;
import com.evcs.tenant.dto.stats.StationOrderCount;
import com.evcs.tenant.dto.stats.OrderDailySummary;
import com.evcs.tenant.dto.stats.OrderHourlyCount;
import com.evcs.tenant.dto.stats.OrderTrendPoint;
import com.evcs.tenant.mapper.SysTenantMapper;
import com.evcs.tenant.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Dashboard统计服务实现。
 *
 * <p>统计数据的归属服务为 order/station/auth，本服务只做租户范围解析
 * （自身 + 全部后代租户）、跨服务聚合与展示层装配；不再跨服务直查数据库。
 * 任一外部查询失败时按原行为降级为默认值/空集合，避免前端报错。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private static final DateTimeFormatter CREATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int RANKING_LIMIT = 5;
    private static final int UTILIZATION_WINDOW_DAYS = 30;

    private final SysTenantMapper sysTenantMapper;
    private final StationUsageClient stationUsageClient;
    private final OrderStatsClient orderStatsClient;
    private final AuthStatsClient authStatsClient;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的Dashboard统计数据", tenantId);
        try {
            List<Long> tenantIds = tenantScope(tenantId);

            Long tenantCount = sysTenantMapper.countTenants(tenantId);
            long userCount = authStatsClient.countActiveUsers(tenantIds);

            long stationCount = 0;
            long chargerCount = 0;
            for (var usage : stationUsageClient.getUsageCounts(tenantIds).values()) {
                stationCount += usage.stationCount();
                chargerCount += usage.chargerCount();
            }

            OrderDailySummary summary = orderStatsClient.getDailySummary(LocalDate.now(), tenantIds);

            return DashboardStatsDTO.builder()
                    .tenantCount(tenantCount != null ? tenantCount : 0L)
                    .userCount(userCount)
                    .stationCount(stationCount)
                    .chargerCount(chargerCount)
                    .todayOrderCount(summary.orderCount() == null ? 0L : summary.orderCount())
                    .todayChargingAmount(summary.energy() == null ? BigDecimal.ZERO : summary.energy())
                    .todayRevenue(summary.revenue() == null ? BigDecimal.ZERO : summary.revenue())
                    .build();
        } catch (Exception e) {
            log.error("查询Dashboard统计数据失败", e);
            return DashboardStatsDTO.builder()
                    .tenantCount(0L)
                    .userCount(0L)
                    .stationCount(0L)
                    .chargerCount(0L)
                    .todayOrderCount(0L)
                    .todayChargingAmount(BigDecimal.ZERO)
                    .todayRevenue(BigDecimal.ZERO)
                    .build();
        }
    }

    @Override
    public List<RecentOrderDTO> getRecentOrders(Integer limit) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的最近 {} 条订单", tenantId, limit);
        try {
            List<Long> tenantIds = tenantScope(tenantId);
            List<RecentOrderRow> rows = orderStatsClient.getRecentOrders(tenantIds, limit);

            List<Long> stationIds = rows.stream().map(RecentOrderRow::stationId).filter(id -> id != null).distinct().toList();
            List<Long> chargerIds = rows.stream().map(RecentOrderRow::chargerId).filter(id -> id != null).distinct().toList();
            List<Long> userIds = rows.stream().map(RecentOrderRow::userId).filter(id -> id != null).distinct().toList();

            Map<Long, String> stationNames = new HashMap<>();
            stationUsageClient.getStationNames(tenantIds)
                    .forEach(s -> stationNames.put(s.id(), s.stationName()));
            Map<Long, String> chargerCodes = new HashMap<>();
            stationUsageClient.getChargerCodes(tenantIds)
                    .forEach(c -> chargerCodes.put(c.id(), c.chargerCode()));
            Map<Long, String> userNames = new HashMap<>();
            authStatsClient.getUsernames(userIds)
                    .forEach(u -> userNames.put(u.userId(), u.username()));

            List<RecentOrderDTO> result = new ArrayList<>();
            for (RecentOrderRow row : rows) {
                result.add(RecentOrderDTO.builder()
                        .orderId(toStringOrEmpty(row.sessionId()))
                        .stationName(stationNames.getOrDefault(row.stationId(), ""))
                        .chargerCode(chargerCodes.getOrDefault(row.chargerId(), ""))
                        .userName(userNames.getOrDefault(row.userId(), ""))
                        .amount(toBigDecimal(row.amount()))
                        .status(convertOrderStatus(String.valueOf(row.status())))
                        .createTime(formatCreateTime(row.createTime(), CREATE_TIME_FORMATTER))
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.error("查询最近订单失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<StationRankingDTO> getStationRanking() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的充电站排名（Top {}）", tenantId, RANKING_LIMIT);
        try {
            List<Long> tenantIds = tenantScope(tenantId);
            Map<Long, Long> orderCounts = new HashMap<>();
            for (StationOrderCount count : orderStatsClient.getStationOrderCounts(tenantIds)) {
                orderCounts.put(count.stationId(), count.orderCount() == null ? 0L : count.orderCount());
            }

            List<StationRankingDTO> merged = new ArrayList<>();
            for (var station : stationUsageClient.getStationNames(tenantIds)) {
                StationRankingDTO dto = new StationRankingDTO();
                dto.setId(station.id());
                dto.setName(station.stationName() == null ? "" : station.stationName());
                long orders = orderCounts.getOrDefault(station.id(), 0L);
                dto.setOrders((int) orders);
                merged.add(dto);
            }

            merged.sort(Comparator.comparingInt(StationRankingDTO::getOrders).reversed());
            List<StationRankingDTO> top = merged.subList(0, Math.min(RANKING_LIMIT, merged.size()));

            int maxOrders = top.isEmpty() ? 0 : top.get(0).getOrders();
            for (StationRankingDTO dto : top) {
                dto.setPercentage(maxOrders > 0 ? dto.getOrders() * 100 / maxOrders : 0);
            }
            return new ArrayList<>(top);
        } catch (Exception e) {
            log.error("查询充电站排名失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<ChargerUtilizationDTO> getChargerUtilization() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的充电桩利用率（Top {}）", tenantId, RANKING_LIMIT);
        try {
            List<Long> tenantIds = tenantScope(tenantId);
            LocalDate since = LocalDate.now().minusDays(UTILIZATION_WINDOW_DAYS - 1L);
            Map<Long, Integer> activeDays = new HashMap<>();
            for (var row : orderStatsClient.getChargerActiveDays(since, tenantIds)) {
                activeDays.put(row.chargerId(), row.activeDays() == null ? 0 : row.activeDays());
            }

            List<ChargerUtilizationDTO> merged = new ArrayList<>();
            for (var charger : stationUsageClient.getChargerCodes(tenantIds)) {
                ChargerUtilizationDTO dto = new ChargerUtilizationDTO();
                dto.setId(charger.id());
                dto.setCode(charger.chargerCode() == null ? "" : charger.chargerCode());
                int days = activeDays.getOrDefault(charger.id(), 0);
                dto.setUtilization((int) (days * 100L / UTILIZATION_WINDOW_DAYS));
                merged.add(dto);
            }

            merged.sort(Comparator.comparingInt(ChargerUtilizationDTO::getUtilization).reversed());
            return new ArrayList<>(merged.subList(0, Math.min(RANKING_LIMIT, merged.size())));
        } catch (Exception e) {
            log.error("查询充电桩利用率失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public ChargerStatusStatsDTO getChargerStatusStats() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的充电桩状态统计", tenantId);
        try {
            List<Long> tenantIds = tenantScope(tenantId);
            ChargerStatusStatsRow stats = stationUsageClient.getChargerStatusStats(tenantIds);
            return ChargerStatusStatsDTO.builder()
                    .online(nvl(stats.online()))
                    .offline(nvl(stats.offline()))
                    .charging(nvl(stats.charging()))
                    .idle(nvl(stats.idle()))
                    .build();
        } catch (Exception e) {
            log.error("查询充电桩状态统计失败", e);
            return ChargerStatusStatsDTO.builder()
                    .online(0L)
                    .offline(0L)
                    .charging(0L)
                    .idle(0L)
                    .build();
        }
    }

    @Override
    public List<TrendPointDTO> getChargingTrend(Integer days) {
        return getTrend(days, OrderTrendPoint::energy, "充电量");
    }

    @Override
    public List<TrendPointDTO> getRevenueTrend(Integer days) {
        return getTrend(days, OrderTrendPoint::revenue, "收入");
    }

    /**
     * 趋势通用装配：order 服务只返回有订单的日期，此处补齐全系列零值，
     * 与原 generate_series LEFT JOIN 行为一致。
     */
    private List<TrendPointDTO> getTrend(Integer days,
                                         Function<OrderTrendPoint, BigDecimal> metric,
                                         String label) {
        int effectiveDays = days == null || days <= 0 ? 7 : days;
        Long tenantId = TenantContext.getCurrentTenantId();
        try {
            List<Long> tenantIds = tenantScope(tenantId);
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(effectiveDays - 1L);

            Map<LocalDate, BigDecimal> byDate = new HashMap<>();
            for (OrderTrendPoint point : orderStatsClient.getDailyTrends(startDate, tenantIds)) {
                byDate.put(point.date(), metric.apply(point) == null ? BigDecimal.ZERO : metric.apply(point));
            }

            List<TrendPointDTO> list = new ArrayList<>();
            for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
                list.add(TrendPointDTO.builder()
                        .date(d.toString())
                        .value(byDate.getOrDefault(d, BigDecimal.ZERO))
                        .build());
            }
            return list;
        } catch (Exception e) {
            log.error("获取{}趋势失败", label, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<PeriodDistributionDTO> getOrderPeriodDistribution(java.time.LocalDate date, Integer granularity, Long stationId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        try {
            if (date == null) {
                date = LocalDate.now();
            }
            int step = granularity != null && granularity > 0 ? granularity : 3;
            List<Long> tenantIds = tenantScope(tenantId);

            Map<Integer, Long> byHour = new HashMap<>();
            for (OrderHourlyCount h : orderStatsClient.getHourlyHistogram(date, tenantIds, stationId)) {
                if (h.hour() != null) {
                    byHour.put(h.hour(), h.count() == null ? 0L : h.count());
                }
            }

            List<PeriodDistributionDTO> list = new ArrayList<>();
            for (int h = 0; h < 24; h += step) {
                int end = Math.min(h + step, 24);
                long count = 0;
                for (int i = h; i < end; i++) {
                    count += byHour.getOrDefault(i, 0L);
                }
                list.add(PeriodDistributionDTO.builder()
                        .slot(h + "-" + end + "时")
                        .count(count)
                        .build());
            }
            return list;
        } catch (Exception e) {
            log.error("获取订单时段分布失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 租户可见范围：自身 + 全部后代租户（等价于原 sys_tenant ancestors 子查询集合）。
     */
    private List<Long> tenantScope(Long tenantId) {
        List<Long> tenantIds = new ArrayList<>();
        tenantIds.add(tenantId);
        tenantIds.addAll(sysTenantMapper.selectDescendantIds(tenantId));
        return tenantIds;
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 转换订单状态为中文
     */
    private String convertOrderStatus(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "CREATED" -> "已创建";
            case "CHARGING" -> "充电中";
            case "COMPLETED" -> "已完成";
            case "PAID" -> "已支付";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    static String toStringOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    static String formatCreateTime(Object createTime, DateTimeFormatter formatter) {
        if (createTime == null) {
            return "";
        }
        if (createTime instanceof LocalDateTime localDateTime) {
            return localDateTime.format(formatter);
        }
        if (createTime instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(formatter);
        }
        if (createTime instanceof java.util.Date date) {
            return java.time.Instant.ofEpochMilli(date.getTime())
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(formatter);
        }
        // Fallback: best-effort string
        return String.valueOf(createTime);
    }
}
