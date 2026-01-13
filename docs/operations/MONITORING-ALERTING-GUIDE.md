# EVCS 监控告警配置指南

> **版本**: v1.0  
> **创建日期**: 2026-01-13  
> **维护者**: 运维团队  
> **状态**: 已发布

---

## 1. 概述

本文档定义 EVCS 充电站管理系统的监控告警配置，包括 Prometheus 指标采集、Grafana 仪表板、告警规则等。

### 1.1 监控架构

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  微服务     │───▶│ Prometheus  │───▶│  Grafana    │
│ /actuator   │    │  采集存储    │    │  可视化     │
└─────────────┘    └──────┬──────┘    └─────────────┘
                          │
                          ▼
                   ┌─────────────┐
                   │ Alertmanager│
                   │  告警通知    │
                   └─────────────┘
```

### 1.2 监控层次

| 层次 | 监控内容 | 工具 |
|------|----------|------|
| 基础设施 | CPU、内存、磁盘、网络 | Node Exporter |
| 中间件 | PostgreSQL、Redis、RabbitMQ | 专用 Exporter |
| 应用 | JVM、HTTP、业务指标 | Spring Actuator |
| 业务 | 订单量、充电量、收入 | 自定义指标 |

---

## 2. Prometheus 配置

### 2.1 prometheus.yml

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
  # Prometheus 自身
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Node Exporter (主机指标)
  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']

  # PostgreSQL
  - job_name: 'postgresql'
    static_configs:
      - targets: ['postgres-exporter:9187']

  # Redis
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  # RabbitMQ
  - job_name: 'rabbitmq'
    static_configs:
      - targets: ['rabbitmq:15692']

  # EVCS 微服务 (通过 Eureka 发现)
  - job_name: 'evcs-services'
    metrics_path: '/actuator/prometheus'
    eureka_sd_configs:
      - server: 'http://eureka:8761/eureka'
    relabel_configs:
      - source_labels: [__meta_eureka_app_name]
        target_label: application
      - source_labels: [__meta_eureka_app_instance_id]
        target_label: instance
```

### 2.2 服务发现配置（K8s 环境）

```yaml
scrape_configs:
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - evcs
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
```

---

## 3. 告警规则

### 3.1 基础设施告警 (infrastructure-alerts.yml)

```yaml
groups:
  - name: infrastructure
    rules:
      # CPU 使用率
      - alert: HighCpuUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU 使用率过高 ({{ $labels.instance }})"
          description: "CPU 使用率 {{ $value | printf \"%.1f\" }}% 超过 85%"

      - alert: CriticalCpuUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 95
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "CPU 使用率严重过高 ({{ $labels.instance }})"
          description: "CPU 使用率 {{ $value | printf \"%.1f\" }}% 超过 95%"

      # 内存使用率
      - alert: HighMemoryUsage
        expr: (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "内存使用率过高 ({{ $labels.instance }})"
          description: "内存使用率 {{ $value | printf \"%.1f\" }}% 超过 85%"

      - alert: CriticalMemoryUsage
        expr: (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100 > 95
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "内存使用率严重过高 ({{ $labels.instance }})"
          description: "内存使用率 {{ $value | printf \"%.1f\" }}% 超过 95%"

      # 磁盘使用率
      - alert: HighDiskUsage
        expr: (1 - node_filesystem_avail_bytes{fstype!="tmpfs"} / node_filesystem_size_bytes{fstype!="tmpfs"}) * 100 > 80
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "磁盘使用率过高 ({{ $labels.instance }})"
          description: "磁盘 {{ $labels.mountpoint }} 使用率 {{ $value | printf \"%.1f\" }}%"

      - alert: CriticalDiskUsage
        expr: (1 - node_filesystem_avail_bytes{fstype!="tmpfs"} / node_filesystem_size_bytes{fstype!="tmpfs"}) * 100 > 90
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "磁盘使用率严重过高 ({{ $labels.instance }})"
          description: "磁盘 {{ $labels.mountpoint }} 使用率 {{ $value | printf \"%.1f\" }}%"
```

### 3.2 PostgreSQL 告警 (postgresql-alerts.yml)

```yaml
groups:
  - name: postgresql
    rules:
      # 连接数使用率
      - alert: PostgresqlHighConnectionUsage
        expr: sum(pg_stat_activity_count) / pg_settings_max_connections * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL 连接数使用率过高"
          description: "连接数使用率 {{ $value | printf \"%.1f\" }}% 超过 80%"

      # 慢查询
      - alert: PostgresqlSlowQueries
        expr: rate(pg_stat_activity_max_tx_duration{state="active"}[5m]) > 30
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL 存在慢查询"
          description: "活跃事务持续时间超过 30 秒"

      # 复制延迟
      - alert: PostgresqlReplicationLag
        expr: pg_replication_lag > 60
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL 复制延迟过高"
          description: "复制延迟 {{ $value | printf \"%.0f\" }} 秒"

      # 死锁
      - alert: PostgresqlDeadlocks
        expr: increase(pg_stat_database_deadlocks[5m]) > 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL 发生死锁"
          description: "数据库 {{ $labels.datname }} 发生死锁"

      # 表膨胀
      - alert: PostgresqlTableBloat
        expr: pg_stat_user_tables_n_dead_tup / (pg_stat_user_tables_n_live_tup + 1) > 0.2
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL 表膨胀"
          description: "表 {{ $labels.relname }} 死元组比例超过 20%"
```

