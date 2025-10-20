---
applyTo: "**/src/test/**/*.java"
---

# 测试代码开发规范

全模块通用的测试代码编写指南。本规范适用于所有 `src/test` 目录下的 Java 测试类。

## 🏗️ 测试结构 (AAA 模式)

所有测试必须遵循 AAA 三段式模式：
- **Arrange** (准备)：设置测试数据与上下文
- **Act** (执行)：调用被测代码
- **Assert** (断言)：验证执行结果

```java
@Test
@DisplayName("保存站点 - 有效数据时应成功")
void testSaveStation_shouldSucceed_whenValidData() {
    // Arrange - 准备测试数据
    TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
    Station station = TestDataFactory.createStation("Test");
    
    try {
        // Act - 执行被测方法
        Station saved = stationService.save(station);
        
        // Assert - 验证结果
        assertNotNull(saved.getId());
        assertEquals(DEFAULT_TENANT_ID, saved.getTenantId());
    } finally {
        TenantContext.clear();
    }
}
```

## 📝 命名规范

### 测试方法命名模板

```java
@Test
@DisplayName("操作名 - 预期行为 在特定条件下")
void test操作名_should预期行为_when特定条件() {
    // 测试实现
}
```

### 命名示例（优先使用英文方法名）

- `testSaveStation_shouldSucceed_whenValidData()` - 保存站点应成功_当数据有效时
- `testFindStation_shouldThrowException_whenNotFound()` - 查找站点应抛异常_当不存在时
- `testListStations_shouldReturnOnlyTenantData_whenMultipleTenants()` - 列表站点应只返回租户数据_当多租户时
- `testUpdateStation_shouldFail_whenTenantMismatch()` - 更新站点应失败_当租户不匹配时
- `testDeleteStation_shouldSoftDelete_whenHasHistory()` - 删除站点应软删除_当有历史时

### DisplayName 中文说明

使用 `@DisplayName` 为测试添加清晰的中文描述：
```java
@DisplayName("查询站点列表 - 多租户场景下应只返回当前租户数据")
```

## 🧪 基础测试类

**必须**继承合适的基础测试类，不要从零开始编写测试基础设施：

| 基类 | 用途 | 提供能力 |
|------|------|----------|
| `BaseServiceTest` | Service 层测试 | 事务回滚、租户上下文托管 |
| `BaseControllerTest` | Controller 层测试 | MockMvc、JWT 注入、统一返回解析 |
| `BaseTenantTest` | 多租户隔离测试 | 多租户数据隔离验证工具 |

```java
@SpringBootTest
class StationServiceTest extends BaseServiceTest {
    @Autowired
    private StationService stationService;
    
    // 自动获得租户上下文管理与事务回滚能力
}
```

## 🔒 租户上下文管理

### ⚠️ 关键原则：**必须在 finally 中清理**

```java
@Test
@DisplayName("带租户上下文的测试")
void testWithTenant() {
    // Arrange - 设置租户上下文
    TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
    try {
        // Act & Assert - 执行测试
        // 测试代码
    } finally {
        TenantContext.clear(); // 🚨 必须清理，否则影响其他测试
    }
}
```

### 多租户切换场景

```java
@Test
@DisplayName("多租户切换 - 验证数据隔离")
void testMultipleTenantSwitch() {
    try {
        // 租户 1 创建数据
        TenantContext.setCurrentTenantId(TENANT_1_ID);
        Entity entity1 = service.save(createTestEntity("Tenant1"));
        
        // 租户 2 创建数据
        TenantContext.setCurrentTenantId(TENANT_2_ID);
        Entity entity2 = service.save(createTestEntity("Tenant2"));
        
        // 租户 1 查询，应只看到自己的数据
        TenantContext.setCurrentTenantId(TENANT_1_ID);
        List<Entity> results = service.list();
        assertEquals(1, results.size());
        assertEquals(entity1.getId(), results.get(0).getId());
    } finally {
        TenantContext.clear();
    }
}
```

## 🏭 测试数据生成

### 使用 TestDataFactory

