# RFC：租户上下文在异步任务中的传播  
比较 `TaskDecorator` 与 `TransmittableThreadLocal`（TTTL）并提出迁移计划

> **版本**: v0.2-draft | **最后更新**: 2025-11-10 | **作者**: 架构 / 平台团队 | **状态**: Draft
>
> 🧵 **范围**: 说明租户上下文在异步/线程池场景的传播方案与迁移计划

---

## 概要（Summary）
本 RFC 讨论在异步/线程池场景下如何可靠、安全、可测试地把租户上下文（`TenantContext`）从调度线程传递到执行线程的两种方案：  
- 方案 A：基于 Spring 的 `TaskDecorator`（显式在 `ThreadPoolTaskExecutor`/提交点装饰任务）  
- 方案 B：采用第三方 `TransmittableThreadLocal`（自动传播，通过替换 `ThreadLocal` 或包装 executor）

文档比较两者的优缺点、测试/回滚策略、迁移步骤与时间估算，并给出推荐路线（保守分阶段迁移 + 针对性引入 TTTL 的备选路径）。

---

## 背景（Background）
项目中通过 `TenantContext`（基于 `ThreadLocal`）保存当前请求/消息处理链路的租户信息（`tenantId`、`userId`、`tenantType`、`ancestors` 等），并在以下位置依赖它实现租户隔离：  
- HTTP 请求链（`TenantInterceptor`）  
- 消息消费者（RabbitMQ listener 等）在处理消息时设置 `TenantContext`  
- MyBatis 多租户处理器 `CustomTenantLineHandler`、`@DataScope` 验证等  

问题来源于：线程池会复用线程，`ThreadLocal` 的值不会自动从提交线程传给工作线程，导致异步任务看到“空的”或“错误的”租户上下文，导致权限/隔离问题或数据污染。我们需要一套可推广的、可验证的方案将 `TenantContext` 在异步边界处正确传递并避免泄露。

---

## 目标（Goals）
- 确保异步任务在执行时能访问正确的 `TenantContext`，避免跨租户数据泄漏或权限判断失效。  
- 方案应易于测试、故障可回退、对性能影响可控，并便于在现有模块逐步推广。  
- 尽量减少对现有业务代码侵入（但要保证可见性与审计性）。

---

## 方案概述（Options）

### 方案 A：`TaskDecorator`（当前已实现的显式方式）
核心思想：在 `ThreadPoolTaskExecutor` 上注册一个 `TaskDecorator`（例如 `TenantContextTaskDecorator`），该装饰器在任务提交时捕获提交线程的 `TenantContext` 快照，任务在执行前将该快照应用到执行线程，执行结束后恢复/清理执行线程的原上下文。

关键点：
- 对 Spring 管理的 `ThreadPoolTaskExecutor` 直接生效（设置 `executor.setTaskDecorator(decorator)`）。  
- 对直接使用 `Executors.newXXX()` 创建的 `ExecutorService`，需要在提交点手动装饰任务或用一个包装的 `ExecutorService`（`TenantContextPropagatingExecutorService`）替换。  
- 需要保证任务内部和消息消费者在 `finally` 中清理 `TenantContext`（已是现有最佳实践）。

优点：
- 无需第三方依赖，基于 Spring 官方 API，语义明确。  
- 显式、易于测试（可直接对 `decorate()` 行为添加单元测试）。  
- 恢复旧上下文/防止泄漏的控制权在我们手里，便于审计与排查。

缺点：
- 需要在每个 executor（或每处显式提交点）注册装饰器或手动装饰任务，可能会遗漏某些第三方库创建的线程池。  
- 如果代码库中存在大量 `Executors.*` 使用点，改造工作量较大。

适用场景：
- 我们主导创建并注入 executor 的场景（大部分服务代码）。  
- 希望显式控制、审计上下文传播的场景。

实现参考：
- 我们已在 `evcs-common` 中新增 `TenantContextTaskDecorator` 与 `AsyncConfig`（示例文件：`evcs-common/src/main/java/com/evcs/common/config/TenantContextTaskDecorator.java` 和 `AsyncConfig.java`）。

---

### 方案 B：`TransmittableThreadLocal`（TTTL，阿里开源库）
核心思想：使用 `TransmittableThreadLocal`（或其包装能力）替代普通 `ThreadLocal`，并在创建/注册线程池时通过库提供的包装器（例如 `TtlExecutors.getTtlExecutorService(...)` 或 `TtlRunnable.get(...)`）或 agent 方案，使得线程池在任务提交时自动复制上下文。

