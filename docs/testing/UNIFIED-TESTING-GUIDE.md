# EVCS充电站管理系统 - 统一测试指南

> **版本**: v1.1 | **最后更新**: 2025-11-10 | **维护者**: 测试负责人 | **状态**: 活跃
>
> 📋 **唯一权威测试文档**：覆盖单元、集成、端到端与回归测试规范

## 🎯 概述

本文档提供EVCS充电站管理平台的完整测试框架指南，涵盖从单元测试到集成测试的所有测试类型和最佳实践。

### 测试目标
- **代码覆盖率**: Service层 ≥ 80%, Controller层 ≥ 70%, Repository层 ≥ 60%
- **测试质量**: 100%测试通过率，零测试失败
- **测试速度**: 单元测试执行时间 < 5分钟，集成测试 < 15分钟

## 🛠️ 测试框架栈

### 核心测试工具
- **JUnit 5**: 核心测试框架
- **Spring Boot Test**: Spring Boot测试支持
- **MockMvc**: Controller层测试
- **Mockito**: Mock对象框架
- **H2 Database**: 内存数据库用于测试
- **TestContainers**: 容器化集成测试（可选）
- **JaCoCo**: 代码覆盖率分析

### 测试依赖
```gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.mockito:mockito-core'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'com.h2database:h2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

## 📋 测试类型

### 1. 单元测试（Unit Tests）

**目的**: 测试单个类或方法的功能，不依赖Spring上下文
**特点**: 快速执行，隔离性好，数量最多

```java
@ExtendWith(MockitoExtension.class)
@Slf4j
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("应该成功创建订单")
    void shouldCreateOrderSuccessfully() {
        // Given - 准备测试数据
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

        // When - 执行测试方法
        OrderDTO result = orderService.create(request);

        // Then - 验证结果
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

### 2. 集成测试（Integration Tests）

**目的**: 测试多个组件协同工作，依赖Spring上下文
**特点**: 更真实的测试环境，包含数据库和外部服务

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional
class OrderControllerIntegrationTest {

    @Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("evcs_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("应该成功创建和查询订单")
    void shouldCreateAndRetrieveOrder() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
            .orderNo("INTEGRATION-001")
            .amount(new BigDecimal("200.00"))
            .build();

        // When - 创建订单
        ResponseEntity<ApiResponse<OrderDTO>> createResponse = restTemplate.postForEntity(
            "/api/v1/orders", request, new ParameterizedTypeReference<ApiResponse<OrderDTO>>() {});

        // Then - 验证创建结果
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().isSuccess()).isTrue();

        OrderDTO createdOrder = createResponse.getBody().getData();
        assertThat(createdOrder.getOrderNo()).isEqualTo("INTEGRATION-001");

        // When - 查询订单
        ResponseEntity<ApiResponse<OrderDTO>> getResponse = restTemplate.getForEntity(
            "/api/v1/orders/" + createdOrder.getId(),
            new ParameterizedTypeReference<ApiResponse<OrderDTO>>() {});

        // Then - 验证查询结果
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getData().getOrderNo()).isEqualTo("INTEGRATION-001");
    }
}
```

### 3. Controller层测试

**目的**: 测试REST API端点
**特点**: 使用MockMvc，不启动完整服务器

```java
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtUtil.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("创建订单 - 成功")
    @WithMockUser(roles = {"ADMIN"})
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
            .orderNo("CONTROLLER-001")
            .amount(new BigDecimal("300.00"))
            .build();

        OrderDTO mockResponse = OrderDTO.builder()
            .id(1L)
            .orderNo("CONTROLLER-001")
            .amount(new BigDecimal("300.00"))
            .status(OrderStatus.PENDING)
            .build();

        when(orderService.create(any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value("CONTROLLER-001"))
                .andExpect(jsonPath("$.data.amount").value(300.00));
    }

    @Test
    @DisplayName("创建订单 - 输入验证失败")
    void shouldReturnBadRequestWhenInvalidInput() throws Exception {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
            .orderNo("")  // 无效的订单号
            .amount(new BigDecimal("-100"))  // 无效的金额
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
```

## 🔧 测试工具类

### 1. 测试数据构建器

```java
public class TestDataBuilder {

    public static Order createTestOrder() {
        return Order.builder()
            .id(1L)
            .orderNo("TEST-ORDER-" + System.currentTimeMillis())
            .amount(new BigDecimal("100.00"))
            .status(OrderStatus.PENDING)
            .tenantId(1L)
            .createdAt(LocalDateTime.now())
            .build();
    }

    public static CreateOrderRequest createOrderRequest() {
        return CreateOrderRequest.builder()
            .orderNo("TEST-ORDER-" + System.currentTimeMillis())
            .amount(new BigDecimal("100.00"))
            .stationId(1L)
            .userId(1L)
            .build();
    }

    public static Station createTestStation() {
        return Station.builder()
            .id(1L)
            .name("测试充电站")
            .address("测试地址")
            .capacity(10)
            .status(StationStatus.ACTIVE)
            .tenantId(1L)
            .build();
    }
}
```

### 2. 租户上下文工具

```java
public class TenantTestUtils {

    private static final Long DEFAULT_TENANT_ID = 1L;

    @BeforeEach
    void setUpTenantContext() {
        TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    public static void withTenant(Long tenantId, Runnable runnable) {
        try {
            TenantContext.setCurrentTenantId(tenantId);
            runnable.run();
        } finally {
            TenantContext.clear();
        }
    }
}
```

### 3. 数据库测试工具

```java
@TestConfiguration
public class TestDatabaseConfig {

    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:schema.sql")
            .addScript("classpath:test-data.sql")
            .build();
    }

    @Bean
    public TestDataInitializer testDataInitializer() {
        return new TestDataInitializer();
    }
}
```

## 📊 测试数据管理

### 1. 测试数据初始化

```java
@Component
public class TestDataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StationRepository stationRepository;

    @PostConstruct
    public void initializeTestData() {
        if (userRepository.count() == 0) {
            createTestUsers();
            createTestStations();
        }
    }

    private void createTestUsers() {
        User admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin"))
            .email("admin@test.com")
            .role(UserRole.ADMIN)
            .tenantId(1L)
            .build();
        userRepository.save(admin);
    }

    private void createTestStations() {
        Station station = Station.builder()
            .name("测试充电站")
            .address("测试地址")
            .capacity(10)
            .status(StationStatus.ACTIVE)
            .tenantId(1L)
            .build();
        stationRepository.save(station);
    }
}
```

### 2. 测试数据清理

```java
@AfterEach
void cleanTestData() {
    // 清理测试数据，保持数据库干净
    orderRepository.deleteAll();
    stationRepository.deleteAll();
    userRepository.deleteAll();
}
```

## 🚀 运行测试

### 1. 运行所有测试

```bash
# 运行所有测试
./gradlew test

