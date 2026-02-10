package com.evcs.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.tenant.entity.TenantAuditLog;
import com.evcs.tenant.service.ITenantAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户审计日志控制器
 */
@Tag(name = "租户审计日志", description = "跨层级管理行为审计日志查询")
@RestController
@RequestMapping("/tenant/audit-log")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
public class TenantAuditLogController {

    private final ITenantAuditLogService auditLogService;

    @Operation(summary = "分页查询审计日志", description = "支持按租户、操作类型、时间范围筛选")
    @GetMapping("/page")
    @PreAuthorize("hasPermission(null, 'audit:query')")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能查看本租户及下级租户的审计日志")
    public Result<IPage<TenantAuditLog>> getAuditLogPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Long size,
            @Parameter(description = "目标租户ID") @RequestParam(required = false) Long targetTenantId,
            @Parameter(description = "操作类型") @RequestParam(required = false) String action,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        long total = auditLogService.countAuditLog(targetTenantId, action, startTime, endTime);
        List<TenantAuditLog> records = (total > 0)
                ? auditLogService.queryAuditLogPage(targetTenantId, action, startTime, endTime,
                        (int) ((current - 1) * size), size.intValue())
                : List.of();

        Page<TenantAuditLog> page = new Page<>(current, size, total);
        page.setRecords(records);
        return Result.success(page);
    }

    @Operation(summary = "获取操作类型列表", description = "获取所有审计操作类型枚举")
    @GetMapping("/actions")
    public Result<Map<String, String>> getActionTypes() {
        Map<String, String> actions = new HashMap<>();
        actions.put("CREATE_TENANT", "创建租户");
        actions.put("DELETE_TENANT", "删除租户");
        actions.put("UPDATE_TENANT", "更新租户");
        actions.put("UPDATE_QUOTA", "更新配额");
        actions.put("UPDATE_STATUS", "更新状态");
        actions.put("RESET_PASSWORD", "重置密码");
        actions.put("DISABLE_RECURSIVE", "递归禁用");
        actions.put("CROSS_LAYER_READ", "跨层级只读");
        actions.put("CROSS_LAYER_WRITE", "跨层级写入");
        actions.put("MOVE_TENANT", "移动租户");
        return Result.success(actions);
    }

    @Operation(summary = "跨层级操作统计", description = "统计最近一段时间内的跨层级操作情况")
    @GetMapping("/statistics/cross-layer")
    @PreAuthorize("hasPermission(null, 'audit:statistics')")
    public Result<List<Map<String, Object>>> statisticsCrossLayer(
            @Parameter(description = "开始时间，默认7天前") @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime) {
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        return Result.success(auditLogService.statisticsCrossLayer(startTime));
    }
}
