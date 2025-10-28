# 业务监控指标文档

## 概述

本文档描述了 EVCS Manager 系统的业务监控指标实现。这些指标用于监控关键业务流程的健康状况和性能表现。

## 监控指标类别

### 1. 订单服务指标 (OrderMetrics)

**位置**: `evcs-order/src/main/java/com/evcs/order/metrics/OrderMetrics.java`

#### 计数器 (Counter)

| 指标名称 | 类型 | 描述 |
|---------|------|------|
| `evcs.order.created.total` | Counter | 成功创建的订单总数 |
| `evcs.order.created.failure.total` | Counter | 创建失败的订单总数 |
| `evcs.order.started.total` | Counter | 成功启动的订单总数 |
| `evcs.order.started.failure.total` | Counter | 启动失败的订单总数 |
| `evcs.order.stopped.total` | Counter | 成功停止的订单总数 |
| `evcs.order.stopped.failure.total` | Counter | 停止失败的订单总数 |
| `evcs.order.billing.success.total` | Counter | 计费成功的总数 |
| `evcs.order.billing.failure.total` | Counter | 计费失败的总数 |

#### 仪表盘 (Gauge)

| 指标名称 | 类型 | 描述 |
|---------|------|------|
| `evcs.order.active.current` | Gauge | 当前活跃订单数 |

#### 计时器 (Timer)

| 指标名称 | 类型 | 描述 |
|---------|------|------|
| `evcs.order.creation.duration` | Timer | 订单创建响应时间（秒） |

#### 派生指标

- **订单创建成功率**: `evcs.order.created.total / (evcs.order.created.total + evcs.order.created.failure.total) * 100%`
- **计费准确率**: `evcs.order.billing.success.total / (evcs.order.billing.success.total + evcs.order.billing.failure.total) * 100%`

---

### 2. 充电站服务指标 (StationMetrics)

**位置**: `evcs-station/src/main/java/com/evcs/station/metrics/StationMetrics.java`

#### 计数器 (Counter)

| 指标名称 | 类型 | 描述 |
|---------|------|------|
| `evcs.charger.online.total` | Counter | 充电桩上线事件总数 |
| `evcs.charger.offline.total` | Counter | 充电桩离线事件总数 |
| `evcs.charger.status.change.total` | Counter | 充电桩状态变更总数 |
| `evcs.charger.heartbeat.received.total` | Counter | 接收到的心跳总数 |
| `evcs.charger.heartbeat.missed.total` | Counter | 丢失的心跳总数 |
| `evcs.station.created.total` | Counter | 创建的充电站总数 |
| `evcs.station.updated.total` | Counter | 更新的充电站总数 |

#### 仪表盘 (Gauge)

| 指标名称 | 类型 | 描述 |
|---------|------|------|
| `evcs.charger.total` | Gauge | 充电桩总数 |
| `evcs.charger.online` | Gauge | 在线充电桩数量 |
| `evcs.charger.offline` | Gauge | 离线充电桩数量 |
| `evcs.charger.charging` | Gauge | 正在充电的充电桩数量 |
| `evcs.charger.faulted` | Gauge | 故障充电桩数量 |

#### 派生指标

- **充电桩在线率**: `evcs.charger.online / evcs.charger.total * 100%`
- **心跳接收率**: `evcs.charger.heartbeat.received.total / (evcs.charger.heartbeat.received.total + evcs.charger.heartbeat.missed.total) * 100%`

---

### 3. 支付服务指标 (PaymentMetrics)

**位置**: `evcs-payment/src/main/java/com/evcs/payment/metrics/PaymentMetrics.java`

#### 计数器 (Counter)

