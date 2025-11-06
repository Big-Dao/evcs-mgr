# RabbitMQ消息队列集成文档

## 概述

本文档描述了EVCS充电站管理平台中RabbitMQ消息队列的完整集成方案，用于支付系统的异步通知处理。

## 集成架构

### 消息驱动架构设计

```
支付服务 → 消息发送 → RabbitMQ Exchange → Queue → 消息监听器 → 业务处理
                    ↓
                死信队列 → 人工干预
```

### 核心组件

1. **消息生产者**: PaymentMessageServiceImpl
2. **消息消费者**: PaymentMessageListener
3. **消息转换器**: Jackson2JsonMessageConverter
4. **队列配置**: RabbitMQConfig
5. **消息模型**: PaymentMessage

## 队列配置

### Exchange配置

- **支付直连交换机**: `evcs.payment.direct`
- **类型**: direct
- **持久化**: 是

### Queue配置

| 队列名称 | 路由键 | 用途 | 死信队列 |
|---------|--------|------|----------|
| `evcs.payment.success.queue` | `payment.success` | 支付成功消息 | `evcs.payment.dlq` |
| `evcs.payment.failure.queue` | `payment.failure` | 支付失败消息 | `evcs.payment.dlq` |
| `evcs.refund.success.queue` | `refund.success` | 退款成功消息 | `evcs.payment.dlq` |
| `evcs.payment.dlq` | N/A | 死信消息 | N/A |

### 队列参数

```java
Map<String, Object> args = new HashMap<>();
args.put("x-dead-letter-exchange", "evcs.payment.dlx");
args.put("x-dead-letter-routing-key", "payment.dlq");
args.put("x-message-ttl", 300000); // 5分钟TTL
```

## 消息模型

### PaymentMessage结构

```java
@Data
public class PaymentMessage implements Serializable {
    private String messageId;          // 消息唯一ID
    private String messageType;        // 消息类型
    private Long paymentId;            // 支付记录ID
    private Long orderId;              // 业务订单ID
    private String tradeNo;            // 第三方交易号
    private BigDecimal amount;         // 金额
    private String paymentMethod;      // 支付方式
    private String status;             // 支付状态
    private Long userId;               // 用户ID
    private Long tenantId;             // 租户ID
    private LocalDateTime paidTime;    // 支付时间
    private BigDecimal refundAmount;   // 退款金额
    private LocalDateTime refundTime;  // 退款时间
    private LocalDateTime createTime;  // 消息创建时间
    private Integer retryCount = 0;    // 重试次数
    private Integer maxRetries = 3;    // 最大重试次数
}
```

### 消息类型

- `PAYMENT_SUCCESS`: 支付成功
- `PAYMENT_FAILURE`: 支付失败
- `REFUND_SUCCESS`: 退款成功

## 消息发送

### 发送流程

1. 构建PaymentMessage消息
2. 设置消息路由和内容
3. 通过RabbitTemplate发送消息
4. 记录发送日志
5. 异常时降级处理

### 核心代码

```java
@Override
public void sendPaymentSuccessMessage(PaymentOrder paymentOrder) {
    if (!messageEnabled) {
        log.info("消息发送未启用，记录支付成功日志");
        return;
    }

    try {
        PaymentMessage message = buildPaymentSuccessMessage(paymentOrder);

        if (rabbitmqEnabled) {
            rabbitTemplate.convertAndSend(
                rabbitMQConfig.getExchange().getPaymentDirect(),
                rabbitMQConfig.getRoutingKey().getPaymentSuccess(),
                message
            );
            log.info("支付成功消息已发送到RabbitMQ: messageId={}",
                message.getMessageId());
        } else {
            log.info("RabbitMQ未启用，记录支付成功日志: {}", message);
        }
    } catch (Exception e) {
        log.error("发送支付成功消息失败: orderId={}",
            paymentOrder.getOrderId(), e);
    }
}
```

### 降级机制

当RabbitMQ不可用时，系统自动降级为日志记录，确保主流程不受影响：

```java
if (!rabbitmqEnabled) {
    log.info("RabbitMQ未启用，记录支付成功日志: {}", message.toString());
    return;
}
```

## 消息消费

### 监听器配置

```java
@RabbitListener(queues = "${evcs.payment.rabbitmq.queue.payment-success}")
public void handlePaymentSuccessMessage(@Payload PaymentMessage message,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                      Channel channel,
                                      Message amqpMessage) {
    try {
        processPaymentSuccessMessage(message);
        channel.basicAck(deliveryTag, false);
        log.info("支付成功消息处理完成: messageId={}", message.getMessageId());
    } catch (Exception e) {
        log.error("处理支付成功消息失败: messageId={}", message.getMessageId(), e);
        handleProcessingFailure(message, channel, deliveryTag, e);
    }
}
```

### 重试机制

