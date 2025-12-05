# AI编程助手项目规范指南
 
> **版本**: v1.1 | **最后更新**: 2025-11-10 | **维护者**: 技术负责人 | **状态**: 活跃
>
> 🤖 **用途**: 规范所有 AI 编程助手在 EVCS 项目中的使用方式

## 🎯 适用范围

本规范适用于以下AI编程助手在EVCS项目中的使用：
- 🤖 **Claude Code** (主要)
- 🧠 **GitHub Copilot**
- 🛠️ **Cursor**
- 🔧 **Tabnine**
- 📝 **CodeLlama**

## 📋 核心规范原则

### 1. 项目架构遵循
- **严格遵循微服务架构**，不得跨模块创建循环依赖
- **遵守DDD分层架构**：Controller → Service → Repository → Entity
- **保持Spring Boot最佳实践**
- **API设计遵循RESTful规范**

### 2. 代码风格统一
- **Java代码**: 遵循Google Java Style Guide
- **JavaScript/TypeScript**: 遵循Airbnb Style Guide
- **YAML配置**: 使用2空格缩进，保持一致性
- **SQL语句**: 使用大写关键字，表名和字段名使用小写

### 3. 安全规范
- **密码加密**: 使用BCryptPasswordEncoder
- **API安全**: 所有API必须通过网关，实现认证授权
- **敏感信息**: 不得硬编码，使用环境变量或配置中心
- **SQL注入**: 使用MyBatis参数化查询

### 4. 性能规范
- **数据库查询**: 避免N+1问题，合理使用JOIN
- **缓存策略**: Redis缓存使用Spring Cache注解
- **异步处理**: 耗时操作使用@Async注解
- **资源管理**: 使用try-with-resources管理资源

## 🏗️ 项目架构规范

### 微服务分层结构
```
每个微服务必须遵循以下分层：
├── controller/          # 控制器层 - 处理HTTP请求
├── service/            # 业务逻辑层 - 业务规则处理
├── repository/         # 数据访问层 - 数据库操作
├── entity/            # 实体层 - 数据模型
├── dto/               # 数据传输对象 - API输入输出
├── config/            # 配置类 - Spring配置
└── exception/         # 异常处理 - 自定义异常
```

### 命名规范
```java
// Controller命名
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    // 方法名使用动词+名词
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 实现
    }
}

// Service命名
@Service
public class AuthService {
    // 方法名使用动词
    public User authenticateUser(String username, String password) {
        // 实现
    }
}

// Entity命名
@Entity
@Table(name = "users")
public class User {
    // 字段使用驼峰命名
    @Column(name = "user_name")
    private String userName;
}
```

## 🔧 AI助手使用规范

### Claude Code使用指南
```markdown
当使用Claude Code时，必须：

1. **明确上下文**: 提供相关的文件路径和业务背景
2. **遵循现有模式**: 参考项目中已有的实现模式
3. **保持一致性**: 与现有代码风格保持一致
4. **测试优先**: 生成代码时必须考虑可测试性
5. **错误处理**: 必须包含适当的异常处理
```

### GitHub Copilot配置
```json
// .github/copilot-instructions.md
# EVCS项目Copilot指令

## 技术栈要求
- Spring Boot 3.2.10, Java 21
- Spring Security + JWT认证
- MyBatis Plus + PostgreSQL
- Redis缓存 + RabbitMQ消息队列

## 代码规范
- 严格遵循微服务架构
- 使用Spring Boot最佳实践
- API必须包含异常处理
- 数据库操作使用事务管理
- 所有公共方法必须有JavaDoc

## 禁止事项
- 不得跨服务直接调用数据库
- 不得硬编码敏感信息
- 不得使用System.out.println
- 不得忽略异常处理
```

## 📝 代码模板规范

### Controller模板
```java
@RestController
@RequestMapping("/api/v1/{service-name}")
@Validated
@Slf4j
public class {ServiceName}Controller {

    private final {ServiceName}Service {serviceName}Service;

    public {ServiceName}Controller({ServiceName}Service {serviceName}Service) {
        this.{serviceName}Service = {serviceName}Service;
    }

    @GetMapping
    public ResponseEntity<PageResult<{ServiceName}DTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PageResult<{ServiceName}DTO> result = {serviceName}Service.list(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to list {serviceName}", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<{ServiceName}DTO> create(
            @Valid @RequestBody Create{ServiceName}Request request) {
        try {
            {ServiceName}DTO result = {serviceName}Service.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Failed to create {serviceName}", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
```

