package com.evcs.auth.security;

import com.evcs.auth.entity.Permission;
import com.evcs.auth.entity.Role;
import com.evcs.auth.entity.UserRole;
import com.evcs.auth.service.IPermissionService;
import com.evcs.auth.service.IRoleService;
import com.evcs.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于RBAC的权限评估器
 *
 * 支持细粒度的权限控制：
 * 1. 基于角色的权限验证
 * 2. 基于具体权限的验证
 * 3. 多租户权限隔离
 * 4. 数据权限控制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RbacPermissionEvaluator implements PermissionEvaluator {

    private final IRoleService roleService;
    private final IPermissionService permissionService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        if (permission == null) {
            return true; // 如果没有权限要求，默认允许
        }

        String permissionStr = permission.toString();
        return hasPermission(authentication, permissionStr);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        if (permission == null) {
            return true; // 如果没有权限要求，默认允许
        }

        String permissionStr = permission.toString();
        return hasPermission(authentication, permissionStr);
    }

    /**
     * 检查用户是否具有指定权限
     */
    public boolean hasPermission(Authentication authentication, String permission) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        try {
            // 获取当前租户ID
            Long tenantId = TenantContext.getCurrentTenantId();
            if (tenantId == null) {
                log.warn("无法获取当前租户ID");
                return false;
            }

            // 获取用户角色
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            Set<String> roleCodes = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            if (roleCodes.isEmpty()) {
                log.debug("用户没有分配任何角色");
                return false;
            }

            // 检查超级管理员权限
            if (hasSuperAdminRole(roleCodes)) {
                log.debug("用户具有超级管理员权限，允许所有操作");
                return true;
            }

            // 获取用户权限
            Set<String> userPermissions = getUserPermissions(roleCodes, tenantId);

            // 检查是否具有指定权限
            boolean hasPermission = userPermissions.contains(permission) ||
                    hasPermissionByPattern(userPermissions, permission);

            log.debug("权限检查结果: user={}, permission={}, hasPermission={}",
                    authentication.getName(), permission, hasPermission);

            return hasPermission;

        } catch (Exception e) {
            log.error("权限检查异常: permission={}", permission, e);
            return false;
        }
    }

    /**
     * 检查用户是否具有超级管理员角色
     */
    private boolean hasSuperAdminRole(Set<String> roleCodes) {
        return roleCodes.contains("ROLE_SUPER_ADMIN") || roleCodes.contains("ROLE_ADMIN");
    }

    /**
     * 获取用户权限列表
     */
    private Set<String> getUserPermissions(Set<String> roleCodes, Long tenantId) {
        try {
            // 根据角色编码查询角色
            List<Role> roles = roleService.listByRoleCodes(roleCodes);
            if (roles.isEmpty()) {
                return Set.of();
            }

            // 获取角色ID列表
            List<Long> roleIds = roles.stream()
                    .map(Role::getId)
                    .collect(Collectors.toList());

            // 查询权限列表
            List<Permission> permissions = permissionService.listByRoleIds(roleIds);

            // 过滤启用状态的权限
            return permissions.stream()
                    .filter(permission -> permission.getStatus() != null && permission.getStatus() == 1)
                    .filter(permission -> permission.getPerms() != null && !permission.getPerms().trim().isEmpty())
                    .map(Permission::getPerms)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("获取用户权限失败", e);
            return Set.of();
        }
    }

    /**
     * 通过模式匹配检查权限
     */
    private boolean hasPermissionByPattern(Set<String> userPermissions, String requiredPermission) {
        // 精确匹配
        if (userPermissions.contains(requiredPermission)) {
            return true;
        }

        // 通配符匹配
        for (String userPermission : userPermissions) {
            if (matchesPattern(userPermission, requiredPermission)) {
                return true;
            }
        }

        // 层级权限检查 (例如: user:view 匹配 user:view:detail)
        if (requiredPermission.contains(":")) {
            String basePermission = requiredPermission.substring(0, requiredPermission.indexOf(":"));
            return userPermissions.contains(basePermission);
        }

        return false;
    }

    /**
     * 权限模式匹配
     */
    private boolean matchesPattern(String userPermission, String requiredPermission) {
        if (userPermission.equals("*")) {
            return true;
        }

        if (userPermission.endsWith("*")) {
            String prefix = userPermission.substring(0, userPermission.length() - 1);
            return requiredPermission.startsWith(prefix);
        }

        return false;
    }

    /**
     * 检查用户是否已认证
     */
    private boolean isAuthenticated(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.isAccountNonLocked() && userDetails.isEnabled();
        }

        return authentication.isAuthenticated();
    }

    /**
     * 检查数据权限
     */
    public boolean hasDataPermission(Authentication authentication, Long dataOwnerId) {
        if (!isAuthenticated(authentication)) {
            return false;
        }

        try {
            // 获取用户角色
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            Set<String> roleCodes = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // 检查是否为数据所有者
            if (isDataOwner(authentication, dataOwnerId)) {
                return true;
            }

            // 检查数据权限范围
            Integer maxDataScope = getMaxDataScope(roleCodes);
            if (maxDataScope == null) {
                return false;
            }

            switch (maxDataScope) {
                case 1: // 全部数据权限
                    return true;
                case 2: // 自定义数据权限
                    return checkCustomDataPermission(authentication, dataOwnerId);
                case 3: // 本部门数据权限
                    return checkDepartmentDataPermission(authentication, dataOwnerId);
                case 4: // 本部门及以下数据权限
                    return checkDepartmentAndSubDataPermission(authentication, dataOwnerId);
                case 5: // 仅本人数据权限
                    return isDataOwner(authentication, dataOwnerId);
                default:
                    return false;
            }

        } catch (Exception e) {
            log.error("数据权限检查异常", e);
            return false;
        }
    }

    /**
     * 检查是否为数据所有者
     */
    private boolean isDataOwner(Authentication authentication, Long dataOwnerId) {
        if (dataOwnerId == null) {
            return false;
        }

        try {
            // 从认证信息中提取用户ID
            String username = authentication.getName();
            // 这里需要根据实际的用户信息结构来获取用户ID
            // 可以通过UserService查询用户ID，或者从JWT token中提取
            // 暂时使用简单的用户名匹配（实际项目中应该使用用户ID）
            return username.equals(String.valueOf(dataOwnerId));
        } catch (Exception e) {
            log.debug("Error checking data ownership", e);
            return false;
        }
    }

    /**
     * 获取最大数据权限范围
     */
    private Integer getMaxDataScope(Set<String> roleCodes) {
        try {
            List<Role> roles = roleService.listByRoleCodes(roleCodes);
            return roles.stream()
                    .filter(role -> role.getDataScope() != null)
                    .map(Role::getDataScope)
                    .min(Integer::compare) // 最小的数据权限范围最严格
                    .orElse(null);
        } catch (Exception e) {
            log.error("获取数据权限范围失败", e);
            return null;
        }
    }

    /**
     * 检查自定义数据权限
     */
    private boolean checkCustomDataPermission(Authentication authentication, Long dataOwnerId) {
        try {
            // 自定义数据权限逻辑
            // 可以根据具体业务需求实现，例如：
            // 1. 检查用户是否有权限访问特定部门的数据
            // 2. 检查用户是否有权限访问特定项目的数据
            // 3. 检查用户是否有权限访问特定区域的数据

            // 这里简单实现：如果是数据所有者，则允许访问
            return isDataOwner(authentication, dataOwnerId);
        } catch (Exception e) {
            log.error("Error checking custom data permission", e);
            return false;
        }
    }

    /**
     * 检查本部门数据权限
     */
    private boolean checkDepartmentDataPermission(Authentication authentication, Long dataOwnerId) {
        try {
            // 获取当前用户的部门信息
            String username = authentication.getName();
            // 这里需要根据实际的用户信息结构来获取用户部门
            // 可以通过UserService查询用户的部门信息

            // 简单实现：假设用户名包含部门信息
            // 实际项目中应该查询用户的部门信息，然后检查数据是否属于同一部门
            log.debug("Checking department data permission for user: {}", username);

            // 暂时返回true，实际项目中需要实现部门权限检查
            return true;
        } catch (Exception e) {
            log.error("Error checking department data permission", e);
            return false;
        }
    }

    /**
     * 检查本部门及以下数据权限
     */
    private boolean checkDepartmentAndSubDataPermission(Authentication authentication, Long dataOwnerId) {
        try {
            // 获取当前用户的部门信息
            String username = authentication.getName();
            // 这里需要根据实际的用户信息结构来获取用户部门及子部门

            // 简单实现：假设用户名包含部门信息
            // 实际项目中应该查询用户的部门及子部门信息，然后检查数据是否属于这些部门
            log.debug("Checking department and sub-department data permission for user: {}", username);

            // 暂时返回true，实际项目中需要实现部门及子部门权限检查
            return true;
        } catch (Exception e) {
            log.error("Error checking department and sub-department data permission", e);
            return false;
        }
    }
}