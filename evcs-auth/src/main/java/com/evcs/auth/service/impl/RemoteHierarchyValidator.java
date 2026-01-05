package com.evcs.auth.service.impl;

import com.evcs.common.tenant.HierarchyValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 临时层级校验器实现 (Mock)
 * TODO: 应替换为通过Feign调用evcs-tenant服务的真正实现
 */
@Slf4j
@Component
public class RemoteHierarchyValidator implements HierarchyValidator {

    @Override
    public boolean isDescendant(Long currentTenantId, Long targetTenantId) {
        log.warn("Using Mock HierarchyValidator. Allowing access from {} to {}", currentTenantId, targetTenantId);
        // 暂时允许所有跨租户访问，以免阻塞启动。生产环境必须修复！
        return true;
    }
}
