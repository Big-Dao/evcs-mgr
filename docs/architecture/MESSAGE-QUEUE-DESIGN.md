# EVCS 消息队列设计规范

> **版本**: v1.0  
> **创建日期**: 2026-01-13  
> **维护者**: 架构团队  
> **状态**: 已发布

---

## 1. 概述

本文档定义 EVCS 充电站管理系统中 RabbitMQ 消息队列的设计规范，包括 Exchange、Queue、消息格式、可靠性保障等。

### 1.1 消息架构

```
┌─────────────────────────────────────────────────────────────────┐
│                       RabbitMQ 消息架构                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Producer                 Exchange                  Consumer    │
│  ┌───────────┐           ┌───────────┐           ┌───────────┐ │
│  │ Protocol  │──────────▶│  Topic    │──────────▶│  Station  │ │
│  │ Service   │   (事件)   │ Exchange  │  (路由)   │  Service  │ │
│  └───────────┘           └───────────┘           └───────────┘ │
│                               │                                 │
│                               │                                 │
│                               ▼                                 │
│                          ┌───────────┐                          │
│                          │   Queue   │                          │
│                          │ (持久化)  │                          │
│                          └───────────┘                          │
│                               │                                 │
│                               ▼                                 │
│                          ┌───────────┐                          │
│                          │   DLX     │                          │
│                          │ (死信队列) │                          │
│                          └───────────┘                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Exchange 定义

### 2.1 Exchange 列表

| Exchange 名称 | 类型 | 用途 | 持久化 |
|---------------|------|------|--------|
| `evcs.protocol.events` | Topic | 协议层事件 | ✅ |
| `evcs.order.events` | Topic | 订单事件 | ✅ |
| `evcs.payment.events` | Topic | 支付事件 | ✅ |
| `evcs.station.events` | Topic | 站点事件 | ✅ |
| `evcs.notification` | Fanout | 通知广播 | ✅ |
| `evcs.dlx` | Direct | 死信交换机 | ✅ |

### 2.2 Exchange 配置

```java
@Configuration
public class RabbitMQConfig {

    // 协议事件交换机
    @Bean
    public TopicExchange protocolEventsExchange() {
        return ExchangeBuilder
            .topicExchange("evcs.protocol.events")
            .durable(true)
            .build();
    }

    // 订单事件交换机
    @Bean
    public TopicExchange orderEventsExchange() {
        return ExchangeBuilder
            .topicExchange("evcs.order.events")
            .durable(true)
            .build();
    }

    // 死信交换机
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder
            .directExchange("evcs.dlx")
            .durable(true)
            .build();
    }
}
```

---

## 3. Queue 定义

### 3.1 队列命名规范

```
evcs.<module>.<action>.<consumer>
```

示例:
- `evcs.protocol.heartbeat.station` - 站点服务消费心跳事件
- `evcs.order.created.payment` - 支付服务消费订单创建事件
- `evcs.payment.callback.order` - 订单服务消费支付回调

### 3.2 队列列表

| 队列名称 | Exchange | Routing Key | 消费者 | 说明 |
|----------|----------|-------------|--------|------|
| `evcs.protocol.heartbeat.station` | evcs.protocol.events | protocol.heartbeat.* | Station | 心跳事件 |
| `evcs.protocol.status.station` | evcs.protocol.events | protocol.status.* | Station | 状态变更 |
| `evcs.protocol.charging.order` | evcs.protocol.events | protocol.charging.* | Order | 充电事件 |
| `evcs.order.created.payment` | evcs.order.events | order.created | Payment | 订单创建 |
| `evcs.order.completed.payment` | evcs.order.events | order.completed | Payment | 订单完成 |
| `evcs.payment.callback.order` | evcs.payment.events | payment.callback.* | Order | 支付回调 |

### 3.3 队列配置

```java
@Configuration
public class QueueConfig {

    // 协议心跳队列
    @Bean
    public Queue protocolHeartbeatQueue() {
        return QueueBuilder
            .durable("evcs.protocol.heartbeat.station")
            .withArgument("x-dead-letter-exchange", "evcs.dlx")
            .withArgument("x-dead-letter-routing-key", "dlx.protocol.heartbeat")
            .withArgument("x-message-ttl", 300000) // 5 分钟 TTL
            .build();
    }

