---
applyTo: "evcs-common/**/*.java"
---

# evcs-common 模块开发规范

> **最后更新**: 2025-12-18 | **维护者**: 技术负责人 | **状态**: 已发布

本模块包含共享工具类、多租户框架和通用注解。此模块的变更会影响所有其他模块。
请严格遵循 [PROJECT-CODING-STANDARDS.md](../../docs/overview/PROJECT-CODING-STANDARDS.md) 中的核心规范。

## 关键准则

### 1. 向后兼容性
**必须保持向后兼容**
- 不要在没有迁移计划的情况下修改现有公共 API
- 在删除功能之前先标记为 @Deprecated
- 提供清晰的升级路径和文档

### 2. 多租户框架
**这是租户隔离的核心实现**
- `TenantContext` - 核心上下文管理，必须正确管理
- **异步传播** - 必须遵循 [TENANT-CONTEXT-ASYNC-RFC.md](../../docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md) 规范
- `CustomTenantLineHandler` - SQL 过滤器，需要彻底测试
- `@DataScope` - 核心注解，变更需要全面测试

### 3. 零业务逻辑
**保持本模块不含业务逻辑**
- 只包含工具类、框架和横切关注点
- 业务逻辑属于具体的服务模块（evcs-station、evcs-order 等）

### 4. 可观测性与链路追踪
**全链路可追溯**
- **TraceID 透传**：所有跨服务调用（Feign/RestTemplate）和消息队列（RabbitMQ）必须携带 TraceID。
- **MDC 管理**：日志输出必须包含 `traceId` 和 `spanId`。
- **关键事件**：在业务关键节点（如下单、支付、发指令）必须打印包含 TraceID 的 INFO 日志。

---

## ✅ 测试要求

在修改本模块代码时，必须包含以下测试：

- ✅ 多个并发租户上下文的测试
- ✅ 复杂查询的 SQL 过滤测试
- ✅ 不同注解组合的 AOP 切面测试
- ✅ 租户过滤逻辑的性能测试

---

## 常见模式

### 租户上下文管理

> **重要提示**：关于异步任务中的上下文传播（线程池、@Async），请务必阅读 [TENANT-CONTEXT-ASYNC-RFC.md](../../docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md)。
> 禁止直接使用 `new Thread()` 或未装饰的线程池，这会导致租户上下文丢失。

```java
// 租户上下文使用示例（同步场景）
try {
    TenantContext.setCurrentTenantId(tenantId);
    // 执行业务逻辑
} finally {
    TenantContext.clear(); // 关键：必须在 finally 中调用防止泄漏
}
```

### 异步上下文传播

在配置线程池时，**必须**使用 `TenantContextTaskDecorator` 确保租户上下文正确传递到子线程。

```java
// 线程池配置示例
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // ... 核心参数配置 ...
    // 关键：设置任务装饰器以传播租户上下文
    executor.setTaskDecorator(new TenantContextTaskDecorator());
    executor.initialize();
    return executor;
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

## 重要注意事项

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

## 修改本模块时的检查清单

- [ ] 是否保持了向后兼容性？
- [ ] 是否添加了充分的单元测试？
- [ ] 是否测试了多租户场景？
- [ ] 是否更新了相关文档？
- [ ] 是否考虑了性能影响？
- [ ] 是否避免了引入业务逻辑？

---

**最后更新**: 2025-10-20

