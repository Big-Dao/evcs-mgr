# EVCS项目共享上下文

> **本文档为所有AI助手提供共享的项目基础信息**
> **更新日期**: 2025-11-07

## 🎯 项目概述

这是一个基于Spring Boot 3.2.10 + Java 21的微服务架构电动汽车充电站管理系统（EVCS），专为小规模业务优化设计，支持多租户隔离。

### 📋 完整规范
**🔥 重要：请首先阅读项目完整规范文档**: [PROJECT-CODING-STANDARDS.md](../../docs/overview/PROJECT-CODING-STANDARDS.md)

该文档包含了生成高质量代码所需的**所有规范要求**，包括：
- 强制架构规范和代码模板
- 必须使用的注解和禁止的模式
- 完整的质量检查清单
- 测试要求和性能优化标准

**在生成任何代码之前，请务必遵循PROJECT-CODING-STANDARDS.md中的所有规范！**

## 🏗️ 架构概览

**详细的微服务架构、分层规范、注解要求、禁止模式等完整内容，请参考**: [项目编程规范总览](../../docs/overview/PROJECT-CODING-STANDARDS.md)

该文档包含：
- 完整的微服务架构定义和模块划分
- 强制分层架构和注解使用规范
- 严禁的架构违规和代码模式
- 正确的实现方式示例
- 完整的技术栈和最佳实践

**🔥 重要：所有代码生成必须严格遵循PROJECT-CODING-STANDARDS.md中的规范要求！**

## 🔧 技术栈

### 后端技术栈
- **Spring Boot**: 3.2.10 (最新稳定版)
- **Java**: 21 (LTS版本)
- **Spring Security**: JWT认证 + RBAC权限控制
- **MyBatis Plus**: ORM框架 + 多租户支持
- **PostgreSQL**: 15 (主数据库)
- **Redis**: 7 (缓存 + 会话存储)
- **RabbitMQ**: 3 (消息队列)
- **Gradle**: 8.11.1 (构建工具)

### 缓存策略
- 查询方法：@Cacheable(value = "cacheName", key = "#param")
- 更新方法：@CacheEvict(value = "cacheName", allEntries = true)

### 租户隔离
```java
// 必须设置租户上下文
try {
    TenantContext.setCurrentTenantId(tenantId);
    // 业务逻辑
} finally {
    TenantContext.clear();
}

// 查询方法添加数据权限注解
@DataScope(DataScopeType.TENANT)
public List<{Resource}> getAll() {
    return {resource}Mapper.selectList(null);
}
```

## 📝 编码标准

### 必须使用的注解

#### Controller层
```java
@RestController
@RequestMapping("/api/v1/{resource}")
@Validated
@Slf4j
public class {Resource}Controller {

    @PostMapping
    @PreAuthorize("hasPermission('{resource}', 'create')")
    public ResponseEntity<ApiResponse<{Resource}DTO>> create(
            @Valid @RequestBody Create{Resource}Request request) {
        // 实现
    }
}
```

#### Service层
```java
@Service
@Transactional
@Slf4j
public class {Resource}Service {

    @Cacheable(value = "{resource}s", key = "#id")
    @DataScope(DataScopeType.TENANT)
    public {Resource}DTO getById(Long id) {
        // 查询实现
    }

    @CacheEvict(value = "{resource}s", allEntries = true)
    @Transactional
    public {Resource}DTO create(Create{Resource}Request request) {
        // 创建实现
    }
}
```

#### Entity层
```java
@Entity
@Table(name = "{resource}s", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class {Resource} extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        // 初始化逻辑
    }

    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        // 更新验证逻辑
    }
}
```

## 🚫 严格禁止的模式

### 跨服务数据访问
```java
// ❌ 禁止：跨服务直接访问数据库
@Service
public class OrderService {
    @Autowired
    private UserRepository userRepo; // 跨服务访问 - 严禁！
}

// ✅ 正确：通过Feign客户端调用
@Service
public class OrderService {
    @Autowired
    private UserFeignClient userClient; // 正确方式
}
```

### 硬编码敏感信息
```java
// ❌ 禁止：硬编码敏感信息
String dbUrl = "jdbc:postgresql://localhost:5432/evcs";
String jwtSecret = "my-secret-key";

// ✅ 正确：使用环境变量
@Value("${spring.datasource.url}")
private String dbUrl;

@Value("${app.jwt.secret}")
private String jwtSecret;
```

## ✅ 质量要求

### 测试覆盖率
- **Service层**: >= 80%
- **Controller层**: >= 70%
- **Repository层**: >= 60%

### 性能优化
- 避免N+1查询问题
- 合理使用Spring Cache
- 异步处理耗时操作（@Async）
- 正确释放资源

### 安全要求
- API必须包含认证检查（@PreAuthorize）
- 输入参数必须验证（@Valid）
- SQL操作必须参数化查询
- 租户隔离必须正确实现

---

**注意：本文档提供所有AI助手共享的基础项目信息。各助手特定的使用说明请参考各自的配置文件。**