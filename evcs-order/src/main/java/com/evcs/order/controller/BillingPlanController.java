package com.evcs.order.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.order.entity.BillingPlan;
import com.evcs.order.entity.BillingPlanSegment;
import com.evcs.order.service.IBillingPlanService;
import com.evcs.order.service.IBillingPlanCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public Result<BillingPlan> create(@Valid @RequestBody BillingPlan plan) {
        if (plan.getStatus() != null && plan.getStatus() == 1 && plan.getStationId() != null) {
            long cnt = planService.count(new QueryWrapper<BillingPlan>()
                    .eq("tenant_id", plan.getTenantId())
                    .eq("station_id", plan.getStationId())
                    .eq("status", 1));
            if (cnt >= 16) {
                return Result.fail("每站点启用的计费计划不能超过16个");
            }
        }
        boolean ok = planService.save(plan);
        if (ok && plan.getIsDefault() != null && plan.getIsDefault() == 1 && plan.getStatus() != null && plan.getStatus() == 1) {
            // 置为默认时，取消同站点其他默认
            BillingPlan reset = new BillingPlan();
            reset.setIsDefault(0);
            planService.update(reset, new QueryWrapper<BillingPlan>()
                    .eq("tenant_id", plan.getTenantId())
                    .eq("station_id", plan.getStationId())
                    .eq("status", 1)
                    .ne("id", plan.getId()));
        }
        return ok ? Result.success(plan) : Result.fail("保存失败");

    }
    @GetMapping
    @Operation(summary = "查询计费计划列表")
    @DataScope
    public Result<java.util.List<BillingPlan>> list(@RequestParam(required = false) Long stationId) {
        QueryWrapper<BillingPlan> qw = new QueryWrapper<>();
        if (stationId != null) {
            qw.eq("station_id", stationId);
        }
        return Result.success(planService.list(qw));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询计费计划列表")
    @DataScope
    public Result<IPage<BillingPlan>> page(@RequestParam(defaultValue = "1") Long current,
                                           @RequestParam(defaultValue = "10") Long size,
                                           @RequestParam(required = false) Long stationId) {
        Page<BillingPlan> page = new Page<>(current, size);
        QueryWrapper<BillingPlan> qw = new QueryWrapper<>();
        if (stationId != null) qw.eq("station_id", stationId);
        IPage<BillingPlan> result = planService.page(page, qw);
        return Result.success(result);
    }

    @PostMapping("/{planId}/default")
    @Operation(summary = "设置为默认计划")
    @DataScope
    public Result<Boolean> setDefault(@PathVariable Long planId, @RequestParam Long stationId) {
        BillingPlan plan = planService.getById(planId);
        if (plan == null) return Result.fail("计划不存在");
        BillingPlan patch = new BillingPlan();
        patch.setId(planId);
        patch.setIsDefault(1);
        boolean ok = planService.updateById(patch);
        if (ok) {
            BillingPlan reset = new BillingPlan();
            reset.setIsDefault(0);
            planService.update(reset, new QueryWrapper<BillingPlan>()
                    .eq("tenant_id", plan.getTenantId())
                    .eq("station_id", stationId)
                    .eq("status", 1)
                    .ne("id", planId));
        }
        return Result.success(ok);
    }

    @PutMapping
    @Operation(summary = "更新计费计划")
    @DataScope
    public Result<Boolean> update(@Valid @RequestBody BillingPlan plan) {
        if (plan.getStatus() != null && plan.getStatus() == 1 && plan.getStationId() != null) {
            long cnt = planService.count(new QueryWrapper<BillingPlan>()
                    .eq("tenant_id", plan.getTenantId())
                    .eq("station_id", plan.getStationId())
                    .eq("status", 1)
                    .ne("id", plan.getId()));
            if (cnt >= 16) {
                return Result.fail("每站点启用的计费计划不能超过16个");
            }
        }
        boolean ok = planService.updateById(plan);
        if (ok) {
            // 缓存失效
            if (plan.getStationId() != null) {
                cacheService.invalidate(plan.getStationId(), plan.getId());
                cacheService.invalidateDefault(plan.getStationId());
            }
            
            if (plan.getIsDefault() != null && plan.getIsDefault() == 1 && plan.getStatus() != null && plan.getStatus() == 1) {
                BillingPlan reset = new BillingPlan();
                reset.setIsDefault(0);
                planService.update(reset, new QueryWrapper<BillingPlan>()
                        .eq("tenant_id", plan.getTenantId())
                        .eq("station_id", plan.getStationId())
                        .eq("status", 1)
                        .ne("id", plan.getId()));
            }
        }
        return Result.success(ok);
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
    public Result<Boolean> saveSegments(@PathVariable Long planId, @RequestParam(defaultValue = "false") boolean requireFullDay, @RequestBody List<BillingPlanSegment> segments) {
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
    public Result<List<BillingPlanSegment>> segments(@PathVariable Long planId) {
        return Result.success(planService.listSegments(planId));
    }

    @GetMapping("/{planId}/export")
    @Operation(summary = "导出计划分段为JSON")
    @DataScope
    public Result<List<BillingPlanSegment>> exportSegments(@PathVariable Long planId) {
        return Result.success(planService.listSegments(planId));
    }

    @PostMapping("/{planId}/import")
    @Operation(summary = "导入计划分段(覆盖式)")
    @DataScope
    public Result<Boolean> importSegments(@PathVariable Long planId,
                                          @RequestParam(defaultValue = "false") boolean requireFullDay,
                                          @RequestBody List<BillingPlanSegment> segments) {
        if (segments != null && segments.size() > 96) return Result.fail("分段数量不能超过96");
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
    public Result<BillingPlan> clonePlan(@PathVariable Long planId, @RequestBody BillingPlan payload) {
        BillingPlan np = planService.clonePlan(planId, payload);
        return np != null ? Result.success(np) : Result.fail("克隆失败");
    }
}
