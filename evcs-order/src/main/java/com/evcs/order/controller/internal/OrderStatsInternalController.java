package com.evcs.order.controller.internal;

import com.evcs.common.result.Result;
import com.evcs.order.dto.stats.ChargerActiveDays;
import com.evcs.order.dto.stats.DailySummary;
import com.evcs.order.dto.stats.HourlyCount;
import com.evcs.order.dto.stats.RecentOrderRow;
import com.evcs.order.dto.stats.StationOrderCount;
import com.evcs.order.dto.stats.TrendPoint;
import com.evcs.order.service.OrderStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单统计内部查询端点（服务间调用）。
 *
 * <p>供 tenant 仪表盘聚合使用：网关在边缘封锁 /internal/api/**，
 * 服务内由 InternalApiTokenFilter 校验共享令牌。
 */
@RestController
@RequestMapping("/internal/api/v1/stats/orders")
@RequiredArgsConstructor
public class OrderStatsInternalController {

    private final OrderStatsService orderStatsService;

    @GetMapping("/daily-summary")
    public Result<DailySummary> dailySummary(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(orderStatsService.getDailySummary(date, tenantIds));
    }

    @GetMapping("/trends")
    public Result<List<TrendPoint>> trends(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(orderStatsService.getDailyTrends(startDate, tenantIds));
    }

    @GetMapping("/hourly-histogram")
    public Result<List<HourlyCount>> hourlyHistogram(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("tenantIds") List<Long> tenantIds,
            @RequestParam(value = "stationId", required = false) Long stationId) {
        return Result.success(orderStatsService.getHourlyHistogram(date, tenantIds, stationId));
    }

    @GetMapping("/station-order-counts")
    public Result<List<StationOrderCount>> stationOrderCounts(
            @RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(orderStatsService.getStationOrderCounts(tenantIds));
    }

    @GetMapping("/charger-active-days")
    public Result<List<ChargerActiveDays>> chargerActiveDays(
            @RequestParam("since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
            @RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(orderStatsService.getChargerActiveDays(since, tenantIds));
    }

    @GetMapping("/recent")
    public Result<List<RecentOrderRow>> recent(
            @RequestParam("tenantIds") List<Long> tenantIds,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Result.success(orderStatsService.getRecentOrders(tenantIds, limit));
    }
}
