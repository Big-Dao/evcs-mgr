package com.evcs.tenant.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.tenant.dto.DashboardStatsDTO;
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
