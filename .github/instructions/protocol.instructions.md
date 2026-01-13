---
applyTo: "evcs-protocol/**/*.java"
---

# evcs-protocol 模块开发规范

> **最后更新**: 2025-12-18 | **维护者**: 协议团队 | **状态**: 已发布

本模块负责与充电桩设备通信（OCPP/云快充）。涉及高并发连接和状态管理。
请遵循 [PROJECT-CODING-STANDARDS.md](../../docs/overview/PROJECT-CODING-STANDARDS.md) 中的核心规范。

## 关键要求

### 1. 异步上下文传播
**必须确保租户上下文在异步线程中可用**
- 禁止使用裸 `new Thread()` 或 `Executors.new...`
- 必须使用配置了 `TenantContextTaskDecorator` 的 `ThreadPoolTaskExecutor`
- 参见 [TENANT-CONTEXT-ASYNC-RFC.md](../../docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md)

### 2. 连接管理
**WebSocket/TCP 连接必须具备心跳检测**
- 严格处理连接断开（OnClose）和错误（OnError）事件
- 维护 `Session` 与 `ChargerId` 的映射关系，防止内存泄漏
- 定时清理无效连接

### 3. 事件驱动
**业务逻辑解耦**
- 协议层只负责报文解析和会话维护
- 业务逻辑（如鉴权、订单）通过 RabbitMQ 事件发布到其他模块
- 遵循 [PROTOCOL-EVENT-MODEL.md](../../docs/architecture/PROTOCOL-EVENT-MODEL.md) 定义的事件模型

---

## ✅ 测试准则

- ✅ 异步任务中的 TenantContext 传播测试
- ✅ 高并发连接下的内存泄漏测试
- ✅ 弱网环境下的心跳重连测试
- ✅ 协议报文解析的单元测试

---

## 常见模式

### 异步任务提交

```java
// ✅ 正确：使用注入的 TaskExecutor (已配置 Decorator)
@Autowired
@Qualifier("protocolTaskExecutor")
private Executor taskExecutor;

public void handleMessage(String message) {
    taskExecutor.execute(() -> {
        // 此时 TenantContext 已自动传播
        process(message);
    });
}
```
