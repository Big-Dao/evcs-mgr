package com.evcs.tenant.config;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 租户模块权限评估器。
 *
 * <p>租户模块无独立权限表，授权模型为角色制：
 * SUPER_ADMIN/TENANT_ADMIN/ADMIN 拥有模块内全部业务权限，
 * OPERATOR/USER 仅拥有查询类权限（dashboard:stats、只读查看）。
 * 注册为 PermissionEvaluator 后，@PreAuthorize("hasPermission(...)") 不再落入
 * Spring 的 DenyAllPermissionEvaluator（此前导致仪表盘对所有用户 403）。
 */
@Component
public class TenantRolePermissionEvaluator implements PermissionEvaluator {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(TenantRolePermissionEvaluator.class);

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return evaluate(authentication, permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        return evaluate(authentication, permission);
    }

    private boolean evaluate(Authentication authentication, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        boolean readOnly = isReadOnlyPermission(permission);
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.endsWith("ADMIN") || role.endsWith("SUPER_ADMIN")) {
                return true;
            }
            if (readOnly && (role.equals("ROLE_OPERATOR") || role.equals("ROLE_USER"))) {
                return true;
            }
        }
        log.debug("权限拒绝: user={}, permission={}", authentication.getName(), permission);
        return false;
    }

    private boolean isReadOnlyPermission(Object permission) {
        if (permission == null) {
            return false;
        }
        String p = String.valueOf(permission).toLowerCase();
        return p.startsWith("dashboard") || p.startsWith("view") || p.startsWith("station:list")
                || p.startsWith("charger:query");
    }
}