    // 订单创建队列
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder
            .durable("evcs.order.created.payment")
            .withArgument("x-dead-letter-exchange", "evcs.dlx")
            .withArgument("x-dead-letter-routing-key", "dlx.order.created")
            .build();
    }

    // 绑定
    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder
            .bind(orderCreatedQueue)
            .to(orderEventsExchange)
            .with("order.created");
    }
}
```

---

## 4. 消息格式

### 4.1 消息结构

```json
{
  "messageId": "uuid-xxxx-xxxx",
  "messageType": "order.created",
  "timestamp": "2026-01-13T10:30:00Z",
  "source": "order-service",
  "tenantId": 100001,
  "traceId": "trace-xxxx-xxxx",
  "payload": {
    // 业务数据
  },
  "metadata": {
    "version": "1.0",
    "retryCount": 0
  }
}
```

### 4.2 消息基类

```java
@Data
@SuperBuilder
public abstract class BaseMessage {
    
    @NotNull
    private String messageId;
    
    @NotNull
    private String messageType;
    
    @NotNull
    private Instant timestamp;
    
    @NotNull
    private String source;
    
    @NotNull
    private Long tenantId;
    
    private String traceId;
    
    @Builder.Default
    private MessageMetadata metadata = new MessageMetadata();
    
    @Data
    public static class MessageMetadata {
        private String version = "1.0";
        private int retryCount = 0;
    }
}

// 具体消息
@Data
@SuperBuilder
public class OrderCreatedMessage extends BaseMessage {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long stationId;
    private Long chargerId;
    private BigDecimal amount;
}
```

---

## 5. 可靠性保障

### 5.1 生产者确认

```java
@Configuration
public class RabbitProducerConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // 开启发布确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息发送失败: {}, cause: {}", correlationData, cause);
                // 重试或告警
            }
        });
        
        // 开启返回回调
        template.setReturnsCallback(returned -> {
            log.error("消息无法路由: {}", returned.getMessage());
        });
        
        template.setMandatory(true);
        
        return template;
    }
}
```

### 5.2 消费者确认

```java
@RabbitListener(queues = "evcs.order.created.payment")
public void handleOrderCreated(OrderCreatedMessage message, Channel channel, 
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
    try {
        // 幂等性检查
        if (messageProcessedRepository.exists(message.getMessageId())) {
            log.info("消息已处理，跳过: {}", message.getMessageId());
            channel.basicAck(deliveryTag, false);
            return;
        }
        
        // 处理业务逻辑
        paymentService.processOrderCreated(message);
        
        // 记录已处理
        messageProcessedRepository.save(message.getMessageId());
        
        // 手动确认
        channel.basicAck(deliveryTag, false);
        
    } catch (RetryableException e) {
        // 可重试异常，拒绝并重新入队
        log.warn("处理失败，将重试: {}", message.getMessageId(), e);
        channel.basicNack(deliveryTag, false, true);
        
    } catch (Exception e) {
        // 不可重试异常，拒绝并进入死信队列
        log.error("处理失败，进入死信队列: {}", message.getMessageId(), e);
        channel.basicNack(deliveryTag, false, false);
    }
}
```

### 5.3 幂等性保障

```java
@Service
public class MessageIdempotencyService {

    private final StringRedisTemplate redisTemplate;
    
    private static final String KEY_PREFIX = "msg:processed:";
    private static final long EXPIRE_HOURS = 24;

    public boolean isProcessed(String messageId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + messageId));
    }

    public boolean tryProcess(String messageId) {
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(KEY_PREFIX + messageId, "1", EXPIRE_HOURS, TimeUnit.HOURS);
        return Boolean.TRUE.equals(result);
    }
}
```

---

## 6. 死信队列处理

### 6.1 死信队列配置

```java
@Bean
public Queue dlqOrderCreated() {
    return QueueBuilder
        .durable("evcs.dlq.order.created")
        .build();
}

@Bean
public Binding dlqOrderCreatedBinding() {
    return BindingBuilder
        .bind(dlqOrderCreated())
        .to(dlxExchange())
        .with("dlx.order.created");
}
```

### 6.2 死信处理服务

```java
@Component
public class DeadLetterProcessor {

