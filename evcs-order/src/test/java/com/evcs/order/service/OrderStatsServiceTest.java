package com.evcs.order.service;

import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.common.tenant.TenantContext;
import com.evcs.order.dto.stats.ChargerActiveDays;
import com.evcs.order.dto.stats.DailySummary;
import com.evcs.order.dto.stats.HourlyCount;
import com.evcs.order.dto.stats.RecentOrderRow;
import com.evcs.order.dto.stats.StationOrderCount;
import com.evcs.order.dto.stats.TrendPoint;
import com.evcs.order.entity.ChargingOrder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单统计服务测试（tenant 仪表盘的数据来源，跨服务 SQL 拆解后的归属端）。
 *
 * <p>语义对齐原 DashboardMapper：今日订单数不过滤状态；充电量/收入只统计
 * status IN (1,11)；趋势按天聚合；利用率按"有订单的天数/30天"计算。
 */
@SpringBootTest(classes = {com.evcs.order.OrderServiceApplication.class,
        com.evcs.order.config.TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("订单统计服务")
class OrderStatsServiceTest extends BaseServiceTest {

    @Resource
    private OrderStatsService orderStatsService;

    @Resource
    private IChargingOrderService orderService;

    private LocalDate today = LocalDate.now();

    private void assertDecimalEquals(String expected, BigDecimal actual) {
        assertTrue(new BigDecimal(expected).compareTo(actual) == 0,
                "expected " + expected + " but was " + actual.toPlainString());
    }

    @AfterEach
    void cleanupContext() {
        TenantContext.clear();
    }

    private void saveOrder(Long tenantId, String sessionId, Long stationId, Long chargerId,
                           Long userId, int status, LocalDateTime startTime) {
        TenantContext.setCurrentTenantId(tenantId);
        ChargingOrder order = new ChargingOrder();
        order.setSessionId(sessionId);
        order.setStationId(stationId);
        order.setChargerId(chargerId);
        order.setUserId(userId);
        order.setStatus(status);
        order.setStartTime(startTime);
        order.setEnergy(10.50);
        order.setAmount(new BigDecimal("20.00"));
        order.setCreateTime(startTime);
        orderService.save(order);
    }

    @Test
    @DisplayName("当日汇总 - 订单数不含状态过滤，电量/收入只统计已完成或已支付")
    void dailySummaryShouldFilterStatusForEnergyAndRevenue() {
        saveOrder(1L, "STAT-A", 11L, 21L, 31L, 0, today.atTime(9, 0));   // 进行中：只计订单数
        saveOrder(1L, "STAT-B", 11L, 21L, 31L, 1, today.atTime(10, 0));  // 已完成
        saveOrder(1L, "STAT-C", 11L, 21L, 31L, 11, today.atTime(11, 0)); // 已支付

        DailySummary summary = orderStatsService.getDailySummary(today, List.of(1L));

        assertEquals(3L, summary.orderCount());
        assertDecimalEquals("21.00", summary.energy());
        assertDecimalEquals("40.00", summary.revenue());
    }

    @Test
    @DisplayName("当日汇总 - 只统计指定租户集合")
    void dailySummaryShouldFilterByTenantIds() {
        saveOrder(1L, "STAT-T1", 11L, 21L, 31L, 1, today.atTime(9, 0));
        saveOrder(2L, "STAT-T2", 12L, 22L, 32L, 1, today.atTime(9, 0));

        DailySummary summary = orderStatsService.getDailySummary(today, List.of(1L));

        assertEquals(1L, summary.orderCount());
    }

    @Test
    @DisplayName("趋势 - 按天聚合电量与收入，只返回有订单的日期")
    void trendsShouldGroupByDay() {
        saveOrder(1L, "TREND-A", 11L, 21L, 31L, 1, today.minusDays(1).atTime(8, 0));
        saveOrder(1L, "TREND-B", 11L, 21L, 31L, 1, today.minusDays(1).atTime(9, 0));
        saveOrder(1L, "TREND-C", 11L, 21L, 31L, 11, today.atTime(8, 0));

        List<TrendPoint> points = orderStatsService.getDailyTrends(today.minusDays(2), List.of(1L));

        assertEquals(2, points.size());
        TrendPoint yesterday = points.get(0);
        assertEquals(today.minusDays(1), yesterday.date());
        assertDecimalEquals("21.00", yesterday.energy());
        assertDecimalEquals("40.00", yesterday.revenue());
        TrendPoint todayPoint = points.get(1);
        assertEquals(today, todayPoint.date());
    }

    @Test
    @DisplayName("小时分布 - 应按小时聚合并支持站点过滤")
    void hourlyHistogramShouldGroupByHourAndFilterStation() {
        saveOrder(1L, "HIST-A", 11L, 21L, 31L, 1, today.atTime(9, 15));
        saveOrder(1L, "HIST-B", 11L, 21L, 31L, 1, today.atTime(9, 45));
        saveOrder(1L, "HIST-C", 12L, 22L, 31L, 1, today.atTime(9, 30));

        List<HourlyCount> histogram = orderStatsService.getHourlyHistogram(today, List.of(1L), 11L);

        assertEquals(1, histogram.size());
        assertEquals(9, histogram.get(0).hour().intValue());
        assertEquals(2L, histogram.get(0).count());
    }

    @Test
    @DisplayName("按站订单计数 - 应返回各站点订单数")
    void stationOrderCountsShouldGroupByStation() {
        saveOrder(1L, "SOC-A", 11L, 21L, 31L, 1, today.atTime(9, 0));
        saveOrder(1L, "SOC-B", 11L, 21L, 31L, 1, today.minusDays(1).atTime(9, 0));
        saveOrder(1L, "SOC-C", 12L, 22L, 31L, 1, today.atTime(9, 0));

        List<StationOrderCount> counts = orderStatsService.getStationOrderCounts(List.of(1L));

        assertEquals(2, counts.size());
        StationOrderCount station11 = counts.stream().filter(c -> c.stationId() == 11L).findFirst().orElseThrow();
        assertEquals(2L, station11.orderCount());
    }

    @Test
    @DisplayName("充电桩活跃天数 - 30 天窗口内按去重日期计数")
    void chargerActiveDaysShouldCountDistinctDays() {
        saveOrder(1L, "CAD-A", 11L, 21L, 31L, 1, today.atTime(9, 0));
        saveOrder(1L, "CAD-B", 11L, 21L, 31L, 1, today.minusDays(1).atTime(9, 0));
        saveOrder(1L, "CAD-C", 11L, 21L, 31L, 1, today.minusDays(1).atTime(20, 0));

        List<ChargerActiveDays> rows = orderStatsService.getChargerActiveDays(today.minusDays(29), List.of(1L));

        assertEquals(1, rows.size());
        assertEquals(21L, rows.get(0).chargerId());
        assertEquals(2, rows.get(0).activeDays().intValue());
    }

    @Test
    @DisplayName("最近订单 - 按 createTime 倒序并携带关联ID")
    void recentOrdersShouldReturnNewestFirst() {
        saveOrder(1L, "REC-OLD", 11L, 21L, 31L, 1, today.atTime(8, 0));
        saveOrder(1L, "REC-NEW", 12L, 22L, 32L, 11, today.atTime(9, 0));

        List<RecentOrderRow> rows = orderStatsService.getRecentOrders(List.of(1L), 1);

        assertEquals(1, rows.size());
        assertEquals("REC-NEW", rows.get(0).sessionId());
        assertEquals(12L, rows.get(0).stationId());
        assertEquals(32L, rows.get(0).userId());
        assertTrue(rows.get(0).createTime() != null);
    }
}
