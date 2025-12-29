package com.evcs.tenant.service.impl;

import com.evcs.common.tenant.HierarchyValidator;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 层级校验器实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HierarchyValidatorImpl implements HierarchyValidator {

    private final ISysTenantService tenantService;

    @Override
    public boolean isDescendant(Long currentTenantId, Long targetTenantId) {
        if (currentTenantId == null || targetTenantId == null) {
            return false;
        }

        try {
            // 目前SysTenantService.getTenantById可能会被AOP拦截导致死循环？
            // DataScopeAspect invoke -> HierarchyValidator -> Service -> AOP ->
            // HierarchyValidator
            // Service.getTenantById has NO @DataScope annotation usually?
            // Let's check SysTenantServiceImpl.getTenantById
            // It calls this.getById(id) which depends on BaseMapper.

            // However, SysTenantServiceImpl.getTenantById IS override.
            // Let's check signature.
            // public SysTenant getTenantById(Long tenantId)
            // Does it have @DataScope?
            // Abstract check.

            SysTenant target = tenantService.getTenantById(targetTenantId);
            if (target == null) {
                return false;
            }

            String ancestors = target.getAncestors();
            if (ancestors == null) {
                return false;
            }

            // Ancestors usually: "0,100,101"
            // We pad with comma to ensure exact match
            String fullPath = "," + ancestors + ",";
            String search = "," + currentTenantId + ",";

            return fullPath.contains(search);

        } catch (Exception e) {
            log.error("层级校验异常: current={}, target={}", currentTenantId, targetTenantId, e);
            return false;
        }
    }
}
