package com.evcs.tenant.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.tenant.dto.DashboardStatsDTO;
import com.evcs.tenant.dto.ChargerStatusStatsDTO;
import com.evcs.tenant.dto.TrendPointDTO;
import com.evcs.tenant.dto.PeriodDistributionDTO;
import com.evcs.tenant.dto.RecentOrderDTO;
import com.evcs.tenant.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

/**
 * Dashboard统计控制器
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final IDashboardService dashboardService;
    
    /**
     * 获取Dashboard统计数据 (兼容前端 /statistics 路径)
     */
    @Operation(summary = "获取Dashboard统计", description = "获取租户维度的统计数据")
    @GetMapping({"/stats", "/statistics"})
    @DataScope(value = DataScope.DataScopeType.TENANT,
              description = "仅查看当前租户统计数据")
    public Result<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return Result.success(stats);
    }

    /**
     * 获取充电桩状态统计 (前端 /charger-status 兼容)
     */
    @Operation(summary = "获取充电桩状态统计", description = "获取当前租户下充电桩各状态数量")
    @GetMapping("/charger-status")
    @DataScope(value = DataScope.DataScopeType.TENANT,
              description = "仅查看当前租户充电桩")
    public Result<ChargerStatusStatsDTO> getChargerStatusStats() {
        ChargerStatusStatsDTO stats = dashboardService.getChargerStatusStats();
        return Result.success(stats);
    }

    /**
     * 获取充电量趋势（最近N天）
     */
    @Operation(summary = "获取充电量趋势", description = "最近N天充电量按天聚合")
    @GetMapping("/charging-trend")
    @DataScope(value = DataScope.DataScopeType.TENANT,
              description = "仅查看当前租户订单")
    public Result<List<TrendPointDTO>> getChargingTrend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(dashboardService.getChargingTrend(days));
    }

    /**
     * 获取收入趋势（最近N天）
     */
    @Operation(summary = "获取收入趋势", description = "最近N天收入按天聚合")
    @GetMapping("/revenue-trend")
    @DataScope(value = DataScope.DataScopeType.TENANT,
              description = "仅查看当前租户订单")
    public Result<List<TrendPointDTO>> getRevenueTrend(@RequestParam(defaultValue = "7") Integer days) {
        return Result.success(dashboardService.getRevenueTrend(days));
    }

    /**
     * 获取订单时段分布（当天 3 小时间隔）
     */
    @Operation(summary = "获取订单时段分布", description = "当天订单按3小时时段聚合")
    @GetMapping("/order-period-distribution")
    @DataScope(value = DataScope.DataScopeType.TENANT,
              description = "仅查看当前租户订单")
    public Result<List<PeriodDistributionDTO>> getOrderPeriodDistribution(
            @Parameter(description = "查询日期，格式YYYY-MM-DD，默认今天")
            @RequestParam(required = false) String date,
            @Parameter(description = "粒度（小时），默认3小时")
            @RequestParam(required = false, defaultValue = "3") Integer granularity,
            @Parameter(description = "充电站ID，可选")
            @RequestParam(required = false) Long stationId
    ) {
        java.time.LocalDate d = null;
        try {
            if (date != null && !date.isBlank()) {
                d = java.time.LocalDate.parse(date);
            }
        } catch (Exception ignore) { /* 解析失败则按默认今天 */ }
        return Result.success(dashboardService.getOrderPeriodDistribution(d, granularity, stationId));
    }
    
    /**
     * 获取最近订单列表
     */
    @Operation(summary = "获取最近订单", description = "获取最近的充电订单列表")
    @GetMapping("/recent-orders")
    @DataScope(value = DataScope.DataScopeType.TENANT,
              description = "仅查看当前租户订单")
    public Result<List<RecentOrderDTO>> getRecentOrders(
            @Parameter(description = "返回记录数量，默认10条")
            @RequestParam(defaultValue = "10") Integer limit) {
        List<RecentOrderDTO> orders = dashboardService.getRecentOrders(limit);
        return Result.success(orders);
    }
    
    /**
     * 获取充电站订单排名（Top 5）
     */
    @Operation(summary = "获取充电站排名", description = "获取充电站订单数量排名Top 5")
    @GetMapping("/station-ranking")
    @DataScope(value = DataScope.DataScopeType.TENANT,
              description = "仅查看当前租户充电站")
    public Result<List<com.evcs.tenant.dto.StationRankingDTO>> getStationRanking() {
        return Result.success(dashboardService.getStationRanking());
    }
    
    /**
     * 获取充电桩利用率排名（Top 5）
     */
    @Operation(summary = "获取充电桩利用率", description = "获取充电桩利用率排名Top 5")
    @GetMapping("/charger-utilization")
    @DataScope(value = DataScope.DataScopeType.TENANT,
              description = "仅查看当前租户充电桩")
    public Result<List<com.evcs.tenant.dto.ChargerUtilizationDTO>> getChargerUtilization() {
        return Result.success(dashboardService.getChargerUtilization());
    }
}