关键点：
- 可把 `TenantContext` 的内部 `ThreadLocal` 替换为 `TransmittableThreadLocal`（或在提交时将 Runnable/Callable 用 `TtlRunnable/TtlCallable` 包装）。  
- 库提供的 `TtlExecutors` 可包装已有的 `ExecutorService`，对第三方创建线程池也能生效（如果能在创建点包装）。  
- 有 plugin/agent 能做字节码级别增强，但那通常更复杂并且在运行环境受限。

优点：
- 更“自动化”，无须在每个提交点显式装饰，能覆盖更多第三方/框架创建的线程池（但前提是你在创建点/入口用包装器或 agent）。  
- 对大量分散调用点的代码库（大量直接 `Executors.*`）改造工作量小。  

缺点与风险：
- 引入第三方依赖（需评估许可、兼容性、运维成本）。  
- 传播语义隐式，可能会把不应该传播的 `ThreadLocal`（或测试中临时设置的 `ThreadLocal`）一并传播，造成意外行为或内存泄漏风险。  
- 在某些场景（例如 JDK `ForkJoinPool.commonPool()`、框架内部线程池）需要额外包装或代理，不一定 100% 自动。  
- 测试/排查难度上升：隐式传播会掩盖问题源头，可能导致测试环境与生产行为不一致。  
- 需要对现有 `TenantContext.clear()` 语义做严格审视，确保任务结束后不会留下脏值（TTTL 在包装里通常会有 restore 语义，但仍需覆盖验证）。

适用场景：
- 代码库中大量不可控/分散的线程池创建点，难以通过 `TaskDecorator` 在短期内逐一修复时。  
- 团队愿意引入并承担第三方库风险，接受隐式传播的后果并能在 CI/integration 加强检测。

---

## 详细对比（关键信息点）

- 可见性 / 显式性
  - `TaskDecorator`：显式（推荐），所有传播点可在代码审查时看到（executor 注册/提交点）。  
  - TTTL：隐式，传播在包装或库内部发生，可导致审计盲区。

- 覆盖范围
  - `TaskDecorator`：只覆盖你设置了 decorator 的 `ThreadPoolTaskExecutor`，和所有显式使用装饰器的 `Executors` 提交点。  
  - TTTL：若全面包装所有 executor 或使用 agent，可实现更广覆盖，包括第三方库。

- 依赖与风险
  - `TaskDecorator`：零第三方依赖，风险小。  
  - TTTL：新增第三方依赖，需评估许可、维护、冲突（例如 Tomcat、Spring Boot 版本兼容）。

- 性能
  - 两者均涉及快照与复制开销。一般来说开销微小（捕获少数字段），`TaskDecorator` 性能可控（我们只复制必要字段）。TTTL 可能复制所有 `TransmittableThreadLocal` 字段，开销且不可控。  
  - 需要在目标环境中做基准测试（并发场景、任务粒度小/大两类场景）。

- 恢复/清理语义
  - `TaskDecorator`：我们控制捕获与恢复行为，能在装饰器中清理并恢复旧上下文。  
  - TTTL：包装器通常实现 restore，但必须验证在复杂场景（嵌套提交、异常路径）下语义是否总是正确。

- 可测试性
  - `TaskDecorator`：易于单元测试和并发测试（已增加示例测试）。  
  - TTTL：测试覆盖需要同时包装 executor，隐式状态可能让测试更难设定边界。

---

## 推荐（Decision）
我建议采用“以 `TaskDecorator` 为主、选择性引入 TTTL 的混合策略（Conservative then Evaluate）”，理由如下：

1. 立即可落地、低风险：`TaskDecorator` 已被实现并通过单元测试，适合快速在我们控制的 executor（`ThreadPoolTaskExecutor`）上推广。  
2. 显式优先：我们需要在短期内确保关键业务路径（订单、充电桩、支付）安全，显式方案便于代码审查与回滚。  
3. 对于难以一次性改造的场景（大量第三方库或分散 `Executors` 调用），在完成 `TaskDecorator` 覆盖后进行评估——若清点显示仍存在大量无法覆盖的遗留调用，再考虑引入 TTTL，但需满足更严格的测试与审计门槛。

换言之：先用 `TaskDecorator` 把“可控的部分”修好；再决定是否引入 TTTL 来覆盖剩余“不可控”的部分（并且在引入前做小范围试点与严格测试）。

---

## 迁移计划（分阶段、可回滚）

总体原则：逐步推进、模块优先（先关键业务线：`evcs-order`、`evcs-station`、`evcs-payment`）、每个阶段均包含自动化并发回归测试。

