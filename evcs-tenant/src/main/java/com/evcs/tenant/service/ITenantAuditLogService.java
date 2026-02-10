package com.evcs.tenant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.tenant.entity.TenantAuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 租户审计日志服务
 */
public interface ITenantAuditLogService extends IService<TenantAuditLog> {

    /**
     * 记录审计日志
     *
     * @param operatorTenantId 操作者租户ID
     * @param operatorUserId   操作者用户ID
     * @param targetTenantId   目标租户ID
     * @param action           操作类型
     * @param result           操作结果
     * @param details          详细信息
     */
    void logAudit(Long operatorTenantId, Long operatorUserId, Long targetTenantId,
                  String action, String result, String details);

    /**
     * 记录成功的审计日志
     */
    void logSuccess(Long targetTenantId, String action, String details);

    /**
     * 记录失败的审计日志
     */
    void logFailure(Long targetTenantId, String action, String errorCode, String errorMessage);

    /**
     * 分页查询审计日志
     */
    List<TenantAuditLog> queryAuditLogPage(Long targetTenantId, String action,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           Integer offset, Integer limit);

    /**
     * 统计审计日志数量
     */
    long countAuditLog(Long targetTenantId, String action,
                       LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询跨层级操作统计
     */
    List<Map<String, Object>> statisticsCrossLayer(LocalDateTime startTime);

    // 操作类型常量
    String ACTION_CREATE_TENANT = "CREATE_TENANT";
    String ACTION_DELETE_TENANT = "DELETE_TENANT";
    String ACTION_UPDATE_TENANT = "UPDATE_TENANT";
    String ACTION_UPDATE_QUOTA = "UPDATE_QUOTA";
    String ACTION_UPDATE_STATUS = "UPDATE_STATUS";
    String ACTION_RESET_PASSWORD = "RESET_PASSWORD";
    String ACTION_DISABLE_RECURSIVE = "DISABLE_RECURSIVE";
    String ACTION_CROSS_LAYER_READ = "CROSS_LAYER_READ";
    String ACTION_CROSS_LAYER_WRITE = "CROSS_LAYER_WRITE";
    String ACTION_MOVE_TENANT = "MOVE_TENANT";

    // 操作结果常量
    String RESULT_SUCCESS = "SUCCESS";
    String RESULT_FAILURE = "FAILURE";
}
