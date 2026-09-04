package com.evcs.order.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.order.dto.BillingPlanResponse;
import com.evcs.order.dto.BillingPlanUpsertRequest;
import com.evcs.order.dto.BillingPlanSegmentResponse;
import com.evcs.order.entity.BillingPlan;
import com.evcs.order.entity.BillingPlanSegment;
import com.evcs.order.service.IBillingPlanService;
import com.evcs.order.service.IBillingPlanCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Tag(name = "计费计划", description = "分时电价/服务费计划与分段管理")
@RestController
@RequestMapping("/billing/plans")
@RequiredArgsConstructor
public class BillingPlanController {
    private final IBillingPlanService planService;
    private final IBillingPlanCacheService cacheService;

    @PostMapping
    @Operation(summary = "创建计费计划")
    @DataScope
    public Result<BillingPlanResponse> create(@Valid @RequestBody BillingPlanUpsertRequest request) {
        IBillingPlanService.PlanWriteOutcome outcome = planService.createPlan(request.toEntity());
        return outcome.success() ? Result.success(BillingPlanResponse.from(outcome.plan())) : Result.fail(outcome.error());
    }
    @GetMapping
    @Operation(summary = "查询计费计划列表")
    @DataScope
    public Result<java.util.List<BillingPlan>> list(@RequestParam(required = false) Long stationId) {
        QueryWrapper<BillingPlan> qw = new QueryWrapper<>();
        if (stationId != null) {
            qw.eq("station_id", stationId);
        }
        List<BillingPlan> list = planService.list(qw);
        planService.fillPlanStats(list);
        return Result.success(list);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询计费计划列表")
    @DataScope
    public Result<IPage<BillingPlanResponse>> page(@RequestParam(defaultValue = "1") Long current,
                                           @RequestParam(defaultValue = "10") Long size,
                                           @RequestParam(required = false) Long stationId) {
        Page<BillingPlan> page = new Page<>(current, size);
        QueryWrapper<BillingPlan> qw = new QueryWrapper<>();
                if (stationId != null) {
            qw.eq("station_id", stationId);
        }
        IPage<BillingPlan> result = planService.page(page, qw);
        planService.fillPlanStats(result.getRecords());
        return Result.success(result.convert(BillingPlanResponse::from));
    }

    @PostMapping("/{planId}/default")
    @Operation(summary = "设置为默认计划")
    @DataScope
    public Result<Boolean> setDefault(@PathVariable Long planId, @RequestParam Long stationId) {
        IBillingPlanService.PlanWriteOutcome outcome = planService.setDefaultPlan(planId, stationId);
        if (outcome.plan() == null) {
            return Result.fail(outcome.error());
        }
        return Result.success(outcome.success());
    }

    @PutMapping
    @Operation(summary = "更新计费计划")
    @DataScope
    public Result<Boolean> update(@Valid @RequestBody BillingPlanUpsertRequest request) {
        IBillingPlanService.PlanWriteOutcome outcome = planService.updatePlan(request.toEntity());
        if (!outcome.success()) {
            return Result.fail(outcome.error());
        }
        // 缓存在事务提交后失效（stationId 为定位/失效键，来自请求白名单字段）
        var saved = request.toEntity();
        if (saved.getStationId() != null) {
            cacheService.invalidate(saved.getStationId(), saved.getId());
            cacheService.invalidateDefault(saved.getStationId());
        }
        return Result.success(true);
    }

    @PostMapping("/validate-segments")
    @Operation(summary = "校验分段是否有效(不保存)")
    @DataScope
    public Result<Boolean> validateSegments(@RequestParam(defaultValue = "false") boolean requireFullDay,
                                            @RequestBody List<BillingPlanSegment> segments) {
        if (segments != null && segments.size() > 96) {
            return Result.fail("分段数量不能超过96");
        }
        return Result.success(planService.validateSegments(segments, requireFullDay));
    }

    @PostMapping("/{planId}/segments")
    @Operation(summary = "保存计划分段(覆盖式)")
    @DataScope
    public Result<Boolean> saveSegments(@PathVariable Long planId,
                                        @RequestParam(defaultValue = "false") boolean requireFullDay,
                                        @RequestBody List<BillingPlanSegment> segments) {
        if (segments != null && segments.size() > 96) {
            return Result.fail("分段数量不能超过96");
        }
        boolean ok = planService.saveSegments(planId, segments, requireFullDay);
        if (ok) {
            // 缓存失效
            cacheService.invalidateSegments(planId);
        }
        return Result.success(ok);
    }

    @GetMapping("/{planId}/segments")
    @Operation(summary = "查询计划分段")
    @DataScope
    public Result<List<BillingPlanSegmentResponse>> segments(@PathVariable Long planId) {
        return Result.success(planService.listSegments(planId).stream()
                .map(BillingPlanSegmentResponse::from).toList());
    }

    @GetMapping("/{planId}/export")
    @Operation(summary = "导出计划分段为JSON")
    @DataScope
    public Result<List<BillingPlanSegmentResponse>> exportSegments(@PathVariable Long planId) {
        return Result.success(planService.listSegments(planId).stream()
                .map(BillingPlanSegmentResponse::from).toList());
    }

    @PostMapping("/{planId}/import")
    @Operation(summary = "导入计划分段(覆盖式)")
    @DataScope
    public Result<Boolean> importSegments(@PathVariable Long planId,
                                          @RequestParam(defaultValue = "false") boolean requireFullDay,
                                          @RequestBody List<BillingPlanSegment> segments) {
                if (segments != null && segments.size() > 96) {
            return Result.fail("分段数量不能超过96");
        }
        return Result.success(planService.saveSegments(planId, segments, requireFullDay));
    }

    @PostMapping("/{planId}/refresh")
    @Operation(summary = "刷新计划分段缓存")
    @DataScope
    public Result<Boolean> refresh(@PathVariable Long planId) {
        planService.evictCache(planId);
        cacheService.invalidateSegments(planId);
        return Result.success(true);
    }

    @PostMapping("/{planId}/clone")
    @Operation(summary = "克隆计费计划")
    @DataScope
    public Result<BillingPlanResponse> clonePlan(@PathVariable Long planId, @RequestBody BillingPlanUpsertRequest payload) {
        BillingPlan np = planService.clonePlan(planId, payload.toEntity());
        return np != null ? Result.success(BillingPlanResponse.from(np)) : Result.fail("克隆失败");
    }
}
