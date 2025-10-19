package com.evcs.common.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 租户上下文管理器
 * 使用ThreadLocal存储当前线程的租户信息，确保数据隔离
 */
public class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(
        TenantContext.class
    );

    /**
     * 租户ID线程本地变量
     */
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    /**
     * 用户ID线程本地变量
     */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /**
     * 租户类型线程本地变量
     */
    private static final ThreadLocal<Integer> TENANT_TYPE = new ThreadLocal<>();

    /**
     * 租户祖级路径线程本地变量（用于层级权限控制）
     */
    private static final ThreadLocal<String> TENANT_ANCESTORS =
        new ThreadLocal<>();

    /**
     * 设置租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
        log.debug("设置租户ID: {}", tenantId);
    }

    /**
     * 获取租户ID
     */
    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    // Compatibility aliases
    public static Long getCurrentTenantId() {
        return getTenantId();
    }

    public static void setCurrentTenantId(Long tenantId) {
        setTenantId(tenantId);
    }

    /**
     * 设置用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
        log.debug("设置用户ID: {}", userId);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    // Compatibility aliases
    public static Long getCurrentUserId() {
        return getUserId();
    }

    public static void setCurrentUserId(Long userId) {
        setUserId(userId);
    }

    /**
     * 设置租户类型
     */
    public static void setTenantType(Integer tenantType) {
        TENANT_TYPE.set(tenantType);
        log.debug("设置租户类型: {}", tenantType);
    }

    /**
     * 获取租户类型
     */
    public static Integer getTenantType() {
        return TENANT_TYPE.get();
    }

    /**
     * 设置租户祖级路径
     */
    public static void setTenantAncestors(String ancestors) {
        TENANT_ANCESTORS.set(ancestors);
        log.debug("设置租户祖级路径: {}", ancestors);
    }

    /**
     * 获取租户祖级路径
     */
    public static String getTenantAncestors() {
        return TENANT_ANCESTORS.get();
    }

    /**
     * 检查是否为系统管理员
     */
    public static boolean isSystemAdmin() {
        Integer tenantType = getTenantType();
        return tenantType != null && tenantType == 1; // 1表示平台方
    }

    /**
     * 检查是否有权限访问指定租户的数据
     * @param targetTenantId 目标租户ID
     * @return 是否有权限
     */
    public static boolean hasAccessToTenant(Long targetTenantId) {
        if (targetTenantId == null) {
            return false;
        }

        // 系统管理员可以访问所有租户数据
        if (isSystemAdmin()) {
            return true;
        }

        Long currentTenantId = getTenantId();
        if (currentTenantId == null) {
            return false;
        }

        // 访问自己的数据
        if (currentTenantId.equals(targetTenantId)) {
            return true;
        }

        // 检查是否为上级租户（可以访问下级租户数据）
        String ancestors = getTenantAncestors();
        if (
            ancestors != null && ancestors.contains("," + targetTenantId + ",")
        ) {
            return true;
        }

        return false;
    }

    /**
     * 清除当前线程的租户上下文
     */
    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        TENANT_TYPE.remove();
        TENANT_ANCESTORS.remove();
        log.debug("清除租户上下文");
    }

    /**
     * 获取当前上下文信息（用于调试）
     */
    public static String getContextInfo() {
        return String.format(
            "TenantContext[tenantId=%s, userId=%s, tenantType=%s, ancestors=%s]",
            getTenantId(),
            getUserId(),
            getTenantType(),
            getTenantAncestors()
        );
    }
}