    @Scheduled(fixedDelay = 60000) // 每分钟检查
    public void processDlq() {
        // 从死信队列获取消息
        Message message = rabbitTemplate.receive("evcs.dlq.order.created", 5000);
        
        if (message == null) {
            return;
        }
        
        MessageProperties props = message.getMessageProperties();
        Integer retryCount = (Integer) props.getHeaders().get("x-retry-count");
        
        if (retryCount == null) {
            retryCount = 0;
        }
        
        if (retryCount < 3) {
            // 重试
            props.getHeaders().put("x-retry-count", retryCount + 1);
            rabbitTemplate.send("evcs.order.events", "order.created", message);
            log.info("死信消息重试: retry={}", retryCount + 1);
        } else {
            // 超过重试次数，告警并持久化
            log.error("死信消息超过重试次数: {}", new String(message.getBody()));
            deadLetterRepository.save(message);
            alertService.sendAlert("死信消息处理失败", message.toString());
        }
    }
}
```

---

## 7. 消息发送封装

### 7.1 消息发送服务

```java
@Service
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(OrderCreatedMessage message) {
        publish("evcs.order.events", "order.created", message);
    }

    public void publishPaymentCallback(PaymentCallbackMessage message) {
        publish("evcs.payment.events", "payment.callback." + message.getChannel(), message);
    }

    private void publish(String exchange, String routingKey, BaseMessage message) {
        // 填充通用字段
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(Instant.now());
        }
        if (message.getTenantId() == null) {
            message.setTenantId(TenantContext.getCurrentTenantId());
        }
        if (message.getTraceId() == null) {
            message.setTraceId(MDC.get("traceId"));
        }
        
        try {
            String json = objectMapper.writeValueAsString(message);
            
            rabbitTemplate.convertAndSend(exchange, routingKey, json, msg -> {
                msg.getMessageProperties().setContentType("application/json");
                msg.getMessageProperties().setMessageId(message.getMessageId());
                msg.getMessageProperties().setHeader("tenantId", message.getTenantId());
                msg.getMessageProperties().setHeader("traceId", message.getTraceId());
                return msg;
            });
            
            log.info("消息发送成功: exchange={}, routingKey={}, messageId={}", 
                exchange, routingKey, message.getMessageId());
                
        } catch (Exception e) {
            log.error("消息发送失败", e);
            throw new MessagePublishException("消息发送失败", e);
        }
    }
}
```

---

## 8. 事件定义

### 8.1 协议事件

| 事件 | Routing Key | 说明 |
|------|-------------|------|
| 心跳 | `protocol.heartbeat.{chargerId}` | 充电桩心跳 |
| 状态变更 | `protocol.status.{chargerId}` | 充电桩状态变更 |
| 开始充电 | `protocol.charging.start` | 开始充电事件 |
| 停止充电 | `protocol.charging.stop` | 停止充电事件 |
| 充电数据 | `protocol.charging.data` | 充电过程数据 |

### 8.2 订单事件

| 事件 | Routing Key | 说明 |
|------|-------------|------|
| 订单创建 | `order.created` | 订单创建 |
| 订单开始 | `order.started` | 开始充电 |
| 订单完成 | `order.completed` | 充电完成 |
| 订单取消 | `order.cancelled` | 订单取消 |

### 8.3 支付事件

| 事件 | Routing Key | 说明 |
|------|-------------|------|
| 支付创建 | `payment.created` | 支付单创建 |
| 支付成功 | `payment.callback.success` | 支付成功回调 |
| 支付失败 | `payment.callback.failed` | 支付失败回调 |
| 退款完成 | `payment.refund.completed` | 退款完成 |

---

## 9. 监控

### 9.1 关键指标

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| 队列消息数 | 待处理消息 | > 10000 |
| 消费速率 | 每秒消费数 | - |
| 发布确认延迟 | 确认延迟 | > 1s |
| 死信队列消息数 | 处理失败消息 | > 100 |

### 9.2 Prometheus 指标

```yaml
# RabbitMQ Exporter 配置
rabbitmq_queue_messages
rabbitmq_queue_messages_ready
rabbitmq_queue_consumers
rabbitmq_queue_messages_unacknowledged
```

---

## 10. 相关文档

- [系统架构风险审计报告](RISK-AUDIT-REPORT.md)
- [监控告警配置指南](../operations/MONITORING-ALERTING-GUIDE.md)
- [项目编码规范](../overview/PROJECT-CODING-STANDARDS.md)

---

## 11. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本 |
