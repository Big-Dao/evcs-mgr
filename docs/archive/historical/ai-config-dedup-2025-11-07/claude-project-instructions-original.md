# EVCS项目Claude Code使用指令

## 🎯 项目概述

这是一个基于Spring Boot微服务架构的电动汽车充电站管理系统（EVCS），专为小规模业务优化设计。

## 📋 完整规范

**🔥 重要：请首先阅读项目完整规范文档**: [PROJECT-CODING-STANDARDS.md](../../PROJECT-CODING-STANDARDS.md)

该文档包含了生成高质量代码所需的**所有规范要求**，包括：
- 强制架构规范和代码模板
- 必须使用的注解和禁止的模式
- 完整的质量检查清单
- 测试要求和性能优化标准

**在生成任何代码之前，请务必遵循PROJECT-CODING-STANDARDS.md中的所有规范！**

## 🏗️ 核心架构

### 微服务架构
- **evcs-gateway (8080)**: API网关，路由和安全防护
- **evcs-auth (8081)**: 认证授权服务，JWT + RBAC
- **evcs-station (8082)**: 充电站管理，设备控制
- **evcs-order (8083)**: 订单管理，计费方案
- **evcs-payment (8084)**: 支付服务，支付宝/微信
- **evcs-protocol (8085)**: 协议处理，OCPP/云快充
- **evcs-tenant (8086)**: 租户管理，多租户隔离
- **evcs-monitoring (8087)**: 监控服务，健康检查
- **evcs-config (8888)**: 配置中心，Git配置
- **evcs-eureka (8761)**: 服务注册中心
- **evcs-common**: 公共组件，共享工具类

### 技术栈
- **后端**: Spring Boot 3.2.10, Java 21
- **数据库**: PostgreSQL 15, Redis 7
- **消息队列**: RabbitMQ 3
- **安全**: Spring Security + JWT
- **ORM**: MyBatis Plus
- **缓存**: Spring Cache + Redis
- **构建**: Gradle 8.11.1
- **容器**: Docker + Docker Compose

## 🔧 代码规范要求

### 严格遵循的架构模式
1. **DDD分层架构**: Controller → Service → Repository → Entity
2. **微服务边界**: 不得跨服务直接访问数据库
3. **API设计**: 遵循RESTful规范，统一返回格式
4. **异常处理**: 使用全局异常处理器

### Java代码规范
```java
// 包命名规范
com.evcs.{service-name}.controller
com.evcs.{service-name}.service
com.evcs.{service-name}.repository
com.evcs.{service-name}.entity
com.evcs.{service-name}.dto
com.evcs.{service-name}.config
com.evcs.{service-name}.exception

// 类命名规范
Controller: {ServiceName}Controller
Service: {ServiceName}Service
Repository: {ServiceName}Repository
Entity: {ServiceName}
DTO: {ServiceName}DTO, Create{ServiceName}Request, Update{ServiceName}Request
Exception: {ServiceName}NotFoundException, {ServiceName}Exception
```

### 必须使用的注解
```java
@RestController
@RequestMapping("/api/v1/{service-name}")
@Slf4j
@Validated
public class {ServiceName}Controller {
    // 必须包含异常处理
    // 必须使用@Valid验证输入
    // 必须返回ResponseEntity
}

@Service
@Transactional
@Slf4j
public class {ServiceName}Service {
    // 业务逻辑方法必须使用@Transactional
    // 查询方法使用@Cacheable
    // 更新方法使用@CacheEvict
}

@Entity
@Table(name = "{table_name}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {ServiceName} {
    // 必须包含审计字段
    // 必须包含@PrePersist和@PreUpdate
}
```

## 🚀 开发工作流

### 环境启动
```bash
# 推荐使用核心开发环境
docker-compose -f docker-compose.core-dev.yml up -d

# 检查服务状态
docker-compose -f docker-compose.core-dev.yml ps

# 健康检查
./scripts/docker/health-check.sh
```

### 代码生成要求
1. **优先参考现有实现**: 在生成代码前，先查看项目中类似的实现
2. **保持一致性**: 与现有代码风格、命名规范保持一致
3. **包含测试**: 为新功能生成对应的单元测试
4. **添加文档**: 为公共API生成JavaDoc注释

### 安全要求
- **密码处理**: 必须使用BCryptPasswordEncoder
- **JWT配置**: 使用统一的JWT工具类
- **API认证**: 所有API必须通过网关认证
- **参数验证**: 使用@Valid注解验证输入参数
- **SQL安全**: 使用参数化查询，避免SQL注入

## 📁 项目文件结构

