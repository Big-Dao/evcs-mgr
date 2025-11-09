package com.evcs.auth.tenant;

/**
 * 租户上下文
 */
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * 设置当前租户ID
     */
    public static void setCurrentTenantId(final Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * 获取当前租户ID
     */
    public static Long getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * 清除租户上下文
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}