# 协议事件模型说明 (Protocol Event Model)

> **版本**: v1.1 | **最后更新**: 2025-11-10 | **维护者**: 协议团队 | **状态**: 活跃（云快充/OCPP 事件流联调中）
>
> 🔌 **用途**: 描述协议事件架构、消息流转以及监控告警设计

## 目录

- [一、概述](#一概述)
- [二、事件类型](#二事件类型)
- [三、消息队列架构](#三消息队列架构)
- [四、事件发布](#四事件发布)
- [五、事件消费](#五事件消费)
- [六、错误处理](#六错误处理)
- [七、监控与告警](#七监控与告警)

---

## 一、概述

EVCS Manager系统支持OCPP和云快充两种充电桩协议，通过统一的事件模型和RabbitMQ消息队列实现协议层与业务层的解耦。

### 1.1 设计目标

- **协议无关性**: 业务逻辑不依赖具体协议实现
- **异步解耦**: 协议层和业务层通过消息队列异步通信
- **可扩展性**: 方便添加新的协议支持
- **高可靠性**: 消息持久化、确认机制、死信队列

### 1.2 架构图

```
┌─────────────┐         ┌──────────────┐         ┌────────────┐
│ 充电桩设备  │ ──协议─→ │ 协议适配器    │ ──发布→ │ RabbitMQ   │
│ (OCPP/云快充)│         │ (Service)    │  事件   │ 交换机队列  │
└─────────────┘         └──────────────┘         └────────────┘
                                                        │
                                                        ↓ 消费
                        ┌──────────────┐         ┌────────────┐
                        │ 业务服务      │ ←──订阅─ │ 事件消费者  │
                        │ (订单/充电桩) │         │ (Consumer) │
                        └──────────────┘         └────────────┘
```

---

## 二、事件类型

### 2.1 事件基类 (ProtocolEvent)

所有协议事件都继承自 `ProtocolEvent` 基类：

```java
public abstract class ProtocolEvent implements Serializable {
    private String eventId;           // 事件唯一标识 (UUID)
    private Long chargerId;           // 充电桩ID
    private Long tenantId;            // 租户ID（多租户隔离）
    private EventType eventType;      // 事件类型枚举
    private LocalDateTime eventTime;  // 事件时间
    private String protocolType;      // 协议类型（OCPP/CloudCharge）
    
    public abstract String getRoutingKey();  // 消息路由键
}
```

### 2.2 事件类型枚举

```java
public enum EventType {
    HEARTBEAT,      // 心跳
    STATUS_CHANGE,  // 状态变更
    CHARGING_START, // 开始充电
    CHARGING_STOP,  // 停止充电
    ERROR           // 错误（预留）
}
```

### 2.3 心跳事件 (HeartbeatEvent)

**用途**: 充电桩定期发送心跳，更新最后在线时间

**字段**:
- `lastHeartbeatTime`: 最后心跳时间

**路由键**: `protocol.heartbeat.{protocolType}`  
**示例**: `protocol.heartbeat.OCPP`

**消费者**: `ChargerEventConsumer` (evcs-station)

**处理逻辑**:
```java
// 更新充电桩的最后心跳时间
chargerMapper.updateStatus(chargerId, null, lastHeartbeatTime);
```

---

### 2.4 状态变更事件 (StatusEvent)

**用途**: 充电桩状态发生变化时触发（离线、空闲、充电中、故障）

**字段**:
- `oldStatus`: 旧状态
- `newStatus`: 新状态（0-离线，1-空闲，2-充电中，3-故障）
- `statusDesc`: 状态描述

**路由键**: `protocol.status.{protocolType}`  
**示例**: `protocol.status.CloudCharge`

**消费者**: `ChargerEventConsumer` (evcs-station)

**处理逻辑**:
```java
// 更新充电桩状态
chargerMapper.updateStatus(chargerId, newStatus, LocalDateTime.now());
```

---

### 2.5 开始充电事件 (StartEvent)

**用途**: 充电开始时触发，创建充电订单

**字段**:
- `sessionId`: 会话ID（充电会话唯一标识）
- `userId`: 用户ID
- `orderNo`: 订单号
- `initialEnergy`: 初始电量（kWh）
- `success`: 是否成功
- `message`: 响应消息

**路由键**: `protocol.charging.start`

**消费者**: 
- `ChargerEventConsumer` (evcs-station) - 更新充电桩为充电中状态
- `ChargingEventConsumer` (evcs-order) - 创建充电订单

**处理逻辑**:
```java
// evcs-station: 更新充电桩状态为充电中
chargerMapper.updateStatus(chargerId, 2, LocalDateTime.now());

// evcs-order: 创建充电订单
ChargingOrder order = new ChargingOrder();
order.setChargerId(chargerId);
order.setUserId(userId);
order.setSessionId(sessionId);
order.setStartTime(LocalDateTime.now());
order.setStatus(1);  // 充电中
orderService.save(order);
```

---

### 2.6 停止充电事件 (StopEvent)

**用途**: 充电结束时触发，更新充电订单

**字段**:
- `sessionId`: 会话ID
- `orderNo`: 订单号
- `energy`: 充电量（kWh）
- `duration`: 充电时长（分钟）
- `reason`: 结束原因（正常结束/异常结束）
- `success`: 是否成功
- `message`: 响应消息

**路由键**: `protocol.charging.stop`

**消费者**: 
- `ChargerEventConsumer` (evcs-station) - 更新充电桩为空闲状态
- `ChargingEventConsumer` (evcs-order) - 更新充电订单为已完成

**处理逻辑**:
```java
// evcs-station: 更新充电桩状态为空闲
chargerMapper.updateStatus(chargerId, 1, LocalDateTime.now());

// evcs-order: 更新充电订单
order.setEndTime(LocalDateTime.now());
order.setStatus(2);  // 已完成
order.setEnergy(energy);
order.setDuration(duration);
orderService.updateById(order);
```

---

## 三、RabbitMQ配置

### 3.1 交换机和队列

| 资源类型 | 名称 | 类型 | 说明 |
|---------|------|------|------|
| Exchange | evcs.protocol.events | Topic | 协议事件主交换机 |
| Queue | evcs.protocol.heartbeat | Durable | 心跳事件队列 |
| Queue | evcs.protocol.status | Durable | 状态事件队列 |
| Queue | evcs.protocol.charging | Durable | 充电事件队列 |
| Exchange | evcs.protocol.dlx | Direct | 死信交换机 |
| Queue | evcs.protocol.dlx.queue | Durable | 死信队列 |

### 3.2 路由键绑定关系

```
evcs.protocol.events (Topic Exchange)
├─ protocol.heartbeat.*     → evcs.protocol.heartbeat
├─ protocol.status.*        → evcs.protocol.status
├─ protocol.charging.start  → evcs.protocol.charging
└─ protocol.charging.stop   → evcs.protocol.charging
```

### 3.3 死信队列配置

所有业务队列配置死信队列，处理失败3次后的消息会进入死信队列：

```java
QueueBuilder.durable(HEARTBEAT_QUEUE)
    .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
    .withArgument("x-dead-letter-routing-key", "dlx")
    .build();
```

---

## 四、事件发布

### 4.1 协议适配器

OCPP和云快充协议适配器实现各自的协议接口，并通过 `ProtocolEventPublisher` 发布事件：

```java
@Service
public class OCPPProtocolServiceImpl implements IOCPPProtocolService {
    private final ProtocolEventPublisher eventPublisher;
    
    @Override
    public boolean sendHeartbeat(Long chargerId) {
        LocalDateTime now = LocalDateTime.now();
        eventPublisher.publishHeartbeat(chargerId, tenantId, "OCPP", now);
        return true;
    }
}
```

### 4.2 消息持久化和确认

- **持久化**: 消息标记为持久化（deliveryMode=2）
- **发布确认**: 启用Publisher Confirms，确保消息到达RabbitMQ
- **强制路由**: 启用mandatory，消息无法路由时触发返回回调

```java
// RabbitTemplate配置
template.setConfirmCallback((correlationData, ack, cause) -> {
    if (!ack) {
        log.error("Message send failed: {}", cause);
    }
});
```

---

## 五、事件消费

### 5.1 消费者配置

- **确认模式**: 手动确认（MANUAL）
- **并发消费**: 3-10个并发消费者
- **预取数量**: 10条消息
- **幂等性**: 使用事件ID或会话ID进行幂等性检查

### 5.2 消费示例

```java
@Component
@RabbitListener(queues = "evcs.protocol.charging")
public class ChargingEventConsumer {
    
    @RabbitHandler
    public void handleChargingStart(StartEvent event, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 幂等性检查
            if (processedEventIds.contains(event.getEventId())) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 设置租户上下文
            TenantContext.setCurrentTenantId(event.getTenantId());
            
            // 业务处理
            processChargingStart(event);
            
            // 手动确认
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("Failed to process event", e);
            // 拒绝消息，不重新入队（避免无限循环）
            channel.basicNack(deliveryTag, false, false);
        } finally {
            TenantContext.clear();
        }
    }
}
```

---

## 六、多租户隔离

### 6.1 租户上下文传递

事件对象包含 `tenantId` 字段，消费者在处理事件前必须设置租户上下文：

```java
TenantContext.setCurrentTenantId(event.getTenantId());
try {
    // 业务处理
} finally {
    TenantContext.clear();  // 确保清理上下文
}
```

### 6.2 自动租户过滤

MyBatis Plus的 `CustomTenantLineHandler` 会自动为SQL添加 `WHERE tenant_id = ?` 条件，确保数据隔离。

---

## 七、调试和监控

### 7.1 协议调试工具

使用 `ProtocolDebugController` 模拟协议事件：

```bash
# 模拟心跳
POST /debug/protocol/simulate/heartbeat
{
  "chargerId": 1,
  "tenantId": 1,
  "protocolType": "OCPP"
}

# 模拟开始充电
POST /debug/protocol/simulate/start
{
  "chargerId": 1,
  "tenantId": 1,
  "userId": 100,
  "protocolType": "OCPP",
  "initialEnergy": 0.0
}

# 模拟停止充电
POST /debug/protocol/simulate/stop
{
  "chargerId": 1,
  "sessionId": "xxx-xxx-xxx",
  "orderNo": "ORD1234567890",
  "energy": 12.5,
  "duration": 60,
  "reason": "Normal stop"
}
```

### 7.2 事件历史查询

```bash
GET /debug/protocol/events/history?chargerId=1&eventType=HEARTBEAT&limit=20
```

### 7.3 协议统计

```bash
GET /debug/protocol/stats
```

响应示例：
```json
{
  "code": 200,
  "data": {
    "totalEvents": 1000,
    "ocppEvents": 600,
    "cloudChargeEvents": 400
  }
}
```

---

## 八、错误处理

### 8.1 消息重试

消费失败的消息会重新入队，最多重试3次：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
```

### 8.2 死信队列

重试3次后仍失败的消息进入死信队列，需要人工介入处理：

```bash
# 查看死信队列消息
rabbitmqadmin get queue=evcs.protocol.dlx.queue count=10
```

### 8.3 异常监控

使用日志和监控系统跟踪消息处理失败：

```java
log.error("Failed to process event: eventId={}, chargerId={}", 
         event.getEventId(), event.getChargerId(), e);
```

---

## 九、最佳实践

### 9.1 幂等性设计

- 使用事件ID或业务唯一键（如sessionId）进行幂等性检查
- 生产环境使用Redis存储已处理的事件ID（带TTL）

### 9.2 租户上下文管理

- 消费者处理前必须设置租户上下文
- 使用try-finally确保上下文清理

### 9.3 错误处理

- 区分可重试错误和不可重试错误
- 业务异常不重新入队，避免无限循环
- 网络异常可以重新入队

### 9.4 性能优化

- 合理设置并发消费者数量
- 批量处理相关事件
- 异步处理非关键业务

---

## 十、扩展指南

### 10.1 添加新事件类型

1. 创建事件类继承 `ProtocolEvent`
2. 定义路由键
3. 在协议适配器中发布事件
4. 实现消费者处理逻辑

### 10.2 添加新协议支持

1. 实现协议接口（如 `IXxxProtocolService`）
2. 在协议方法中调用 `ProtocolEventPublisher` 发布事件
3. 无需修改消费者代码（协议无关）

---

**维护者**: EVCS Dev Team  
**更新时间**: 2025-10-12
