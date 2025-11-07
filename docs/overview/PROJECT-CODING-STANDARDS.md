# EVCS充电站管理系统 - 编程规范总览

> **最后更新**: 2025-11-07 | **维护者**: 技术负责人 | **状态**: 活跃
>
> 📋 **本文档为AI编程助手（Claude、GitHub Copilot、CodeX等）提供完整的项目规范**

## 🎯 项目概述

**项目名称**: EVCS充电站管理系统
**架构**: Spring Boot 3.2.10 + Java 21微服务架构
**特点**: 多租户数据隔离、小规模业务优化（2GB内存内）
**代码规范版本**: v1.0.0

## 📋 目录 (TOC)

- [🏗️ 强制架构规范](#-强制架构规范)
  - [微服务模块划分](#1-微服务模块划分)
  - [严格分层架构](#2-严格分层架构)
  - [严禁的架构违规](#3-严禁的架构违规)
  - [必须使用的注解](#4-必须使用的注解)
    - [Controller层](#controller层)
    - [Service层](#service层)
    - [Entity层](#entity层)
- [🔧 技术栈约束](#-技术栈约束)
  - [后端技术栈](#后端技术栈)
  - [缓存策略](#缓存策略)
  - [租户隔离实现](#租户隔离实现)
- [📝 编码标准](#-编码标准)
  - [命名规范](#1-命名规范)
  - [异常处理](#2-异常处理)
  - [日志记录](#3-日志记录)
  - [参数验证](#4-参数验证)
- [🚫 严格禁止的模式](#-严格禁止的模式)
  - [跨服务数据访问](#1-跨服务数据访问)
  - [硬编码配置](#2-硬编码配置)
  - [数据库直接访问](#3-数据库直接访问)
- [🧪 质量要求](#-质量要求)
  - [测试覆盖率](#1-测试覆盖率)
  - [代码质量检查](#2-代码质量检查)
  - [性能要求](#3-性能要求)
- [📚 相关文档](#-相关文档)

## 🏗️ 强制架构规范

### 1. 微服务模块划分
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

### 2. 严格分层架构
```
Controller层 → Service层 → Repository层 → Entity层
```

### 3. 严禁的架构违规
- ❌ **跨服务数据库访问**: 如OrderService中注入UserRepository
- ❌ **业务逻辑在Controller**: Controller只处理HTTP请求/响应
- ❌ **直接返回Entity**: 必须使用DTO返回数据
- ❌ **硬编码敏感信息**: 使用环境变量配置

### 4. 必须使用的注解

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

## 🔧 技术栈约束

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
```java
// 查询方法
@Cacheable(value = "cacheName", key = "#param")

// 更新方法
@CacheEvict(value = "cacheName", allEntries = true)
```

### 租户隔离实现
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

### 1. 命名规范
- **类名**: PascalCase (如StationService)
- **方法名**: camelCase (如getStationById)
- **常量**: UPPER_SNAKE_CASE (如MAX_RETRY_COUNT)
- **数据库**: snake_case (如station_id)

### 2. 异常处理
```java
// ✅ 正确的异常处理
try {
    riskyOperation();
} catch (BusinessException e) {
    log.warn("业务异常: {}", e.getMessage());
    throw e;
} catch (Exception e) {
    log.error("系统异常", e);
    throw new BusinessException("系统处理失败");
}

// ❌ 禁止的空catch块
try {
    riskyOperation();
} catch (Exception e) {
    // 空catch块 - 严禁！
}
```

### 3. 日志记录
```java
// ✅ 正确：使用日志框架
log.info("创建充电站成功: {}", station.getId());
log.debug("查询充电站: {}", id);
log.error("系统异常", e);

// ❌ 禁止：使用System.out
System.out.println("debug info"); // 严禁！
```

### 4. 参数验证
```java
// Controller层
public ResponseEntity<ApiResponse<StationDTO>> create(
        @Valid @RequestBody CreateStationRequest request) {
    // @Valid注解进行自动验证
}

// Service层
private void validateCreateRequest(CreateStationRequest request) {
    if (StringUtils.isBlank(request.getName())) {
        throw new ValidationException("充电站名称不能为空");
    }
}
```

## 🚫 严格禁止的模式

### 1. 跨服务数据访问
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

### 2. 硬编码敏感信息
```java
// ❌ 禁止：硬编码敏感信息
String dbUrl = "jdbc:postgresql://localhost:5432/evcs";
String jwtSecret = "my-secret-key";
String alipayAppId = "2021000000000000";

// ✅ 正确：使用环境变量
@Value("${spring.datasource.url}")
private String dbUrl;

@Value("${app.jwt.secret}")
private String jwtSecret;
```

### 3. 忽略异常处理
```java
// ❌ 禁止：空catch块
try {
    riskyOperation();
} catch (Exception e) {
    // 忽略异常 - 严禁！
}

// ❌ 禁止：吞掉异常
try {
    riskyOperation();
} catch (Exception e) {
    log.info("操作失败"); // 信息丢失
}
```

## ✅ 质量要求

### 1. 测试覆盖率
- **Service层**: >= 80%
- **Controller层**: >= 70%
- **Repository层**: >= 60%
- 必须包含边界条件和异常情况测试

### 2. 性能优化
- 避免N+1查询问题
- 合理使用Spring Cache
- 异步处理耗时操作（@Async）
- 正确释放资源（try-with-resources）

### 3. 安全要求
- API必须包含认证检查（@PreAuthorize）
- 输入参数必须验证（@Valid）
- SQL操作必须参数化查询
- 租户隔离必须正确实现

### 4. 代码质量
- 方法长度 <= 50行
- 类长度 <= 500行
- 圈复杂度 <= 10
- 无代码重复（重复率 <= 3%）

## 🧪 单元测试模板

```java
@ExtendWith(MockitoExtension.class)
@Slf4j
class {Resource}ServiceTest {

    @Mock
    private {Resource}Repository {resource}Repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private {Resource}Service {resource}Service;

    @Test
    @DisplayName("应该成功创建{resource}")
    void shouldCreate{Resource}Successfully() {
        // Given - 准备测试数据
        Create{Resource}Request request = Create{Resource}Request.builder()
            .name("Test Station")
            .address("Test Address")
            .build();

        {Resource} saved{Resource} = {Resource}.builder()
            .id(1L)
            .name("Test Station")
            .build();

        when({resource}Repository.save(any({Resource}.class))).thenReturn(saved{Resource});
        when({resource}Repository.existsByName(anyString())).thenReturn(false);

        // Set tenant context
        TenantContext.setCurrentTenantId(1L);

        // When - 执行测试方法
        {Resource}DTO result = {resource}Service.create(request);

        // Then - 验证结果
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Station");

        verify({resource}Repository).save(any({Resource}.class));
        verify({resource}Repository).existsByName("Test Station");

        // Cleanup
        TenantContext.clear();
    }

    @Test
    @DisplayName("{resource}名称重复时应该抛出异常")
    void shouldThrowExceptionWhen{Resource}NameExists() {
        // Given
        Create{Resource}Request request = Create{Resource}Request.builder()
            .name("Duplicate Station")
            .build();

        when({resource}Repository.existsByName("Duplicate Station")).thenReturn(true);
        TenantContext.setCurrentTenantId(1L);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> {resource}Service.create(request));

        assertThat(exception.getMessage()).contains("名称已存在");

        TenantContext.clear();
    }
}
```

## 🐳 Docker配置标准

```dockerfile
# 所有服务统一使用以下配置
FROM gradle:8.11-jdk21-alpine AS build
# 构建阶段...

FROM eclipse-temurin:21-jre-alpine AS runtime
# 运行时阶段...

# 统一JVM参数（小规模业务优化）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0"
```

## 📋 质量检查清单

### 每次代码生成后必须检查
- [ ] 是否遵循微服务架构原则
- [ ] 是否包含适当的异常处理
- [ ] 是否使用Spring Cache注解
- [ ] 是否包含事务管理(@Transactional)
- [ ] 是否包含日志记录(@Slf4j)
- [ ] 是否包含单元测试
- [ ] 是否符合命名规范

### 安全检查
- [ ] 敏感信息是否使用环境变量
- [ ] API是否包含认证检查(@PreAuthorize)
- [ ] 输入参数是否包含验证(@Valid)
- [ ] SQL操作是否使用参数化查询
- [ ] 租户隔离是否正确实现

### 性能检查
- [ ] 数据库查询是否避免N+1问题
- [ ] 是否合理使用缓存
- [ ] 异步操作是否使用@Async
- [ ] 资源是否正确释放

## 🚀 快速开始

### 1. AI助手使用方法
**GitHub Copilot**: 自动读取项目规范
**Claude Code**: 读取`.claude/project-instructions.md`
**ChatGPT/CodeX**: 使用本规范作为上下文

### 2. 开发环境启动
```bash
# 核心开发环境（6个服务，~2GB内存）
docker-compose -f docker-compose.core-dev.yml up -d

# 完整生产环境（11个服务，~4GB内存）
docker-compose up -d
```

### 3. 质量检查
```bash
# 运行所有质量检查
./gradlew qualityCheck

# 运行预提交脚本
./scripts/pre-commit-check.sh

# 运行测试和覆盖率
./gradlew test jacocoTestReport
```

## 📚 相关文档

- [AI助手详细指南](../development/AI-ASSISTANT-GUIDELINES.md)
- [代码质量清单](../development/CODE-QUALITY-CHECKLIST.md)
- [项目结构说明](../operations/PROJECT-STRUCTURE.md)
- [部署指南](../deployment/DEPLOYMENT-GUIDE.md)
- [故障排除](../troubleshooting/ERROR_PREVENTION_CHECKLIST.md)

---

**版本**: v1.0.0
**更新日期**: 2025-11-07
**维护团队**: EVCS开发团队

通过遵循本规范，AI编程助手可以为EVCS项目生成高质量、符合企业级标准的一致性代码。