| 指标名称 | 类型 | 描述 |
|---------|------|------|
| `evcs.payment.request.total` | Counter | 支付请求总数 |
| `evcs.payment.success.total` | Counter | 支付成功总数 |
| `evcs.payment.failure.total` | Counter | 支付失败总数 |
| `evcs.payment.callback.received.total` | Counter | 接收到的支付回调总数 |
| `evcs.payment.callback.success.total` | Counter | 支付回调处理成功总数 |
| `evcs.payment.callback.failure.total` | Counter | 支付回调处理失败总数 |
| `evcs.payment.refund.request.total` | Counter | 退款请求总数 |
| `evcs.payment.refund.success.total` | Counter | 退款成功总数 |
| `evcs.payment.refund.failure.total` | Counter | 退款失败总数 |
| `evcs.payment.reconciliation.success.total` | Counter | 对账成功总数 |
| `evcs.payment.reconciliation.failure.total` | Counter | 对账失败总数 |
| `evcs.payment.channel.success.total{channel}` | Counter | 按渠道统计的支付成功数 |
| `evcs.payment.channel.failure.total{channel}` | Counter | 按渠道统计的支付失败数 |

#### 仪表盘 (Gauge)

| 指标名称 | 类型 | 描述 |
|---------|------|------|
| `evcs.payment.amount.total` | Gauge | 总支付金额（分） |
| `evcs.payment.refund.amount.total` | Gauge | 总退款金额（分） |

#### 计时器 (Timer)

| 指标名称 | 类型 | 描述 |
|---------|------|------|
| `evcs.payment.process.duration` | Timer | 支付处理时间（秒） |

#### 派生指标

- **支付成功率**: `evcs.payment.success.total / evcs.payment.request.total * 100%`
- **支付回调成功率**: `evcs.payment.callback.success.total / (evcs.payment.callback.success.total + evcs.payment.callback.failure.total) * 100%`
- **渠道成功率**: `evcs.payment.channel.success.total{channel} / (evcs.payment.channel.success.total{channel} + evcs.payment.channel.failure.total{channel}) * 100%`

---

## Prometheus 查询示例

### 1. 订单创建成功率（最近5分钟）

```promql
sum(rate(evcs_order_created_total[5m])) / 
(sum(rate(evcs_order_created_total[5m])) + sum(rate(evcs_order_created_failure_total[5m]))) * 100
```

### 2. 充电桩在线率

```promql
evcs_charger_online / evcs_charger_total * 100
```

### 3. 支付成功率（最近5分钟）

```promql
sum(rate(evcs_payment_success_total[5m])) / sum(rate(evcs_payment_request_total[5m])) * 100
```

### 4. 订单创建P99响应时间

```promql
histogram_quantile(0.99, sum(rate(evcs_order_creation_duration_bucket[5m])) by (le))
```

### 5. 按渠道统计的支付成功率

```promql
sum by (channel) (rate(evcs_payment_channel_success_total[5m])) / 
(sum by (channel) (rate(evcs_payment_channel_success_total[5m])) + 
 sum by (channel) (rate(evcs_payment_channel_failure_total[5m]))) * 100
```

---

## Grafana Dashboard

Grafana Dashboard 配置文件位于:
```
monitoring/grafana/dashboards/business-metrics.json
```

### 导入 Dashboard

