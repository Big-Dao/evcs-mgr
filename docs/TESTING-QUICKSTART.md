# 测试框架快速开始

5分钟快速上手EVCS测试框架。

## 第一步：创建测试类

### 1. Service层测试

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
        // Arrange
        YourEntity entity = new YourEntity();
        entity.setCode(TestDataFactory.generateCode("TEST"));
        entity.setName("测试数据");
        
        // Act
        boolean result = yourService.save(entity);
        
        // Assert
        assertTrue(result);
        assertNotNull(entity.getId());
        assertEquals(DEFAULT_TENANT_ID, entity.getTenantId());
    }
}
```

### 2. Controller层测试

```java
package com.evcs.yourmodule.controller;

import com.evcs.common.test.base.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("你的控制器测试")
class YourControllerTest extends BaseControllerTest {
    
    @Test
    @DisplayName("GET接口测试")
    void testGetApi() throws Exception {
        mockMvc.perform(get("/api/resource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
    
    @Test
    @DisplayName("POST接口测试")
    void testPostApi() throws Exception {
        YourRequest request = new YourRequest();
        request.setName("test");
        
        mockMvc.perform(post("/api/resource")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isOk());
    }
}
```

### 3. 多租户隔离测试

```java
package com.evcs.yourmodule.service;

import com.evcs.common.test.base.BaseTenantIsolationTest;
import com.evcs.common.test.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = YourApplication.class)
@DisplayName("多租户隔离测试")
class YourTenantIsolationTest extends BaseTenantIsolationTest {
    
    @Resource
    private IYourService yourService;
    
    @Test
    @DisplayName("数据隔离测试")
    void testDataIsolation() {
        // 租户1创建数据
        Long dataId = runAsTenant(1L, () -> {
            YourEntity entity = new YourEntity();
            entity.setCode(TestDataFactory.generateCode("TENANT1"));
            yourService.save(entity);
            return entity.getId();
        });
        
        // 租户2不能访问租户1的数据
        runAsTenant(2L, () -> {
            YourEntity entity = yourService.getById(dataId);
            assertTenantIsolation(entity, "租户2不应该能访问租户1的数据");
        });
    }
}
```

## 第二步：添加测试配置

在模块的 `src/test/resources` 目录创建 `application-test.yml`:

```yaml
spring:
  application:
    name: your-module-test

  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
    username: sa
    password: 

  sql:
    init:
      mode: always

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

evcs:
  tenant:
    enabled: true
    tenant-id-column: tenant_id
    default-tenant-id: 1
```

## 第三步：运行测试

### 命令行运行

```bash
# 运行所有测试
./gradlew test

# 运行指定模块测试
./gradlew :your-module:test

# 运行指定测试类
./gradlew test --tests YourServiceTest

# 运行指定测试方法
./gradlew test --tests YourServiceTest.testCreate

# 生成覆盖率报告
./gradlew test jacocoTestReport
# 查看报告: build/reports/jacoco/test/html/index.html
```

### IDE运行

在IntelliJ IDEA或Eclipse中，右键点击测试类或测试方法，选择"Run"。

## 常用工具类

### TestDataFactory - 生成测试数据

```java
// 生成唯一ID
Long id = TestDataFactory.generateId();

// 生成编码
String code = TestDataFactory.generateCode("PREFIX"); // PREFIX_1234567890

// 生成名称
String name = TestDataFactory.generateName("测试"); // 测试_1001

// 生成邮箱
String email = TestDataFactory.generateEmail("user"); // user@test.com

// 生成手机号
String phone = TestDataFactory.generatePhone(); // 13812345678

// 生成地址
String address = TestDataFactory.generateAddress("北京"); // 北京市测试路1001号

// 生成随机数
int value = TestDataFactory.randomInt(1, 100); // 1-99
double price = TestDataFactory.randomDouble(10.0, 100.0); // 10.0-99.9
```

### TenantTestHelper - 租户上下文管理

```java
// 设置租户上下文
TenantTestHelper.setupTenantContext(1L, 1L);

// 清理租户上下文
TenantTestHelper.clearTenantContext();

// 带上下文执行操作（自动清理）
TenantTestHelper.withTenant(1L, () -> {
    // 在租户1的上下文中执行
});

// 带上下文执行并返回结果
Long result = TenantTestHelper.withTenant(1L, () -> {
    return yourService.doSomething();
});

// 检查上下文是否设置
boolean isSet = TenantTestHelper.isTenantContextSet();
```

### AssertionHelper - 自定义断言

```java
// 断言集合大小
AssertionHelper.assertCollectionSize(list, 5);

// 断言集合不为空
AssertionHelper.assertCollectionNotEmpty(list);

// 断言字符串不为空
AssertionHelper.assertStringNotBlank(str);

// 断言时间接近（5秒内）
AssertionHelper.assertTimeNear(expected, actual, 5);

// 断言数值在范围内
AssertionHelper.assertInRange(value, 0, 100);

// 断言列表包含元素
AssertionHelper.assertListContains(list, element);

// 断言租户ID有效
AssertionHelper.assertValidTenantId(tenantId);
```

## 测试最佳实践

### ✅ 推荐做法

1. **使用测试基类** - 自动处理租户上下文
2. **使用@DisplayName** - 提高测试可读性
3. **遵循AAA模式** - Arrange, Act, Assert
4. **使用TestDataFactory** - 生成唯一的测试数据
5. **测试应该独立** - 不依赖其他测试
6. **添加断言消息** - 便于定位问题

```java
@Test
@DisplayName("保存数据 - 有效数据应该成功")
void testSaveWithValidData() {
    // Arrange
    YourEntity entity = new YourEntity();
    entity.setCode(TestDataFactory.generateCode("TEST"));
    
    // Act
    boolean result = yourService.save(entity);
    
    // Assert
    assertTrue(result, "保存应该成功");
    assertNotNull(entity.getId(), "ID应该被自动生成");
}
```

### ❌ 避免做法

1. **硬编码测试数据** - 可能导致测试冲突
2. **测试相互依赖** - 难以维护
3. **忽略清理** - 可能影响其他测试
4. **不写断言消息** - 难以定位问题

```java
// ❌ 不好的做法
@Test
void test1() {
    entity.setCode("TEST001"); // 硬编码
    service.save(entity);
}

// ✅ 好的做法
@Test
@DisplayName("测试1")
void test1() {
    entity.setCode(TestDataFactory.generateCode("TEST")); // 动态生成
    service.save(entity);
}
```

## 常见问题

### Q: 测试中租户上下文未设置？
A: 确保测试类继承了 `BaseServiceTest` 或手动设置：
```java
TenantContext.setCurrentTenantId(1L);
```

### Q: 测试相互影响？
A: 使用 `TestDataFactory` 生成唯一数据，确保测试独立。

### Q: 如何测试异常情况？
A: 使用 `assertThrows`:
```java
@Test
void testInvalidInput() {
    assertThrows(IllegalArgumentException.class, () -> {
        yourService.save(null);
    });
}
```

### Q: 如何测试异步操作？
A: 使用 `BaseIntegrationTest` 的 `waitFor` 或 `retryUntilSuccess` 方法。

## 下一步

- 阅读完整的 [测试指南](TESTING-GUIDE.md)
- 查看 [StationServiceTest](../evcs-station/src/test/java/com/evcs/station/service/StationServiceTest.java) 示例
- 查看 [StationTenantIsolationTest](../evcs-station/src/test/java/com/evcs/station/service/StationTenantIsolationTest.java) 示例

## 获取帮助

- 查看 [evcs-common/src/test/java/com/evcs/common/test/README.md](../evcs-common/src/test/java/com/evcs/common/test/README.md)
- 查看示例代码
- 提交Issue

祝测试愉快！🎉
