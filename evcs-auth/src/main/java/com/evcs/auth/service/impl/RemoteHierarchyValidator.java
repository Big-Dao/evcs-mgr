package com.evcs.auth.service.impl;

import com.evcs.auth.client.TenantHierarchyClient;
import com.evcs.common.tenant.HierarchyValidator;
import com.evcs.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 层级校验器实现：通过Feign调用 evcs-tenant 的内部接口。
 *
 * 安全策略：fail-closed（远程不可用或返回异常时，一律拒绝跨租户访问）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteHierarchyValidator implements HierarchyValidator {

    private final TenantHierarchyClient tenantHierarchyClient;

    @Override
    public boolean isDescendant(Long currentTenantId, Long targetTenantId) {
        try {
            Result<Boolean> result = tenantHierarchyClient.isDescendant(currentTenantId, targetTenantId);
            if (result == null) {
                log.warn("Tenant hierarchy check returned null (fail-closed): current={}, target={}", currentTenantId, targetTenantId);
                return false;
            }
            if (!result.isSuccess()) {
                log.warn(
                        "Tenant hierarchy check failed (fail-closed): current={}, target={}, code={}, message={}",
                        currentTenantId,
                        targetTenantId,
                        result.getCode(),
                        result.getMessage()
                );
                return false;
            }
            return Boolean.TRUE.equals(result.getData());
        } catch (Exception e) {
            log.error("Tenant hierarchy check exception (fail-closed): current={}, target={}", currentTenantId, targetTenantId, e);
            return false;
        }
    }
}
