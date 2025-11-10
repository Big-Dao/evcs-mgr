# EVCS项目代码质量检查清单

> **版本**: v1.1 | **最后更新**: 2025-11-10 | **维护者**: 技术负责人 | **状态**: 活跃
>
> ✅ **用途**: 统一代码审查、质量保障与自动化验收要求

## 🎯 检查清单概述

本检查清单确保EVCS项目的代码质量、安全性和性能符合标准，适用于所有AI编程助手和开发人员。

## 📋 检查清单分类

### 🔴 强制要求 (必须满足)

#### 架构和设计
- [ ] **微服务边界**: 严格遵循服务边界，不得跨服务直接访问数据库
- [ ] **DDD分层**: Controller → Service → Repository → Entity分层清晰
- [ ] **循环依赖**: 不存在模块间的循环依赖
- [ ] **单一职责**: 每个类和方法职责单一明确

#### 安全要求
- [ ] **输入验证**: 所有API输入使用`@Valid`或`@Validated`验证
- [ ] **认证授权**: 所有API包含`@PreAuthorize`权限检查
- [ ] **密码加密**: 使用`BCryptPasswordEncoder`加密密码
- [ ] **敏感信息**: 敏感配置使用环境变量，不硬编码
- [ ] **SQL安全**: 使用参数化查询，防止SQL注入
- [ ] **JWT安全**: JWT密钥使用环境变量配置

#### 事务和数据一致性
- [ ] **事务管理**: 更新操作使用`@Transactional`注解
- [ ] **缓存一致性**: 更新操作使用`@CacheEvict`失效缓存
- [ ] **租户隔离**: 多租户数据隔离正确实现
- [ ] **审计字段**: 实体包含创建时间、更新时间等审计字段

#### 异常处理
- [ ] **全局异常处理**: 使用`@ControllerAdvice`统一异常处理
- [ ] **业务异常**: 自定义业务异常，不抛出原始异常
- [ ] **日志记录**: 异常情况正确记录日志
- [ ] **空catch块**: 不允许空的catch块

#### 代码规范
- [ ] **命名规范**: 遵循Java命名约定
- [ ] **注释要求**: 公共API包含JavaDoc注释
- [ ] **代码格式**: 遵循项目代码格式规范
- [ ] **魔法值**: 不使用魔法数字和字符串

### 🟡 重要要求 (强烈推荐)

#### 性能优化
- [ ] **N+1查询**: 避免N+1查询问题
- [ ] **缓存使用**: 合理使用Spring Cache
- [ ] **分页查询**: 大数据量查询使用分页
- [ ] **异步处理**: 耗时操作使用`@Async`
- [ ] **资源管理**: 使用try-with-resources管理资源
- [ ] **数据库连接**: 合理配置数据库连接池

#### 测试覆盖
- [ ] **单元测试**: Service层单元测试覆盖率 >= 80%
- [ ] **集成测试**: Controller层集成测试覆盖率 >= 70%
- [ ] **边界测试**: 包含边界条件和异常情况测试
- [ ] **测试命名**: 测试方法命名清晰，使用`@DisplayName`

#### 日志和监控
- [ ] **日志级别**: 合理使用日志级别
- [ ] **日志内容**: 日志信息包含必要的上下文
- [ ] **性能监控**: 关键操作包含性能监控
- [ ] **健康检查**: 实现Spring Boot Actuator健康检查

### 🟢 建议要求 (推荐遵循)

#### 代码可维护性
- [ ] **方法长度**: 单个方法不超过50行
- [ ] **类长度**: 单个类不超过500行
- [ ] **参数数量**: 方法参数不超过5个
- [ ] **复杂度**: 避免过于复杂的嵌套逻辑

#### 文档和注释
- [ ] **README更新**: 新功能更新相关文档
- [ ] **API文档**: 重要API包含使用示例
- [ ] **配置说明**: 复杂配置包含说明注释
- [ ] **变更日志**: 重要变更记录在CHANGELOG中

