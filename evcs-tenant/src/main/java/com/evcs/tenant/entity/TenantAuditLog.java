package com.evcs.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户审计日志实体
 * <p>记录跨层级管理行为，包括创建子租户、禁用/恢复、配额变更等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant_audit_log")
public class TenantAuditLog extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作者租户ID
     */
    private Long operatorTenantId;

    /**
     * 操作者用户ID
     */
    private Long operatorUserId;

    /**
     * 操作者用户名（冗余字段，便于查询）
     */
    private String operatorUserName;

    /**
     * 目标租户ID
     */
    private Long targetTenantId;

    /**
     * 目标租户名称（冗余字段）
     */
    private String targetTenantName;

    /**
     * 操作类型：CREATE_TENANT, DELETE_TENANT, UPDATE_QUOTA, UPDATE_STATUS,
     * RESET_PASSWORD, DISABLE_RECURSIVE, CROSS_LAYER_READ, CROSS_LAYER_WRITE
     */
    private String action;

    /**
     * 操作结果：SUCCESS, FAILURE
     */
    private String result;

    /**
     * 错误码（失败时记录）
     */
    private String errorCode;

    /**
     * 错误信息（失败时记录）
     */
    private String errorMessage;

    /**
     * 详细信息（JSON格式，记录变更前后的差异）
     */
    private String details;

    /**
     * 请求ID/追踪ID
     */
    private String traceId;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
}
