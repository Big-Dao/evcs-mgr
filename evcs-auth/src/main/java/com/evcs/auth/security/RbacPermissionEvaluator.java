package com.evcs.auth.security;

import com.evcs.auth.entity.Permission;
import com.evcs.auth.entity.Role;
import com.evcs.auth.service.IPermissionService;
import com.evcs.auth.service.IRoleService;
import com.evcs.common.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC权限评估器
 */
@Component("rbac")
@SuppressWarnings("EI_EXPOSE_REP2")
public class RbacPermissionEvaluator {

    private final IRoleService roleService;
    private final IPermissionService permissionService;

    public RbacPermissionEvaluator(IRoleService roleService, IPermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    /**
     * 检查是否有指定权限
     */
    public boolean hasPermission(final Authentication authentication, final String permission) {
        if (authentication == null) {
            return false;
        }
        if (permission == null) {
            return true;
        }
        if (permission.trim().isEmpty()) {
            return false;
        }

        // 检查租户上下文
        if (TenantContext.getCurrentTenantId() == null) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) {
            return true;
        }

        try {
            // 获取用户角色
            Set<String> roleCodes = getRoleCodes(authentication);
            if (roleCodes.isEmpty()) {
                return false;
            }

            // 查询角色信息
            List<Role> roles = roleService.listByRoleCodes(roleCodes);
            if (roles.isEmpty()) {
                return false;
            }

            // 查询权限
            List<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toList());
            List<Permission> permissions = permissionService.listByRoleIds(roleIds);

            // 检查权限匹配
            return permissions.stream()
                    .anyMatch(p -> p.getStatus() == 1 &&
                            !p.getPerms().trim().isEmpty() &&
                            matchesPermission(p.getPerms(), permission));

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查数据权限
     */
    public boolean hasDataPermission(Authentication authentication, Long dataId) {
        if (authentication == null) {
            return false;
        }

        // 检查租户上下文
        if (TenantContext.getCurrentTenantId() == null) {
            return false;
        }

        // 超级管理员拥有所有数据权限
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) {
            return true;
        }

        try {
            // 获取用户角色
            Set<String> roleCodes = getRoleCodes(authentication);
            if (roleCodes.isEmpty()) {
                return false;
            }

            // 查询角色信息
            List<Role> roles = roleService.listByRoleCodes(roleCodes);
            if (roles.isEmpty()) {
                return false;
            }

            // 检查数据权限范围
            return roles.stream()
                    .anyMatch(role -> {
                        int dataScope = role.getDataScope() != null ? role.getDataScope() : 0;
                        switch (dataScope) {
                            case 1: // 全部数据权限
                                return true;
                            case 5: // 仅本人数据权限
                                if (dataId == null) {
                                    return false;
                                }
                                String username = authentication.getName();
                                return username.equals(dataId.toString());
                            default:
                                return false;
                        }
                    });

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 带目标域对象的权限检查
     */
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (permission == null) {
            return true;
        }
        if (permission instanceof String) {
            return hasPermission(authentication, (String) permission);
        }
        return false;
    }

    /**
     * 带目标ID的权限检查
     */
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (permission instanceof String) {
            return hasPermission(authentication, (String) permission);
        }
        return false;
    }

    /**
     * 获取用户角色编码
     */
    private Set<String> getRoleCodes(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * 检查是否有指定角色
     */
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(role));
    }

    /**
     * 权限匹配（支持通配符和层级匹配）
     */
    private boolean matchesPermission(String userPermission, String requiredPermission) {
        if (userPermission.equals(requiredPermission)) {
            return true;
        }

        // 通配符匹配
        if (userPermission.endsWith(":*")) {
            String prefix = userPermission.substring(0, userPermission.length() - 2);
            return requiredPermission.startsWith(prefix + ":");
        }

        // 层级权限匹配
        if (!userPermission.contains(":") && requiredPermission.startsWith(userPermission + ":")) {
            return true;
        }

        return false;
    }
}
