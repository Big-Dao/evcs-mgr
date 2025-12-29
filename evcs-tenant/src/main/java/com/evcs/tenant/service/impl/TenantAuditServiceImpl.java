package com.evcs.tenant.service.impl;

import com.evcs.common.tenant.TenantContext;
import com.evcs.common.audit.TenantAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TenantAuditServiceImpl implements TenantAuditService {

    @Override
    public void logOperation(String action, Long targetTenantId, String details) {
        Long operatorTenant = TenantContext.getTenantId();
        Long operatorUser = TenantContext.getUserId();

        // 结构化日志输出，便于后续ELK收集或grep
        log.info("AUDIT_LOG | action={} | target_tenant={} | operator_tenant={} | operator_user={} | details={}",
                action, targetTenantId, operatorTenant, operatorUser, details);

        // TODO: 实现数据库落库 (Need TenantAuditLogMapper)
    }
}
