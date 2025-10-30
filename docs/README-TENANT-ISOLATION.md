# 充电站管理平台 - 多租户数据隔离系统

> **最后更新**: 2025-10-20  
> **维护者**: EVCS Architecture Team  
> **状态**: 已发布

## 系统概述

本系统是一个充电站管理平台，支持多租户、分层级的数据隔离架构。系统基于Spring Boot 3.2 + PostgreSQL + MyBatis Plus构建，采用微服务架构设计，能够支持500个充电站，每个站点最多5000个充电桩的规模。

## 多租户隔离架构

### 1. 隔离层级

- **数据库层隔离**: 通过PostgreSQL的tenant_id字段实现行级数据隔离
- **SQL层隔离**: 使用MyBatis Plus的TenantLineHandler自动添加租户条件
- **服务层隔离**: 通过AOP切面实现方法级权限控制
- **API层隔离**: 使用Spring Security和自定义注解控制接口访问

### 2. 核心组件

#### 2.1 租户上下文管理 (TenantContext)
```java
// 线程本地存储租户信息
public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    
    public static void setCurrentTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    public static Long getCurrentTenantId() {
        return TENANT_ID.get();
    }
    
    public static void clear() {
        TENANT_ID.remove();
    }
}
```

#### 2.2 租户拦截器 (TenantInterceptor)
- 自动从请求头或JWT令牌中提取租户ID
- 在请求开始时设置租户上下文
- 在请求结束时清理租户上下文

#### 2.3 SQL自动过滤 (CustomTenantLineHandler)
```java
@Override
public Expression getTenantId() {
    Long tenantId = TenantContext.getCurrentTenantId();
    if (tenantId != null) {
        return new LongValue(tenantId);
    }
    return new NullValue();
}

@Override
public String getTenantIdColumn() {
    return "tenant_id";
}

@Override
public boolean ignoreTable(String tableName) {
    // 忽略系统表和无需租户隔离的表
    return Arrays.asList("sys_tenant", "sys_user", "sys_role").contains(tableName);
}
```

#### 2.4 数据权限注解 (@DataScope)
```java
@DataScope(value = DataScope.Type.CHILDREN, description = "只能查看子租户数据")
public List<ChargingStation> getStationList() {
    // 方法实现
}
```

支持的权限类型：
- **ALL**: 查看所有数据（超级管理员）
- **SELF**: 只能查看当前租户数据
- **CHILDREN**: 可以查看当前租户及其子租户数据

#### 2.5 AOP权限切面 (DataScopeAspect)
- 拦截带有@DataScope注解的方法
- 根据权限类型动态修改SQL查询条件
- 支持层级租户权限验证

### 3. 数据库设计

#### 3.1 租户表结构 (sys_tenant)
```sql
CREATE TABLE sys_tenant (
    tenant_id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(64) UNIQUE NOT NULL,
    tenant_name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    ancestors TEXT DEFAULT '0',
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_time TIMESTAMP,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);
```

#### 3.2 业务表设计
所有业务表都包含`tenant_id`字段：
```sql
CREATE TABLE charging_station (
    station_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,  -- 租户隔离字段
    station_code VARCHAR(64) NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    -- 其他业务字段...
    FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id)
);
```

### 4. 权限控制流程

```
HTTP请求 → TenantInterceptor → 提取租户ID → 设置TenantContext
    ↓
Controller方法 → @DataScope注解 → DataScopeAspect切面
    ↓
Service方法 → MyBatis查询 → TenantLineHandler → 自动添加WHERE tenant_id = ?
    ↓
数据库查询 → 返回结果 → 清理TenantContext
```

### 5. 安全特性

1. **自动SQL注入防护**: 所有查询自动添加租户条件，防止跨租户数据泄露
2. **层级权限控制**: 支持父租户管理子租户，子租户无法访问父租户数据
3. **上下文隔离**: 使用ThreadLocal确保多线程环境下的租户上下文隔离
4. **权限验证**: 结合Spring Security实现接口级权限控制

### 6. 配置说明

#### 6.1 应用配置 (application.yml)
```yaml
evcs:
  tenant:
    enabled: true
    tenant-id-column: tenant_id
    ignore-tables:
      - sys_tenant
      - sys_user
      - sys_role
    default-tenant-id: 1
```

#### 6.2 MyBatis Plus配置
```java
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 多租户插件
        TenantLineInnerInterceptor tenantLineInnerInterceptor = new TenantLineInnerInterceptor();
        tenantLineInnerInterceptor.setTenantLineHandler(new CustomTenantLineHandler());
        interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        
        return interceptor;
    }
}
```

## 使用示例

### 1. 创建租户
```java
@PostMapping
@DataScope(value = DataScope.Type.CHILDREN)
public Result<Void> createTenant(@RequestBody SysTenant tenant) {
    // 只能在当前租户下创建子租户
    tenantService.saveTenant(tenant);
    return Result.success();
}
```

### 2. 查询数据
```java
@GetMapping("/stations")
@DataScope(value = DataScope.Type.SELF)
public Result<List<ChargingStation>> getStations() {
    // 自动过滤，只返回当前租户的充电站
    return Result.success(stationService.list());
}
```

### 3. 层级查询
```java
@GetMapping("/stations/all")
@DataScope(value = DataScope.Type.CHILDREN)
public Result<List<ChargingStation>> getAllStations() {
    // 返回当前租户及其子租户的所有充电站
    return Result.success(stationService.list());
}
```

## 部署说明

1. **数据库**: PostgreSQL 15+
2. **缓存**: Redis 7+
3. **消息队列**: RabbitMQ 3.8+
4. **Java版本**: OpenJDK 21
5. **构建工具**: Gradle 8.5

## 测试验证

系统提供了完整的测试用例来验证多租户隔离功能：

```bash
# 运行测试
./gradlew test

# 启动服务
./gradlew bootRun
```

## 注意事项

1. 所有业务表必须包含`tenant_id`字段
2. 系统表（如用户、角色表）需要在忽略列表中
3. 跨租户操作需要特殊权限验证
4. 备份和恢复需要考虑租户数据隔离
5. 监控和日志记录应包含租户信息

## 扩展性

- 支持动态添加新的租户
- 支持租户数据迁移
- 支持租户级别的配置隔离
- 支持租户级别的性能监控

---

该多租户隔离系统确保了充电站管理平台中不同租户数据的安全隔离，为平台的稳定运行和数据安全提供了强有力的保障。

