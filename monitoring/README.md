# EVCS Manager 监控配置

本目录包含EVCS Manager的监控体系配置文件，包括ELK日志收集、Grafana Dashboard和告警规则。

## 目录结构

```
monitoring/
├── README.md                              # 本文件
├── elk/                                   # ELK日志收集配置
│   ├── logstash-pipeline.conf            # Logstash管道配置
│   └── elasticsearch-template.json       # Elasticsearch索引模板
├── grafana/                               # Grafana配置
│   ├── dashboards/                       # Dashboard配置
│   │   ├── system-overview.json          # 系统总览Dashboard
│   │   └── business-monitoring.json      # 业务监控Dashboard
│   └── alerts/                           # 告警配置
│       └── alert-rules.yml               # Prometheus告警规则
└── prometheus/                            # Prometheus配置（待添加）
```

## 快速开始

### 1. ELK日志收集

#### 前置条件

- Docker & Docker Compose
- Elasticsearch 8.x
- Logstash 8.x
- Kibana 8.x

#### 部署步骤

```bash
# 1. 创建docker-compose.yml（示例）
cat > docker-compose-elk.yml << 'EOF'
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data

  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    ports:
      - "5000:5000"
    volumes:
      - ./elk/logstash-pipeline.conf:/usr/share/logstash/pipeline/logstash.conf
    depends_on:
      - elasticsearch

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch

volumes:
  elasticsearch-data:
EOF

# 2. 启动ELK Stack
docker-compose -f docker-compose-elk.yml up -d

# 3. 验证服务状态
curl http://localhost:9200/_cluster/health
curl http://localhost:5601/api/status
```

#### 配置应用日志输出

在应用的 `application.yml` 中配置：

```yaml
logging:
  file:
    name: /var/log/evcs/${spring.application.name}.log
```

或配置Logstash TCP输出（推荐用于开发环境）：

```xml
<!-- logback-spring.xml -->
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>localhost:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
</appender>
```

### 2. Grafana Dashboard

#### 前置条件

- Grafana 10.x
- Prometheus (作为数据源)

#### 部署步骤

```bash
# 1. 启动Prometheus和Grafana
docker run -d -p 9090:9090 \
  --name prometheus \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

docker run -d -p 3000:3000 \
  --name grafana \
  grafana/grafana

# 2. 访问Grafana
# URL: http://localhost:3000
# 默认用户名/密码: admin/admin
```

#### 配置Prometheus数据源

1. 登录Grafana
2. 进入 `Configuration` > `Data Sources`
3. 点击 `Add data source`
4. 选择 `Prometheus`
5. 配置URL: `http://prometheus:9090`（如果使用Docker网络）或 `http://localhost:9090`
6. 点击 `Save & Test`

#### 导入Dashboard

**方式1：通过UI导入**

1. 点击 `+` 图标 > `Import`
2. 点击 `Upload JSON file`
3. 选择 `grafana/dashboards/system-overview.json`
4. 选择Prometheus数据源
5. 点击 `Import`

重复以上步骤导入 `business-monitoring.json`

**方式2：通过API导入**

```bash
# 导入系统总览Dashboard
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @grafana/dashboards/system-overview.json

# 导入业务监控Dashboard
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @grafana/dashboards/business-monitoring.json
```

### 3. Prometheus告警规则

#### 配置告警规则

在 `prometheus.yml` 中添加规则文件：

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

# 告警规则文件
rule_files:
  - '/etc/prometheus/alerts/alert-rules.yml'

# 抓取配置
scrape_configs:
  - job_name: 'evcs-order'
    static_configs:
      - targets: ['order-service:8080']
    metrics_path: '/actuator/prometheus'

  - job_name: 'evcs-station'
    static_configs:
      - targets: ['station-service:8082']
    metrics_path: '/actuator/prometheus'

  - job_name: 'evcs-tenant'
    static_configs:
      - targets: ['tenant-service:8081']
    metrics_path: '/actuator/prometheus'

# Alertmanager配置
alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - 'alertmanager:9093'
```

#### 启动带有告警规则的Prometheus

```bash
docker run -d -p 9090:9090 \
  --name prometheus \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v $(pwd)/grafana/alerts/alert-rules.yml:/etc/prometheus/alerts/alert-rules.yml \
  prom/prometheus
```

#### 配置Alertmanager（可选）

```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'default-receiver'

receivers:
  - name: 'default-receiver'
    webhook_configs:
      - url: 'http://your-webhook-url/alert'
    email_configs:
      - to: 'ops@example.com'
        from: 'alertmanager@example.com'
        smarthost: 'smtp.example.com:587'