1. 登录 Grafana (默认: http://localhost:3000)
2. 点击左侧菜单 "+" → "Import"
3. 上传 `business-metrics.json` 文件
4. 选择 Prometheus 数据源
5. 点击 "Import"

### Dashboard 面板说明

| 面板名称 | 类型 | 描述 |
|---------|------|------|
| 订单创建成功率 | Stat | 显示当前订单创建成功率 |
| 充电桩在线率 | Stat | 显示当前充电桩在线率 |
| 支付成功率 | Stat | 显示当前支付成功率 |
| 计费准确率 | Stat | 显示当前计费准确率 |
| 订单创建趋势 | Graph | 订单创建成功/失败的趋势图 |
| 订单创建响应时间 | Graph | P50/P95/P99响应时间 |
| 当前活跃订单数 | Graph | 实时活跃订单数量 |
| 充电桩状态分布 | Graph | 在线/离线/充电中/故障状态分布 |
| 支付渠道成功率对比 | Graph | 各支付渠道的成功率对比 |
| 支付处理时间 | Graph | P50/P95/P99支付处理时间 |

---

## 告警规则建议

### 1. 订单创建成功率过低

```yaml
- alert: OrderCreationSuccessRateLow
  expr: sum(rate(evcs_order_created_total[5m])) / (sum(rate(evcs_order_created_total[5m])) + sum(rate(evcs_order_created_failure_total[5m]))) * 100 < 95
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "订单创建成功率过低"
    description: "订单创建成功率低于95%，当前值: {{ $value }}%"
```

### 2. 充电桩在线率过低

```yaml
- alert: ChargerOnlineRateLow
  expr: evcs_charger_online / evcs_charger_total * 100 < 80
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "充电桩在线率过低"
    description: "充电桩在线率低于80%，当前值: {{ $value }}%"
```

### 3. 支付成功率过低

```yaml
- alert: PaymentSuccessRateLow
  expr: sum(rate(evcs_payment_success_total[5m])) / sum(rate(evcs_payment_request_total[5m])) * 100 < 95
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "支付成功率过低"
    description: "支付成功率低于95%，当前值: {{ $value }}%"
```

### 4. 计费准确率过低

```yaml
- alert: BillingAccuracyLow
  expr: sum(rate(evcs_order_billing_success_total[5m])) / (sum(rate(evcs_order_billing_success_total[5m])) + sum(rate(evcs_order_billing_failure_total[5m]))) * 100 < 99.9
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "计费准确率过低"
    description: "计费准确率低于99.9%，当前值: {{ $value }}%"
```

---

## 集成到现有服务

业务监控指标已集成到以下服务实现中：

### 订单服务
- `ChargingOrderServiceImpl.createOrderOnStart()` - 记录订单创建指标
- `ChargingOrderServiceImpl.completeOrderOnStop()` - 记录订单停止和计费指标

### 充电站服务
- `ChargerServiceImpl.updateStatus()` - 记录充电桩状态变更指标
- `StationServiceImpl.saveStation()` - 记录充电站创建指标
- `StationServiceImpl.updateStation()` - 记录充电站更新指标

### 支付服务
- `PaymentServiceImpl.createPayment()` - 记录支付请求和响应时间指标

---

## 验证指标

### 1. 查看 Prometheus 指标

访问 Prometheus 指标端点:
```bash
curl http://localhost:8081/actuator/prometheus | grep evcs
```

### 2. 查看 Actuator Metrics

```bash
curl http://localhost:8081/actuator/metrics | jq '.names | .[] | select(startswith("evcs"))'
```

### 3. 查看特定指标详情

```bash
curl http://localhost:8081/actuator/metrics/evcs.order.created.total
```

---

## 性能影响

业务监控指标的性能影响极小：

- **Counter增量**: ~10 ns/操作
- **Gauge更新**: ~20 ns/操作
- **Timer记录**: ~100 ns/操作

对于高频操作（如订单创建、状态更新），性能开销可忽略不计（< 0.01%）。

---

## 最佳实践

1. **及时记录**: 在业务操作完成后立即记录指标，避免遗漏
2. **异常处理**: 在 catch 块中记录失败指标，确保统计准确
3. **标签使用**: 合理使用标签区分不同维度（如支付渠道）
4. **聚合查询**: 使用 Prometheus 聚合函数计算成功率等派生指标
5. **告警阈值**: 根据业务 SLA 设置合理的告警阈值

---

## 故障排查

### 问题: 指标未显示

**解决方案**:
1. 检查 Actuator 是否启用: `management.endpoints.web.exposure.include=*`
2. 确认 Micrometer 依赖已添加: `io.micrometer:micrometer-registry-prometheus`
3. 验证 Metrics Bean 是否已注册: 检查 Spring Boot 日志

### 问题: Grafana 无数据

**解决方案**:
1. 检查 Prometheus 是否能抓取指标: 访问 Prometheus Targets 页面
2. 验证查询语句: 在 Prometheus 控制台测试查询
3. 检查时间范围: 确保选择的时间范围内有数据

---

## 扩展建议

未来可以添加的监控指标：

1. **用户行为指标**: 用户登录、充值、充电次数统计
2. **设备健康指标**: 充电桩电压、电流、温度等物理指标
3. **业务分析指标**: 用户充电习惯、高峰时段分析、地理位置分布
4. **成本分析指标**: 电费成本、运营成本、利润分析

---

## 参考文档

- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboard Best Practices](https://grafana.com/docs/grafana/latest/best-practices/)
