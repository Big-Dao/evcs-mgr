package com.evcs.common.audit;

/**
 * 租户审计服务接口
 * 定义租户管理相关的敏感操作审计标准
 */
public interface TenantAuditService {

    // 预定义动作常量
    String ACTION_DISABLE_Recursive = "DISABLE_RECURSIVE";
    String ACTION_UPDATE_QUOTA = "UPDATE_QUOTA";
    String ACTION_UPDATE_STATUS = "UPDATE_STATUS";
    String ACTION_CROSS_MANAGE_USER = "CROSS_MANAGE_USER";

    /**
     * 记录操作日志
     *
     * @param action         动作代码
     * @param targetTenantId 目标租户ID
     * @param details        详细信息
     */
    void logOperation(String action, Long targetTenantId, String details);
}
