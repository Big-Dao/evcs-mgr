package com.evcs.tenant.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.tenant.entity.QuotaCheckResult;
import com.evcs.tenant.service.ITenantQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户配额管理控制器
 */
@Tag(name = "租户配额管理", description = "租户资源配额查询与设置")
@RestController
@RequestMapping("/tenant/quota")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
public class TenantQuotaController {

    private final ITenantQuotaService quotaService;

    @Operation(summary = "获取租户配额使用情况", description = "查询指定租户的配额使用详情")
    @GetMapping("/usage/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能查看本租户及下级租户的配额")
    public Result<ITenantQuotaService.TenantQuotaUsage> getQuotaUsage(
            @Parameter(description = "租户ID") @PathVariable Long tenantId) {
        return Result.success(quotaService.getQuotaUsage(tenantId));
    }

    @Operation(summary = "更新租户配额", description = "设置租户的资源配额限制（需要上级权限）")
    @PutMapping("/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能更新下级租户的配额")
    public Result<Void> updateQuota(
            @Parameter(description = "租户ID") @PathVariable Long tenantId,
            @Parameter(description = "最大用户数") @RequestParam(required = false) Integer maxUsers,
            @Parameter(description = "最大站点数") @RequestParam(required = false) Integer maxStations,
            @Parameter(description = "最大充电桩数") @RequestParam(required = false) Integer maxChargers) {

        quotaService.updateQuota(tenantId, maxUsers, maxStations, maxChargers);
        return Result.successMessage("配额更新成功");
    }

    @Operation(summary = "检查是否可创建子租户", description = "检查指定租户是否还可以创建新的子租户")
    @GetMapping("/check/create-child/{parentId}")
    public Result<QuotaCheckResult> checkCanCreateChildTenant(
            @Parameter(description = "父租户ID") @PathVariable Long parentId) {
        return Result.success(quotaService.checkCanCreateChildTenant(parentId));
    }

    @Operation(summary = "检查是否可添加站点", description = "检查指定租户是否还可以添加新站点")
    @GetMapping("/check/add-station/{tenantId}")
    public Result<QuotaCheckResult> checkCanAddStation(
            @Parameter(description = "租户ID") @PathVariable Long tenantId) {
        return Result.success(quotaService.checkCanAddStation(tenantId));
    }

    @Operation(summary = "检查是否可添加充电桩", description = "检查指定租户是否还可以添加新充电桩")
    @GetMapping("/check/add-charger/{tenantId}")
    public Result<QuotaCheckResult> checkCanAddCharger(
            @Parameter(description = "租户ID") @PathVariable Long tenantId) {
        return Result.success(quotaService.checkCanAddCharger(tenantId));
    }

    @Operation(summary = "检查是否可添加用户", description = "检查指定租户是否还可以添加新用户")
    @GetMapping("/check/add-user/{tenantId}")
    public Result<QuotaCheckResult> checkCanAddUser(
            @Parameter(description = "租户ID") @PathVariable Long tenantId) {
        return Result.success(quotaService.checkCanAddUser(tenantId));
    }
}
