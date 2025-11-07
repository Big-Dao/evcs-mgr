# EVCS充电桩管理系统 - GitHub Copilot使用指南

## 🎯 项目概述

基于Spring Boot 3.2.10 + Java 21的微服务架构电动汽车充电站管理系统，专为小规模业务优化设计。

## 📋 完整规范

**🔥 重要：请首先阅读项目完整规范文档**: [PROJECT-CODING-STANDARDS.md](../../PROJECT-CODING-STANDARDS.md)

该文档包含了生成高质量代码所需的**所有规范要求**，包括：
- 强制架构规范和代码模板
- 必须使用的注解和禁止的模式
- 完整的质量检查清单
- 测试要求和性能优化标准

**在生成任何代码之前，请务必遵循PROJECT-CODING-STANDARDS.md中的所有规范！**

## 🏗️ 核心架构

### 微服务模块 (evcs-*)
- **evcs-auth**: 认证授权服务 (8081) - JWT认证、权限管理
- **evcs-gateway**: API网关 (8080) - 路由、限流、安全防护
- **evcs-station**: 充电站管理 (8082) - 站点、充电桩控制
- **evcs-order**: 订单管理 (8083) - 充电订单、计费方案
- **evcs-payment**: 支付服务 (8084) - 支付宝/微信支付集成
- **evcs-protocol**: 协议处理 (8085) - OCPP、云快充协议
- **evcs-tenant**: 租户管理 (8086) - 多租户隔离
- **evcs-monitoring**: 监控服务 (8087) - 系统监控
- **evcs-config**: 配置中心 (8888) - 配置管理
- **evcs-eureka**: 服务注册中心 (8761) - 服务发现
- **evcs-common**: 公共组件 - 共享工具类、实体基类

## 🔧 技术栈要求

### 后端技术栈
- **Spring Boot**: 3.2.10 (最新稳定版)
- **Java**: 21 (LTS版本)
- **Spring Security**: JWT认证 + RBAC权限控制
- **MyBatis Plus**: ORM框架 + 多租户支持
- **PostgreSQL**: 15 (主数据库)
- **Redis**: 7 (缓存 + 会话存储)
- **RabbitMQ**: 3 (消息队列)
- **Gradle**: 8.11.1 (构建工具)

## 🏢 多租户架构 (重点)

### 数据隔离链路
```
HTTP请求 → JWT解析 → TenantContext设置 → MyBatis Plus拦截器 → SQL自动添加tenant_id
```

### 关键组件
- **TenantContext**: 线程本地租户上下文管理
- **CustomTenantLineHandler**: MyBatis Plus租户行级过滤器
- **@DataScope**: 声明式数据权限注解
- **BaseEntity**: 包含tenant_id、审计字段的实体基类

### 租户使用规范
```java
// ✅ 正确：设置租户上下文
@Service
public class OrderService {
    public void createOrder(CreateOrderRequest request) {
        try {
            TenantContext.setCurrentTenantId(getTenantIdFromToken());
            // 业务逻辑
        } finally {
            TenantContext.clear();
        }
    }
}

// ✅ 正确：使用数据权限注解
@DataScope(DataScopeType.TENANT)
public List<Order> getOrders() {
    return orderMapper.selectList(null);
}
```

## 📝 代码生成规范

### 严格遵循的架构模式
```java
// Controller层 - 只处理HTTP请求
@RestController
@RequestMapping("/api/v1/orders")
@Validated
@Slf4j
public class OrderController {
    @PostMapping
    public Result<OrderDTO> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.create(request));
    }
}

// Service层 - 业务逻辑 + 事务管理
@Service
@Transactional
@Slf4j
public class OrderService {
    @Cacheable(value = "orders", key = "#id")
    @DataScope(DataScopeType.TENANT)
    public OrderDTO getById(Long id) {
        Order order = orderMapper.selectById(id);
        return OrderDTO.fromEntity(order);
    }
}

// Entity层 - 数据模型
@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(callSuper = true)
public class Order extends BaseEntity {
    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
}
```

