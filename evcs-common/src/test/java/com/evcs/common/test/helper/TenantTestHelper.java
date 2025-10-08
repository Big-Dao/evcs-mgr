package com.evcs.common.test.helper;

import com.evcs.common.tenant.TenantContext;

/**
 * 租户测试辅助类
 * 提供租户上下文管理的便捷方法
 */
public class TenantTestHelper {

    /**
     * 设置测试租户上下文
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     */
    public static void setupTenantContext(Long tenantId, Long userId) {
        TenantContext.setCurrentTenantId(tenantId);
        TenantContext.setCurrentUserId(userId);
    }

    /**
     * 设置测试租户上下文（使用默认用户ID 1L）
     * 
     * @param tenantId 租户ID
     */
    public static void setupTenantContext(Long tenantId) {
        setupTenantContext(tenantId, 1L);
    }

    /**
     * 清理租户上下文
     */
    public static void clearTenantContext() {
        TenantContext.clear();
    }

    /**
     * 执行带租户上下文的操作
     * 操作完成后自动清理上下文
     * 
     * @param tenantId 租户ID
     * @param action 要执行的操作
     */
    public static void withTenant(Long tenantId, Runnable action) {
        try {
            setupTenantContext(tenantId);
            action.run();
        } finally {
            clearTenantContext();
        }
    }

    /**
     * 执行带租户上下文的操作并返回结果
     * 操作完成后自动清理上下文
     * 
     * @param tenantId 租户ID
     * @param supplier 要执行的操作
     * @return 操作结果
     */
    public static <T> T withTenant(Long tenantId, java.util.function.Supplier<T> supplier) {
        try {
            setupTenantContext(tenantId);
            return supplier.get();
        } finally {
            clearTenantContext();
        }
    }

    /**
     * 验证租户上下文是否已设置
     * 
     * @return true if tenant context is set
     */
    public static boolean isTenantContextSet() {
        return TenantContext.getCurrentTenantId() != null;
    }

    /**
     * 获取当前租户ID
     * 
     * @return 当前租户ID
     */
    public static Long getCurrentTenantId() {
        return TenantContext.getCurrentTenantId();
    }

    /**
     * 获取当前用户ID
     * 
     * @return 当前用户ID
     */
    public static Long getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }
}