# 运行特定模块测试
./gradlew :evcs-auth:test
./gradlew :evcs-order:test

# 并行运行测试（提高速度）
./gradlew test --parallel
```

### 2. 生成测试报告

```bash
# 生成测试覆盖率报告
./gradlew test jacocoTestReport

# 生成HTML测试报告
./gradlew test --continue

# 查看测试报告
open build/reports/tests/test/index.html
open build/reports/jacoco/test/html/index.html
```

### 3. 运行特定测试

```bash
# 运行单个测试类
./gradlew test --tests OrderServiceTest

# 运行特定测试方法
./gradlew test --tests OrderServiceTest.shouldCreateOrderSuccessfully

# 运行包含特定字符串的测试
./gradlew test --tests "*Order*"
```

## 📈 测试覆盖率

### 1. JaCoCo配置

```gradle
jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/entity/**',
                '**/dto/**',
                '**/vo/**',
                '**/*Application.class'
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            enabled = true
            element = 'BUNDLE'
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.80
            }
        }
    }
}
```

### 2. 覆盖率目标

| 层级 | 目标覆盖率 | 说明 |
|------|------------|------|
| Service层 | ≥ 80% | 核心业务逻辑必须充分测试 |
| Controller层 | ≥ 70% | API端点测试 |
| Repository层 | ≥ 60% | 数据访问层测试 |
| 整体项目 | ≥ 75% | 综合覆盖率 |

## 🎯 最佳实践

### 1. 测试命名规范

```java
// ✅ 好的测试方法命名
@Test
@DisplayName("应该成功创建订单当输入有效时")
void shouldCreateOrderSuccessfully_whenValidInput() { }

@Test
@DisplayName("应该抛出异常当订单号重复时")
void shouldThrowException_whenOrderNoIsDuplicate() { }