## 🔍 详细检查标准

### Controller层检查

#### ✅ 正确实现
```java
@RestController
@RequestMapping("/api/v1/orders")
@Validated
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasPermission('order', 'create')")
    public ResponseEntity<ApiResponse<OrderDTO>> create(
            @Valid @RequestBody CreateOrderRequest request) {
        try {
            OrderDTO result = orderService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result));
        } catch (BusinessException e) {
            log.warn("创建订单失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("创建订单异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("系统内部错误"));
        }
    }
}
```

#### ❌ 常见错误
```java
// 错误1: 缺少权限检查
@PostMapping
public ResponseEntity<?> create(@RequestBody CreateOrderRequest request) {
    // 缺少@PreAuthorize注解
}

// 错误2: 缺少输入验证
@PostMapping
public ResponseEntity<?> create(@RequestBody CreateOrderRequest request) {
    // 缺少@Valid注解
}

// 错误3: 异常处理不当
@PostMapping
public ResponseEntity<?> create(@RequestBody CreateOrderRequest request) {
    try {
        return ResponseEntity.ok(orderService.create(request));
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build(); // 信息丢失
    }
}

// 错误4: 业务逻辑在Controller
@PostMapping
public ResponseEntity<?> create(@RequestBody CreateOrderRequest request) {
    if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        return ResponseEntity.badRequest().build(); // 业务逻辑应该在Service层
    }
}
```

### Service层检查

#### ✅ 正确实现
```java
@Service
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "orders", key = "#id")
    @DataScope(DataScopeType.TENANT)
    public OrderDTO getById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException("订单不存在: " + id));
        return OrderDTO.fromEntity(order);
    }

    @CacheEvict(value = "orders", allEntries = true)
    @Transactional
    public OrderDTO create(CreateOrderRequest request) {
        // 参数验证
        validateCreateRequest(request);

        // 检查重复
        if (orderRepository.existsByOrderNo(request.getOrderNo())) {
            throw new BusinessException("订单号已存在: " + request.getOrderNo());
        }

        Order order = Order.builder()
            .orderNo(request.getOrderNo())
            .amount(request.getAmount())
            .status(OrderStatus.PENDING)
            .tenantId(TenantContext.getCurrentTenantId())
            .build();

        order = orderRepository.save(order);
        log.info("创建订单成功: {}", order.getId());

        return OrderDTO.fromEntity(order);
    }

    private void validateCreateRequest(CreateOrderRequest request) {
        if (StringUtils.isBlank(request.getOrderNo())) {
            throw new ValidationException("订单号不能为空");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("订单金额必须大于0");
        }
    }
}
```

#### ❌ 常见错误
```java
// 错误1: 缺少事务管理
@Service
public class OrderService {
    public OrderDTO create(CreateOrderRequest request) {
        // 缺少@Transactional注解
    }
}

// 错误2: 缺少缓存管理
@Service
@Transactional
public class OrderService {
    public OrderDTO create(CreateOrderRequest request) {
        // 缺少@CacheEvict注解
    }
}

// 错误3: 直接返回实体
@Service
@Transactional
public class OrderService {
    public Order create(CreateOrderRequest request) {
        // 直接返回实体，应该返回DTO
        return orderRepository.save(order);
    }
}

// 错误4: 跨服务访问数据库
@Service
@Transactional
public class OrderService {
    @Autowired
    private UserRepository userRepository; // 跨服务访问
}
```

### Entity层检查

#### ✅ 正确实现
```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_no", columnList = "order_no"),
    @Index(name = "idx_tenant_id", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Order extends BaseEntity {

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        // 业务逻辑验证
        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new IllegalStateException("结束时间不能早于开始时间");
        }
    }
}
```

#### ❌ 常见错误
```java
// 错误1: 缺少审计字段
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private Long id;
    // 缺少created_at, updated_at等审计字段
}

// 错误2: 缺少租户字段
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    // 缺少tenant_id字段
}

// 错误3: 使用不当的数据类型
@Entity
@Table(name = "orders")
public class Order {
    @Column(name = "amount")
    private String amount; // 金额应该使用BigDecimal
}
```