```

## Dashboard说明

### 系统总览Dashboard

显示系统整体运行状态：

- **服务健康状态**: 各服务的UP/DOWN状态
- **QPS趋势**: 过去1小时的请求速率
- **响应时间**: P50、P95、P99响应时间分布
- **错误率**: 5XX错误率趋势
- **JVM内存**: 堆内存使用情况

### 业务监控Dashboard

显示业务指标：

- **订单趋势**: 今日、昨日、上周订单量对比
- **订单成功率**: 实时订单成功率
- **充电桩在线率**: 充电桩在线百分比
- **充电桩状态分布**: 各状态充电桩数量（饼图）
- **营收统计**: 今日、本月、本年营收
- **活跃充电会话**: 当前正在充电的桩数
- **支付成功率**: 支付交易成功率
- **计费准确率**: 计费系统准确性

## 告警说明

### 系统告警

| 告警名称 | 触发条件 | 严重程度 | 描述 |
|---------|---------|---------|------|
| ServiceDown | 服务不可用 | critical | 服务宕机超过1分钟 |
| HighErrorRate | 错误率 > 5% | warning | 接口错误率过高 |
| HighResponseTime | P95 > 2秒 | warning | 响应时间过长 |
| HighMemoryUsage | 内存使用 > 85% | warning | 内存即将耗尽 |

### 业务告警

| 告警名称 | 触发条件 | 严重程度 | 描述 |
|---------|---------|---------|------|
| HighOrderFailureRate | 失败率 > 10% | warning | 订单创建失败率高 |
| HighChargerOfflineRate | 离线率 > 20% | warning | 充电桩大量离线 |
| HighPaymentFailureRate | 失败率 > 5% | critical | 支付失败率高 |
| LowBillingAccuracy | 准确率 < 95% | critical | 计费准确率低 |
| NoActiveOrders | 营业时间无订单 | warning | 长时间无充电订单 |

## 指标列表

### 应用指标（Spring Boot Actuator）

- `http_server_requests_seconds_count`: HTTP请求总数
- `http_server_requests_seconds_sum`: HTTP请求总耗时
- `http_server_requests_seconds_max`: 最大响应时间
- `jvm_memory_used_bytes`: JVM内存使用
- `jvm_threads_live_threads`: 活跃线程数
- `process_cpu_usage`: 进程CPU使用率

### 业务指标（需在各服务实现）

**订单服务 (evcs-order)**:
- `evcs_order_created_total`: 订单创建总数
- `evcs_order_success_rate`: 订单成功率
- `evcs_order_amount_total`: 订单金额总计

**充电站服务 (evcs-station)**:
- `evcs_charger_online_rate`: 充电桩在线率
- `evcs_charger_status_count`: 各状态充电桩数量
- `evcs_charger_charging_count`: 充电中的桩数

**支付服务 (evcs-payment)**:
- `evcs_payment_success_rate`: 支付成功率
- `evcs_payment_amount_total`: 支付金额总计

**计费服务**:
- `evcs_billing_accuracy_rate`: 计费准确率

## 故障排查

### ELK相关

**问题：日志未被Elasticsearch索引**

1. 检查Logstash状态：
   ```bash
   docker logs logstash
   ```

2. 检查Elasticsearch索引：
   ```bash
   curl http://localhost:9200/_cat/indices?v
   ```

3. 验证日志格式是否为JSON

**问题：Kibana无法连接Elasticsearch**

1. 检查网络连接：
   ```bash
   curl http://elasticsearch:9200/_cluster/health
   ```

2. 检查Kibana配置中的Elasticsearch URL

### Grafana相关

**问题：Dashboard显示"No Data"**

1. 检查Prometheus数据源配置
2. 验证应用是否暴露 `/actuator/prometheus` 端点
3. 检查Prometheus是否成功抓取指标：
   ```bash
   curl http://localhost:9090/api/v1/targets
   ```

**问题：告警未触发**

1. 检查告警规则是否加载：
   访问 http://localhost:9090/rules

2. 检查告警状态：
   访问 http://localhost:9090/alerts

3. 验证Alertmanager配置

### 应用相关

**问题：指标未暴露**

在 `application.yml` 中启用Prometheus端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

## 相关文档

- [EVCS监控体系指南](../docs/MONITORING-GUIDE.md) - 详细的监控配置和使用指南
- [Prometheus官方文档](https://prometheus.io/docs/)
- [Grafana官方文档](https://grafana.com/docs/)
- [ELK Stack文档](https://www.elastic.co/guide/index.html)

## 维护建议

1. **定期清理Elasticsearch索引**
   - 建议保留30天的日志数据
   - 使用Elasticsearch ILM (Index Lifecycle Management)

2. **监控监控系统本身**
   - 监控Elasticsearch磁盘使用率
   - 监控Prometheus存储空间
   - 设置Grafana备份策略

3. **优化告警规则**
   - 根据实际运行情况调整阈值
   - 避免告警风暴
   - 定期回顾和更新告警规则

4. **Dashboard维护**
   - 定期更新Dashboard以反映新功能
   - 收集用户反馈优化可视化
   - 版本控制Dashboard JSON文件

