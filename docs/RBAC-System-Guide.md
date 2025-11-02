# RBAC权限系统使用指南

## 概述

本项目实现了一套完整的基于角色（Role-Based Access Control, RBAC）的权限管理系统，支持细粒度的权限控制、多租户数据隔离和数据权限管理。

## 系统架构

### 核心组件

1. **RbacPermissionEvaluator** - 权限评估器
   - 位置：`evcs-auth/src/main/java/com/evcs/auth/security/RbacPermissionEvaluator.java`
   - 功能：实现Spring Security的PermissionEvaluator接口，提供权限验证功能

2. **实体类**
   - `User` - 用户实体
   - `Role` - 角色实体
   - `Permission` - 权限实体
   - `UserRole` - 用户角色关联实体
   - `RolePermission` - 角色权限关联实体

3. **数据访问层**
   - `UserRoleMapper` - 用户角色关联数据访问
   - `RolePermissionMapper` - 角色权限关联数据访问
   - `RoleMapper` - 角色数据访问
   - `PermissionMapper` - 权限数据访问

4. **服务层**
   - `IRoleService` - 角色服务接口
   - `IPermissionService` - 权限服务接口
   - `RoleServiceImpl` - 角色服务实现
   - `PermissionServiceImpl` - 权限服务实现

## 功能特性

### 1. 基于角色的权限控制

系统支持通过角色来管理用户权限，用户可以拥有多个角色，每个角色可以拥有多个权限。

```java
// 示例：检查用户是否具有指定权限
@PreAuthorize("hasPermission('user:view')")
public List<User> listUsers() {
    return userService.list();
}
```

### 2. 多种权限匹配模式

- **精确匹配**：权限字符串必须完全匹配
- **通配符匹配**：支持 `*` 通配符，如 `user:*` 匹配所有用户相关权限
- **层级权限**：支持层级权限，如 `user` 权限自动包含 `user:view`、`user:create` 等

### 3. 多租户权限隔离

系统支持多租户架构，每个租户的权限数据完全隔离：

```java
// 租户上下文自动应用
Long tenantId = TenantContext.getCurrentTenantId();
// 权限验证会自动考虑当前租户范围
```

### 4. 数据权限控制

支持5种数据权限范围：

1. **全部数据权限** (dataScope = 1)
   - 可以访问所有数据

2. **自定义数据权限** (dataScope = 2)
   - 根据自定义规则访问数据

3. **本部门数据权限** (dataScope = 3)
   - 只能访问本部门的数据

4. **本部门及以下数据权限** (dataScope = 4)
   - 可以访问本部门及子部门的数据

5. **仅本人数据权限** (dataScope = 5)
   - 只能访问自己的数据

```java
// 示例：数据权限检查
@PreAuthorize("hasPermission(#dataOwnerId, 'DATA_ACCESS')")
public DataObject getData(Long dataOwnerId) {
    return dataService.getById(dataOwnerId);
}
```

### 5. 超级管理员权限

系统支持超级管理员角色，拥有所有权限：

```java
// 支持的超级管理员角色
private boolean hasSuperAdminRole(Set<String> roleCodes) {
    return roleCodes.contains("ROLE_SUPER_ADMIN") || roleCodes.contains("ROLE_ADMIN");
}
```

## 使用方法

### 1. 配置Spring Security

在Spring Security配置中启用RBAC权限评估器：

```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PermissionEvaluator permissionEvaluator(
            IRoleService roleService,
            IPermissionService permissionService) {
        return new RbacPermissionEvaluator(roleService, permissionService);
    }
}
```

### 2. 方法级权限控制

使用Spring Security的注解进行权限控制：

```java
@Service
public class UserService {

    // 需要用户查看权限
    @PreAuthorize("hasPermission('user:view')")
    public List<User> listUsers() {
        return userMapper.selectList(null);
    }

    // 需要用户创建权限
    @PreAuthorize("hasPermission('user:create')")
    public void createUser(User user) {
        userMapper.insert(user);
    }

    // 需要数据权限检查
    @PreAuthorize("hasPermission(#userId, 'DATA_ACCESS')")
    public User getUser(Long userId) {
        return userMapper.selectById(userId);
    }
}
```

### 3. 数据库设计

#### 用户表 (sys_user)
```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    tenant_id BIGINT NOT NULL,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 角色表 (sys_role)
```sql
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    data_scope INT DEFAULT 5,
    status INT DEFAULT 1,
    sort INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 权限表 (sys_permission)
