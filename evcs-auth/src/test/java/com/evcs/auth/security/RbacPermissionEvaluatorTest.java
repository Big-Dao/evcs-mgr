package com.evcs.auth.security;

import com.evcs.auth.entity.Permission;
import com.evcs.auth.entity.Role;
import com.evcs.auth.service.IPermissionService;
import com.evcs.auth.service.IRoleService;
import com.evcs.common.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * RBAC权限评估器测试
 */
@ExtendWith(MockitoExtension.class)
class RbacPermissionEvaluatorTest {

    @Mock
    private IRoleService roleService;

    @Mock
    private IPermissionService permissionService;

    @InjectMocks
    private RbacPermissionEvaluator rbacPermissionEvaluator;

    private Authentication adminAuth;
    private Authentication userAuth;
    private Authentication superAdminAuth;

    @BeforeEach
    void setUp() {
        // 设置租户上下文
        TenantContext.setCurrentTenantId(1L);

        // 创建超级管理员认证
        UserDetails superAdminUser = User.builder()
                .username("superadmin")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                .build();
        superAdminAuth = new UsernamePasswordAuthenticationToken(superAdminUser, null, superAdminUser.getAuthorities());

        // 创建管理员认证
        UserDetails adminUser = User.builder()
                .username("admin")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        adminAuth = new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities());

        // 创建普通用户认证
        UserDetails normalUser = User.builder()
                .username("user1")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        userAuth = new UsernamePasswordAuthenticationToken(normalUser, null, normalUser.getAuthorities());