### 3.3 Redis 告警 (redis-alerts.yml)

```yaml
groups:
  - name: redis
    rules:
      # 内存使用率
      - alert: RedisHighMemoryUsage
        expr: redis_memory_used_bytes / redis_memory_max_bytes * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis 内存使用率过高"
          description: "内存使用率 {{ $value | printf \"%.1f\" }}% 超过 80%"

      # 连接数
      - alert: RedisTooManyConnections
        expr: redis_connected_clients > 500
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis 连接数过多"
          description: "当前连接数 {{ $value }}"

      # 键过期
      - alert: RedisKeyEviction
        expr: increase(redis_evicted_keys_total[5m]) > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis 键被驱逐"
          description: "5分钟内驱逐 {{ $value }} 个键"
```

### 3.4 RabbitMQ 告警 (rabbitmq-alerts.yml)

```yaml
groups:
  - name: rabbitmq
    rules:
      # 消息积压
      - alert: RabbitmqMessageBacklog
        expr: sum(rabbitmq_queue_messages_ready) > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "RabbitMQ 消息积压"
          description: "待处理消息数 {{ $value }}"

      # 队列消费者为零
      - alert: RabbitmqNoConsumer
        expr: rabbitmq_queue_consumers == 0 and rabbitmq_queue_messages_ready > 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "RabbitMQ 队列无消费者"
          description: "队列 {{ $labels.queue }} 无消费者但有待处理消息"

      # 内存告警
      - alert: RabbitmqHighMemory
        expr: rabbitmq_process_resident_memory_bytes / 1024 / 1024 > 400
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "RabbitMQ 内存使用过高"
          description: "内存使用 {{ $value | printf \"%.0f\" }} MB"
```

### 3.5 应用服务告警 (application-alerts.yml)

```yaml
groups:
  - name: application
    rules:
      # 服务宕机
      - alert: ServiceDown
        expr: up{job="evcs-services"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务宕机 ({{ $labels.application }})"
          description: "服务 {{ $labels.application }} 实例 {{ $labels.instance }} 不可用"

      # JVM 堆内存
      - alert: JvmHighHeapUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM 堆内存使用率过高 ({{ $labels.application }})"
          description: "堆内存使用率 {{ $value | printf \"%.1f\" }}%"

      # GC 时间过长
      - alert: JvmHighGcTime
        expr: rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m]) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM GC 时间过长 ({{ $labels.application }})"
          description: "平均 GC 暂停时间 {{ $value | printf \"%.2f\" }} 秒"

      # HTTP 错误率
      - alert: HighHttpErrorRate
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application) / sum(rate(http_server_requests_seconds_count[5m])) by (application) * 100 > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "HTTP 错误率过高 ({{ $labels.application }})"
          description: "5xx 错误率 {{ $value | printf \"%.1f\" }}%"

      # HTTP 延迟
      - alert: HighHttpLatency
        expr: histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application)) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "HTTP 延迟过高 ({{ $labels.application }})"
          description: "P99 延迟 {{ $value | printf \"%.2f\" }} 秒"

      # 线程池
      - alert: ThreadPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max * 100 > 90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "连接池即将耗尽 ({{ $labels.application }})"
          description: "连接池使用率 {{ $value | printf \"%.1f\" }}%"
```

### 3.6 业务告警 (business-alerts.yml)

```yaml
groups:
  - name: business
    rules:
      # 订单创建异常
      - alert: LowOrderCreationRate
        expr: sum(rate(order_created_total[10m])) < 1 and hour() >= 8 and hour() <= 22
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "订单创建率异常低"
          description: "10分钟内几乎无新订单，可能存在系统问题"

      # 支付成功率
      - alert: LowPaymentSuccessRate
        expr: sum(rate(payment_success_total[10m])) / sum(rate(payment_total[10m])) * 100 < 95
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "支付成功率下降"
          description: "支付成功率 {{ $value | printf \"%.1f\" }}% 低于 95%"

      # 充电桩离线
      - alert: HighChargerOfflineRate
        expr: sum(charger_status{status="offline"}) / sum(charger_status) * 100 > 10
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "充电桩离线率过高"
          description: "离线率 {{ $value | printf \"%.1f\" }}% 超过 10%"
```