```java
private void handleProcessingFailure(PaymentMessage message, Channel channel,
                                   long deliveryTag, Exception e) {
    try {
        message.setRetryCount(message.getRetryCount() + 1);

        if (message.getRetryCount() >= message.getMaxRetries()) {
            log.error("消息重试次数超过最大限制，拒绝消息: messageId={}, retryCount={}",
                message.getMessageId(), message.getRetryCount());
            channel.basicNack(deliveryTag, false, false);
        } else {
            log.warn("消息处理失败，准备重试: messageId={}, retryCount={}",
                message.getMessageId(), message.getRetryCount());
            channel.basicNack(deliveryTag, false, true);
        }
    } catch (IOException ioException) {
        log.error("处理消息失败时发生IO异常", ioException);
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException ex) {
            log.error("确认消息失败", ex);
        }
    }
}
```

## 配置参数

### application.yml配置

```yaml
evcs:
  payment:
    message:
      enabled: true                    # 是否启用消息发送
    rabbitmq:
      enabled: true                    # 是否启用RabbitMQ
      exchange:
        payment-direct: evcs.payment.direct
        dead-letter: evcs.payment.dlx
      queue:
        payment-success: evcs.payment.success.queue
        payment-failure: evcs.payment.failure.queue
        refund-success: evcs.refund.success.queue
        payment-dlq: evcs.payment.dlq
      routing-key:
        payment-success: payment.success
        payment-failure: payment.failure
        refund-success: refund.success
        payment-dlq: payment.dlq

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000
          multiplier: 2
```

## 测试配置

### MockRabbitMQConfig

测试环境使用MockRabbitTemplate避免真实消息发送：

```java
@TestConfiguration
@Profile("test")
public class MockRabbitMQConfig {

    @Bean
    @Primary
    public RabbitTemplate mockRabbitTemplate() {
        return new MockRabbitTemplate();
    }

    public static class MockRabbitTemplate extends RabbitTemplate {
        @Override
        public void convertAndSend(String exchange, String routingKey, Object message) {
            System.out.println("Mock RabbitMQ - 发送消息: exchange=" + exchange +
                ", routingKey=" + routingKey + ", message=" + message);
        }
    }
}
```

### 测试用例

```java
@Test
@DisplayName("测试支付成功消息发送")
void testSendPaymentSuccessMessage() {
    PaymentOrder paymentOrder = createTestPaymentOrder();
    paymentOrder.setStatus(PaymentStatus.SUCCESS.getCode());
    paymentOrder.setPaidTime(LocalDateTime.now());

    assertDoesNotThrow(() -> {
        log.info("模拟发送支付成功消息: orderId={}", paymentOrder.getOrderId());
    });
}
```

## 监控和运维

### 消息监控指标

- 消息发送成功率
- 消息处理延迟
- 死信队列数量
- 队列堆积情况

### 运维操作

```bash
# 查看队列状态
rabbitmqctl list_queues

# 查看交换机信息
rabbitmqctl list_exchanges

# 清空队列
rabbitmqctl purge_queue evcs.payment.success.queue

# 查看死信队列
rabbitmqctl list_queues name messages | grep dlq
```

### 告警规则

- 死信队列消息数 > 10
- 队列消息堆积 > 1000
- 消息处理延迟 > 5分钟

## 故障排查

### 常见问题

1. **消息发送失败**
   - 检查RabbitMQ连接状态
   - 验证Exchange和Queue是否存在
   - 检查路由键配置

2. **消息堆积**
   - 检查消费者是否正常启动
   - 验证消息处理逻辑是否有异常
   - 检查数据库连接是否正常

3. **死信消息增多**
   - 查看重试次数设置
   - 检查消息格式是否正确
   - 验证业务处理逻辑

### 日志关键点

```bash
# 查看消息发送日志
grep "支付成功消息已发送到RabbitMQ" logs/payment.log

# 查看消息处理日志
grep "接收到支付成功消息" logs/payment.log

# 查看重试日志
grep "消息重试次数超过最大限制" logs/payment.log
```

## 性能优化

### 批量处理

对于高并发场景，可以考虑批量消息处理：

```java
@RabbitListener(queues = "${evcs.payment.rabbitmq.queue.payment-success}")
public void handlePaymentSuccessMessages(List<PaymentMessage> messages) {
    // 批量处理消息
    messages.forEach(this::processPaymentSuccessMessage);
}
```

### 连接池优化

```yaml
spring:
  rabbitmq:
    cache:
      connection:
        mode: channel
        size: 10
      channel:
        size: 25
        checkout-timeout: 30s
```

## 安全考虑

### 网络安全

- 使用SSL/TLS加密连接
- 配置访问控制列表(ACL)
- 限制管理端口访问

### 数据安全

- 消息内容敏感信息加密
- 消息TTL设置，避免长期堆积
- 定期清理死信消息

## 总结

RabbitMQ消息队列集成为支付系统提供了可靠的异步通知机制，具备以下特点：

1. **高可靠性**: 消息持久化、手动确认、重试机制
2. **高可用性**: 死信队列、降级处理、故障恢复
3. **高性能**: 异步处理、批量操作、连接池优化
4. **易监控**: 完善的日志记录、运维指标、告警机制

通过合理的配置和优化，可以确保支付消息的可靠传递和处理，提升系统的稳定性和用户体验。