阶段 0 — 准备（1-2 天）
- 审计/清点：列出全仓库中所有 executor 创建点与 `@Async` 使用点（包括直接 `Executors.*`）。生成清单（优先级标签：high/medium/low）。  
- 确认标准：定义通过的并发测试与泄露检测用例（如 `TenantContextTaskDecoratorTest` 一类）。  
- 文档：把本 RFC 更新到 `evcs-mgr/docs/TENANT-CONTEXT-ASYNC-RFC.md` 并在团队内部同步。

阶段 1 — 在 `evcs-common` 与核心 executor 上部署 `TaskDecorator`（1-3 天）
- 已完成：`TenantContextTaskDecorator` 与 `AsyncConfig`（将 `chargingExecutor` 作为默认 `taskExecutor`）。  
- 任务：
  - 在所有 Spring 管理的 `ThreadPoolTaskExecutor` bean 上注册 `TenantContextTaskDecorator`。  
  - 把 `@Async` 不指定 executor 的默认执行器指向已装饰的 `taskExecutor`（避免遗漏）。  
  - 增加 `AsyncUncaughtExceptionHandler` 日志里输出 `TenantContext.getContextInfo()` 以便排查异步异常时能看到租户上下文。  
- 验证：
  - 在 CI 中运行新增的并发测试及集成测试（消息消费 + 异步业务流程）。  
  - 手工回归：关键业务用例在 dev env 通过。

阶段 2 — 改造直接 `Executors.*` 与第三方未被覆盖点（2-5 天）
- 两条可选策略（按项目实际情况选其一或混合）：
  - A. 逐个替换/封装：为每个直接创建的 `ExecutorService` 提供替换实现（或使用 `TenantContextPropagatingExecutorService`），在 `submit/execute` 时自动调用 `decorator.decorate(...)`。  
  - B. 统一工厂：在代码库提供 `ExecutorFactory`（或 `ExecutorUtils`），替代直接 `Executors` 的调用点，方便集中管理与装饰。  
- 任务：
  - 实现 `TenantContextPropagatingExecutorService`（包装现有 executor 的 `submit` / `execute` 等，自动装饰任务）。  
  - 在清单中高优先级位置替换为包装器/工厂调用。  
- 验证：
  - 针对替换点运行并发泄漏测试；在集成环境观察任务执行的租户正确性。

阶段 3 — 覆盖消息/事件路径（1-3 天）
- 要求消息消费者（Rabbit listener 等）在处理消息时：
  - 在进入消息处理时 `TenantContext.setTenantId(event.getTenantId())` / `setUserId(...)`；在 `finally` 中 `TenantContext.clear()`（已有实践）。  
  - 如果消息处理触发异步任务，务必使用装饰后的 executor（或手动 `decorate`）。  
- 验证：端到端消费 + 异步处理集成测试。

阶段 4 — 试点 TTTL（可选，视 Phase1/2 覆盖情况而定，3-7 天）
- 条件：清点后仍有大量不可控制的 executor（第三方库/插件/框架）且逐一改造成本过高。  
- 试验步骤：
  1. 在一个非关键模块（例如 `evcs-monitoring` 或 `evcs-integration` 的某个子系统）引入 `com.alibaba:transmittable-thread-local` 指定版本并在本模块进行小范围封装（仅覆盖模块内 executor）。  
  2. 通过 `TtlExecutors.getTtlExecutorService(...)` 包装最难改造的 executor，加入详尽的并发与回归测试，观察行为差异与可能的副作用（例如测试污染、性能下降、内存占用、隐式传播项）。  
  3. 如果试点通过，再评估是否将 TTTL 扩展到其它必须覆盖的不可控场景；同时保留 `TaskDecorator` 做为首选与补充方案。  
- 风险控制：引入 TTTL 必须在 CI 增加严格的泄漏检测（线程池重用场景、nested async、异常路径）和性能基准。

阶段 5 — 回滚与长驻检测（持续）
- 回滚计划：若新改动导致问题，通过 feature branch 回退 executor 改造或禁用 TTTL 引入；由于 `TaskDecorator` 显式并可快速回退，风险与成本低。  
- 长期监控：增加运行时指标、异常跟踪（记录 tenantId），并定期运行并发泄漏检测测试。

---

## 测试矩阵（建议自动化测试覆盖）
- 单元测试
  - `TenantContextTaskDecorator`：快照/恢复/异常路径/嵌套提交覆盖（已有示例）。  
  - `TenantContextPropagatingExecutorService`：submit/execute/submit Callable 行为、restore 语义。  
