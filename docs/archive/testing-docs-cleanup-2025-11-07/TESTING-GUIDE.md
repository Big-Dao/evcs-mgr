# 测试指南

本文档提供EVCS Manager项目的测试框架使用指南和最佳实践。

## 目录
- [测试框架概述](#测试框架概述)
- [测试类型](#测试类型)
- [测试基类](#测试基类)
- [测试工具类](#测试工具类)
- [编写测试](#编写测试)
- [多租户测试](#多租户测试)
- [测试命名规范](#测试命名规范)
- [最佳实践](#最佳实践)

## 测试框架概述

项目使用以下测试框架和工具：
- **JUnit 5**: 核心测试框架
- **Spring Boot Test**: Spring Boot测试支持
- **MockMvc**: Controller层测试
- **H2 Database**: 内存数据库用于测试
- **TestContainers**: 容器化集成测试（可选）

## 测试类型

### 1. 单元测试（Unit Tests）
测试单个类或方法的功能，不依赖Spring上下文。

```java
class UtilityClassTest {
    @Test
    void testCalculation() {
        int result = Calculator.add(1, 2);
        assertEquals(3, result);
    }
}
```

### 2. Service层测试
测试业务逻辑层，使用Spring上下文。

```java
@SpringBootTest(classes = StationServiceApplication.class)
class StationServiceTest extends BaseServiceTest {
    @Resource
    private IStationService stationService;
    
    @Test
    void testSaveStation() {
        Station station = new Station();
        station.setStationCode("TEST001");
        // ... 设置其他属性
        
        boolean result = stationService.saveStation(station);
        assertTrue(result);
        assertNotNull(station.getStationId());
    }
}
```

### 3. Controller层测试
测试REST API接口。

```java
@SpringBootTest
class StationControllerTest extends BaseControllerTest {
    
    @Test
    void testGetStations() throws Exception {
        mockMvc.perform(get("/api/stations")
                .header("Authorization", "Bearer " + getTestToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

### 4. 集成测试（Integration Tests）
测试多个组件的集成。

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderFlowIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void testCompleteOrderFlow() {
        // 测试从订单创建到完成的完整流程
    }
}
```

## 测试基类

项目提供了多个测试基类，简化测试代码编写：

### BaseServiceTest
Service层测试基类，提供：
- 自动设置和清理租户上下文
- 事务回滚支持
- 租户切换方法

使用示例：
```java
@SpringBootTest(classes = YourApplication.class)
class YourServiceTest extends BaseServiceTest {
    
    @Override
    protected Long getTestTenantId() {
        return 2L; // 使用租户ID 2进行测试
    }
    
    @Test
    void testMethod() {
        // 租户上下文已自动设置
        // 测试代码...
    }
}
```

### BaseControllerTest
Controller层测试基类，提供：
- MockMvc支持
- ObjectMapper支持
- 租户上下文管理
- JSON转换方法

使用示例：
```java
@SpringBootTest
class YourControllerTest extends BaseControllerTest {
    
    @Test
    void testApi() throws Exception {
        YourRequest request = new YourRequest();
        // 设置请求参数
        
        mockMvc.perform(post("/api/resource")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isOk());
    }
}
```

### BaseTenantIsolationTest
多租户隔离测试基类，提供：
- 租户切换方法
- 租户隔离断言
- 多租户场景测试支持

使用示例：
```java
@SpringBootTest(classes = YourApplication.class)
class TenantDataIsolationTest extends BaseTenantIsolationTest {
    
    @Test
    void testDataIsolation() {
        // 租户1创建数据
        Long dataId = runAsTenant(1L, () -> {
            return createTestData();
        });
        
        // 租户2不能访问租户1的数据
        runAsTenant(2L, () -> {
            Data data = dataService.getById(dataId);
            assertTenantIsolation(data, "租户2不应该能访问租户1的数据");
        });
    }
}
```

### BaseIntegrationTest
集成测试基类，提供：
- 完整Spring上下文
- 异步操作等待方法
- 重试机制支持

## 测试工具类

### TenantTestHelper
租户上下文管理辅助类。

```java
// 设置租户上下文
TenantTestHelper.setupTenantContext(1L, 1L);

// 执行带租户上下文的操作
TenantTestHelper.withTenant(1L, () -> {
    // 操作会自动在租户1的上下文中执行
    // 完成后自动清理上下文
});

// 获取当前租户ID
Long tenantId = TenantTestHelper.getCurrentTenantId();
```

### TestDataFactory
测试数据生成工厂。

```java
// 生成唯一ID
Long id = TestDataFactory.generateId();

// 生成测试编码
String code = TestDataFactory.generateCode("STATION");

// 生成测试邮箱
String email = TestDataFactory.generateEmail("testuser");

// 生成测试手机号
String phone = TestDataFactory.generatePhone();

// 生成随机数
int value = TestDataFactory.randomInt(1, 100);
```

### AssertionHelper
自定义断言辅助类。

```java
// 断言集合大小
AssertionHelper.assertCollectionSize(list, 5);

// 断言集合不为空
AssertionHelper.assertCollectionNotEmpty(list);

// 断言时间接近
AssertionHelper.assertTimeNear(expected, actual, 5);

// 断言字符串不为空
AssertionHelper.assertStringNotBlank(str);

// 断言数值在范围内
AssertionHelper.assertInRange(value, 0, 100);

// 断言租户ID有效
AssertionHelper.assertValidTenantId(tenantId);
```

## 编写测试

### 基本结构

遵循AAA模式（Arrange-Act-Assert）：

```java
@Test
void testMethodName() {
    // Arrange - 准备测试数据
    Station station = new Station();
    station.setStationCode("TEST001");
    station.setStationName("测试站点");
    
    // Act - 执行测试操作
    boolean result = stationService.saveStation(station);
    
    // Assert - 验证结果
    assertTrue(result);
    assertNotNull(station.getStationId());
    assertEquals(DEFAULT_TENANT_ID, station.getTenantId());
}
```

### 使用@BeforeEach和@AfterEach

```java
@BeforeEach
void setUp() {
    // 每个测试前执行
    // 初始化测试数据
}

@AfterEach
void tearDown() {
    // 每个测试后执行
    // 清理测试数据（通常不需要，因为有@Transactional）
}
```

### 使用@BeforeAll和@AfterAll

```java
@BeforeAll
static void setUpClass() {
    // 所有测试前执行一次
    // 初始化昂贵的资源
}

@AfterAll
static void tearDownClass() {
    // 所有测试后执行一次
    // 清理资源
}
```

## 多租户测试

### 测试租户数据隔离

```java
@Test
void testTenantDataIsolation() {
    // 租户1创建数据
    Long stationId = runAsTenant(1L, () -> {
        Station station = new Station();
        station.setStationCode("TENANT1_STATION");
        stationService.saveStation(station);
        return station.getStationId();
    });
    
    // 租户2不能访问租户1的数据
    runAsTenant(2L, () -> {
        Station station = stationService.getById(stationId);
        assertNull(station, "租户2不应该能访问租户1的数据");
    });
    
    // 租户1可以访问自己的数据
    runAsTenant(1L, () -> {
        Station station = stationService.getById(stationId);
        assertNotNull(station, "租户1应该能访问自己的数据");
    });
}
```

### 测试租户层级权限

```java
@Test
void testTenantHierarchyAccess() {
    // 父租户创建数据
    Long parentTenantId = 1L;
    Long childTenantId = 2L;
    
    Long stationId = runAsTenant(parentTenantId, () -> {
        Station station = new Station();
        stationService.saveStation(station);
        return station.getStationId();
    });
    
    // 子租户不能访问父租户的数据（除非有特殊权限）
    runAsTenant(childTenantId, () -> {
        Station station = stationService.getById(stationId);
        assertNull(station);
    });
}
```

### 测试上下文切换

```java
@Test
void testContextSwitch() {
    // 初始租户上下文
    assertCurrentTenant(DEFAULT_TENANT_ID);
    
    // 切换到租户2
    switchTenant(2L);
    assertCurrentTenant(2L);
    
    // 切换回租户1
    switchTenant(1L);
    assertCurrentTenant(1L);
}
```

## 测试命名规范

### 类命名
- Service测试：`{ServiceName}Test`
- Controller测试：`{ControllerName}Test`
- 集成测试：`{Feature}IntegrationTest`
- 租户隔离测试：`{Feature}TenantIsolationTest`

### 方法命名
使用描述性名称，说明测试的内容：

```java
// 推荐
@Test
void testSaveStation_WithValidData_ShouldReturnTrue()

@Test
void testGetStation_WithInvalidId_ShouldReturnNull()

@Test
void testDeleteStation_WhenHasChargers_ShouldThrowException()

// 可选的简短形式
@Test
void testSaveStation()

@Test
void testGetStationWithInvalidId()
```

## 最佳实践

### 1. 测试应该独立
每个测试应该独立运行，不依赖其他测试的执行顺序。

```java
// 好的做法
@Test
void testMethod1() {
    // 创建自己的测试数据
    Station station = createTestStation();
    // 执行测试
}

@Test
void testMethod2() {
    // 创建自己的测试数据
    Station station = createTestStation();
    // 执行测试
}
```

### 2. 使用有意义的断言消息

```java
// 好的做法
assertEquals(1L, station.getTenantId(), 
            "Station应该属于租户1");

// 不好的做法
assertEquals(1L, station.getTenantId());
```

### 3. 测试边界条件

```java
@Test
void testPagination_WithZeroPageSize_ShouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> {
        stationService.queryStationPage(new Page<>(1, 0), null);
    });
}
```

### 4. 使用@DisplayName提高可读性

```java
@Test
@DisplayName("保存充电站 - 有效数据应该返回成功")
void testSaveStationWithValidData() {
    // 测试代码
}
```

### 5. 测试异常情况

```java
@Test
void testSaveStation_WithDuplicateCode_ShouldThrowException() {
    Station station1 = new Station();
    station1.setStationCode("CODE001");
    stationService.saveStation(station1);
    
    Station station2 = new Station();
    station2.setStationCode("CODE001");
    
    assertThrows(DuplicateKeyException.class, () -> {
        stationService.saveStation(station2);
    });
}
```

### 6. 使用测试数据构建器

```java
// 创建测试数据构建器
private Station buildTestStation(String code) {
    Station station = new Station();
    station.setStationCode(code);
    station.setStationName("测试站点-" + code);
    station.setAddress("测试地址");
    station.setLatitude(39.9087);
    station.setLongitude(116.4089);
    station.setStatus(1);
    return station;
}

@Test
void testSaveStation() {
    Station station = buildTestStation("TEST001");
    boolean result = stationService.saveStation(station);
    assertTrue(result);
}
```

### 7. 避免在测试中使用Thread.sleep()

使用Awaitility或其他异步测试工具：

```java
// 好的做法
await().atMost(5, SECONDS).until(() -> 
    service.getStatus() == Status.COMPLETED
);

// 不好的做法
Thread.sleep(5000);
```

### 8. 清理测试数据

虽然使用了@Transactional自动回滚，但对于某些场景仍需要手动清理：

```java
@AfterEach
void tearDown() {
    // 清理缓存
    cacheManager.clear();
    // 清理租户上下文
    TenantContext.clear();
}
```

## 运行测试

### 运行所有测试
```bash
./gradlew test
```

### 运行指定模块的测试
```bash
./gradlew :evcs-station:test
```

### 运行指定测试类
```bash
./gradlew test --tests StationServiceTest
```

### 运行指定测试方法
```bash
./gradlew test --tests StationServiceTest.testSaveStation
```

### 运行集成测试
```bash
./gradlew test --tests *IntegrationTest
```

### 生成测试报告
```bash
./gradlew test
# 报告位置: build/reports/tests/test/index.html
```

## 测试覆盖率

### 查看测试覆盖率
```bash
./gradlew test jacocoTestReport
# 报告位置: build/reports/jacoco/test/html/index.html
```

### 目标覆盖率
- 核心业务逻辑：> 80%
- Service层：> 70%
- Controller层：> 60%
- 工具类：> 90%

## 常见问题

### 1. 测试中租户上下文未设置
确保测试类继承了`BaseServiceTest`或手动设置租户上下文：
```java
TenantContext.setCurrentTenantId(1L);
```

### 2. 测试数据库连接失败
检查`application-test.yml`配置，确保H2数据库配置正确。

### 3. 测试相互影响
确保每个测试方法都是独立的，使用`@Transactional`或在`@AfterEach`中清理数据。

### 4. 测试运行缓慢
- 减少Spring上下文的启动次数
- 使用mock代替真实依赖
- 将集成测试与单元测试分离

## 参考资料

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)

