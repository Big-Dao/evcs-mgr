# EVCS项目AI助手统一配置

> **版本**: v1.0 | **更新日期**: 2025-11-07
>
> 📋 **本文档为所有AI编程助手提供统一的项目配置**

## 🎯 项目概述

基于Spring Boot 3.2.10 + Java 21的微服务架构电动汽车充电站管理系统（EVCS），专为小规模业务优化设计，支持多租户隔离。

### 📋 完整规范
**🔥 重要：请首先阅读项目完整规范文档**: [PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md)

该文档包含了生成高质量代码所需的**所有规范要求**，包括：
- 强制架构规范和代码模板
- 必须使用的注解和禁止的模式
- 完整的质量检查清单
- 测试要求和性能优化标准

**在生成任何代码之前，请务必遵循PROJECT-CODING-STANDARDS.md中的所有规范！**

## 🏗️ 微服务架构

### 微服务模块划分
```
evcs-gateway (8080)     - API网关，路由和安全防护
evcs-auth (8081)       - 认证授权服务，JWT + RBAC
evcs-station (8082)    - 充电站管理，设备控制
evcs-order (8083)      - 订单管理，计费方案
evcs-payment (8084)    - 支付服务，支付宝/微信
evcs-protocol (8085)   - 协议处理，OCPP/云快充
evcs-tenant (8086)     - 租户管理，多租户隔离
evcs-monitoring (8087) - 监控服务，健康检查
evcs-config (8888)     - 配置中心，Git配置
evcs-eureka (8761)     - 服务注册中心
evcs-common            - 公共组件，共享工具类
```

### 严格分层架构
```
Controller层 → Service层 → Repository层 → Entity层
```

### 严禁的架构违规
- ❌ **跨服务数据库访问**: 如OrderService中注入UserRepository
- ❌ **业务逻辑在Controller**: Controller只处理HTTP请求/响应
- ❌ **直接返回Entity**: 必须使用DTO返回数据
- ❌ **硬编码敏感信息**: 使用环境变量配置

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

## 🚀 项目启动

### 开发环境（推荐）
```bash
# 启动核心服务
docker-compose -f docker-compose.core-dev.yml up -d

# 检查服务状态
docker-compose -f docker-compose.core-dev.yml ps

# 查看服务日志
docker-compose -f docker-compose.core-dev.yml logs -f
```

### 生产环境
```bash
# 启动完整服务
docker-compose up -d

# 添加监控服务
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

## 🤖 AI助手使用指导

### Claude Code
- **集成方式**: 自动读取项目根目录下的本配置文件
- **使用场景**: 项目级代码生成、架构分析、重构建议
- **最佳实践**: 提供清晰的业务需求，包含完整的上下文信息

### GitHub Copilot
- **集成方式**: 自动读取.github/copilot-instructions.md（指向本文件）
- **使用场景**: 日常编码、实时代码补全、测试生成
- **最佳实践**: 编写清晰的注释，提供具体的业务需求

### OpenAI CodeX
- **集成方式**: API调用时包含本配置文件内容
- **使用场景**: 批量代码生成、复杂功能实现
- **最佳实践**: 提供结构化的prompt，包含详细的业务需求

### 通用使用技巧
1. **明确需求**: 清楚描述要实现的功能
2. **提供上下文**: 包含相关的业务场景和技术要求
3. **遵循规范**: 严格按照PROJECT-CODING-STANDARDS.md中的规范
4. **包含测试**: 为生成的代码编写相应的测试
5. **验证结果**: 人工审查生成的代码质量和安全性

## ⚠️ 严格执行文档规范

**所有AI助手必须严格遵守文档规范，详细要求请参考：**
- **[文档维护指南](DOCUMENTATION-MAINTENANCE-GUIDE.md)** ⚠️ **必须严格执行**
- **[项目编程规范](../overview/PROJECT-CODING-STANDARDS.md)**

### 🚫 最高优先级禁止事项
- ❌ **严禁在非指定目录创建文档**
- ❌ **严禁使用不符合规范的文件名**
- ❌ **严禁创建重复或相似的文档**
- ❌ **严禁破坏现有目录结构**

### ✅ 必须遵守的要求
- ✅ **只能在docs/分类目录中创建文档**
- ✅ **必须使用小写+连字符的语义化命名**
- ✅ **创建前必须检查是否已存在相似文档**
- ✅ **必须更新所有相关的引用链接**

> **注意**: 违反文档规范的行为将立即纠正，相关文件将被删除或重命名。

## 📚 相关文档

- [项目编程规范总览](../overview/PROJECT-CODING-STANDARDS.md)
- [统一部署指南](../deployment/DEPLOYMENT-GUIDE.md)
- [Docker配置指南](../deployment/DOCKER-CONFIGURATION-GUIDE.md)
- [API设计规范](API-DESIGN-STANDARDS.md)
- [数据库设计规范](DATABASE-DESIGN-STANDARDS.md)
- [统一测试指南](../testing/UNIFIED-TESTING-GUIDE.md)
- [文档维护指南](DOCUMENTATION-MAINTENANCE-GUIDE.md) ⚠️ **必须严格执行**

---

**通过这个统一配置和严格的文档规范执行，所有AI编程助手都能获得一致、完整的项目信息，从而生成高质量、符合架构规范的代码。严禁任何违反文档规范的行为！**