package com.evcs.tenant.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.tenant.dto.DashboardStatsDTO;
import com.evcs.tenant.dto.RecentOrderDTO;
import com.evcs.tenant.service.IDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dashboard统计控制器
 */
@Tag(name = "Dashboard统计", description = "Dashboard数据统计接口")
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
}