- 并发测试（必需）
  - 高并发线程池复用场景：100 线程 × N 次提交，检测租户污染（类似 `TenantContextConcurrencyTest`）。  
- 集成测试
  - HTTP 请求 → 异步任务 → DB 操作（验证 `tenant_id`）；消息消费者 → 异步任务 → DB 操作；在每条路径断言租户隔离。  
- 负载/基准测试（optional but recommended）
  - 在并发任务短粒度场景下衡量开销（装饰器 vs TTTL），观测延迟、吞吐与 GC/内存影响。  

---

## 监控 & 可观测性
建议：
- 异步未捕获异常日志中附带 `TenantContext.getContextInfo()`（已在 `AsyncConfig` 的 `AsyncUncaughtExceptionHandler` 中增加）。  
- 新增度量指标：
  - `async.tasks.propagation.failures`（传播失败/异常次数）  
  - `async.tasks.leak_detected`（在定期自检或 CI 并发测试中检测到租户上下文泄漏的事件计数）  
- 在发生异常时，将 tenantId 作为关键维度传入日志与追踪（确保敏感数据遮盖策略依旧被遵守）。

---

## 回滚 / 失败应对策略
- 若在部署后发现租户上下文污染或隐式传播副作用：  
  1. 先回滚最近变更的 executor 注册/包装。  
  2. 在短期内对关键路径强制做“任务内验证”（在关键业务异步任务入口断言 `TenantContext.getTenantId() != null`，以便快速发现问题）。  
  3. 分段恢复：先把 `TaskDecorator` 部署到少量关键模块，验证通过再放开。

---

## 估算工时（粗略）
- 阶段 0（审计与准备）：1–2 天  
- 阶段 1（核心 executor 部署与测试）：1–3 天  
- 阶段 2（替换/包装 `Executors` 使用点）：2–5 天（视代码散布程度）  
- 阶段 3（消息路径集成测试）：1–3 天  
- 阶段 4（若需 TTTL 试点并评估）：3–7 天  
总计（保守）：约 1–3 周（按模块与优先级分批推进）

---

## 开放问题（需团队决策）
1. 我们是否允许在平台级别引入第三方依赖（TTTL）？对许可与长期维护是否接受？  
2. 是否愿意把 `TaskDecorator` 的默认注册作为代码审查强约定（即新建 executor 必须注入装饰器）？  
3. 是否需要为某些关键路径（计费、支付）设置 CI 阈值（例如并发隔离测试必须通过才能合并）？  
4. 对于遗留第三方库（创建内部 thread pool），我们是否有权限/能力在创建点插桩或替换为包装后的 executor？

---

## 决策请求（Decision Required）
我建议我们按推荐路径执行：立即把 `TaskDecorator` 在可控 executor 上全量部署（Phase 1），同时启动审计（Phase 0）。审计结果决定是否需要引入 TTTL（Phase 4）。请团队批准以下内容以便开始执行：  
- 批准 Phase 0 + Phase 1 的改动（低风险、立即执行）。  
- 指派一位负责人做审计（Phase 0 的执行与清单验收）。  
- 确定是否允许在试点模块中评估 TTTL（答复“是/否”并指定试点模块）。

---

## 参考（Repo 文件/类）
- 已实现（示例）：`evcs-common/src/main/java/com/evcs/common/config/TenantContextTaskDecorator.java`（`TaskDecorator` 实现）  
- 已实现（示例）：`evcs-common/src/main/java/com/evcs/common/config/AsyncConfig.java`（注册 decorated `ThreadPoolTaskExecutor`，并设置 `AsyncUncaughtExceptionHandler`）  
- 需要新增/改造（建议）：`TenantContextPropagatingExecutorService`（包装器实现，用于替代或包装直创建的 `ExecutorService`）

---

## 附录：快速对照表（简要）
- 可控 executor（Spring bean） → 优先使用 `TaskDecorator`（显式、低风险）  
- 不可控或分散 executor（第三方/库内部） → 考虑 TTTL 包装或 agent；优先先在模块内试点并严格测试  
- 迁移优先级：关键业务（订单/支付/站点） > 辅助业务 > 工具/集成测试模块

---

如果你同意上述推荐，我可以：
- 立刻在仓库中开始 Phase 0（生成 executor 清单并标注优先级），并把结果提交给你；  
- 或者直接按 Phase 1 对 `evcs-order`、`evcs-station` 的 executor 注册 `TenantContextTaskDecorator` 并增加必要的并发集成测试。  

请回复你希望我下一步执行的具体项（例如：“开始审计清单” 或 “直接在 evcs-order 部署 TaskDecorator”）。谢谢。
