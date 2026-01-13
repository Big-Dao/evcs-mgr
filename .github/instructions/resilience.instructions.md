---
applyTo: "**/*.java"
priority: high
---

# 弹性与容错设计规范 (Resilience & Fault Tolerance)

> **最后更新**: 2025-12-18 | **维护者**: 架构团队 | **状态**: 已发布

本规范适用于所有涉及外部通信（HTTP/RPC/MQ）的模块，特别是 `evcs-integration`, `evcs-payment`, `evcs-protocol`。
请遵循 [PROJECT-CODING-STANDARDS.md](../../docs/overview/PROJECT-CODING-STANDARDS.md) 中的核心规范。

## 关键要求

### 1. 超时控制 (Timeouts)
**所有外部调用必须显式设置超时**
- **连接超时 (Connect Timeout)**: 建议 1-3秒。快速失败，避免线程阻塞。
- **读取超时 (Read Timeout)**: 根据业务容忍度设置（如支付接口 10秒，短信接口 5秒）。
- **禁止**使用默认的无限超时。

### 2. 重试策略 (Retry)
**智能重试，避免雪崩**
- **禁止**使用 `while(true)` 或固定次数的简单循环。
- **必须**使用指数退避（Exponential Backoff）算法（如：1s -> 2s -> 4s）。
- **仅对**瞬态故障（如 Network Error, 503 Service Unavailable）重试。
- **禁止**对业务错误（如 400 Bad Request, 余额不足）重试。

### 3. 熔断机制 (Circuit Breaker)
**保护系统，防止级联故障**
- 对所有第三方依赖（支付网关、短信服务、IoT平台）配置熔断器。
- 当失败率超过阈值（如 50%）时，快速失败并触发降级逻辑。
- 推荐使用 Resilience4j 或 Sentinel。

---

## ✅ 测试准则

- ✅ 模拟网络超时的测试（验证超时设置生效）
- ✅ 模拟服务不可用的测试（验证熔断器开启）
- ✅ 重试次数与间隔的验证测试

---

## 常见模式

### 使用 Resilience4j 进行重试

```java
@Retry(name = "paymentApi", fallbackMethod = "fallbackPayment")
public PaymentResponse callPaymentApi(PaymentRequest request) {
    // 调用外部服务
    return restTemplate.postForObject(url, request, PaymentResponse.class);
}

// 降级方法
public PaymentResponse fallbackPayment(PaymentRequest request, Exception e) {
    log.warn("支付服务不可用，执行降级逻辑: {}", e.getMessage());
    return PaymentResponse.failed("服务暂时不可用，请稍后重试");
}
```
