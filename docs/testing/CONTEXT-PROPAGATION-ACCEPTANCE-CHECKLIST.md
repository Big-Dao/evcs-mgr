# 多租户 / TraceId / 异步上下文传播 —— 落地验收清单

> 用途：为“多租户隔离 + traceId/requestId + 异步上下文传播”提供可执行的验收基线（测试 + 人工检查）。

**最后更新**: 2026-01-06  \
**维护者**: 架构团队 / 平台团队  \
**状态**: 已发布

---

## 适用范围

- 适用模块：所有后端服务与共享组件（尤其 `evcs-common`、对外调用较多的服务）
- 适用场景：
  - HTTP 请求链路（入站 → 业务 → 出站调用）
  - 异步执行（线程池 / `@Async` / 定时任务 / 事件监听）
  - 跨服务调用（Feign/RestTemplate/WebClient 等）

## 权威参考（SSOT / RFC）

- 编码与架构规范（SSOT）：[docs/overview/PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md)
- 租户异步上下文 RFC：[docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md](../architecture/TENANT-CONTEXT-ASYNC-RFC.md)
- 异步上下文传播概念文档：[docs/concepts/ASYNC-CONTEXT-PROPAGATION.md](../concepts/ASYNC-CONTEXT-PROPAGATION.md)

---

## 一、自动化验收（必须）

### 1) 单元测试（`evcs-common`）

验收点：

- **TenantContext 在线程切换时必须传播**，并且任务结束后 **不得泄漏** 到复用线程。
- **MDC（traceId/requestId）在线程切换时必须传播**，并且任务结束后 **不得泄漏**。
- **出站 HTTP headers 必须携带租户与 trace 信息**，且不得覆盖调用方已显式设置的 header。

对应测试（示例）：

- `TenantContextTaskDecoratorTest`：验证 TaskDecorator 传播 TenantContext + MDC，并在任务后恢复 worker-thread 旧值。
- `TenantContextPropagatingExecutorServiceTest`：验证包装器传播 TenantContext + MDC，并在任务后恢复 worker-thread 旧值。
- `TenantContextTaskDecoratorTest` / `TenantContextPropagatingExecutorServiceTest`：验证异常/取消等故障场景下也能恢复/清理上下文，避免复用线程污染。
- `OutgoingRequestContextHeadersTest`：验证 `OutgoingRequestContextHeaders.applyTo(HttpHeaders)` 的 header 填充与回退逻辑。
- `TraceMdcTest`：验证“非 HTTP 入口”（MQ / 定时任务）在当前执行范围内补齐 MDC(traceId/requestId)，并在结束后恢复/清理，避免复用线程污染。
- `OutboundHttpContextPropagationTest`（`evcs-payment`）：验证入站 HTTP header → RequestIdFilter/TenantInterceptor → 出站 RestTemplate 调用时，下游服务可收到租户与 trace headers。
- `OrderServiceClientResilienceTest`（`evcs-payment`）：验证出站调用具备显式超时，且在 5xx/网络异常触发重试时 traceId/requestId 不丢失、出站 header 保持透传。
- `WechatPayChannelServiceResilienceTest`（`evcs-payment`）：验证微信SDK调用具备显式超时，且对瞬态 5xx 触发重试时 traceId/requestId 不丢失（4xx 不重试）。
- `ReactorSchedulerContextPropagationTest`（`evcs-gateway`）：验证 `publishOn(evcsReactorScheduler)` 线程切换时仍能传播 TenantContext + MDC，且不泄漏。
- `ProtocolChargingEventListenerContextTest`（`evcs-order`）：验证 MQ consumer 入口在处理消息期间设置 TenantContext + MDC(traceId/requestId)，并在返回后清理/恢复。
- `OCPPSessionManagerTraceMdcTest`（`evcs-protocol`）：验证 `@Scheduled` 入口不会把补齐的 MDC(requestId) 泄漏到后续复用线程。

执行命令：

```bash
./gradlew :evcs-common:test --warning-mode all

# 可选：MQ consumer 入口链路验收（入口建立 trace + tenant，并确保不泄漏）
./gradlew :evcs-order:test --tests "com.evcs.order.mq.ProtocolChargingEventListenerContextTest" --warning-mode all

# 可选：@Scheduled 入口链路验收（确保 MDC 不泄漏到复用线程）
./gradlew :evcs-protocol:test --tests "com.evcs.protocol.websocket.OCPPSessionManagerTraceMdcTest" --warning-mode all

# 可选：出站 HTTP 真实链路验收（RestTemplate -> mock downstream server）
./gradlew :evcs-payment:test --tests "com.evcs.payment.integration.OutboundHttpContextPropagationTest" --warning-mode all

# 可选：出站 HTTP 超时/重试/trace 不丢失验收（OrderServiceClient -> mock downstream server）
./gradlew :evcs-payment:test --tests "com.evcs.payment.integration.OrderServiceClientResilienceTest" --warning-mode all

# 可选：微信SDK调用超时/重试/trace 不丢失验收
./gradlew :evcs-payment:test --tests "com.evcs.payment.service.channel.WechatPayChannelServiceResilienceTest" --warning-mode all

# 可选：包含 Reactor publishOn/subscribeOn 的验收
./gradlew :evcs-gateway:test --warning-mode all
```

