package com.evcs.order.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.order.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "对账与校验", description = "每日离线校验与手动触发")
@RestController
@RequestMapping("/reconcile")
@RequiredArgsConstructor
public class ReconcileController {
    private final ReconciliationService service;

    @PostMapping("/run-daily")
    @Operation(summary = "手动触发昨日订单一致性校验")
    @DataScope
    public Result<ReconciliationService.ReconcileResult> runDaily() {
        return Result.success(service.runDailyCheck());
    }
}