### 必须使用的注解
```java
// 所有Service类
@Service
@Transactional
@Slf4j

// 查询方法
@Cacheable(value = "cacheName", key = "#param")
@DataScope(DataScopeType.TENANT)

// 更新方法
@CacheEvict(value = "cacheName", allEntries = true)
@Transactional

// Controller方法
@Valid @RequestBody
@PreAuthorize("hasPermission('order', 'create')")
```

## 🚫 严格禁止的模式

### 跨服务数据访问
```java
// ❌ 禁止：跨服务直接访问数据库
@Service
public class OrderService {
    @Autowired
    private UserRepository userRepo; // 跨服务访问
}
```

### 硬编码配置
```java
// ❌ 禁止：硬编码敏感信息
String dbUrl = "jdbc:postgresql://localhost:5432/evcs";
String jwtSecret = "my-secret-key";
String alipayAppId = "2021000000000000";
```

### 忽略异常处理
```java
// ❌ 禁止：空catch块
try {
    riskyOperation();
} catch (Exception e) {
    // 空catch块
}
```

### 直接使用System.out
```java
// ❌ 禁止：使用System.out
System.out.println("Debug info");
// ✅ 正确：使用日志
log.info("Debug info: {}", param);
```

## 🚀 小规模业务优化 (当前重点)

### JVM配置优化
```yaml
# 小规模业务推荐配置
JAVA_OPTS: >
  -Xms256m -Xmx512m
  -XX:+UseContainerSupport
  -XX:MaxRAMPercentage=60.0
  -server
```

### 开发环境启动
```bash
# 推荐使用核心开发环境 (6个服务，~2GB内存)
docker-compose -f docker-compose.core-dev.yml up -d

# 检查服务状态
docker-compose -f docker-compose.core-dev.yml ps
```

### 缓存策略
```java
// ✅ 正确：使用Spring Cache
@Cacheable(value = "stations", key = "#tenantId + '_' + #page")
public PageResult<StationDTO> getStations(int page, int size) {
    // 实现
}
```

## 🧪 测试规范

### 单元测试模板
```java
@ExtendWith(MockitoExtension.class)
@Slf4j
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("应该成功创建订单")
    void shouldCreateOrderSuccessfully() {
        // Given - 准备测试数据
        // When - 执行测试方法
        // Then - 验证结果
    }
}
```

### 测试覆盖率要求
- **Service层**: >= 80%
- **Controller层**: >= 70%
- **Repository层**: >= 60%

## 🔧 构建和部署

### 构建命令
```bash
# 全量构建 (包含测试)
./gradlew build

# 快速构建 (跳过测试)
./gradlew build -x test

# 单模块运行
./gradlew :evcs-auth:bootRun

# 单模块测试
./gradlew :evcs-auth:test
```

### Docker配置
```yaml
# 所有服务统一使用Dockerfile (已移除Dockerfile.simple)
# 统一基础镜像: eclipse-temurin:21-jre-alpine
# 统一Gradle版本: gradle:8.11-jdk21-alpine
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

## 🔄 协议事件流

### RabbitMQ消息架构
```java
// 协议服务发布事件
@RabbitListener(queues = "evcs.protocol.events")
public class ProtocolEventConsumer {
    public void handleProtocolEvent(ProtocolEvent event) {
        // 设置租户上下文
        TenantContext.setCurrentTenantId(event.getTenantId());
        try {
            // 处理事件
        } finally {
            TenantContext.clear();
        }
    }
}
```

## 📚 重要文档链接

- [部署指南](docs/quick-start/DEPLOYMENT-GUIDE.md)
- [项目结构说明](docs/operations/PROJECT-STRUCTURE.md)
- [AI助手规范](docs/02-development/AI-ASSISTANT-GUIDELINES.md)
- [故障排除](docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md)

## 🎯 当前开发重点

1. **小规模业务优化**: 内存使用控制在2GB以内
2. **启动速度优化**: 核心服务2分钟内启动完成
3. **代码规范化**: 严格遵循微服务架构模式
4. **AI助手集成**: 确保生成的代码符合项目规范

通过遵循这些指南，GitHub Copilot可以生成高质量、符合EVCS项目标准的一致性代码。

