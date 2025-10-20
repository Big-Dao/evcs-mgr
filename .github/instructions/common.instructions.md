---
applyTo: "evcs-common/**/*.java"
---

# evcs-common 模块开发规范

本模块包含共享工具类、多租户框架和通用注解。此模块的变更会影响所有其他模块。

## 🚨 关键准则

### 1. 向后兼容性
**必须保持向后兼容**
- 不要在没有迁移计划的情况下修改现有公共 API
- 在删除功能之前先标记为 @Deprecated
- 提供清晰的升级路径和文档

### 2. 多租户框架
**这是租户隔离的核心实现**
- `TenantContext` - ThreadLocal 存储，必须正确管理
- `CustomTenantLineHandler` - SQL 过滤器，需要彻底测试
- `@DataScope` - 核心注解，变更需要全面测试

### 3. 零业务逻辑
**保持本模块不含业务逻辑**
- 只包含工具类、框架和横切关注点
- 业务逻辑属于具体的服务模块（evcs-station、evcs-order 等）

---

## ✅ 测试要求

在修改本模块代码时，必须包含以下测试：

- ✅ 多个并发租户上下文的测试
- ✅ 复杂查询的 SQL 过滤测试
- ✅ 不同注解组合的 AOP 切面测试
- ✅ 租户过滤逻辑的性能测试

---

## 📝 常见模式

### 租户上下文管理

```java
// 租户上下文管理 - 标准模式
public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    
    public static void setCurrentTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    public static void clear() {
        TENANT_ID.remove(); // 关键：必须在 finally 中调用
    }
}

// 使用示例
try {
    TenantContext.setCurrentTenantId(tenantId);
    // 执行业务逻辑
} finally {
    TenantContext.clear(); // 防止内存泄漏
}
```

### 自定义异常处理

```java
// 租户访问异常
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TenantAccessException extends RuntimeException {
    public TenantAccessException(String message) {
        super(message);
    }
}
```

---

## ⚠️ 重要注意事项

### 配置管理
- `IGNORE_TABLES` 列表的变更必须经过仔细审查
- 确保不会意外将需要租户隔离的表加入忽略列表

### 线程安全
- 租户上下文必须正确清理以防止泄漏
- 所有工具类应该是无状态的

### AOP 切面
- 所有 AOP 切面应该有正确的执行顺序（@Order）
- 避免切面之间的循环依赖

### 性能考虑
- 租户过滤逻辑会影响所有查询性能
- 变更后必须进行性能基准测试

---

## 🔧 修改本模块时的检查清单

- [ ] 是否保持了向后兼容性？
- [ ] 是否添加了充分的单元测试？
- [ ] 是否测试了多租户场景？
- [ ] 是否更新了相关文档？
- [ ] 是否考虑了性能影响？
- [ ] 是否避免了引入业务逻辑？

---

**最后更新**: 2025-10-20

