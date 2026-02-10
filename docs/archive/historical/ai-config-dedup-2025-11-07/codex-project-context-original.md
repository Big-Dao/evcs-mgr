# EVCS充电站管理系统 - CodeX项目上下文

## 🎯 系统概述
基于Spring Boot 3.2.10 + Java 21的微服务架构电动汽车充电站管理系统，支持多租户隔离。

## 📋 完整规范

**🔥 重要：请首先阅读项目完整规范文档**: [PROJECT-CODING-STANDARDS.md](../../PROJECT-CODING-STANDARDS.md)

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

### 严格分层
```
Controller层 → Service层 → Repository层 → Entity层
```

### 严禁的架构违规
- ❌ 跨服务直接访问数据库（如OrderService中使用UserRepository）
- ❌ 业务逻辑在Controller中
- ❌ 直接返回Entity，必须使用DTO
- ❌ 硬编码敏感信息

### 必须的注解模式
```java
// Controller
@RestController @RequestMapping @Validated @Slf4j
@PreAuthorize("hasPermission('resource', 'action')")

// Service
@Service @Transactional @Slf4j
@Cacheable/@CacheEvict @DataScope(DataScopeType.TENANT)

// Entity
@Entity @Table @Data @EqualsAndHashCode(callSuper = true)
extends BaseEntity
```

## 🔧 技术栈约束

### 后端技术
- Spring Boot 3.2.10
- Java 21 (LTS)
- Spring Security + JWT
- MyBatis Plus (多租户支持)
- PostgreSQL 15
- Redis 7
- Gradle 8.11

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
```

## 📝 代码模板

### Controller模板
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
        try {
            {Resource}DTO result = {resource}Service.create(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result));
        } catch (BusinessException e) {
            log.warn("创建{resource}失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("创建{resource}异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("系统内部错误"));
        }
    }
}
```

### Service模板
```java
@Service
@Transactional
@Slf4j
public class {Resource}Service {

    @Cacheable(value = "{resource}s", key = "#id")
    @DataScope(DataScopeType.TENANT)
    public {Resource}DTO getById(Long id) {
        {Resource} {resource} = {resource}Mapper.selectById(id);
        if ({resource} == null) {
            throw new {Resource}NotFoundException("{resource}不存在: " + id);
        }
        return {Resource}DTO.fromEntity({resource});
    }

    @CacheEvict(value = "{resource}s", allEntries = true)
    @Transactional
    public {Resource}DTO create(Create{Resource}Request request) {
        validateCreateRequest(request);

        {Resource} {resource} = {Resource}.builder()
            // 字段赋值
            .tenantId(TenantContext.getCurrentTenantId())
            .build();

        {resource} = {resource}Mapper.insert({resource});
        log.info("创建{resource}成功: {}", {resource}.getId());

        return {Resource}DTO.fromEntity({resource});
    }
}
```

### Entity模板
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

    @Column(name = "name", nullable = false, length = 100)
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

## 🚫 严格禁止

### 安全违规
```java
// ❌ 禁止硬编码
String dbUrl = "jdbc:postgresql://localhost:5432/evcs";
String jwtSecret = "my-secret-key";

// ❌ 禁止跨服务访问
@Service
public class OrderService {
    @Autowired
    private UserRepository userRepo; // 违规
}
```

### 异常处理违规
```java
// ❌ 禁止空catch块
try {
    riskyOperation();
} catch (Exception e) {
    // 空catch块
}

// ❌ 禁止System.out
System.out.println("debug"); // 使用log.info()
```

## ✅ 质量要求

### 测试覆盖率
- Service层 >= 80%
- Controller层 >= 70%
- 必须包含边界条件和异常测试

### 性能要求
- 避免N+1查询问题
- 合理使用缓存
- 异步处理耗时操作
- 正确释放资源

### 日志要求
- 使用@Slf4j注解
- 关键操作记录日志
- 异常情况记录完整上下文

通过遵循这些规范，CodeX可以为EVCS项目生成高质量、符合架构要求的代码。