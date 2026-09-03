package com.evcs.tenant.service;

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
import com.evcs.tenant.dto.stats.ChargerActiveDaysRow;
import com.evcs.tenant.dto.stats.ChargerCodeRow;
import com.evcs.tenant.dto.stats.ChargerStatusStatsRow;
import com.evcs.tenant.dto.stats.OrderDailySummary;
import com.evcs.tenant.dto.stats.OrderHourlyCount;
import com.evcs.tenant.dto.stats.OrderTrendPoint;
import com.evcs.tenant.dto.stats.RecentOrderRow;
import com.evcs.tenant.dto.stats.StationNameRow;
import com.evcs.tenant.dto.stats.StationOrderCount;
import com.evcs.tenant.dto.stats.UsernameRow;
import com.evcs.tenant.mapper.SysTenantMapper;
import com.evcs.tenant.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 仪表盘聚合逻辑测试：跨服务数据 → 展示 DTO 的合并、排序、补零与降级。
 */
class DashboardServiceImplTest {

    private SysTenantMapper sysTenantMapper;
    private StationUsageClient stationUsageClient;
    private OrderStatsClient orderStatsClient;
    private AuthStatsClient authStatsClient;
    private DashboardServiceImpl service;

    private static final LocalDate TODAY = LocalDate.now();