**必须**使用 `TestDataFactory` 生成唯一测试数据，避免硬编码导致冲突：

```java
// ✅ 推荐 - 自动生成唯一标识
String stationCode = TestDataFactory.generateCode("STATION");
String connectorCode = TestDataFactory.generateCode("CONNECTOR");

// ❌ 禁止 - 硬编码会导致并发测试冲突
String stationCode = "TEST001";
```

### 测试实体工厂方法

```java
@Test
@DisplayName("使用工厂方法创建测试数据")
void testUsingFactory() {
    TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
    try {
        // 使用工厂方法创建完整测试数据
        Station station = TestDataFactory.createStation("TestStation");
        Connector connector = TestDataFactory.createConnector(station.getId(), "AC");
        
        // 数据已包含必要字段，直接使用
        assertNotNull(station.getCode());
        assertTrue(station.getCode().startsWith("STATION_"));
    } finally {
        TenantContext.clear();
    }
}
```

## 🛡️ 多租户隔离测试

### 必测场景

**每个实体操作都必须验证租户隔离**，包括：
1. **查询隔离**：只能查到当前租户数据
2. **更新隔离**：不能修改其他租户数据
3. **删除隔离**：不能删除其他租户数据

### 标准隔离测试模板

```java
@Test
@DisplayName("列表查询 - 应只返回当前租户数据")
void testList_shouldReturnOnlyCurrentTenantData() {
    try {
        // Arrange - 为两个租户创建数据
        TenantContext.setCurrentTenantId(TENANT_1_ID);
        Entity entity1 = service.save(createTestEntity("Tenant1Entity"));
        
        TenantContext.setCurrentTenantId(TENANT_2_ID);
        Entity entity2 = service.save(createTestEntity("Tenant2Entity"));
        
        // Act - 作为租户 1 查询
        TenantContext.setCurrentTenantId(TENANT_1_ID);
        List<Entity> results = service.list();
        
        // Assert - 只返回租户 1 的数据
        assertEquals(1, results.size(), "应只返回当前租户的 1 条数据");
        assertEquals(entity1.getId(), results.get(0).getId());
        assertEquals(TENANT_1_ID, results.get(0).getTenantId());
    } finally {
        TenantContext.clear();
    }
}

@Test
@DisplayName("更新操作 - 不能修改其他租户数据")
void testUpdate_shouldFail_whenCrossTenantAccess() {
    try {
        // Arrange - 租户 2 创建数据
        TenantContext.setCurrentTenantId(TENANT_2_ID);
        Entity entity = service.save(createTestEntity("Tenant2Entity"));
        Long entityId = entity.getId();
        
        // Act - 租户 1 尝试更新租户 2 的数据
        TenantContext.setCurrentTenantId(TENANT_1_ID);
        Entity toUpdate = new Entity();
        toUpdate.setId(entityId);
        toUpdate.setName("Hacked");
        
        // Assert - 应更新失败或抛出异常
        assertThrows(Exception.class, () -> {
            service.updateById(toUpdate);
        }, "不应能更新其他租户的数据");
    } finally {
        TenantContext.clear();
    }
}
```

## ⚡ 性能测试

对性能敏感的代码添加基准测试：

```java
@Test
@DisplayName("批量操作 - 应在可接受时间内完成")
void testBulkOperation_shouldCompleteWithinAcceptableTime() {
    TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
    try {
        long start = System.currentTimeMillis();
        
        // 执行批量操作
        List<Entity> entities = generateTestEntities(1000);
        service.saveBatch(entities);
        
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 5000, 
            String.format("批量保存 1000 条应在 5 秒内完成，实际耗时 %dms", duration));
    } finally {
        TenantContext.clear();
    }
}

@Test
@DisplayName("复杂查询 - 应有索引优化")
void testComplexQuery_shouldHaveIndexOptimization() {
    TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
    try {
        // 准备大量测试数据
        prepareTestData(10000);
        
        long start = System.currentTimeMillis();
        List<Entity> results = service.complexQuery(params);
        long duration = System.currentTimeMillis() - start;
        
        assertTrue(duration < 1000, "复杂查询应在 1 秒内完成（需检查索引）");
    } finally {
        TenantContext.clear();
    }
}
```

