# EVCS 充电站管理系统 — 编程与架构规范（SSOT）

版本：v1.1｜最后更新：2025-11-10｜维护：技术负责人｜状态：活跃

本文件为“单一来源”（Single Source of Truth，SSOT）编码与架构规范。助手使用说明请参见 `docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md`，不要在本文件重复助手指令。

## 范围
- 适用层级：后端服务与共享组件（Controller／Service／Domain／Repository）
- 适用主题：分层与依赖约束、编码规范、异常与日志、缓存与性能、多租户上下文、测试与质量门禁

## 目录
- 强制架构规范
  - 微服务模块划分
  - 严格分层架构
  - 严禁的架构违规
- 编码标准
  - 命名规范
  - 异常处理
  - 日志记录
  - 参数校验
- 技术栈约束
  - 后端技术栈
  - 缓存策略
  - 多租户隔离实现
- 严格禁止的模式
  - 跨服务数据库访问
  - 硬编码敏感配置
  - 直接暴露 Entity
- 质量与性能要求
  - 测试覆盖率
  - 代码质量检查
  - 性能与监控
- 相关文档索引

---

## 强制架构规范

### 1. 微服务模块划分（示例）
```
evcs-gateway (8080)     - API网关，路由与安全防护（统一鉴权）
evcs-auth (8081)        - 认证授权服务，JWT + RBAC + 多租户上下文
evcs-station (8082)     - 充电站管理，设备控制与状态同步
evcs-order (8083)       - 订单管理，计费方案，订单数据基线
evcs-payment (8084)     - 支付服务，支付宝/微信沙箱接入（迭代中）
evcs-protocol (8085)    - 协议处理，OCPP / 云快充事件流
evcs-tenant (8086)      - 租户管理，多租户隔离
evcs-monitoring (8087)  - 监控服务，健康检查
evcs-config (8888)      - 配置中心，Git 配置
evcs-eureka (8761)      - 服务注册中心
evcs-common             - 公共组件，共享工具类
```

### 2. 严格分层架构
```
Controller ← Service ← Domain/Repository ← Entity
```
- Controller：仅处理 HTTP/REST 输入输出、鉴权、参数校验与错误映射
- Service：业务编排与事务边界，不持久化直连
- Domain/Repository：领域模型、聚合根与仓储，负责数据访问
- DTO：对外返回统一使用 DTO，禁止直接返回 Entity

### 3. 严禁的架构违规
- 跨服务直接访问数据库或注入他服务的 Repository（例如 OrderService 注入 UserRepository）
- 在 Controller 层堆积业务逻辑
- 直接返回 Entity 或暴露内部字段
- 硬编码敏感信息（密钥、数据库连接），统一走配置/环境变量

---

## 编码标准

### 命名规范
- 包/模块：`com.evcs.<service>.<layer>`（如 `com.evcs.order.service`）
- 类/接口：业务语义清晰，避免缩写；接口以功能命名，不以技术命名
- 方法：动词+对象（`createOrder`、`findByTenantId`）
- 常量：全大写下划线（`MAX_RETRY`），配置键以点分（`evcs.order.maxRetry`）

### 异常处理
- 统一异常基类与错误码；ControllerAdvice 映射到标准错误响应
- 业务异常与系统异常分层；避免吞异常，记录上下文信息
- 异步与线程池中需传播租户/请求ID/追踪信息

### 日志记录
- 关键路径使用结构化日志（JSON 或键值对）；包含租户、请求ID、用户
- 禁止在 INFO 打印敏感信息；DEBUG 受配置门控
- 性能关键环节记录耗时与关键指标

### 参数校验
- 使用 Bean Validation（`@Valid`, `@NotNull` 等）
- Controller 输入校验与错误提示一致化；避免在 Service 再做重复校验

---

## 技术栈约束
- Java 21 / Spring Boot 3.2.x；Gradle/Maven 按服务统一脚手架
- 数据访问优先 JPA/MyBatis 中的一种，禁止混用除非 RFC 说明
- 缓存：优先使用 Spring Cache + Redis，明确 TTL 与失效策略
- 消息：Kafka/RabbitMQ 统一封装，禁止裸 `new Thread` 与无上下文任务提交

### 多租户隔离实现
- 请求进入时解析并注入租户上下文（TenantContext）
- 异步传播使用 TransmittableThreadLocal 或统一包装（详见 `docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`）
- 跨租户数据禁止拼接；仓储层按租户分区或条件严格过滤

---

## 严格禁止的模式
- 直接访问他库表或跨边界聚合
- 在代码中硬编码环境/密钥
- 在领域层引入 Web/外部框架依赖

---

## 质量与性能要求
- 测试：核心路径必须具备单元测试与必要的集成测试；覆盖率达到团队门槛
- 代码质量：静态检查（SpotBugs/Checkstyle）与安全审计通过；关键路径考虑缓存与性能
- 监控：埋点与指标（QPS/RT/ErrorRate），健康检查与告警链路完整

---

## 相关文档索引
- 架构/编码总规范（本文件）
- AI 助手统一配置：`docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md`
- 租户异步上下文：`docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`
- 共享项目概述：`.ai-assistant-config/SHARED-PROJECT-CONTEXT.md`

备注：助手使用说明（CodeX/Claude/Copilot）仅在统一配置文件中维护，避免重复与漂移。
