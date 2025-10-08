package com.evcs.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 用于标记需要进行数据权限控制的方法
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    
    /**
     * 数据权限类型
     */
    DataScopeType value() default DataScopeType.TENANT;
    
    /**
     * 是否允许跨租户访问（仅系统管理员）
     */
    boolean allowCrossTenant() default false;
    
    /**
     * 是否允许访问下级租户数据
     */
    boolean allowSubTenant() default true;
    
    /**
     * 自定义数据权限处理器类
     */
    Class<? extends DataScopeHandler> handler() default DefaultDataScopeHandler.class;
    
    /**
     * 数据权限描述
     */
    String description() default "";
    
    /**
     * 数据权限类型枚举
     */
    enum DataScopeType {
        /**
         * 租户级别：只能访问本租户的数据
         */
        TENANT,
        
        /**
         * 层级租户：可以访问本租户及下级租户的数据
         */
        TENANT_HIERARCHY,
        
        /**
         * 用户级别：只能访问用户自己的数据
         */
        USER,
        
        /**
         * 角色级别：根据角色的数据权限范围访问数据
         */
        ROLE,
        
        /**
         * 自定义：使用自定义处理器
         */
        CUSTOM,
        
        /**
         * 无限制：系统管理员级别，可以访问所有数据
         */
        ALL
    }
    
    /**
     * 数据权限处理器接口
     */
    interface DataScopeHandler {
        /**
         * 检查数据访问权限
         * @param targetTenantId 目标租户ID
         * @param targetUserId 目标用户ID
         * @param operation 操作类型
         * @return 是否有权限
         */
        boolean hasPermission(Long targetTenantId, Long targetUserId, String operation);
    }
    
    /**
     * 默认数据权限处理器
     */
    class DefaultDataScopeHandler implements DataScopeHandler {
        @Override
        public boolean hasPermission(Long targetTenantId, Long targetUserId, String operation) {
            return com.evcs.common.tenant.TenantContext.hasAccessToTenant(targetTenantId);
        }
    }
}