## ✅ 断言最佳实践

### 必须添加断言消息

```java
// ✅ 推荐 - 失败时能快速定位问题
assertEquals(expected, actual, "站点编码应与预期一致");
assertTrue(result.getTenantId().equals(TENANT_1_ID), 
    "返回数据的 tenant_id 必须是当前租户");

// ❌ 禁止 - 失败时无法判断原因
assertEquals(expected, actual);
assertTrue(condition);
```

### 验证租户字段

```java
@Test
@DisplayName("创建实体 - 应自动填充 tenant_id")
void testCreate_shouldAutoFillTenantId() {
    TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
    try {
        Entity entity = new Entity();
        entity.setName("Test");
        // 注意：不手动设置 tenantId
        
        Entity saved = service.save(entity);
        
        // 验证 MyBatis Plus 自动填充
        assertNotNull(saved.getTenantId(), "tenant_id 应被自动填充");
        assertEquals(DEFAULT_TENANT_ID, saved.getTenantId(), 
            "tenant_id 应为当前上下文租户");
    } finally {
        TenantContext.clear();
    }
}
```

### 边界条件测试

```java
@Test
@DisplayName("边界条件测试")
void testBoundaryConditions() {
    TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
    try {
        // null 值
        assertThrows(IllegalArgumentException.class, 
            () -> service.save(null), "保存 null 应抛出异常");
        
        // 空列表
        List<Entity> emptyList = service.listByIds(Collections.emptyList());
        assertTrue(emptyList.isEmpty(), "空 ID 列表应返回空结果");
        
        // 大数据集
        List<Entity> largeList = service.list(generateLargeQueryParams(10000));
        assertTrue(largeList.size() <= 10000, "应支持大数据集查询");
    } finally {
        TenantContext.clear();
    }
}
```

## 🚫 不要测试的内容

为提高测试效率，**不要**测试以下内容：

- ❌ 框架功能（Spring、MyBatis Plus 的标准行为）
- ❌ Getter/Setter（除非有业务逻辑）
- ❌ 明显的委托调用（仅转发到其他服务）
- ❌ DTO 转换（除非有复杂映射逻辑）
- ✅ **专注于**：业务逻辑、多租户隔离、数据一致性

## 🔧 测试覆盖率要求

| 层级 | 最低覆盖率 | 说明 |
|------|-----------|------|
| Service 层 | 80% | 核心业务逻辑必须覆盖 |
| Controller 层 | 70% | 主要路径与权限验证 |
| Entity/DTO | 不要求 | 简单 POJO 无需测试 |
| Util 工具类 | 90% | 通用工具必须高覆盖 |

运行覆盖率报告：
```bash
./gradlew test jacocoTestReport
# 报告位置: build/reports/jacoco/test/html/index.html
```

## 📋 测试代码检查清单

编写或修改测试时，确认以下各项：

- [ ] 继承了合适的基础测试类（`BaseServiceTest`/`BaseControllerTest`）
- [ ] 使用 `@DisplayName` 添加中文说明
- [ ] 测试方法遵循 AAA 模式（Arrange - Act - Assert）
- [ ] 使用 `TestDataFactory` 生成唯一测试数据
- [ ] 在 `finally` 块中清理 `TenantContext`
- [ ] 验证了多租户隔离（查询/更新/删除）
- [ ] 所有断言都有清晰的错误消息
- [ ] 测试了正常场景 + 异常场景
- [ ] 验证了 `tenant_id` 字段正确填充
- [ ] 对性能敏感代码添加了基准测试
- [ ] 测试数据在测试后自动回滚（事务管理）
- [ ] 没有测试框架标准功能或简单 Getter/Setter

---

**最后更新**: 2025-10-20  
**维护说明**: 本规范是测试代码质量的基准，任何违反规范的测试代码都可能导致 CI 失败。