### 标准微服务结构
```
evcs-{service-name}/
├── src/main/java/com/evcs/{service-name}/
│   ├── controller/         # REST控制器
│   ├── service/           # 业务逻辑层
│   │   └── impl/         # 服务实现类
│   ├── repository/        # 数据访问层
│   ├── entity/           # JPA实体类
│   ├── dto/              # 数据传输对象
│   ├── config/           # Spring配置类
│   ├── exception/        # 自定义异常
│   └── util/             # 工具类
├── src/main/resources/
│   ├── application.yml   # 应用配置
│   └── mapper/           # MyBatis映射文件
├── src/test/java/        # 单元测试
├── build.gradle          # 构建配置
└── Dockerfile            # 容器配置
```

### 配置文件规范
```yaml
# application.yml
server:
  port: {port}

spring:
  application:
    name: evcs-{service-name}
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  redis:
    host: ${SPRING_DATA_REDIS_HOST}
    port: ${SPRING_DATA_REDIS_PORT}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL}
```

## 🧪 测试规范

### 单元测试要求
```java
@ExtendWith(MockitoExtension.class)
@Slf4j
class {ServiceName}ServiceTest {

    @Mock
    private {ServiceName}Repository {serviceName}Repository;

    @InjectMocks
    private {ServiceName}Service {serviceName}Service;

    @Test
    @DisplayName("测试场景描述")
    void shouldDoSomethingWhenCondition() {
        // Given - 准备测试数据
        // When - 执行测试方法
        // Then - 验证结果
    }
}
```

### 测试覆盖率要求
- **Service层**: 覆盖率 >= 80%
- **Controller层**: 覆盖率 >= 70%
- **Repository层**: 覆盖率 >= 60%

## 🔍 代码审查要点

### 必须检查项
- [ ] 是否遵循微服务架构原则
- [ ] 是否包含适当的异常处理
- [ ] 是否使用Spring Cache注解
- [ ] 是否包含事务管理
- [ ] 是否包含日志记录
- [ ] 是否包含单元测试
- [ ] 是否符合命名规范

### 性能考虑
- [ ] 是否避免N+1查询问题
- [ ] 是否合理使用缓存
- [ ] 是否包含分页查询
- [ ] 异步操作是否使用@Async

### 安全检查
- [ ] 敏感信息是否使用环境变量
- [ ] 输入参数是否包含验证
- [ ] SQL操作是否安全
- [ ] API是否包含认证检查

## 🚨 常见错误预防

### 避免的反模式
```java
// ❌ 跨服务直接数据库访问
@Autowired
private OtherServiceRepository otherRepository;

// ❌ 硬编码配置信息
String url = "http://localhost:8080";

// ❌ 忽略异常处理
try {
    riskyOperation();
} catch (Exception e) {
    // 空catch块
}

// ❌ Controller中包含业务逻辑
@PostMapping
public Result create(Request req) {
    if (req.getName().length() < 3) {  // 业务逻辑应该在Service层
        return Result.error("名称太短");
    }
}
```

### 推荐的最佳实践
```java
// ✅ 使用服务间调用
@Autowired
private OtherServiceClient otherServiceClient;

// ✅ 使用配置属性
@Value("${other.service.url}")
private String otherServiceUrl;

// ✅ 正确的异常处理
try {
    riskyOperation();
} catch (SpecificException e) {
    log.error("操作失败: {}", e.getMessage(), e);
    throw new BusinessException("操作失败", e);
}

// ✅ Controller只负责请求处理
@PostMapping
public Result create(@Valid @RequestBody Request req) {
    return {serviceName}Service.create(req);
}
```

## 📋 质量检查清单

在生成或修改代码时，请确认：

### 功能性
- [ ] 需求是否完整实现
- [ ] 边界条件是否处理
- [ ] 异常情况是否考虑
- [ ] 业务逻辑是否正确

### 技术性
- [ ] 代码是否可维护
- [ ] 性能是否可接受
- [ ] 安全性是否满足要求
- [ ] 测试是否充分

### 规范性
- [ ] 代码风格是否一致
- [ ] 注释是否充分
- [ ] 命名是否规范
- [ ] 架构是否遵循项目规范

## 📚 重要文档链接

- [项目结构说明](docs/operations/PROJECT-STRUCTURE.md)
- [部署指南](docs/quick-start/DEPLOYMENT-GUIDE.md)
- [服务参考](docs/quick-start/SERVICES-REFERENCE.md)
- [故障排除](docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md)

## 🔧 调试技巧

### 常用调试命令
```bash
# 查看服务日志
docker-compose logs -f {service-name}

# 进入容器调试
docker exec -it {container-name} bash

# 检查服务健康状态
curl http://localhost:{port}/actuator/health

# 查看服务注册情况
curl http://localhost:8761/eureka/apps
```

### 日志级别配置
```yaml
logging:
  level:
    com.evcs: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

通过遵循这些指令，Claude Code可以生成高质量、符合项目规范的一致性代码。