    @BeforeEach
    void setUp() {
        sysTenantMapper = mock(SysTenantMapper.class);
        stationUsageClient = mock(StationUsageClient.class);
        orderStatsClient = mock(OrderStatsClient.class);
        authStatsClient = mock(AuthStatsClient.class);
        service = new DashboardServiceImpl(sysTenantMapper, stationUsageClient, orderStatsClient, authStatsClient);

        TenantContext.setCurrentTenantId(1L);
        when(sysTenantMapper.selectDescendantIds(1L)).thenReturn(List.of());
        when(sysTenantMapper.countTenants(1L)).thenReturn(3L);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("统计总览 - 应聚合三个服务的计数")
    void statsShouldAggregateAcrossServices() {
        when(authStatsClient.countActiveUsers(anyList())).thenReturn(7L);
        when(stationUsageClient.getUsageCounts(anyList())).thenReturn(Map.of(
                1L, new com.evcs.tenant.dto.StationUsageCount(1L, 3, 5),
                2L, new com.evcs.tenant.dto.StationUsageCount(2L, 1, 0)));
        when(orderStatsClient.getDailySummary(org.mockito.ArgumentMatchers.eq(TODAY), anyList()))
                .thenReturn(new OrderDailySummary(12L, new BigDecimal("40.5"), new BigDecimal("88")));

        DashboardStatsDTO stats = service.getDashboardStats();

        assertEquals(3L, stats.getTenantCount());
        assertEquals(7L, stats.getUserCount());
        assertEquals(4L, stats.getStationCount());
        assertEquals(5L, stats.getChargerCount());
        assertEquals(12L, stats.getTodayOrderCount());
        assertEquals(new BigDecimal("40.5"), stats.getTodayChargingAmount());
    }

    @Test
    @DisplayName("统计总览 - 任一服务失败应降级为默认值")
    void statsShouldDegradeWhenServiceFails() {
        when(authStatsClient.countActiveUsers(anyList())).thenThrow(new IllegalStateException("auth down"));

        DashboardStatsDTO stats = service.getDashboardStats();

        assertEquals(0L, stats.getUserCount());
        assertEquals(0L, stats.getTenantCount());
        assertEquals(BigDecimal.ZERO, stats.getTodayRevenue());
    }

    @Test
    @DisplayName("站点排名 - 应合并无订单站点、按订单数排序并计算百分比")
    void rankingShouldMergeAndSort() {
        when(orderStatsClient.getStationOrderCounts(anyList())).thenReturn(List.of(
                new StationOrderCount(11L, 5L),
                new StationOrderCount(12L, 2L)));
        when(stationUsageClient.getStationNames(anyList())).thenReturn(List.of(
                new StationNameRow(11L, "站A"),
                new StationNameRow(12L, "站B"),
                new StationNameRow(13L, "站C")));

        List<StationRankingDTO> ranking = service.getStationRanking();

        assertEquals(3, ranking.size());
        assertEquals("站A", ranking.get(0).getName());
        assertEquals(5, ranking.get(0).getOrders());
        assertEquals(100, ranking.get(0).getPercentage());
        assertEquals(40, ranking.get(1).getPercentage());
        assertEquals(0, ranking.get(2).getOrders());
        assertEquals(0, ranking.get(2).getPercentage());
    }

    @Test
    @DisplayName("充电桩利用率 - 应按 活跃天数/30 计算")
    void utilizationShouldBeActiveDaysOver30() {
        when(orderStatsClient.getChargerActiveDays(org.mockito.ArgumentMatchers.any(), anyList())).thenReturn(List.of(
                new ChargerActiveDaysRow(21L, 6)));
        when(stationUsageClient.getChargerCodes(anyList())).thenReturn(List.of(
                new ChargerCodeRow(21L, "CH-21"),
                new ChargerCodeRow(22L, "CH-22")));

        List<ChargerUtilizationDTO> utilization = service.getChargerUtilization();

        assertEquals(2, utilization.size());
        assertEquals(20, utilization.get(0).getUtilization());
        assertEquals(0, utilization.get(1).getUtilization());
    }

    @Test
    @DisplayName("趋势 - 应补齐无订单日期的零值")
    void trendShouldFillMissingDays() {
        when(orderStatsClient.getDailyTrends(org.mockito.ArgumentMatchers.any(), anyList())).thenReturn(List.of(
                new OrderTrendPoint(TODAY.minusDays(1), new BigDecimal("3"), BigDecimal.ZERO),
                new OrderTrendPoint(TODAY, new BigDecimal("4"), new BigDecimal("8"))));

        List<TrendPointDTO> trend = service.getChargingTrend(3);

        assertEquals(3, trend.size());
        assertEquals(TODAY.minusDays(2).toString(), trend.get(0).getDate());
        assertEquals(BigDecimal.ZERO, trend.get(0).getValue());
        assertEquals(new BigDecimal("3"), trend.get(1).getValue());
        assertEquals(new BigDecimal("4"), trend.get(2).getValue());
    }

    @Test
    @DisplayName("时段分布 - 应按粒度合并小时计数")
    void distributionShouldBucketByGranularity() {
        when(orderStatsClient.getHourlyHistogram(org.mockito.ArgumentMatchers.any(), anyList(), anyLong()))
                .thenReturn(List.of(
                        new OrderHourlyCount(9, 1L),
                        new OrderHourlyCount(10, 2L),
                        new OrderHourlyCount(11, 3L)));

        List<PeriodDistributionDTO> distribution = service.getOrderPeriodDistribution(TODAY, 3, 11L);

        assertEquals(8, distribution.size());
        assertEquals("9-12时", distribution.get(3).getSlot());
        assertEquals(6L, distribution.get(3).getCount());
        assertEquals("0-3时", distribution.get(0).getSlot());
        assertEquals(0L, distribution.get(0).getCount());
    }

    @Test
    @DisplayName("最近订单 - 应填充站点/充电桩/用户名")
    void recentOrdersShouldFillDisplayNames() {
        when(orderStatsClient.getRecentOrders(anyList(), org.mockito.ArgumentMatchers.eq(10)))
                .thenReturn(List.of(new RecentOrderRow(
                        "SESSION-1", 11L, 21L, 31L, new BigDecimal("9.9"), 1,
                        LocalDateTime.of(2026, 9, 3, 10, 0))));
        when(stationUsageClient.getStationNames(anyList())).thenReturn(List.of(new StationNameRow(11L, "站A")));
        when(stationUsageClient.getChargerCodes(anyList())).thenReturn(List.of(new ChargerCodeRow(21L, "CH-21")));
        when(authStatsClient.getUsernames(anyList())).thenReturn(List.of(new UsernameRow(31L, "user31")));

        List<RecentOrderDTO> orders = service.getRecentOrders(10);

        assertEquals(1, orders.size());
        assertEquals("SESSION-1", orders.get(0).getOrderId());
        assertEquals("站A", orders.get(0).getStationName());
        assertEquals("CH-21", orders.get(0).getChargerCode());
        assertEquals("user31", orders.get(0).getUserName());
        assertEquals("2026-09-03 10:00:00", orders.get(0).getCreateTime());
    }

    @Test
    @DisplayName("充电桩状态统计 - 应透传聚合结果并在失败时降级为零")
    void chargerStatusShouldPassThroughAndDegrade() {
        when(stationUsageClient.getChargerStatusStats(anyList()))
                .thenReturn(new ChargerStatusStatsRow(2L, 1L, 1L, 1L));

        ChargerStatusStatsDTO stats = service.getChargerStatusStats();
        assertEquals(2L, stats.getOnline());

        when(stationUsageClient.getChargerStatusStats(anyList()))
                .thenThrow(new IllegalStateException("station down"));
        ChargerStatusStatsDTO degraded = service.getChargerStatusStats();
        assertEquals(0L, degraded.getOnline());
        assertTrue(degraded.getIdle() == 0L);
    }
}