### Service模板
```java
@Service
@Transactional
@Slf4j
public class {ServiceName}Service {

    private final {ServiceName}Repository {serviceName}Repository;
    private final RedisTemplate<String, Object> redisTemplate;

    public {ServiceName}Service({ServiceName}Repository {serviceName}Repository,
                                RedisTemplate<String, Object> redisTemplate) {
        this.{serviceName}Repository = {serviceName}Repository;
        this.redisTemplate = redisTemplate;
    }

    @Cacheable(value = "{serviceName}", key = "#page + '_' + #size")
    public PageResult<{ServiceName}DTO> list(int page, int size) {
        Page<{ServiceName}> entityPage = {serviceName}Repository.findAll(
            PageRequest.of(page - 1, size, Sort.by("id").descending())
        );
        return PageResult.of(entityPage, {ServiceName}DTO::fromEntity);
    }

    @CacheEvict(value = "{serviceName}", allEntries = true)
    @Transactional
    public {ServiceName}DTO create(Create{ServiceName}Request request) {
        {ServiceName} entity = new {ServiceName}();
        // 设置属性
        BeanUtils.copyProperties(request, entity);

        entity = {serviceName}Repository.save(entity);
        log.info("Created {serviceName}: {}", entity.getId());

        return {ServiceName}DTO.fromEntity(entity);
    }
}
```

### Entity模板
```java
@Entity
@Table(name = "{table_name}")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {ServiceName} {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

## 🧪 测试规范

### 单元测试模板
```java
@ExtendWith(MockitoExtension.class)
@Slf4j
class {ServiceName}ServiceTest {

    @Mock
    private {ServiceName}Repository {serviceName}Repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private {ServiceName}Service {serviceName}Service;

    @Test
    @DisplayName("应该成功创建{serviceName}")
    void shouldCreate{ServiceName}Successfully() {
        // Given
        Create{ServiceName}Request request = Create{ServiceName}Request.builder()
            .name("Test")
            .build();

        {ServiceName} savedEntity = {ServiceName}.builder()
            .id(1L)
            .name("Test")
            .build();

        when({serviceName}Repository.save(any({ServiceName}.class)))
            .thenReturn(savedEntity);

        // When
        {ServiceName}DTO result = {serviceName}Service.create(request);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test");

        verify({serviceName}Repository).save(any({ServiceName}.class));
    }
}
```

## 🔍 代码审查检查点

### 必须检查项
- [ ] 是否遵循微服务架构原则
- [ ] 是否包含适当的异常处理
- [ ] 是否使用了Spring Cache注解
- [ ] 是否包含事务管理(@Transactional)
- [ ] 是否包含日志记录(@Slf4j)
- [ ] 是否包含单元测试
- [ ] 是否包含JavaDoc注释
- [ ] 是否符合命名规范

### 安全检查项
- [ ] 敏感信息是否使用环境变量
- [ ] API是否包含认证检查
- [ ] 输入参数是否包含验证(@Valid)
- [ ] SQL操作是否使用参数化查询
- [ ] 密码是否使用加密存储

### 性能检查项
- [ ] 数据库查询是否避免N+1问题
- [ ] 是否合理使用缓存
- [ ] 异步操作是否使用@Async
- [ ] 资源是否正确释放

## 🚨 禁止模式

### 严格禁止的代码模式
```java
// ❌ 禁止：跨服务直接访问数据库
@Autowired
private OtherServiceRepository otherRepository;

// ❌ 禁止：硬编码敏感信息
String password = "admin123";

// ❌ 禁止：使用System.out.println
System.out.println("Debug info");

// ❌ 禁止：忽略异常处理
try {
    // risky operation
} catch (Exception e) {
    // empty catch block
}

// ❌ 禁止：在Controller中包含业务逻辑
@PostMapping
public ResponseEntity<?> create(@RequestBody Request request) {
    // Business logic should be in Service layer
    if (request.getName().length() < 3) {
        return ResponseEntity.badRequest().build();
    }
}
```

## 📋 质量检查清单

在提交代码前，AI助手必须确认以下检查项：

### 功能性检查
- [ ] 功能需求是否完整实现
- [ ] 边界条件是否处理
- [ ] 异常情况是否考虑
- [ ] 业务逻辑是否正确

### 非功能性检查
- [ ] 代码是否可维护
- [ ] 性能是否可接受
- [ ] 安全性是否满足要求
- [ ] 测试覆盖率是否足够

### 规范性检查
- [ ] 代码风格是否一致
- [ ] 注释是否充分
- [ ] 命名是否规范
- [ ] 架构是否遵循项目规范

## 🔧 工具配置

### IDE配置(.editorconfig)
```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.java]
indent_style = space
indent_size = 4

[*.{yml,yaml}]
indent_style = space
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

### ESLint配置(用于前端)
```json
{
  "extends": ["@typescript-eslint/recommended"],
  "rules": {
    "no-console": "error",
    "prefer-const": "error",
    "no-var": "error"
  }
}
```

通过遵循这些规范，AI编程助手可以生成高质量、一致性强、符合项目要求的代码，大大提高开发效率和代码质量。
