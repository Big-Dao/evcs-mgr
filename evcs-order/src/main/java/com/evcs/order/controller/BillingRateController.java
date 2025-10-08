package com.evcs.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.order.entity.BillingRate;
import com.evcs.order.service.IBillingRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费策略", description = "按租户/站配置计费策略")
@RestController
@RequestMapping("/billing/rates")
@RequiredArgsConstructor
public class BillingRateController {
    private final IBillingRateService rateService;

    @PostMapping
    @Operation(summary = "新增计费策略")
    @DataScope
    public Result<BillingRate> create(@Valid @RequestBody BillingRate rate) {
        rateService.save(rate);
        return Result.success(rate);
    }

    @PutMapping
    @Operation(summary = "更新计费策略")
    @DataScope
    public Result<Boolean> update(@Valid @RequestBody BillingRate rate) {
        return Result.success(rateService.updateById(rate));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    @DataScope
    public Result<IPage<BillingRate>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Long stationId) {
        Page<BillingRate> p = new Page<>(page, size);
        var qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<BillingRate>();
        if (stationId != null) qw.eq("station_id", stationId);
        return Result.success(rateService.page(p, qw));
    }
}