### 2) 门禁规则（必须）

验收点：

- 禁止裸 `new Thread(...)` / `Executors.*`（生产代码），避免上下文丢失。
- 禁止未显式指定 `Executor` 的 `CompletableFuture.*Async` / `parallelStream()` 等导致上下文丢失的用法。

执行命令：

```bash
./gradlew check
```

---

## 二、人工检查清单（建议作为 Code Review 模板）

### 1) 入站链路（HTTP Request → 线程内上下文）

- [ ] 入口处能够解析并写入租户上下文（`TenantContext`）
- [ ] 入口处能够解析并写入 `traceId/requestId`（MDC）
- [ ] 日志格式中包含 `traceId`（兼容 `requestId`）
- [ ] 响应 `Result<T>` 自动带 `traceId`（业务代码不手动拼装）

### 2) 出站链路（HTTP Client）

- [ ] 出站请求使用统一的 header 注入工具（见 `OutgoingRequestContextHeaders.applyTo(...)`）
- [ ] `X-Trace-Id` 优先来自 MDC 的 `traceId`（缺失时回退 `requestId`）
- [ ] `X-Request-Id` 优先来自 MDC 的 `requestId`（缺失时回退 `traceId`）
- [ ] `X-Tenant-Id` / `X-User-Id` / `X-Tenant-Type` / `X-Tenant-Ancestors` 能随 TenantContext 透传
- [ ] 调用方已显式设置 header 时不被覆盖（避免二次封装时“意外改写”）

### 3) 异步执行（线程池 / @Async / 调度）

- [ ] Spring 管理的线程池配置了 `TenantContextTaskDecorator`（用于 TenantContext + MDC 传播）
- [ ] `@Async` 显式指定受管 executor（例如 `@Async("chargingExecutor")`）
- [ ] 任务执行结束后，worker thread 的 TenantContext 与 MDC 均会被清理/恢复（避免跨请求污染）
- [ ] 任何自建 `ExecutorService` 必须使用包装器（例如 `TenantContextPropagatingExecutorService.wrap(...)`）或等价机制

### 4) 无请求入口（MQ consumer / @Scheduled）

- [ ] MQ consumer 入口：在处理消息期间必须存在 MDC(traceId/requestId)，并优先从 messageId/eventId 衍生；处理结束后必须恢复/清理，避免复用线程污染
- [ ] MQ consumer 入口：若消息 payload 含 tenantId/userId，应在处理期间写入 `TenantContext`，并在 finally 中清理
- [ ] @Scheduled 入口：必须在执行期间确保 MDC(traceId/requestId) 存在，且执行结束后恢复/清理
- [ ] 推荐使用统一工具类（例如 `TraceMdc.ensureTracePresent()` / `TraceMdc.withTraceId(...)`）封装上述逻辑

### 5) 故障场景（异常 / 超时 / 取消）

- [ ] 即使异步任务抛异常或被取消，worker thread 的 TenantContext/MDC 也不会泄漏
- [ ] 超时/重试等机制不会吞掉 traceId（日志能串联）

---

## 四、交付与 CI 约定（团队建议）

目标：**CI 仍在跑时不需要开发者本地阻塞等待**，但合并必须以仓库 required checks 通过为准。

- 建议开启 GitHub Auto-merge：让 CI 在 GitHub Actions 上跑完后自动合并，开发者不必盯着等。
- 本地收尾可提前做：切回 `main`、`git fetch --prune`、保持工作区干净即可；不要在本地做“手工合并绕过 CI”。
- 查看 CI 是否跑完：
  - GitHub 页面：PR → Checks / Actions
  - 使用 `gh`：`gh pr checks <PR_NUMBER> --watch --interval 15`

---

---

## 三、常见反模式（必须避免）

- 生产代码中使用 `new Thread(...)` 或 `Executors.newFixedThreadPool(...)` 直接提交任务
- 使用 `CompletableFuture.supplyAsync(...)` 且未显式传入受管 `Executor`
- 在 worker thread 中写入 MDC/TenantContext 但不清理，导致复用线程污染
- 出站请求未携带 `X-Tenant-Id` 与 `X-Trace-Id`，导致跨服务链路断裂

---

## 五、实施锚点（Repo 关键类）

- TaskDecorator（Tenant + MDC）：`evcs-common/src/main/java/com/evcs/common/config/TenantContextTaskDecorator.java`
- ExecutorService 包装器（Tenant + MDC）：`evcs-common/src/main/java/com/evcs/common/executor/TenantContextPropagatingExecutorService.java`
- 非请求入口 Trace 保障（MDC traceId/requestId）：`evcs-common/src/main/java/com/evcs/common/trace/TraceMdc.java`
- 出站 header 注入：`evcs-common/src/main/java/com/evcs/common/http/OutgoingRequestContextHeaders.java`
- Header 常量：`evcs-common/src/main/java/com/evcs/common/http/EvcsHeaderNames.java`
