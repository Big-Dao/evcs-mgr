package com.evcs.tenant.service.impl;

import com.evcs.common.audit.TenantAuditService;
import com.evcs.common.tenant.TenantContext;
import com.evcs.tenant.service.ITenantAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 租户审计服务实现
 * <p>桥接 common 模块的接口和 tenant 模块的数据库服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantAuditServiceImpl implements TenantAuditService {

    private final ITenantAuditLogService auditLogService;

    // 操作类型常量（与 TenantAuditLogService 保持一致）
    public static final String ACTION_CREATE_TENANT = "CREATE_TENANT";
    public static final String ACTION_DELETE_TENANT = "DELETE_TENANT";
    public static final String ACTION_UPDATE_TENANT = "UPDATE_TENANT";
    public static final String ACTION_UPDATE_QUOTA = "UPDATE_QUOTA";
    public static final String ACTION_UPDATE_STATUS = "UPDATE_STATUS";
    public static final String ACTION_RESET_PASSWORD = "RESET_PASSWORD";
    public static final String ACTION_DISABLE_RECURSIVE = "DISABLE_RECURSIVE";
    public static final String ACTION_CROSS_LAYER_READ = "CROSS_LAYER_READ";
    public static final String ACTION_CROSS_LAYER_WRITE = "CROSS_LAYER_WRITE";
    public static final String ACTION_MOVE_TENANT = "MOVE_TENANT";

    @Override
    public void logOperation(String action, Long targetTenantId, String details) {
        Long operatorTenant = TenantContext.getCurrentTenantId();
        Long operatorUser = TenantContext.getCurrentUserId();

        // 结构化日志输出，便于后续ELK收集或grep
        log.info("AUDIT_LOG | action={} | target_tenant={} | operator_tenant={} | operator_user={} | details={}",
                action, targetTenantId, operatorTenant, operatorUser, details);

        // 数据库落库
        auditLogService.logAudit(operatorTenant, operatorUser, targetTenantId,
                action, auditLogService.RESULT_SUCCESS, details);
    }
}
