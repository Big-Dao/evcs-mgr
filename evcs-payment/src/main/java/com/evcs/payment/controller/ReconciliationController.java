package com.evcs.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.payment.dto.ReconciliationQuery;
import com.evcs.payment.dto.ReconciliationRequest;
import com.evcs.payment.dto.ReconciliationResult;
import com.evcs.payment.entity.ReconciliationTask;
import com.evcs.payment.service.IReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 对账控制器
 */
@Tag(name = "对账管理", description = "支付对账功能")
@RestController
@RequestMapping("/reconciliation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'TENANT_ADMIN')")
public class ReconciliationController {

    private final IReconciliationService reconciliationService;

    @GetMapping("/tasks")
    @Operation(summary = "分页查询对账任务")
    @PreAuthorize("hasPermission(null, 'reconciliation:query')")
    public Result<Page<ReconciliationTask>> getTaskList(ReconciliationQuery query) {
        return Result.success(reconciliationService.getTaskList(query));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "获取对账任务详情")
    public Result<ReconciliationTask> getTaskDetail(@PathVariable Long id) {
        return Result.success(reconciliationService.getTaskDetail(id));
    }

    @GetMapping("/report/{taskNo}")
    @Operation(summary = "获取对账报告")
    public Result<Object> getReport(@PathVariable String taskNo) {
        return Result.success(reconciliationService.getReport(taskNo));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行对账")
    @DataScope
    @PreAuthorize("hasPermission(null, 'reconciliation:execute')")
    public Result<ReconciliationResult> executeReconciliation(@RequestBody ReconciliationRequest request) {
        ReconciliationResult result = reconciliationService.reconcile(request);
        return Result.success(result);
    }

    @PostMapping("/daily/{channel}")
    @Operation(summary = "每日自动对账")
    @DataScope
    public Result<ReconciliationResult> dailyReconciliation(@PathVariable String channel) {
        ReconciliationResult result = reconciliationService.dailyReconciliation(channel);
        return Result.success(result);
    }
}
