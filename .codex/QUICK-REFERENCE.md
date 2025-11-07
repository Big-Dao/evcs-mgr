# CodeX EVCS项目快速参考

## 🚀 CodeX使用方法

### 方法1：GitHub Copilot（推荐）
- 已配置`.github/copilot-instructions.md`
- 自动读取项目规范
- 在IDE中实时提供代码建议

### 方法2：ChatGPT/Claude API调用
```python
import openai

def generate_evcs_code(requirement):
    context = """
    你正在为EVCS充电站管理系统编写代码。项目基于Spring Boot 3.2.10 + Java 21微服务架构。
    严格遵循多租户隔离、DDD分层架构。严禁跨服务数据库访问，必须使用DTO返回数据。

    必须注解：Controller(@RestController,@Validated,@Slf4j,@PreAuthorize)
              Service(@Service,@Transactional,@Slf4j,@Cacheable/@DataScope)
              Entity(extends BaseEntity, 包含tenant_id)

    严禁：硬编码敏感信息、空catch块、System.out.println、跨服务Repository注入
    """

    prompt = f"{context}\n\n请为以下需求生成代码：\n{requirement}"

    response = openai.Completion.create(
        engine="code-davinci-002",
        prompt=prompt,
        temperature=0.1,
        max_tokens=2000
    )

    return response.choices[0].text
```

## 📋 核心规范速查

### Controller层必备
```java
@RestController
@RequestMapping("/api/v1/stations")
@Validated
@Slf4j
public class StationController {

    @PostMapping
    @PreAuthorize("hasPermission('station', 'create')")
    public ResponseEntity<ApiResponse<StationDTO>> create(
            @Valid @RequestBody CreateStationRequest request) {
        // 实现
    }
}
```

### Service层必备
```java
@Service
@Transactional
@Slf4j
public class StationService {

    @Cacheable(value = "stations", key = "#id")
    @DataScope(DataScopeType.TENANT)
    public StationDTO getById(Long id) {
        // 实现
    }

    @CacheEvict(value = "stations", allEntries = true)
    @Transactional
    public StationDTO create(CreateStationRequest request) {
        // 实现
    }
}
```

### Entity层必备
```java
@Entity
@Table(name = "stations")
@Data
@EqualsAndHashCode(callSuper = true)
public class Station extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
    }
}
```

## ❌ 常见错误避免

### 1. 跨服务数据访问（严禁）
```java
// ❌ 错误
@Service
public class OrderService {
    @Autowired
    private UserRepository userRepo; // 违规！
}

// ✅ 正确：通过Feign客户端调用
@Service
public class OrderService {
    @Autowired
    private UserFeignClient userClient; // 正确
}
```

### 2. 硬编码敏感信息（严禁）
```java
// ❌ 错误
String dbUrl = "jdbc:postgresql://localhost:5432/evcs";
String jwtSecret = "my-secret-key";

// ✅ 正确：使用环境变量
@Value("${spring.datasource.url}")
private String dbUrl;

@Value("${app.jwt.secret}")
private String jwtSecret;
```

### 3. 异常处理不当（严禁）
```java
// ❌ 错误
try {
    riskyOperation();
} catch (Exception e) {
    // 空catch块
}

// ✅ 正确
try {
    riskyOperation();
} catch (BusinessException e) {
    log.warn("业务异常: {}", e.getMessage());
    throw e;
} catch (Exception e) {
    log.error("系统异常", e);
    throw new BusinessException("系统处理失败");
}
```

## ✅ 质量检查清单

生成代码后检查：

### 架构合规
- [ ] 遵循DDD分层架构
- [ ] 无跨服务数据库访问
- [ ] 正确实现租户隔离
- [ ] 包含必需注解

### 安全性
- [ ] 包含权限检查(@PreAuthorize)
- [ ] 输入验证(@Valid)
- [ ] 无硬编码敏感信息
- [ ] 异常处理完整

### 性能
- [ ] 使用缓存注解
- [ ] 避免N+1查询
- [ ] 合理使用异步处理
- [ ] 正确资源管理

### 测试
- [ ] 包含单元测试
- [ ] 覆盖率达标
- [ ] 测试边界条件
- [ ] 测试异常情况

## 🔗 相关文档

- [完整规范](docs/02-development/AI-ASSISTANT-GUIDELINES.md)
- [质量清单](docs/02-development/CODE-QUALITY-CHECKLIST.md)
- [GitHub Copilot配置](.github/copilot-instructions.md)
- [项目上下文](.codex/project-context.md)

通过以上配置和规范，CodeX可以为EVCS项目生成高质量、符合所有架构要求的代码。