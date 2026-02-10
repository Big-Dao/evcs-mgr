# EVCS Manager 监控体系指南

> **版本**: v1.2 | **最后更新**: 2026-02-10 | **维护者**: DevOps 团队 | **状态**: 活跃
>
> **说明**: 本文档整合了监控与告警配置的完整指南

## 概述

本文档描述EVCS Manager的完整监控体系，包括日志、指标、告警和健康检查。

## 目录

1. [日志体系](#日志体系)
2. [指标监控](#指标监控)
3. [Grafana Dashboard](#grafana-dashboard)
4. [告警规则](#告警规则)
5. [健康检查](#健康检查)
6. [熔断与限流](#熔断与限流)

---

## 日志体系

### JSON格式日志

系统使用 `logstash-logback-encoder` 实现JSON格式日志输出，配置文件位于 `evcs-common/src/main/resources/logback-spring.xml`。

#### 日志字段

```json
{
  "@timestamp": "2025-10-11T10:00:00.000Z",
  "level": "INFO",
  "logger": "com.evcs.order.service.OrderServiceImpl",
  "thread": "http-nio-8080-exec-1",
  "message": "Order created successfully",
  "app": "evcs-order",
  "traceId": "abc123...",
  "tenantId": 1001,
  "userId": 2001
}
```

#### 日志级别规范

- **ERROR**: 系统错误、未捕获异常、需要立即处理的问题
- **WARN**: 业务警告、降级操作、可恢复的错误
- **INFO**: 关键业务节点（订单创建、支付成功、充电开始/结束）
- **DEBUG**: 详细调试信息（仅在开发环境启用）

#### 敏感信息脱敏

使用 `SensitiveDataMasker` 工具类脱敏：

```java
import com.evcs.common.util.SensitiveDataMasker;

// 手机号脱敏：138****1234
String maskedPhone = SensitiveDataMasker.maskPhone("13812341234");

// 身份证号脱敏：前6后4
String maskedIdCard = SensitiveDataMasker.maskIdCard("110101199001011234");

// 银行卡号脱敏：前4后4
String maskedBankCard = SensitiveDataMasker.maskBankCard("6222021234567890");

// 密码脱敏：完全隐藏
String maskedPassword = SensitiveDataMasker.maskPassword("mySecretPassword");
```

### ELK日志收集

#### Logstash配置

配置文件：`monitoring/elk/logstash-pipeline.conf`

从文件和TCP端口收集日志，解析JSON格式并发送到Elasticsearch。

#### Elasticsearch索引模板

配置文件：`monitoring/elk/elasticsearch-template.json`

定义日志索引的mapping和settings：
- 索引分片：3个主分片，1个副本
- 索引名称模式：`evcs-YYYY.MM.dd`
- 字段类型优化：keyword类型用于精确匹配，text类型用于全文搜索

#### Kibana查询示例

```
# 查询特定租户的错误日志
level:ERROR AND tenantId:1001

# 查询特定时间范围的订单日志
@timestamp:[2025-10-11T00:00:00 TO 2025-10-11T23:59:59] AND message:order

# 查询包含异常堆栈的日志
_exists_:stack_trace
```

---

## 指标监控

### Prometheus指标

系统通过 `/actuator/prometheus` 端点暴露Prometheus格式的指标。

#### 系统指标

Spring Boot Actuator自动暴露的指标：

- `http_server_requests_seconds_count`: HTTP请求总数
- `http_server_requests_seconds_sum`: HTTP请求总耗时
- `http_server_requests_seconds_max`: HTTP请求最大耗时
- `jvm_memory_used_bytes`: JVM内存使用量
- `jvm_memory_max_bytes`: JVM最大内存
- `jvm_gc_pause_seconds_count`: GC次数
- `jvm_threads_live_threads`: 活跃线程数
- `process_cpu_usage`: 进程CPU使用率
- `system_cpu_usage`: 系统CPU使用率

#### 业务指标

需要在各服务中实现以下自定义指标：

**订单服务指标** (evcs-order):
- `evcs_order_created_total` (Counter): 订单创建总数
- `evcs_order_success_rate` (Gauge): 订单成功率
- `evcs_order_amount_total` (Counter): 订单金额总计

**充电桩服务指标** (evcs-station):
- `evcs_charger_online_rate` (Gauge): 充电桩在线率
- `evcs_charger_status_count` (Gauge): 各状态充电桩数量
- `evcs_charger_charging_count` (Gauge): 充电中的桩数

**支付服务指标** (evcs-payment):
- `evcs_payment_success_rate` (Gauge): 支付成功率
- `evcs_payment_amount_total` (Counter): 支付金额总计

**计费服务指标**:
- `evcs_billing_accuracy_rate` (Gauge): 计费准确率

### 指标实现示例

```java
package com.evcs.order.metrics;

import com.evcs.common.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderMetrics extends BusinessMetrics {
    
    private Counter orderCreatedCounter;
    private AtomicInteger orderSuccessCount = new AtomicInteger(0);
    private AtomicInteger orderTotalCount = new AtomicInteger(0);
    
    public OrderMetrics(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }
    
    @Override
    protected void registerMetrics() {
        // 订单创建计数器
        orderCreatedCounter = createCounter(
            "evcs_order_created_total",
            "Total number of orders created"
        );
        
        // 订单成功率
        createGauge(
            "evcs_order_success_rate",
            "Order success rate",
            orderSuccessCount,
            () -> {
                int total = orderTotalCount.get();
                return total > 0 ? (double) orderSuccessCount.get() / total : 1.0;
            }
        );
    }
    
    public void recordOrderCreated() {
        incrementCounter(orderCreatedCounter);
        orderTotalCount.incrementAndGet();
    }
    
    public void recordOrderSuccess() {
        orderSuccessCount.incrementAndGet();
    }
}
```

---

## Grafana 仪表板

### 推荐仪表板

| 仪表板 | Dashboard ID | 说明 |
|--------|--------------|------|
| Node Exporter Full | 1860 | 主机资源监控 |
| PostgreSQL Database | 9628 | PostgreSQL 监控 |
| Redis Dashboard | 763 | Redis 监控 |
| RabbitMQ Overview | 10991 | RabbitMQ 监控 |
| Spring Boot Statistics | 12900 | Spring Boot 应用监控 |
| JVM Micrometer | 4701 | JVM 详细监控 |

### 系统总览Dashboard

### 系统总览Dashboard

配置文件：`monitoring/grafana/dashboards/system-overview.json`

包含以下面板：
1. **服务健康状态**: 显示各服务的UP/DOWN状态
2. **QPS趋势图**: 显示过去1小时的请求速率
3. **响应时间趋势图**: 显示P50、P95、P99响应时间
4. **错误率趋势图**: 显示5XX错误率
5. **JVM内存使用**: 显示堆内存使用情况

### 自定义业务仪表板

核心指标查询：

```promql
# 实时订单量 (每分钟)
sum(rate(order_created_total[1m])) * 60

# 支付成功率
sum(rate(payment_success_total[5m])) / sum(rate(payment_total[5m])) * 100

# 充电桩状态分布
sum by (status) (charger_status)

# API 响应时间 P99
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))
```

### Dashboard导入

配置文件：`monitoring/grafana/dashboards/business-monitoring.json`

包含以下面板：
1. **订单创建趋势**: 对比今日、昨日、上周的订单量
2. **订单成功率**: Gauge显示当前成功率
3. **充电桩在线率**: Gauge显示在线率
4. **充电桩状态分布**: 饼图显示各状态桩数量
5. **营收统计**: 显示今日、本月、本年营收
6. **实时充电中桩数**: 显示正在充电的桩数量
7. **支付成功率**: Gauge显示支付成功率
8. **计费准确率**: Gauge显示计费准确率

### Dashboard导入

1. 登录Grafana
2. 点击 `+` → `Import`
3. 上传对应的JSON文件或粘贴JSON内容
4. 选择Prometheus数据源
5. 点击 `Import`

---

## 告警规则

配置文件：`monitoring/prometheus/rules/*.yml`

### 基础设施告警

| 告警名称 | 触发条件 | 严重程度 | 持续时间 |
|---------|---------|---------|---------|
| HighCpuUsage | CPU使用率 > 85% | warning | 5分钟 |
| CriticalCpuUsage | CPU使用率 > 95% | critical | 2分钟 |
| HighMemoryUsage | 内存使用率 > 85% | warning | 5分钟 |
| CriticalMemoryUsage | 内存使用率 > 95% | critical | 2分钟 |
| HighDiskUsage | 磁盘使用率 > 80% | warning | 10分钟 |
| CriticalDiskUsage | 磁盘使用率 > 90% | critical | 5分钟 |

### PostgreSQL 告警

| 告警名称 | 触发条件 | 严重程度 | 持续时间 |
|---------|---------|---------|---------|
| PostgresqlHighConnectionUsage | 连接数使用率 > 80% | warning | 5分钟 |
| PostgresqlSlowQueries | 慢查询 > 30秒 | warning | 5分钟 |
| PostgresqlReplicationLag | 复制延迟 > 60秒 | warning | 5分钟 |
| PostgresqlDeadlocks | 发生死锁 | warning | 1分钟 |

### Redis 告警

| 告警名称 | 触发条件 | 严重程度 | 持续时间 |
|---------|---------|---------|---------|
| RedisHighMemoryUsage | 内存使用率 > 80% | warning | 5分钟 |
| RedisTooManyConnections | 连接数 > 500 | warning | 5分钟 |
| RedisKeyEviction | 5分钟驱逐 > 100个键 | warning | 5分钟 |

### RabbitMQ 告警

| 告警名称 | 触发条件 | 严重程度 | 持续时间 |
|---------|---------|---------|---------|
| RabbitmqMessageBacklog | 消息积压 > 10000 | warning | 5分钟 |
| RabbitmqNoConsumer | 队列无消费者但有消息 | critical | 5分钟 |
| RabbitmqHighMemory | 内存使用 > 400MB | warning | 5分钟 |

### 应用服务告警

| 告警名称 | 触发条件 | 严重程度 | 持续时间 |
|---------|---------|---------|---------|
| ServiceDown | 服务不可用 | critical | 1分钟 |
| JvmHighHeapUsage | JVM堆内存 > 85% | warning | 5分钟 |
| JvmHighGcTime | 平均GC暂停 > 0.5秒 | warning | 5分钟 |
| HighHttpErrorRate | 5xx错误率 > 5% | warning | 5分钟 |
| HighHttpLatency | P99延迟 > 1秒 | warning | 5分钟 |

### 业务告警

| 告警名称 | 触发条件 | 严重程度 | 持续时间 |
|---------|---------|---------|---------|
| LowOrderCreationRate | 营业时间订单率过低 | warning | 10分钟 |
| LowPaymentSuccessRate | 支付成功率 < 95% | warning | 10分钟 |
| HighChargerOfflineRate | 充电桩离线率 > 10% | warning | 10分钟 |

### 告警分级响应

| 级别 | 响应时间 | 处理人员 | 通知方式 |
|------|----------|----------|----------|
| Critical | 5 分钟 | 值班运维 + 开发主管 | 电话 + 钉钉 + 邮件 |
| Warning | 30 分钟 | 值班运维 | 钉钉 + 邮件 |
| Info | 工作时间 | 运维团队 | 邮件 |

### Prometheus 配置

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

rule_files:
  - "/etc/prometheus/rules/*.yml"

scrape_configs:
  # EVCS 微服务 (通过 Eureka 发现)
  - job_name: 'evcs-services'
    metrics_path: '/actuator/prometheus'
    eureka_sd_configs:
      - server: 'http://eureka:8761/eureka'
```

### Alertmanager 配置

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default-receiver'
  routes:
    # 严重告警立即通知
    - match:
        severity: critical
      receiver: 'critical-receiver'
      group_wait: 10s
      repeat_interval: 1h

receivers:
  - name: 'critical-receiver'
    email_configs:
      - to: 'ops-critical@evcs.com'
    webhook_configs:
      - url: 'http://dingtalk-webhook:8060/dingtalk/ops/send'
```

---

## 健康检查

### 健康检查端点

- `/actuator/health`: 总体健康状态
- `/actuator/health/readiness`: 就绪检查（K8s使用）
- `/actuator/health/liveness`: 存活检查（K8s使用）

### 自定义健康检查

系统包含以下自定义健康检查器：

#### 数据库健康检查

`CustomDatabaseHealthIndicator` - 检查PostgreSQL连接和响应时间

```json
{
  "status": "UP",
  "components": {
    "customDatabase": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "responseTime": "15ms",
        "status": "Connected"
      }
    }
  }
}
```

### Kubernetes配置

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 3
```

---

## 熔断与限流

### Resilience4j配置

默认配置文件：`evcs-common/src/main/resources/resilience4j-defaults.yml`

### 熔断器（Circuit Breaker）

#### 支付服务熔断

```yaml
resilience4j.circuitbreaker:
  instances:
    paymentService:
      failureRateThreshold: 50        # 失败率阈值50%
      waitDurationInOpenState: 5m     # 熔断5分钟
      slidingWindowSize: 50           # 滑动窗口50个请求
```

#### 协议服务熔断

```yaml
resilience4j.circuitbreaker:
  instances:
    protocolService:
      slowCallDurationThreshold: 2s   # 慢调用阈值2秒
      slowCallRateThreshold: 50       # 慢调用率50%
      waitDurationInOpenState: 3m     # 熔断3分钟
```

### 使用示例

```java
@Service
public class PaymentService {
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentService")
    @RateLimiter(name = "paymentRateLimiter")
    public PaymentResult processPayment(PaymentRequest request) {
        // 调用支付接口
        return paymentClient.pay(request);
    }
    
    private PaymentResult paymentFallback(PaymentRequest request, Exception e) {
        log.error("Payment failed, using fallback", e);
        return PaymentResult.failed("Payment service unavailable");
    }
}
```

### 监控熔断器状态

访问 `/actuator/circuitbreakers` 查看所有熔断器状态：

```json
{
  "circuitBreakers": {
    "paymentService": {
      "state": "CLOSED",
      "metrics": {
        "failureRate": 2.5,
        "slowCallRate": 0.0,
        "bufferedCalls": 20,
        "failedCalls": 0
      }
    }
  }
}
```

---

## 部署与验证

### 1. 启动基础设施

```bash
# 启动Prometheus
docker run -d -p 9090:9090 \
  -v $(pwd)/monitoring/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

# 启动Grafana
docker run -d -p 3000:3000 grafana/grafana

# 启动监控/日志/链路追踪（可选，作为 overlay 叠加到主编排）
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml --profile monitoring up -d

docker compose -f docker-compose.yml -f docker-compose.monitoring.yml --profile logging up -d

docker compose -f docker-compose.yml -f docker-compose.monitoring.yml --profile tracing up -d
```

### 2. 验证指标收集

访问应用的Prometheus端点：
```
http://localhost:8080/actuator/prometheus
```

### 3. 验证健康检查

```bash
curl http://localhost:8080/actuator/health
```

### 4. 导入Grafana Dashboard

1. 访问 http://localhost:3000 （默认用户名/密码：admin/admin）
2. 添加Prometheus数据源
3. 导入 Dashboard JSON文件

### 5. 验证告警

触发告警条件后，在Grafana Alerting页面查看：
```
http://localhost:3000/alerting/list
```

---

## 最佳实践

### 日志记录

1. **关键业务节点必须记录INFO日志**
   ```java
   log.info("Order created: orderId={}, amount={}", orderId, amount);
   ```

2. **错误必须记录上下文信息**
   ```java
   log.error("Payment failed: orderId={}, error={}", orderId, e.getMessage(), e);
   ```

3. **敏感信息必须脱敏**
   ```java
   log.info("User login: phone={}", SensitiveDataMasker.maskPhone(phone));
   ```

### 指标命名

- 使用下划线分隔：`evcs_order_created_total`
- 包含单位后缀：`_seconds`, `_bytes`, `_total`
- 保持一致的前缀：`evcs_`

### 告警配置

- 避免告警风暴：设置合理的持续时间
- 分级告警：critical（立即处理）、warning（关注）、info（记录）
- 提供上下文：在annotations中包含足够的信息

### 熔断配置

- 根据业务特点调整参数
- 必须提供fallback方法
- 监控熔断器状态，及时调整阈值

---

## 故障排查

### 指标未收集

1. 检查actuator是否启用：`management.endpoints.web.exposure.include=prometheus`
2. 检查Prometheus配置的target是否正确
3. 查看应用日志是否有错误

### 告警未触发

1. 检查Prometheus规则是否加载：访问 http://localhost:9090/rules
2. 检查告警条件是否正确
3. 检查Alertmanager配置

### 日志未收集到ELK

1. 检查Logstash pipeline配置
2. 检查日志文件路径是否正确
3. 查看Logstash日志：`docker logs logstash`

---

## 相关链接

- [Prometheus官方文档](https://prometheus.io/docs/)
- [Grafana官方文档](https://grafana.com/docs/)
- [Resilience4j官方文档](https://resilience4j.readme.io/)
- [Spring Boot Actuator文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