---

## 4. Alertmanager 配置

### 4.1 alertmanager.yml

```yaml
global:
  resolve_timeout: 5m
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alertmanager@evcs.com'
  smtp_auth_username: 'alertmanager@evcs.com'
  smtp_auth_password: 'password'

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

    # 业务告警
    - match:
        group: business
      receiver: 'business-receiver'

receivers:
  - name: 'default-receiver'
    email_configs:
      - to: 'ops@evcs.com'
        send_resolved: true

  - name: 'critical-receiver'
    email_configs:
      - to: 'ops-critical@evcs.com'
        send_resolved: true
    webhook_configs:
      - url: 'http://dingtalk-webhook:8060/dingtalk/ops/send'
        send_resolved: true

  - name: 'business-receiver'
    email_configs:
      - to: 'business@evcs.com'
        send_resolved: true

inhibit_rules:
  # 抑制同一实例的低级别告警
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'instance']
```

---

## 5. Grafana 仪表板

### 5.1 仪表板列表

| 仪表板 | Dashboard ID | 说明 |
|--------|--------------|------|
| Node Exporter Full | 1860 | 主机资源监控 |
| PostgreSQL Database | 9628 | PostgreSQL 监控 |
| Redis Dashboard | 763 | Redis 监控 |
| RabbitMQ Overview | 10991 | RabbitMQ 监控 |
| Spring Boot Statistics | 12900 | Spring Boot 应用监控 |
| JVM Micrometer | 4701 | JVM 详细监控 |

### 5.2 自定义业务仪表板

```json
{
  "title": "EVCS 业务监控",
  "panels": [
    {
      "title": "实时订单量 (每分钟)",
      "type": "stat",
      "targets": [
        {
          "expr": "sum(rate(order_created_total[1m])) * 60"
        }
      ]
    },
    {
      "title": "支付成功率",
      "type": "gauge",
      "targets": [
        {
          "expr": "sum(rate(payment_success_total[5m])) / sum(rate(payment_total[5m])) * 100"
        }
      ]
    },
    {
      "title": "充电桩状态分布",
      "type": "piechart",
      "targets": [
        {
          "expr": "sum by (status) (charger_status)"
        }
      ]
    },
    {
      "title": "API 响应时间 P99",
      "type": "timeseries",
      "targets": [
        {
          "expr": "histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))"
        }
      ]
    }
  ]
}
```

---

## 6. 微服务指标配置

### 6.1 Spring Boot Actuator 配置

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:default}
```

### 6.2 自定义业务指标

```java
@Component
public class BusinessMetrics {

    private final Counter orderCreatedCounter;
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;
    private final Gauge chargerOnlineGauge;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.orderCreatedCounter = Counter.builder("order.created")
            .description("Total number of orders created")
            .register(meterRegistry);

        this.paymentSuccessCounter = Counter.builder("payment.success")
            .description("Total number of successful payments")
            .register(meterRegistry);

        this.paymentFailedCounter = Counter.builder("payment.failed")
            .description("Total number of failed payments")
            .register(meterRegistry);

        this.chargerOnlineGauge = Gauge.builder("charger.online", this, BusinessMetrics::countOnlineChargers)
            .description("Number of online chargers")
            .register(meterRegistry);
    }

    public void recordOrderCreated() {
        orderCreatedCounter.increment();
    }

    public void recordPaymentSuccess() {
        paymentSuccessCounter.increment();
    }

    public void recordPaymentFailed() {
        paymentFailedCounter.increment();
    }

    private double countOnlineChargers() {
        // 返回在线充电桩数量
        return chargerService.countOnline();
    }
}
```

---

## 7. 告警响应流程

### 7.1 告警分级响应

| 级别 | 响应时间 | 处理人员 | 通知方式 |
|------|----------|----------|----------|
| Critical | 5 分钟 | 值班运维 + 开发主管 | 电话 + 钉钉 + 邮件 |
| Warning | 30 分钟 | 值班运维 | 钉钉 + 邮件 |
| Info | 工作时间 | 运维团队 | 邮件 |

### 7.2 告警处理 SOP

1. **确认告警**: 登录 Grafana 查看告警详情
2. **初步诊断**: 根据告警类型查看相关指标
3. **定位问题**: 查看日志、调用链
4. **处理问题**: 按故障排查手册处理
5. **验证恢复**: 确认指标恢复正常
6. **记录总结**: 填写故障报告

---

## 8. 相关文档

- [故障排查手册](TROUBLESHOOTING-GUIDE.md)
- [备份恢复操作手册](BACKUP-RECOVERY-GUIDE.md)
- [资源规划指南](../deployment/RESOURCE-PLANNING-GUIDE.md)
- [系统架构风险审计报告](../architecture/RISK-AUDIT-REPORT.md)

---

## 9. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本，定义监控告警配置 |
