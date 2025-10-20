# 测试框架完整指南

> 本指南涵盖 EVCS Manager 项目的测试框架使用、最佳实践和快速开始。

**最后更新**: 2025-10-20  
**相关文档**: 
- [测试环境快速启动](../../TEST-ENVIRONMENT-QUICKSTART.md)
- [测试覆盖率报告](TEST-COVERAGE-REPORT.md)
- [测试修复指南](TEST-FIX-GUIDE.md)

---

## 📚 目录

1. [概述](#概述)
2. [5分钟快速开始](#5分钟快速开始)
3. [测试框架架构](#测试框架架构)
4. [测试基类详解](#测试基类详解)
5. [测试工具类](#测试工具类)
6. [编写测试用例](#编写测试用例)
7. [运行和覆盖率](#运行和覆盖率)
8. [最佳实践](#最佳实践)

---

## 概述

### 测试框架特性

本项目提供完整的测试基础设施：

- ✅ **4个测试基类** - Service/Controller/TenantIsolation/Integration
- ✅ **自动租户上下文管理** - 无需手动设置和清理
- ✅ **测试数据工厂** - 自动生成唯一测试数据
- ✅ **丰富工具类** - TenantTestHelper、ResultMatcher等
- ✅ **代码覆盖率** - JaCoCo集成，HTML报告
- ✅ **事务回滚** - 测试后自动清理数据

### 当前测试状态

| 模块 | 测试数量 | 覆盖率 | 状态 |
|------|----------|--------|------|
| evcs-common | 12 | 65% | ✅ 通过 |
| evcs-station | 8 | 45% | ✅ 通过 |
| evcs-order | 5 | 30% | ⚠️ 待提升 |
| evcs-payment | 3 | 25% | ⚠️ 待提升 |
| **总计** | **28+** | **41%** | **目标: 80%** |

---

## 5分钟快速开始

### 步骤 1: 创建 Service 测试

```java
package com.evcs.yourmodule.service;

import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.common.test.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = YourApplication.class)
@DisplayName("你的服务测试")
class YourServiceTest extends BaseServiceTest {
    
    @Resource
    private IYourService yourService;
    
    @Test
    @DisplayName("创建数据 - 正常流程")
    void testCreate() {
        // Arrange - 使用工厂生成唯一数据
        YourEntity entity = new YourEntity();
        entity.setCode(TestDataFactory.generateCode("TEST"));
        entity.setName("测试数据");
        
        // Act - 执行被测方法
        boolean result = yourService.save(entity);
        
        // Assert - 验证结果
        assertTrue(result);
        assertNotNull(entity.getId());
        assertEquals(DEFAULT_TENANT_ID, entity.getTenantId());
    }
    
    @Test
    @DisplayName("查询列表 - 应只返回当前租户数据")
    void testList_shouldReturnOnlyCurrentTenantData() {
        // Arrange - 为两个租户创建数据
        runAsTenant(TENANT_1_ID, () -> {
            YourEntity entity1 = new YourEntity();
            entity1.setCode(TestDataFactory.generateCode("T1"));
            yourService.save(entity1);
        });
        
        runAsTenant(TENANT_2_ID, () -> {
            YourEntity entity2 = new YourEntity();
            entity2.setCode(TestDataFactory.generateCode("T2"));
            yourService.save(entity2);
        });
        
        // Act - 作为租户1查询
        runAsTenant(TENANT_1_ID, () -> {
            List<YourEntity> results = yourService.list();
            
            // Assert - 只返回租户1的数据
            assertEquals(1, results.size());
            assertEquals(TENANT_1_ID, results.get(0).getTenantId());
        });
    }
}
```

### 步骤 2: 创建 Controller 测试

```java
package com.evcs.yourmodule.controller;

import com.evcs.common.test.base.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = YourApplication.class)
@DisplayName("你的控制器测试")
class YourControllerTest extends BaseControllerTest {
    
    @Test
    @DisplayName("GET /api/your-resource - 应返回200")
    void testGetResource() throws Exception {
        mockMvc.perform(get("/api/your-resource")
                .header("Authorization", "Bearer " + createMockJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
```

### 步骤 3: 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行特定模块测试
./gradlew :evcs-station:test

# 生成覆盖率报告
./gradlew test jacocoTestReport

# 查看报告: build/reports/jacoco/test/html/index.html
```

---

## 测试框架架构

### 架构图

```
测试框架层次结构
├── evcs-common/src/testFixtures/
│   ├── base/                           # 测试基类
│   │   ├── BaseServiceTest            # Service层测试基类
│   │   ├── BaseControllerTest         # Controller层测试基类
│   │   ├── BaseTenantTest             # 租户隔离测试基类
│   │   └── BaseIntegrationTest        # 集成测试基类
│   │
│   ├── util/                          # 测试工具类
│   │   ├── TestDataFactory            # 测试数据生成
│   │   └── TenantTestHelper           # 租户测试辅助
│   │
│   └── config/                        # 测试配置
│       └── TestConfig                 # 测试环境配置
│
└── 各模块/src/test/
    ├── java/                          # 测试代码
    │   └── com/evcs/module/
    │       ├── service/               # Service测试
    │       └── controller/            # Controller测试
    │
    └── resources/
        └── application-test.yml       # 测试配置文件
```

### 依赖关系

```gradle
// 各模块的 build.gradle
dependencies {
    // 引入测试基础设施
    testImplementation(testFixtures(project(':evcs-common')))
    
    // Spring Boot 测试支持
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

---

## 测试基类详解

### 1. BaseServiceTest - Service层测试

**用途**: 所有 Service 层测试的基类

**提供功能**:
- ✅ 自动设置和清理租户上下文
- ✅ 事务回滚（测试后数据自动清理）
- ✅ 租户切换方法 `runAsTenant()`
- ✅ 预定义租户ID和用户ID常量

**示例**:

```java
@SpringBootTest
class StationServiceTest extends BaseServiceTest {
    
    @Test
    void testSave() {
        // 无需手动设置 TenantContext，BaseServiceTest 已处理
        Station station = new Station();
        station.setCode(TestDataFactory.generateCode("STATION"));
        
        boolean result = stationService.save(station);
        
        assertTrue(result);
        assertEquals(DEFAULT_TENANT_ID, station.getTenantId());
    }
    
    @Test
    void testMultiTenantScenario() {
        // 使用 runAsTenant 切换租户
        runAsTenant(TENANT_1_ID, () -> {
            // 租户1的操作
            stationService.save(createStation("T1"));
        });
        
        runAsTenant(TENANT_2_ID, () -> {
            // 租户2的操作
            stationService.save(createStation("T2"));
        });
        
        // 验证隔离
        runAsTenant(TENANT_1_ID, () -> {
            List<Station> results = stationService.list();
            assertEquals(1, results.size());
        });
    }
}
```

**关键方法**:
- `runAsTenant(Long tenantId, Runnable action)` - 以指定租户执行操作
- `runAsTenant(Long tenantId, Supplier<T> action)` - 以指定租户执行并返回结果

**预定义常量**:
- `DEFAULT_TENANT_ID = 1L` - 默认租户ID
- `DEFAULT_USER_ID = 1L` - 默认用户ID
- `TENANT_1_ID = 100L` - 测试租户1
- `TENANT_2_ID = 200L` - 测试租户2

### 2. BaseControllerTest - Controller层测试

**用途**: 所有 Controller 层测试的基类

**提供功能**:
- ✅ MockMvc 配置
- ✅ JWT Token 生成
- ✅ JSON 序列化/反序列化
- ✅ 租户上下文管理
- ✅ 响应结果提取工具

**示例**:

```java
@SpringBootTest
@AutoConfigureMockMvc
class StationControllerTest extends BaseControllerTest {
    
    @Test
    void testCreateStation() throws Exception {
        StationDTO dto = new StationDTO();
        dto.setName("测试站点");
        dto.setCode(TestDataFactory.generateCode("STATION"));
        
        mockMvc.perform(post("/api/stations")
                .header("Authorization", "Bearer " + createMockJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").exists());
    }
    
    @Test
    void testGetStation() throws Exception {
        // 准备测试数据
        Long stationId = createTestStation();
        
        mockMvc.perform(get("/api/stations/" + stationId)
                .header("Authorization", "Bearer " + createMockJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").value(stationId));
    }
}
```

**关键方法**:
- `createMockJwt()` - 创建模拟JWT Token
- `toJson(Object obj)` - 对象转JSON字符串
- `fromJson(String json, Class<T> clazz)` - JSON字符串转对象

### 3. BaseTenantTest - 多租户隔离测试

**用途**: 专门测试多租户隔离的基类

**提供功能**:
- ✅ 租户切换断言
- ✅ 租户隔离验证方法
- ✅ 上下文验证
- ✅ 多租户场景模板

**示例**:

```java
@SpringBootTest
class StationTenantIsolationTest extends BaseTenantTest {
    
    @Test
    void testListIsolation() {
        // Arrange - 为两个租户创建数据
        Long station1Id = runAsTenant(TENANT_1_ID, () -> {
            Station station = new Station();
            station.setCode(TestDataFactory.generateCode("T1"));
            stationService.save(station);
            return station.getId();
        });
        
        Long station2Id = runAsTenant(TENANT_2_ID, () -> {
            Station station = new Station();
            station.setCode(TestDataFactory.generateCode("T2"));
            stationService.save(station);
            return station.getId();
        });
        
        // Act & Assert - 验证租户1只能看到自己的数据
        assertTenantIsolation(
            TENANT_1_ID,
            () -> stationService.list(),
            results -> {
                assertEquals(1, results.size());
                assertEquals(station1Id, results.get(0).getId());
            }
        );
        
        // 验证租户2只能看到自己的数据
        assertTenantIsolation(
            TENANT_2_ID,
            () -> stationService.list(),
            results -> {
                assertEquals(1, results.size());
                assertEquals(station2Id, results.get(0).getId());
            }
        );
    }
}
```

**关键方法**:
- `assertTenantIsolation()` - 断言租户隔离
- `assertTenantContext()` - 断言租户上下文正确

### 4. BaseIntegrationTest - 集成测试

**用途**: 跨模块集成测试

**提供功能**:
- ✅ 继承 BaseServiceTest 所有特性
- ✅ 多服务交互支持
- ✅ 外部服务Mock
- ✅ 完整业务流程测试

---

## 测试工具类

### 1. TestDataFactory - 测试数据生成

**用途**: 生成唯一的测试数据，避免冲突

**核心方法**:

```java
// 生成唯一编码（带时间戳和随机数）
String code = TestDataFactory.generateCode("STATION");
// 结果: STATION_20251020132045_ABC123

// 生成唯一名称
String name = TestDataFactory.generateName("测试站点");
// 结果: 测试站点_20251020132045

// 生成手机号
String phone = TestDataFactory.generatePhone();
// 结果: 138XXXXXXXX

// 生成邮箱
String email = TestDataFactory.generateEmail();
// 结果: testXXXXXX@test.com

// 生成随机整数
int randomInt = TestDataFactory.randomInt(1, 100);

// 生成随机布尔值
boolean randomBool = TestDataFactory.randomBoolean();
```

**完整实体生成示例**:

```java
Station station = new Station();
station.setCode(TestDataFactory.generateCode("STATION"));
station.setName(TestDataFactory.generateName("测试站点"));
station.setAddress("测试地址");
station.setContactPhone(TestDataFactory.generatePhone());
station.setContactEmail(TestDataFactory.generateEmail());
```

### 2. TenantTestHelper - 租户测试辅助

**用途**: 简化多租户测试场景

**核心方法**:

```java
// 验证租户隔离
TenantTestHelper.verifyTenantIsolation(
    stationService::list,
    TENANT_1_ID,
    TENANT_2_ID
);

// 为多个租户创建数据
Map<Long, List<Station>> dataByTenant = TenantTestHelper.createDataForTenants(
    Arrays.asList(TENANT_1_ID, TENANT_2_ID),
    tenantId -> {
        Station station = new Station();
        station.setCode(TestDataFactory.generateCode("T" + tenantId));
        stationService.save(station);
        return station;
    }
);

// 清理租户数据
TenantTestHelper.cleanupTenantData(TENANT_1_ID, stationService);
```

---

## 编写测试用例

### AAA 模式（Arrange-Act-Assert）

所有测试都应遵循 AAA 模式：

```java
@Test
@DisplayName("保存站点 - 应自动填充租户ID")
void testSave_shouldAutoFillTenantId() {
    // Arrange - 准备测试数据
    Station station = new Station();
    station.setCode(TestDataFactory.generateCode("STATION"));
    station.setName("测试站点");
    // 注意：不设置 tenantId
    
    // Act - 执行被测方法
    boolean result = stationService.save(station);
    
    // Assert - 验证结果
    assertTrue(result, "保存应成功");
    assertNotNull(station.getId(), "ID应被填充");
    assertEquals(DEFAULT_TENANT_ID, station.getTenantId(), 
        "tenant_id应被自动填充为当前租户");
}
```

### 测试命名规范

```java
// 方法命名: test[方法名]_should[预期结果]_when[条件]
@Test
@DisplayName("中文描述 - 清晰说明测试意图")
void testSave_shouldSucceed_whenValidData() { }

void testUpdate_shouldFail_whenTenantMismatch() { }

void testList_shouldReturnEmpty_whenNoData() { }
```

### 常见测试场景模板

#### 1. CRUD 测试

```java
@Test
void testCreate() {
    Entity entity = createTestEntity();
    boolean result = service.save(entity);
    assertTrue(result);
    assertNotNull(entity.getId());
}

@Test
void testRead() {
    Entity saved = service.save(createTestEntity());
    Entity found = service.getById(saved.getId());
    assertNotNull(found);
    assertEquals(saved.getCode(), found.getCode());
}

@Test
void testUpdate() {
    Entity saved = service.save(createTestEntity());
    saved.setName("Updated");
    boolean result = service.updateById(saved);
    assertTrue(result);
    
    Entity updated = service.getById(saved.getId());
    assertEquals("Updated", updated.getName());
}

@Test
void testDelete() {
    Entity saved = service.save(createTestEntity());
    boolean result = service.removeById(saved.getId());
    assertTrue(result);
    
    Entity deleted = service.getById(saved.getId());
    assertNull(deleted);
}
```

#### 2. 租户隔离测试

```java
@Test
void testTenantIsolation() {
    // 租户1创建数据
    Long id1 = runAsTenant(TENANT_1_ID, () -> {
        Entity entity = createTestEntity();
        service.save(entity);
        return entity.getId();
    });
    
    // 租户2创建数据
    Long id2 = runAsTenant(TENANT_2_ID, () -> {
        Entity entity = createTestEntity();
        service.save(entity);
        return entity.getId();
    });
    
    // 租户1查询，只能看到自己的数据
    runAsTenant(TENANT_1_ID, () -> {
        List<Entity> results = service.list();
        assertEquals(1, results.size());
        assertEquals(id1, results.get(0).getId());
    });
    
    // 租户1不能访问租户2的数据
    runAsTenant(TENANT_1_ID, () -> {
        Entity other = service.getById(id2);
        assertNull(other); // 或 assertThrows
    });
}
```

#### 3. 异常测试

```java
@Test
void testInvalidInput_shouldThrowException() {
    Entity entity = new Entity();
    // 缺少必填字段
    
    assertThrows(IllegalArgumentException.class, () -> {
        service.save(entity);
    });
}

@Test
void testNotFound_shouldReturnNull() {
    Entity result = service.getById(999999L);
    assertNull(result);
}
```

---

## 运行和覆盖率

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行特定模块
./gradlew :evcs-station:test

# 运行特定测试类
./gradlew :evcs-station:test --tests StationServiceTest

# 运行特定测试方法
./gradlew :evcs-station:test --tests StationServiceTest.testSave

# 持续运行（监听文件变化）
./gradlew test --continuous
```

### 生成覆盖率报告

```bash
# 生成 JaCoCo 覆盖率报告
./gradlew test jacocoTestReport

# 查看报告
# HTML: build/reports/jacoco/test/html/index.html
# XML: build/reports/jacoco/test/jacocoTestReport.xml

# 验证覆盖率阈值
./gradlew jacocoTestCoverageVerification
```

### 覆盖率要求

| 层级 | 最低覆盖率 | 目标覆盖率 |
|------|-----------|-----------|
| Service 层 | 70% | 80% |
| Controller 层 | 60% | 75% |
| Util 工具类 | 80% | 90% |
| **整体** | **65%** | **80%** |

---

## 最佳实践

### ✅ DO - 推荐做法

1. **使用测试基类**
   ```java
   // ✅ 正确
   class MyServiceTest extends BaseServiceTest { }
   
   // ❌ 错误
   class MyServiceTest { } // 需要手动管理上下文
   ```

2. **使用 TestDataFactory 生成数据**
   ```java
   // ✅ 正确 - 每次生成唯一值
   String code = TestDataFactory.generateCode("PREFIX");
   
   // ❌ 错误 - 硬编码会导致冲突
   String code = "TEST001";
   ```

3. **清晰的测试名称和断言消息**
   ```java
   // ✅ 正确
   @DisplayName("保存站点 - 应自动填充租户ID")
   assertEquals(expected, actual, "tenant_id应为当前租户");
   
   // ❌ 错误
   @Test
   void test1() { }
   assertEquals(expected, actual); // 失败时不知道原因
   ```

4. **测试租户隔离**
   ```java
   // ✅ 每个实体操作都应有租户隔离测试
   @Test
   void testList_tenantIsolation() {
       // 验证多租户场景
   }
   ```

5. **使用 @DisplayName**
   ```java
   // ✅ 正确
   @DisplayName("创建站点 - 有效数据应成功")
   
   // ❌ 错误 - 依赖方法名不够清晰
   void testCreateStation() { }
   ```

### ❌ DON'T - 避免做法

1. **不要测试框架功能**
   ```java
   // ❌ 不要测试 Spring/MyBatis 的标准行为
   @Test
   void testSpringAutowiring() { }
   ```

2. **不要测试简单 Getter/Setter**
   ```java
   // ❌ 不必要的测试
   @Test
   void testGetName() {
       station.setName("Test");
       assertEquals("Test", station.getName());
   }
   ```

3. **不要依赖测试执行顺序**
   ```java
   // ❌ 错误 - 测试应独立
   @Test
   @Order(1)
   void test1_createData() { }
   
   @Test
   @Order(2)
   void test2_useDataFromTest1() { } // 依赖 test1
   ```

4. **不要在测试间共享状态**
   ```java
   // ❌ 错误
   private static Station sharedStation;
   
   @Test
   void test1() {
       sharedStation = new Station(); // 影响其他测试
   }
   ```

5. **不要忘记清理租户上下文**
   ```java
   // ❌ 错误 - BaseServiceTest 会自动处理
   // 如果手动设置，必须在 finally 清理
   TenantContext.setCurrentTenantId(1L);
   // ... 测试代码
   // 忘记 TenantContext.clear(); 会影响其他测试
   ```

---

## 附录

### A. 测试配置文件示例

`src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/evcs_test
    username: postgres
    password: test_password
  jpa:
    hibernate:
      ddl-auto: create-drop  # 测试后自动清理
  
  redis:
    host: localhost
    port: 6379
    database: 1  # 使用独立的测试数据库

# 日志配置
logging:
  level:
    com.evcs: DEBUG
    org.springframework.test: INFO
```

### B. Gradle 测试配置

`build.gradle`:

```gradle
test {
    useJUnitPlatform()
    
    // 测试日志
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
    
    // JVM 参数
    jvmArgs '-Xmx2g'
    
    // 并行执行
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
}

// JaCoCo 配置
jacoco {
    toolVersion = "0.8.11"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.65  // 最低 65% 覆盖率
            }
        }
    }
}
```

### C. 常用测试命令速查

```bash
# 运行测试
./gradlew test                              # 所有测试
./gradlew :evcs-station:test                # 单模块
./gradlew test --tests StationServiceTest   # 单类
./gradlew test --tests *Service*            # 模式匹配

# 覆盖率
./gradlew jacocoTestReport                  # 生成报告
./gradlew jacocoTestCoverageVerification    # 验证阈值

# 调试
./gradlew test --debug                      # 调试模式
./gradlew test --info                       # 详细日志
./gradlew test --stacktrace                 # 完整堆栈

# 清理
./gradlew clean test                        # 清理后测试
./gradlew test --rerun-tasks                # 强制重新运行
```

---

## 获取帮助

- 📖 查看 [.github/instructions/test.instructions.md](../../.github/instructions/test.instructions.md) - GitHub Copilot 测试规范
- 📊 查看 [TEST-COVERAGE-REPORT.md](TEST-COVERAGE-REPORT.md) - 当前覆盖率报告
- 🔧 查看 [TEST-FIX-GUIDE.md](TEST-FIX-GUIDE.md) - 测试问题修复指南
- 🚀 查看 [../../TEST-ENVIRONMENT-QUICKSTART.md](../../TEST-ENVIRONMENT-QUICKSTART.md) - 环境快速启动

---

**维护者**: EVCS Team  
**最后更新**: 2025-10-20  
**版本**: v2.0 (合并版)

