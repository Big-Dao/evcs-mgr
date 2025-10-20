# EVCS 测试框架

本目录包含项目的通用测试基础设施，为所有模块提供统一的测试支持。

## 目录结构

```
com.evcs.common.test
├── base/                    # 测试基类
│   ├── BaseServiceTest.java        # Service层测试基类
│   ├── BaseControllerTest.java     # Controller层测试基类
│   ├── BaseTenantIsolationTest.java # 多租户隔离测试基类
│   └── BaseIntegrationTest.java    # 集成测试基类
├── helper/                  # 测试辅助类
│   └── TenantTestHelper.java       # 租户测试辅助工具
└── util/                    # 测试工具类
    ├── TestDataFactory.java        # 测试数据工厂
    └── AssertionHelper.java        # 自定义断言工具
```

## 快速开始

### 1. Service层测试

```java
@SpringBootTest(classes = YourApplication.class)
class YourServiceTest extends BaseServiceTest {
    
    @Resource
    private IYourService yourService;
    
    @Test
    void testYourMethod() {
        // 租户上下文已自动设置，可直接使用
        YourEntity entity = new YourEntity();
        entity.setName("test");
        
        boolean result = yourService.save(entity);
        
        assertTrue(result);
        assertNotNull(entity.getId());
        assertEquals(DEFAULT_TENANT_ID, entity.getTenantId());
    }
}
```

### 2. Controller层测试

```java
@SpringBootTest
class YourControllerTest extends BaseControllerTest {
    
    @Test
    void testGetApi() throws Exception {
        mockMvc.perform(get("/api/resource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
    
    @Test
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
@SpringBootTest(classes = YourApplication.class)
class YourTenantIsolationTest extends BaseTenantIsolationTest {
    
    @Test
    void testDataIsolation() {
        // 租户1创建数据
        Long dataId = runAsTenant(1L, () -> {
            YourEntity entity = new YourEntity();
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

### 4. 使用测试工具类

```java
// 生成测试数据
Long id = TestDataFactory.generateId();
String code = TestDataFactory.generateCode("PREFIX");
String email = TestDataFactory.generateEmail("user");

// 租户上下文管理
TenantTestHelper.setupTenantContext(1L);
TenantTestHelper.withTenant(2L, () -> {
    // 在租户2的上下文中执行
});

// 自定义断言
AssertionHelper.assertCollectionSize(list, 5);
AssertionHelper.assertStringNotBlank(str);
AssertionHelper.assertValidTenantId(tenantId);
```

## 测试基类特性

### BaseServiceTest
- ✅ 自动设置租户上下文
- ✅ 自动清理租户上下文
- ✅ 事务回滚支持（@Transactional）
- ✅ 租户切换方法
- ✅ 可覆盖租户ID和用户ID

### BaseControllerTest
- ✅ MockMvc支持
- ✅ ObjectMapper支持
- ✅ JSON转换方法
- ✅ 租户上下文管理
- ✅ 响应提取工具

### BaseTenantIsolationTest
- ✅ 租户切换方法（runAsTenant）
- ✅ 租户隔离断言
- ✅ 上下文验证方法
- ✅ 多租户场景测试支持

### BaseIntegrationTest
- ✅ 完整Spring上下文
- ✅ 异步操作等待
- ✅ 重试机制
- ✅ 继承所有BaseServiceTest特性

## 最佳实践

1. **始终使用测试基类** - 避免重复的上下文管理代码
2. **使用TestDataFactory生成测试数据** - 保证数据唯一性
3. **测试应该独立** - 不依赖执行顺序
4. **使用有意义的断言消息** - 便于定位问题
5. **测试边界条件** - 包括异常情况
6. **验证多租户隔离** - 确保数据安全

## 配置文件

在模块的`src/test/resources`目录下创建`application-test.yml`：

```yaml
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
    
evcs:
  tenant:
    enabled: true
    tenant-id-column: tenant_id
```

## 相关文档

- [完整测试指南](../../../../../../docs/TESTING-GUIDE.md)
- [多租户架构说明](../../../../../../README-TENANT-ISOLATION.md)

## 支持

如有问题，请参考完整的测试指南文档或联系开发团队。