        // 默认模拟：根据角色编码生成基础角色信息
        lenient().when(roleService.listByRoleCodes(anySet())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Set<String> roleCodes = (Set<String>) invocation.getArgument(0);
            if (roleCodes == null || roleCodes.isEmpty()) {
                return List.of();
            }
            List<Role> roles = new ArrayList<>();
            for (String code : roleCodes) {
                Role role = new Role();
                role.setRoleCode(code);
                if ("ROLE_ADMIN".equals(code)) {
                    role.setId(2L);
                    role.setDataScope(1);
                } else if ("ROLE_USER".equals(code)) {
                    role.setId(1L);
                    role.setDataScope(5);
                } else if ("ROLE_SUPER_ADMIN".equals(code)) {
                    role.setId(3L);
                    role.setDataScope(1);
                } else {
                    role.setId(100L + roles.size());
                    role.setDataScope(0);
                }
                roles.add(role);
            }
            return roles;
        });

        // 默认模拟：为常见角色提供基础权限
        lenient().when(permissionService.listByRoleIds(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Long> roleIds = (List<Long>) invocation.getArgument(0);
            if (roleIds == null || roleIds.isEmpty()) {
                return List.of();
            }
            List<Permission> permissions = new ArrayList<>();
            for (Long roleId : roleIds) {
                if (roleId == null) {
                    continue;
                }
                if (roleId == 2L || roleId == 3L) { // 管理员及超管
                    Permission viewPermission = new Permission();
                    viewPermission.setId(10L + roleId);
                    viewPermission.setPerms("user:view");
                    viewPermission.setStatus(1);
                    permissions.add(viewPermission);
                } else if (roleId == 1L) { // 普通用户默认只读权限
                    Permission viewPermission = new Permission();
                    viewPermission.setId(20L);
                    viewPermission.setPerms("user:view");
                    viewPermission.setStatus(1);
                    permissions.add(viewPermission);
                }
            }
            return permissions;
        });
    }

    @Test
    void testSuperAdminHasAllPermissions() {
        // 超级管理员应该具有所有权限
        assertTrue(rbacPermissionEvaluator.hasPermission(superAdminAuth, "user:view"));
        assertTrue(rbacPermissionEvaluator.hasPermission(superAdminAuth, "user:create"));
        assertTrue(rbacPermissionEvaluator.hasPermission(superAdminAuth, "station:delete"));
        assertTrue(rbacPermissionEvaluator.hasPermission(superAdminAuth, "any:permission"));
    }

    @Test
    void testUnauthenticatedUserHasNoPermission() {
        Authentication nullAuth = null;
        assertFalse(rbacPermissionEvaluator.hasPermission(nullAuth, "user:view"));
    }

    @Test
    void testNullPermissionReturnsTrue() {
        // 没有权限要求时默认允许
        assertTrue(rbacPermissionEvaluator.hasPermission(userAuth, new Object(), (Object) null));
        assertTrue(rbacPermissionEvaluator.hasPermission(userAuth, new Object(), (String) null));
    }

    @Test
    void testUserWithRolePermissions() {
        // 模拟用户角色
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setRoleCode("ROLE_USER");
        userRole.setDataScope(5); // 仅本人数据权限

        lenient().when(roleService.listByRoleCodes(anySet())).thenReturn(List.of(userRole));

        // 模拟用户权限
        Permission userPermission = new Permission();
        userPermission.setId(1L);
        userPermission.setPerms("user:view");
        userPermission.setStatus(1);

        when(permissionService.listByRoleIds(any())).thenReturn(List.of(userPermission));

        // 测试权限检查
        assertTrue(rbacPermissionEvaluator.hasPermission(userAuth, "user:view"));
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, "user:create"));
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, "admin:manage"));
    }

    @Test
    void testPermissionPatternMatching() {
        // 模拟用户角色和权限
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setRoleCode("ROLE_USER");
        userRole.setDataScope(5);

        lenient().when(roleService.listByRoleCodes(anySet())).thenReturn(List.of(userRole));

        // 模拟通配符权限
        Permission wildcardPermission = new Permission();
        wildcardPermission.setId(1L);
        wildcardPermission.setPerms("user:*");
        wildcardPermission.setStatus(1);

        when(permissionService.listByRoleIds(any())).thenReturn(List.of(wildcardPermission));

        // 测试通配符权限匹配
        assertTrue(rbacPermissionEvaluator.hasPermission(userAuth, "user:view"));
        assertTrue(rbacPermissionEvaluator.hasPermission(userAuth, "user:create"));
        assertTrue(rbacPermissionEvaluator.hasPermission(userAuth, "user:update"));
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, "admin:view"));
    }

    @Test
    void testHierarchicalPermissionMatching() {
        // 模拟用户角色和权限
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setRoleCode("ROLE_USER");
        userRole.setDataScope(5);

        lenient().when(roleService.listByRoleCodes(anySet())).thenReturn(List.of(userRole));

        // 模拟层级权限
        Permission basePermission = new Permission();
        basePermission.setId(1L);
        basePermission.setPerms("user");
        basePermission.setStatus(1);

        when(permissionService.listByRoleIds(any())).thenReturn(List.of(basePermission));

        // 测试层级权限匹配
        assertTrue(rbacPermissionEvaluator.hasPermission(userAuth, "user"));
        assertTrue(rbacPermissionEvaluator.hasPermission(userAuth, "user:view"));
        assertTrue(rbacPermissionEvaluator.hasPermission(userAuth, "user:create:detail"));
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, "admin"));
    }

    @Test
    void testDataPermissionAllScope() {
        // 模拟具有全部数据权限的角色
        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setRoleCode("ROLE_ADMIN");
        adminRole.setDataScope(1); // 全部数据权限

        lenient().when(roleService.listByRoleCodes(anySet())).thenReturn(List.of(adminRole));

        // 测试数据权限
        assertTrue(rbacPermissionEvaluator.hasDataPermission(adminAuth, 100L));
        assertTrue(rbacPermissionEvaluator.hasDataPermission(adminAuth, 200L));
        assertTrue(rbacPermissionEvaluator.hasDataPermission(adminAuth, null));
    }

    @Test
    void testDataPermissionOwnerOnly() {
        // 模拟仅本人数据权限的角色
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setRoleCode("ROLE_USER");
        userRole.setDataScope(5); // 仅本人数据权限

        lenient().when(roleService.listByRoleCodes(anySet())).thenReturn(List.of(userRole));

        // 创建一个测试用户，用户名对应要测试的数据ID
        UserDetails testUser = User.builder()
                .username("100") // 用户名设置为要测试的数据ID
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        Authentication testAuth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        // 测试数据权限
        assertTrue(rbacPermissionEvaluator.hasDataPermission(testAuth, 100L)); // 用户名"100"匹配数据ID 100
        assertFalse(rbacPermissionEvaluator.hasDataPermission(testAuth, 200L)); // 用户名"100"不匹配数据ID 200
        assertFalse(rbacPermissionEvaluator.hasDataPermission(testAuth, null)); // null数据ID返回false
    }

    @Test
    void testPermissionEvaluationWithTargetDomainObject() {
        // 测试带目标域对象的权限评估
        assertTrue(rbacPermissionEvaluator.hasPermission(superAdminAuth, new Object(), "user:view"));
        assertTrue(rbacPermissionEvaluator.hasPermission(adminAuth, new Object(), "user:view"));
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, new Object(), "user:create"));
    }

    @Test
    void testPermissionEvaluationWithTargetId() {
        // 测试带目标ID的权限评估
        assertTrue(rbacPermissionEvaluator.hasPermission(superAdminAuth, 100L, "User", "user:view"));
        assertTrue(rbacPermissionEvaluator.hasPermission(adminAuth, 100L, "User", "user:view"));
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, 100L, "User", "user:create"));
    }

    @Test
    void testDisabledPermissionsAreIgnored() {
        // 模拟用户角色
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setRoleCode("ROLE_USER");
        userRole.setDataScope(5);

        lenient().when(roleService.listByRoleCodes(anySet())).thenReturn(List.of(userRole));

        // 模拟禁用的权限
        Permission disabledPermission = new Permission();
        disabledPermission.setId(1L);
        disabledPermission.setPerms("user:view");
        disabledPermission.setStatus(0); // 禁用状态

        when(permissionService.listByRoleIds(any())).thenReturn(List.of(disabledPermission));

        // 测试禁用权限不被授予
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, "user:view"));
    }

    @Test
    void testEmptyPermissionsAreIgnored() {
        // 模拟用户角色
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setRoleCode("ROLE_USER");
        userRole.setDataScope(5);

        lenient().when(roleService.listByRoleCodes(anySet())).thenReturn(List.of(userRole));

        // 模拟空权限
        Permission emptyPermission = new Permission();
        emptyPermission.setId(1L);
        emptyPermission.setPerms("   "); // 空字符串
        emptyPermission.setStatus(1);

        lenient().when(permissionService.listByRoleIds(any())).thenReturn(List.of(emptyPermission));

        // 测试空权限不被授予
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, "   "));
    }

    @Test
    void testNoTenantContextReturnsFalse() {
        // 清除租户上下文
        TenantContext.clear();

        // 测试无租户上下文时权限检查失败
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, "user:view"));
        assertFalse(rbacPermissionEvaluator.hasDataPermission(userAuth, 100L));
    }

    @Test
    void testExceptionHandling() {
        // 模拟服务异常
        when(roleService.listByRoleCodes(anySet())).thenThrow(new RuntimeException("Service error"));

        // 测试异常处理
        assertFalse(rbacPermissionEvaluator.hasPermission(userAuth, "user:view"));
        assertFalse(rbacPermissionEvaluator.hasDataPermission(userAuth, 100L));
    }
}
