package com.evcs.auth.service.impl;

import com.evcs.common.audit.TenantAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 简单的租户审计实现 (Auth模块)
 * 目前仅记录日志，后续可对接统一审计服务
 */
@Slf4j
@Service
public class SimpleTenantAuditServiceImpl implements TenantAuditService {

    @Override
    public void logOperation(String action, Long targetTenantId, String details) {
        log.info("[AUDIT] Action: {}, TargetTenant: {}, Details: {}", action, targetTenantId, details);
    }
}
