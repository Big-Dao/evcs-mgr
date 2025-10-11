package com.evcs.payment.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.payment.dto.ReconciliationRequest;
import com.evcs.payment.dto.ReconciliationResult;
import com.evcs.payment.service.IReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 对账控制器
 */
@Tag(name = "对账管理", description = "支付对账功能")
@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final IReconciliationService reconciliationService;

    @PostMapping("/execute")
    @Operation(summary = "执行对账")
    @DataScope
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
