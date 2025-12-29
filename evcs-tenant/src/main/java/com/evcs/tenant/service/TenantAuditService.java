package com.evcs.tenant.service;

/**
 * 租户审计服务
 * 负责记录租户管理相关的敏感操作日志
 */
public interface TenantAuditService {

    /**
     * 记录操作日志
     * 
     * @param action         动作代码 (e.g. DISABLE_TENANT)
     * @param targetTenantId 目标租户ID
     * @param details        详细信息/快照
     */
    void logOperation(String action, Long targetTenantId, String details);
}