```sql
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    perms VARCHAR(200),
    parent_id BIGINT DEFAULT 0,
    type INT DEFAULT 2, -- 1:菜单 2:按钮 3:接口
    path VARCHAR(200),
    component VARCHAR(200),
    icon VARCHAR(100),
    sort INT DEFAULT 0,
    status INT DEFAULT 1,
    visible INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 用户角色关联表 (sys_user_role)
```sql
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id, tenant_id)
);
```

#### 角色权限关联表 (sys_role_permission)
```sql
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);
```

### 4. 权限编码规范

建议采用模块:操作格式的权限编码：

```
用户管理：user:view, user:create, user:update, user:delete
角色管理：role:view, role:create, role:update, role:delete
权限管理：permission:view, permission:create, permission:update, permission:delete
充电站管理：station:view, station:create, station:update, station:delete
订单管理：order:view, order:create, order:update, order:delete
支付管理：payment:view, payment:create, payment:refund
```

## 最佳实践

### 1. 权限设计原则

- **最小权限原则**：只授予用户完成任务所需的最小权限
- **职责分离**：不同角色承担不同职责，避免权限过度集中
- **定期审查**：定期检查和清理不必要的权限

### 2. 角色设计建议

建议创建以下基础角色：

- **SUPER_ADMIN**：超级管理员，拥有所有权限
- **ADMIN**：管理员，拥有业务管理权限
- **OPERATOR**：操作员，拥有日常操作权限
- **USER**：普通用户，拥有基本权限
- **VIEWER**：查看者，只有只读权限

### 3. 数据权限使用

根据业务需求合理设置数据权限范围：

- 超级管理员：全部数据权限
- 部门经理：本部门及以下数据权限
- 普通员工：仅本人数据权限

### 4. 性能优化建议

- **缓存权限数据**：频繁访问的权限数据建议使用缓存
- **批量权限检查**：避免在循环中进行单个权限检查
- **权限预加载**：在用户登录时预加载常用权限

### 5. 安全建议

- **密码加密**：用户密码必须加密存储
- **会话管理**：合理设置会话超时时间
- **审计日志**：记录重要权限操作的审计日志
- **权限变更通知**：权限变更时及时通知相关用户

## 测试

系统提供了完整的单元测试，覆盖以下场景：

- 超级管理员权限验证
- 普通用户权限验证
- 权限模式匹配（精确、通配符、层级）
- 数据权限检查
- 多租户权限隔离
- 异常情况处理

运行测试：

```bash
./gradlew :evcs-auth:test --tests "*RbacPermissionEvaluatorTest"
```

## 扩展开发

### 1. 自定义权限评估器

可以通过继承RbacPermissionEvaluator来实现自定义权限逻辑：

```java
@Component
public class CustomPermissionEvaluator extends RbacPermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, String permission) {
        // 自定义权限逻辑
        if (isSpecialPermission(permission)) {
            return checkSpecialPermission(authentication, permission);
        }
        return super.hasPermission(authentication, permission);
    }
}
```

### 2. 动态权限

支持动态权限配置，可以通过管理界面实时调整权限：

```java
@Service
public class DynamicPermissionService {

    public void refreshUserPermissions(Long userId) {
        // 清除用户权限缓存
        // 重新加载用户权限
    }
}
```

### 3. 权限注解

可以创建自定义权限注解简化使用：

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission('user:view')")
public @interface RequireUserViewPermission {
}
```

## 常见问题

### Q: 如何处理权限变更？

A: 权限变更后，需要清除相关用户的权限缓存，可以调用以下方法：

```java
// 清除特定用户权限缓存
permissionService.clearUserPermissionCache(userId);

// 或者让用户重新登录获取最新权限
```

### Q: 如何实现临时权限？

A: 可以通过创建临时角色和权限来实现：

```java
// 创建临时角色
Role tempRole = new Role();
tempRole.setRoleName("临时权限");
tempRole.setRoleCode("TEMP_PERMISSION");
tempRole.setExpireTime(expireTime); // 设置过期时间
roleService.save(tempRole);
```

### Q: 如何处理权限继承？

A: 可以通过设置父权限来实现权限继承：

```java
// 父权限
Permission parentPermission = new Permission();
parentPermission.setPerms("user");
parentPermission.setType(1); // 菜单类型

// 子权限
Permission childPermission = new Permission();
childPermission.setPerms("user:view");
childPermission.setParentId(parentPermission.getId());
```

## 总结

本RBAC权限系统提供了完整、灵活、安全的权限管理解决方案，支持：

- 基于角色的权限控制
- 多种权限匹配模式
- 多租户数据隔离
- 细粒度数据权限控制
- 完整的测试覆盖

通过合理使用这套系统，可以有效管理应用权限，保障系统安全。