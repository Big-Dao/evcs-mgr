package com.evcs.tenant.controller.internal;

import com.evcs.common.result.Result;
import com.evcs.common.tenant.HierarchyValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoints for tenant hierarchy checks.
 *
 * Note: This endpoint is designed for service-to-service calls.
 */
@Validated
@RestController
@RequestMapping("/internal/api/v1/tenant-hierarchy")
@RequiredArgsConstructor
public class TenantHierarchyInternalController {

    private final HierarchyValidator hierarchyValidator;

    @Operation(summary = "Check tenant descendant", description = "Returns whether targetTenantId is a descendant of currentTenantId")
    @GetMapping("/descendant")
    public Result<Boolean> isDescendant(
            @Parameter(description = "Current tenant id (parent)") @RequestParam("currentTenantId") @NotNull Long currentTenantId,
            @Parameter(description = "Target tenant id (potential child)") @RequestParam("targetTenantId") @NotNull Long targetTenantId) {
        boolean descendant = hierarchyValidator.isDescendant(currentTenantId, targetTenantId);
        return Result.success("查询成功", descendant);
    }
}