### 测试代码检查

#### ✅ 正确实现
```java
@ExtendWith(MockitoExtension.class)
@Slf4j
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("应该成功创建订单")
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
            .orderNo("ORDER-2024-001")
            .amount(new BigDecimal("100.00"))
            .build();

        Order savedOrder = Order.builder()
            .id(1L)
            .orderNo("ORDER-2024-001")
            .amount(new BigDecimal("100.00"))
            .status(OrderStatus.PENDING)
            .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderRepository.existsByOrderNo(anyString())).thenReturn(false);

        // Set tenant context
        TenantContext.setCurrentTenantId(1L);

        // When
        OrderDTO result = orderService.create(request);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderNo()).isEqualTo("ORDER-2024-001");
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

        verify(orderRepository).save(any(Order.class));
        verify(orderRepository).existsByOrderNo("ORDER-2024-001");

        // Cleanup
        TenantContext.clear();
    }

    @Test
    @DisplayName("订单号重复时应该抛出异常")
    void shouldThrowExceptionWhenOrderNoExists() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
            .orderNo("DUPLICATE-ORDER")
            .amount(new BigDecimal("50.00"))
            .build();

        when(orderRepository.existsByOrderNo("DUPLICATE-ORDER")).thenReturn(true);
        TenantContext.setCurrentTenantId(1L);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> orderService.create(request));

        assertThat(exception.getMessage()).contains("订单号已存在");

        TenantContext.clear();
    }
}
```

#### ❌ 常见错误
```java
// 错误1: 缺少测试场景
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Test
    void testCreate() {
        // 测试场景不完整，缺少边界条件和异常情况
    }
}

// 错误2: 不设置测试上下文
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Test
    void shouldCreateOrder() {
        // 缺少TenantContext设置
        orderService.create(request);
    }
}

// 错误3: 验证不充分
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Test
    void shouldCreateOrder() {
        // When
        orderService.create(request);

        // Then - 缺少断言验证
    }
}
```

## 🔧 自动化检查工具

### Gradle检查插件
```gradle
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot'
    id 'checkstyle'
    id 'pmd'
    id 'com.github.spotbugs'
}

checkstyle {
    toolVersion = '10.12.4'
    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
}

pmd {
    toolVersion = '6.55.0'
    ruleSetFiles = files("${rootDir}/config/pmd/ruleset.xml")
}

spotbugs {
    toolVersion = '4.7.3'
    ignoreFailures = false
}
```

### 质量门禁配置
```yaml
# .github/workflows/quality-check.yml
name: Code Quality Check

on: [push, pull_request]

jobs:
  quality-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Checkstyle
        run: ./gradlew checkstyleMain checkstyleTest

      - name: Run PMD
        run: ./gradlew pmdMain pmdTest

      - name: Run SpotBugs
        run: ./gradlew spotbugsMain spotbugsTest

      - name: Run Tests
        run: ./gradlew test

      - name: Generate Test Coverage Report
        run: ./gradlew jacocoTestReport

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
```

## 📊 质量指标

### 目标指标
- **代码覆盖率**: >= 80%
- **代码重复率**: <= 3%
- **圈复杂度**: <= 10
- **方法长度**: <= 50行
- **类长度**: <= 500行
- **技术债务**: <= 1天

### 监控指标
- **构建成功率**: 100%
- **单元测试通过率**: 100%
- **集成测试通过率**: 100%
- **代码质量评分**: >= A级

## 🚀 持续改进

### 定期审查
- **每周**: 代码质量指标回顾
- **每月**: 技术债务评估
- **每季度**: 质量标准更新

### 培训和分享
- **新员工**: 代码质量培训
- **团队分享**: 质量最佳实践分享
- **外部学习**: 引入新的质量工具和方法

通过遵循这个检查清单，可以确保EVCS项目的代码质量、安全性和性能达到企业级标准。
