# 异步上下文传播（Asynchronous Context Propagation）

> **简介**：本文档解释什么是异步上下文传播，为什么需要它，以及在 EVCS 项目中如何使用它。

**最后更新**: 2025-12-18  
**维护者**: 技术负责人  
**状态**: 已发布

---

## 目录

- [什么是异步上下文传播](#什么是异步上下文传播)
- [为什么需要异步上下文传播](#为什么需要异步上下文传播)
- [问题示例](#问题示例)
- [解决方案](#解决方案)
- [EVCS 项目中的实现](#evcs-项目中的实现)
- [使用指南](#使用指南)
- [最佳实践](#最佳实践)
- [参考资料](#参考资料)

---

## 什么是异步上下文传播

**异步上下文传播**（Asynchronous Context Propagation）是指在异步编程中，将主线程（调用线程）的上下文信息传递到执行异步任务的工作线程的过程。

### 核心概念

在多线程和异步编程中：
- **主线程**：发起异步任务的线程（也叫"提交线程"或"调用线程"）
- **工作线程**：实际执行异步任务的线程（从线程池中获取）
- **上下文信息**：需要在线程间共享的数据，例如：
  - 租户 ID（Tenant ID）
  - 用户 ID（User ID）
  - 请求 ID（Request ID）
  - 安全凭证（Security Credentials）
  - 追踪信息（Trace Context）

### 为什么会有这个问题

Java 的 `ThreadLocal` 是线程本地存储，每个线程都有自己独立的副本。当我们：
1. 在主线程中设置了 `ThreadLocal` 值（如租户 ID）
2. 提交一个异步任务到线程池
3. 任务在**另一个线程**中执行

此时，工作线程看不到主线程的 `ThreadLocal` 值，导致**上下文丢失**。

---

## 为什么需要异步上下文传播

在 EVCS 这样的多租户系统中，异步上下文传播至关重要，原因如下：

### 1. 数据隔离安全

**问题**：如果异步任务中租户上下文丢失，可能导致：
- 查询时没有租户过滤条件 → 可能泄露其他租户的数据
- 插入/更新时缺少租户 ID → 数据归属混乱
- 权限检查失败 → 越权访问

**示例场景**：
```java
// 主线程：HTTP 请求处理
TenantContext.setTenantId(123L);  // 租户 123
orderService.createOrder(order);   // 创建订单

// 订单服务内部异步发送通知
@Async
public void sendNotification(Order order) {
    // ❌ 问题：这里 TenantContext.getTenantId() 可能返回 null
    // 或者返回线程池上一个任务遗留的错误租户 ID！
}
```

### 2. 审计和追踪

- 日志需要记录正确的租户 ID 和用户 ID
- 分布式追踪需要传递 trace ID 和 span ID
- 监控指标需要按租户分类统计

### 3. 业务逻辑正确性

许多业务逻辑依赖上下文信息：
- 计费规则可能因租户而异
- 用户权限需要根据当前用户判断
- 数据查询结果需要按租户过滤

---

## 问题示例

### 示例 1：租户 ID 丢失导致查询失败

```java
// ❌ 错误示例 - 上下文未传播

@Service
public class OrderService {
    
    @Async  // 使用未配置上下文传播的 executor
    public void processOrderAsync(Long orderId) {
        // 此时在工作线程中，TenantContext.getTenantId() 返回 null
        Order order = orderRepository.findById(orderId);  // ❌ SQL 无租户过滤
        // 可能返回其他租户的订单数据！
    }
}
```

**后果**：
- 如果 `findById` 依赖 MyBatis Plus 的租户插件，会因为缺少 `tenant_id` 而查询失败
- 如果手动拼接 SQL，可能查到其他租户的数据

### 示例 2：线程池复用导致上下文污染

```java
// ❌ 更严重的问题 - 线程池复用导致脏数据

// 第一个请求（租户 A）
TenantContext.setTenantId(100L);
executor.submit(() -> {
    // 工作线程 T1 执行任务
    TenantContext.setTenantId(100L);  // 手动设置
    processTask();
    // ❌ 忘记清理 TenantContext.clear()
});

// 第二个请求（租户 B）
TenantContext.setTenantId(200L);
executor.submit(() -> {
    // 工作线程 T1 被复用（线程池特性）
    // ❌ TenantContext.getTenantId() 返回 100（上一个任务遗留）
    processTask();  // 本应处理租户 B 的数据，却使用了租户 A 的上下文！
});
```

**后果**：
- 租户 B 的操作使用了租户 A 的上下文
- 数据写入错误的租户
- 严重的数据安全问题

---

## 解决方案

### 解决方案概述

异步上下文传播的核心思路：

1. **捕获**（Capture）：在任务提交时，捕获主线程的上下文快照
2. **传播**（Propagate）：将快照随任务一起提交到线程池
3. **应用**（Apply）：在工作线程执行任务前，应用快照到工作线程
4. **清理**（Clean）：任务执行后，清理工作线程的上下文，避免污染后续任务
5. **恢复**（Restore）：恢复工作线程执行前的上下文（如果有）

### 实现方式

有两种主要实现方式：

#### 方式 1：显式装饰器（TaskDecorator）

适用于 Spring 管理的 `ThreadPoolTaskExecutor`：

```java
@Bean
public ThreadPoolTaskExecutor taskExecutor(TenantContextTaskDecorator decorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setTaskDecorator(decorator);  // 关键：注册装饰器
    executor.initialize();
    return executor;
}
```

**优点**：
- 显式、易理解
- 易于测试和调试
- 无第三方依赖

**缺点**：
- 需要为每个 executor 显式配置
- 不适用于直接使用 `Executors.newXXX()` 创建的线程池

#### 方式 2：ExecutorService 包装器

适用于任意 `ExecutorService`：

```java
ExecutorService rawExecutor = Executors.newFixedThreadPool(10);
ExecutorService wrappedExecutor = new TenantContextPropagatingExecutorService(rawExecutor);

// 使用包装后的 executor，自动传播上下文
wrappedExecutor.submit(() -> {
    // 这里可以访问主线程的 TenantContext
});
```

**优点**：
- 适用于任何 `ExecutorService`
- 可以包装第三方库创建的线程池
- 统一的接口

**缺点**：
- 需要额外的包装对象
- 略微增加内存开销

---

## EVCS 项目中的实现

EVCS 项目实现了上述两种方式，可根据场景选择使用。

### 1. TenantContextTaskDecorator

**位置**：`evcs-common/src/main/java/com/evcs/common/config/TenantContextTaskDecorator.java`

这是一个 Spring `TaskDecorator` 实现，用于包装 Runnable 任务。

**工作原理**：
```java
@Component
public class TenantContextTaskDecorator implements TaskDecorator {
    
    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. 捕获：获取当前线程的租户上下文快照
        final TenantContextSnapshot captured = TenantContextSnapshot.capture();
        
        // 2. 返回包装后的 Runnable
        return () -> {
            // 3. 保存工作线程当前上下文（如果有）
            final TenantContextSnapshot previous = TenantContextSnapshot.capture();
            try {
                // 4. 应用：将捕获的上下文应用到工作线程
                captured.apply();
                
                // 5. 执行原始任务
                runnable.run();
            } finally {
                // 6. 清理：移除捕获的上下文
                TenantContext.clear();
                
                // 7. 恢复：恢复工作线程之前的上下文
                previous.apply();
            }
        };
    }
}
```

**快照内容**：
```java
private static class TenantContextSnapshot {
    private final Long tenantId;        // 租户 ID
    private final Long userId;          // 用户 ID
    private final Integer tenantType;   // 租户类型
    private final String tenantAncestors; // 租户层级路径
}
```

### 2. TenantContextPropagatingExecutorService

**位置**：`evcs-common/src/main/java/com/evcs/common/executor/TenantContextPropagatingExecutorService.java`

这是一个 `ExecutorService` 包装器，拦截所有任务提交方法。

**工作原理**：
```java
public class TenantContextPropagatingExecutorService implements ExecutorService {
    
    private final ExecutorService delegate;
    
    @Override
    public Future<?> submit(Runnable task) {
        // 包装任务，注入上下文传播逻辑
        return delegate.submit(wrapRunnable(task));
    }
    
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        // 同样支持 Callable
        return delegate.submit(wrapCallable(task));
    }
    
    // 其他方法类似...
}
```

### 3. AsyncConfig 配置

**位置**：`evcs-common/src/main/java/com/evcs/common/config/AsyncConfig.java`

项目已配置默认的异步执行器，自动启用上下文传播：

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Bean(name = {"chargingExecutor", "taskExecutor"})
    public ThreadPoolTaskExecutor chargingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 关键：设置 TaskDecorator
        executor.setTaskDecorator(tenantContextTaskDecorator);
        executor.initialize();
        return executor;
    }
    
    @Override
    public Executor getAsyncExecutor() {
        return chargingExecutor();  // 作为默认 @Async executor
    }
}
```

---

## 使用指南

### 场景 1：使用 Spring @Async 注解

**推荐**：不指定 executor 名称，使用默认配置（已启用上下文传播）

```java
@Service
public class OrderService {
    
    // ✅ 正确 - 使用默认 executor（已配置 TaskDecorator）
    @Async
    public void sendOrderNotification(Order order) {
        // 这里可以安全访问 TenantContext.getTenantId()
        Long tenantId = TenantContext.getTenantId();
        log.info("发送订单通知，租户: {}, 订单: {}", tenantId, order.getId());
    }
}
```

**或者**：显式指定使用 `chargingExecutor`

```java
@Service
public class OrderService {
    
    // ✅ 正确 - 显式指定 chargingExecutor
    @Async("chargingExecutor")
    public void processOrderAsync(Long orderId) {
        Long tenantId = TenantContext.getTenantId();  // ✅ 可以访问
        // 处理订单...
    }
}
```

### 场景 2：使用 ThreadPoolTaskExecutor Bean

```java
@Service
public class PaymentService {
    
    @Autowired
    @Qualifier("chargingExecutor")
    private ThreadPoolTaskExecutor executor;
    
    public void processPayment(Payment payment) {
        // ✅ 使用已配置的 executor，自动传播上下文
        executor.submit(() -> {
            Long tenantId = TenantContext.getTenantId();  // ✅ 可以访问
            processPaymentInternal(payment, tenantId);
        });
    }
}
```

### 场景 3：使用自定义 ExecutorService

如果需要使用 `Executors` 创建的线程池，使用包装器：

```java
@Service
public class ReportService {
    
    private final ExecutorService executor;
    
    public ReportService() {
        // 创建原始 executor
        ExecutorService rawExecutor = Executors.newFixedThreadPool(5);
        
        // ✅ 包装，启用上下文传播
        this.executor = new TenantContextPropagatingExecutorService(rawExecutor);
    }
    
    public void generateReportAsync(ReportRequest request) {
        executor.submit(() -> {
            Long tenantId = TenantContext.getTenantId();  // ✅ 可以访问
            generateReport(request, tenantId);
        });
    }
}
```

### 场景 4：CompletableFuture 异步编程

**问题**：`CompletableFuture.supplyAsync()` 默认使用 `ForkJoinPool.commonPool()`，不会传播上下文。

**解决方案**：显式指定使用已配置的 executor

```java
@Service
public class DataService {
    
    @Autowired
    @Qualifier("chargingExecutor")
    private Executor executor;
    
    public CompletableFuture<List<Data>> loadDataAsync() {
        // ✅ 指定使用已配置的 executor
        return CompletableFuture.supplyAsync(() -> {
            Long tenantId = TenantContext.getTenantId();  // ✅ 可以访问
            return dataRepository.findByTenantId(tenantId);
        }, executor);  // 关键：传入 executor
    }
}
```

### 场景 5：消息监听器中的异步操作

```java
@Component
public class OrderEventListener {
    
    @Autowired
    @Qualifier("chargingExecutor")
    private ThreadPoolTaskExecutor executor;
    
    @RabbitListener(queues = "order.events")
    public void handleOrderEvent(OrderEvent event) {
        // 1. 先设置主线程的上下文（从消息中提取）
        TenantContext.setTenantId(event.getTenantId());
        TenantContext.setUserId(event.getUserId());
        
        try {
            // 2. 提交异步任务（自动传播上下文）
            executor.submit(() -> {
                Long tenantId = TenantContext.getTenantId();  // ✅ 可以访问
                processOrderEvent(event, tenantId);
            });
        } finally {
            // 3. 清理主线程上下文
            TenantContext.clear();
        }
    }
}
```

---

## 最佳实践

### 推荐做法

#### 1. 优先使用已配置的 Spring Executor

```java
// ✅ 推荐
@Async  // 使用默认 executor
public void doSomething() { }

@Async("chargingExecutor")  // 或显式指定
public void doAnotherThing() { }
```

#### 2. 自定义 Executor 必须包装

```java
// ✅ 推荐 - 包装后再使用
ExecutorService executor = new TenantContextPropagatingExecutorService(
    Executors.newFixedThreadPool(10)
);
```

#### 3. 始终在 finally 中清理上下文

```java
// ✅ 推荐
public void handleRequest() {
    TenantContext.setTenantId(tenantId);
    try {
        // 业务逻辑
    } finally {
        TenantContext.clear();  // 必须清理
    }
}
```

#### 4. 异步任务内部也要遵循清理规则

```java
// ✅ 推荐
@Async
public void asyncTask() {
    // 虽然上下文已经传播进来，但如果任务内部修改了上下文
    // 仍然需要清理
    try {
        Long tenantId = TenantContext.getTenantId();
        // 业务逻辑...
    } finally {
        // 如果修改了上下文，确保清理
        // （TaskDecorator 会处理基本清理，但显式清理更安全）
    }
}
```

#### 5. 测试异步上下文传播

```java
// ✅ 推荐 - 为异步方法编写测试
@Test
void shouldPropagateContextToAsyncTask() throws Exception {
    TenantContext.setTenantId(123L);
    try {
        CompletableFuture<Long> future = service.getDataAsync();
        Long result = future.get(1, TimeUnit.SECONDS);
        assertEquals(123L, result);  // 验证上下文正确传播
    } finally {
        TenantContext.clear();
    }
}
```

### 避免做法

#### 1. 不要使用未配置的 Executor

```java
// ❌ 错误 - 不会传播上下文
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> {
    TenantContext.getTenantId();  // ❌ 返回 null 或错误值
});
```

#### 2. 不要忘记清理上下文

```java
// ❌ 错误 - 可能导致上下文泄漏
public void handleRequest() {
    TenantContext.setTenantId(tenantId);
    doSomething();
    // ❌ 忘记调用 TenantContext.clear()
}
```

#### 3. 不要在异步任务中手动设置上下文（除非必要）

```java
// ❌ 不推荐 - 上下文应该由框架自动传播
@Async
public void asyncTask(Long tenantId) {
    TenantContext.setTenantId(tenantId);  // ❌ 不必要，已自动传播
    // ...
}
```

#### 4. 不要使用默认的 ForkJoinPool

```java
// ❌ 错误 - 不会传播上下文
CompletableFuture.supplyAsync(() -> {
    TenantContext.getTenantId();  // ❌ 返回 null
});
```

应该改为：

```java
// ✅ 正确 - 指定 executor
CompletableFuture.supplyAsync(() -> {
    return TenantContext.getTenantId();
}, chargingExecutor);
```

#### 5. 不要在异步任务中访问 HTTP 请求对象

```java
// ❌ 错误 - 请求对象在另一个线程中不可用
@Async
public void asyncTask() {
    HttpServletRequest request = 
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest();  // ❌ 抛出异常或返回错误对象
}
```

应该提前提取需要的信息：

```java
// ✅ 正确 - 提前提取信息
public void handleRequest(HttpServletRequest request) {
    String userId = request.getHeader("User-Id");
    service.asyncTask(userId);  // 传递参数
}
```

---

## 常见问题（FAQ）

### Q1: 为什么不使用 TransmittableThreadLocal（TTL）？

**A**: 项目目前使用 Spring 原生的 `TaskDecorator` 方案，原因如下：
- ✅ 无第三方依赖，降低维护成本
- ✅ 显式控制，易于调试和审计
- ✅ 性能开销可控
- ✅ 测试简单直接

TTL 是备选方案，适用于以下场景：
- 代码库中有大量难以改造的第三方库
- 需要覆盖框架内部创建的线程池

详见 [RFC 文档](../architecture/TENANT-CONTEXT-ASYNC-RFC.md)。

### Q2: 上下文传播会影响性能吗？

**A**: 影响很小：
- 快照捕获：复制 4 个字段（tenantId、userId、tenantType、ancestors）
- 内存开销：每个任务额外占用约 50 字节
- CPU 开销：可忽略不计（纳秒级别）

在 EVCS 项目的基准测试中，上下文传播的开销 < 1%。

### Q3: 如何调试上下文传播问题？

**A**: 
1. 启用 TRACE 日志：
```yaml
logging:
  level:
    com.evcs.common.config.TenantContextTaskDecorator: TRACE
```

2. 在关键位置打印上下文：
```java
log.debug("当前上下文: {}", TenantContext.getContextInfo());
```

3. 使用断点调试，检查 `TenantContextSnapshot` 的值。

### Q4: 异步任务抛出异常时，上下文会泄漏吗？

**A**: 不会。`TaskDecorator` 和 `ExecutorService` 包装器都在 `finally` 块中清理上下文，确保异常情况下也能正确清理。

### Q5: 嵌套异步调用如何处理？

**A**: 自动支持。每次捕获快照时，都会获取当前线程的最新上下文，因此嵌套调用可以正确传播：

```java
@Async
public void outerTask() {
    TenantContext.setTenantId(100L);
    try {
        innerTask();  // 嵌套调用
    } finally {
        TenantContext.clear();
    }
}

@Async
public void innerTask() {
    Long tenantId = TenantContext.getTenantId();  // ✅ 正确获取 100L
}
```

---

## 参考资料

### 项目内部文档

- **[租户上下文异步传播 RFC](../architecture/TENANT-CONTEXT-ASYNC-RFC.md)** - 技术设计和迁移计划
- **[项目编码规范](../overview/PROJECT-CODING-STANDARDS.md)** - 多租户开发规范
- **[测试指南](../testing/UNIFIED-TESTING-GUIDE.md)** - 如何测试异步代码

### 源代码

- `evcs-common/src/main/java/com/evcs/common/config/TenantContextTaskDecorator.java`
- `evcs-common/src/main/java/com/evcs/common/config/AsyncConfig.java`
- `evcs-common/src/main/java/com/evcs/common/executor/TenantContextPropagatingExecutorService.java`
- `evcs-common/src/main/java/com/evcs/common/tenant/TenantContext.java`

### 测试代码

- `evcs-common/src/test/java/com/evcs/common/tenant/TenantContextTaskDecoratorTest.java`
- `evcs-common/src/test/java/com/evcs/common/executor/TenantContextPropagatingExecutorServiceTest.java`
- `evcs-common/src/test/java/com/evcs/common/tenant/TenantContextConcurrencyTest.java`

### 外部资源

- [Spring Framework - Task Execution](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling)
- [Java ThreadLocal Documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ThreadLocal.html)
- [TransmittableThreadLocal (TTL) - Alibaba](https://github.com/alibaba/transmittable-thread-local)

---

## 总结

**异步上下文传播**是多租户系统中的关键技术，用于确保异步任务能够访问正确的租户上下文，避免数据泄漏和权限问题。

EVCS 项目提供了两种实现方式：
1. **TenantContextTaskDecorator** - 适用于 Spring 管理的 executor
2. **TenantContextPropagatingExecutorService** - 适用于任意 `ExecutorService`

**关键要点**：
- ✅ 优先使用项目配置的 `chargingExecutor`
- ✅ 自定义 executor 必须包装
- ✅ 始终在 `finally` 中清理上下文
- ✅ 为异步代码编写测试

遵循本文档的最佳实践，可以确保异步代码的租户隔离安全。

---

**最后更新**: 2025-12-18  
**版本**: v1.0  
**反馈**: 如有问题或建议，请在项目 Issues 中提出。