// ❌ 避免的命名方式
@Test
void test1() { }
@Test
void createOrderTest() { }
```

### 2. 测试结构（Given-When-Then）

```java
@Test
@DisplayName("应该成功创建订单")
void shouldCreateOrderSuccessfully() {
    // Given - 准备测试数据
    CreateOrderRequest request = createValidOrderRequest();
    when(orderRepository.save(any())).thenReturn(mockOrder());
    TenantContext.setCurrentTenantId(1L);

    // When - 执行测试方法
    OrderDTO result = orderService.create(request);

    // Then - 验证结果
    assertThat(result.getOrderNo()).isEqualTo(request.getOrderNo());
    verify(orderRepository).save(any(Order.class));

    // Cleanup - 清理资源
    TenantContext.clear();
}
```

### 3. Mock使用原则

```java
// ✅ 正确：Mock外部依赖
@Mock
private OrderRepository orderRepository;  // 数据访问层

@Mock
private PaymentServiceClient paymentClient;  // 外部服务

// ✅ 正确：不要Mock被测试类
@InjectMocks
private OrderServiceImpl orderService;  // 被测试类

// ❌ 错误：Mock值对象
@Mock
private OrderDTO orderDTO;  // 不要MockDTO
```

### 4. 断言使用原则

```java
// ✅ 使用AssertJ的流式断言
assertThat(result)
    .isNotNull()
    .extracting(OrderDTO::getOrderNo)
    .isEqualTo(expectedOrderNo);

// ✅ 验证异常
assertThrows(BusinessException.class,
    () -> orderService.create(invalidRequest));

// ✅ 验证Mock调用
verify(orderRepository, times(1)).save(any(Order.class));
verifyNoMoreInteractions(orderRepository);
```

### 5. 测试隔离

```java
// ✅ 每个测试方法都有独立的上下文
@Test
void testMethod1() {
    try {
        TenantContext.setCurrentTenantId(1L);
        // 测试逻辑
    } finally {
        TenantContext.clear();
    }
}

@Test
void testMethod2() {
    try {
        TenantContext.setCurrentTenantId(2L);
        // 测试逻辑
    } finally {
        TenantContext.clear();
    }
}

// ✅ 使用@BeforeEach和@AfterEach
@BeforeEach
void setUp() {
    TenantContext.setCurrentTenantId(1L);
    // 其他初始化
}

@AfterEach
void tearDown() {
    TenantContext.clear();
    // 清理资源
}
```

## 🔧 持续集成

### 1. GitHub Actions配置

```yaml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-

    - name: Run tests
      run: ./gradlew test

    - name: Generate test report
      run: ./gradlew jacocoTestReport

    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./build/reports/jacoco/test/jacocoTestReport.xml
```

### 2. 质量门禁

```bash
#!/bin/bash
# quality-gate.sh

echo "🔍 执行质量门禁检查..."

# 运行测试
./gradlew test
if [ $? -ne 0 ]; then
    echo "❌ 测试失败，阻止部署"
    exit 1
fi

# 检查覆盖率
COVERAGE=$(./gradlew jacocoTestReport | grep -o "Total.*[0-9]*%" | grep -o '[0-9]*')
if [ "$COVERAGE" -lt 75 ]; then
    echo "❌ 测试覆盖率不足: ${COVERAGE}% < 75%"
    exit 1
fi

echo "✅ 所有检查通过，允许部署"
```

## 🐛 常见问题和解决方案

### 1. 测试启动慢

**问题**: Spring上下文启动时间过长
**解决方案**:
```java
@SpringBootTest(classes = {TestApplication.class})
@TestPropertySource(properties = {
    "spring.config.location=classpath:application-test.yml",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
```

### 2. 租户上下文问题

**问题**: 测试间租户上下文污染
**解决方案**:
```java
@ExtendWith(TenantContextExtension.class)
class OrderServiceTest {
    // 自动管理租户上下文
}
```

### 3. 数据库连接问题

**问题**: 测试数据库连接失败
**解决方案**:
```java
@Testcontainers
class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }
}
```

## 📚 相关文档

- [项目编码标准](../overview/PROJECT-CODING-STANDARDS.md)
- [AI助手测试规范](../development/AI-ASSISTANTS-INDEX.md)
- [构建指南](../deployment/)
- [故障排除指南](../troubleshooting/)

---

**通过遵循本测试指南，可以确保EVCS项目的高质量和可维护性。**
