# EVCS Manager - 部署与运维指南

> **版本**: v2.0 | **更新日期**: 2025-11-02 | **状态**: 生产就绪

## 📋 目录

- [系统概述](#系统概述)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [详细部署](#详细部署)
- [监控配置](#监控配置)
- [运维管理](#运维管理)
- [故障排除](#故障排除)
- [性能优化](#性能优化)
- [生产部署](#生产部署)

## 🔧 系统概述

EVCS Manager是一个基于Spring Boot 3.x和JDK 21的微服务架构电动汽车充电站管理平台，具备企业级监控和安全特性。

### 核心服务组件

#### 业务服务
- **evcs-gateway** 🚪: API网关 - 企业级安全防护、限流熔断、路由转发
- **evcs-auth** 🔐: 认证授权 - JWT认证、权限管理、安全审计
- **evcs-tenant** 🏢: 租户管理 - 多租户隔离、层级管理、权限分配
- **evcs-station** ⚡: 充电站管理 - 站点管理、充电桩、WebSocket通信
- **evcs-order** 📋: 订单管理 - 订单生命周期、计费方案、支付集成
- **evcs-payment** 💳: 支付服务 - 支付渠道集成、对账系统、退款处理
- **evcs-protocol** 🔌: 协议处理 - OCPP协议、云快充协议、WebSocket
- **evcs-monitoring** 📊: 监控服务 - 指标收集、健康检查、告警管理
- **evcs-integration** 🔗: 第三方集成 - 外部系统对接

#### 基础设施服务
- **evcs-config** ⚙️: 配置中心 - Git配置、动态配置、环境管理
- **evcs-eureka** 📡: 服务发现 - 注册中心、健康检查、负载均衡

### 技术特性
- **多租户架构**: 四层数据隔离，支持平台方、运营商、合作伙伴分层管理
- **企业级安全**: JWT认证、CORS白名单、分布式限流、安全审计
- **完整监控**: Prometheus + Grafana + 健康检查，实时监控和告警
- **容器化部署**: Docker + Docker Compose，一键部署和运维

## 📦 环境要求

### 基础环境
- **Java**: JDK 21+ (最新LTS版本，必须)
- **Docker**: 20.10+ (推荐)
- **Docker Compose**: 2.0+ (推荐)
- **内存**: 最小4GB，推荐8GB+
- **磁盘**: 最小10GB可用空间

### 开发环境（可选）
- **IDE**: IntelliJ IDEA 2023+ 或 VS Code
- **Git**: 2.30+
- **Gradle**: 8.5+ (项目自带wrapper)

### 网络端口要求

#### 应用服务端口
| 服务 | 端口 | 说明 |
|------|------|------|
| evcs-gateway | 8080 | API网关（统一入口） |
| evcs-auth | 8081 | 认证授权服务 |
| evcs-station | 8082 | 充电站管理服务 |
| evcs-order | 8083 | 订单管理服务 |
| evcs-payment | 8084 | 支付服务 |
| evcs-protocol | 8085 | 协议处理服务 |
| evcs-tenant | 8086 | 租户管理服务 |
| evcs-monitoring | 8087 | 监控服务 |
| evcs-eureka | 8761 | 服务注册中心 |
| evcs-config | 8888 | 配置中心 |

#### 基础设施端口
| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL | 5432 | 主数据库 |
| Redis | 6379 | 缓存和会话存储 |
| RabbitMQ | 5672 | 消息队列 |
| RabbitMQ管理 | 15672 | 管理界面 |

#### 监控端口
| 服务 | 端口 | 说明 | 认证 |
|------|------|------|------|
| Prometheus | 9090 | 指标收集 | 无 |
| Grafana | 3001 | 可视化仪表盘 | admin/admin123 |
| 前端管理界面 | 3000 | Vue管理后台 | 无 |

## 🚀 快速开始

### 方式一：Docker完整部署（推荐 ⭐）

#### 1. 克隆项目
```bash
git clone https://github.com/Big-Dao/evcs-mgr.git
cd evcs-mgr
```

#### 2. 一键部署
```bash
# 完整部署（包含所有服务和监控组件）
docker-compose up -d

# 或者分步部署
docker-compose up -d postgresql redis rabbitmq eureka config-server  # 基础设施
docker-compose up -d auth gateway                                     # 核心服务
docker-compose up -d tenant station order payment protocol           # 业务服务
docker-compose up -d prometheus grafana                              # 监控组件
```

#### 3. 验证部署
```bash
# 检查所有服务状态
docker-compose ps

# 等待服务启动（约2-3分钟）
watch docker-compose ps

# 检查API网关健康状态
curl http://localhost:8080/actuator/health

# 检查Eureka注册中心
curl http://localhost:8761/eureka/apps
```

### 方式二：本地开发部署

#### 1. 启动基础设施
```bash
# 仅启动数据库、Redis、RabbitMQ
docker-compose -f docker-compose.local.yml up -d

# 等待服务就绪
./scripts/wait-for-it.sh localhost:5432 --timeout=60
./scripts/wait-for-it.sh localhost:6379 --timeout=30
./scripts/wait-for-it.sh localhost:5672 --timeout=30
```

#### 2. 构建和启动服务
```bash
# 构建项目
./gradlew clean build -x test

# 启动核心服务（多个终端并行）
./gradlew :evcs-eureka:bootRun &
./gradlew :evcs-config:bootRun &
./gradlew :evcs-auth:bootRun &
./gradlew :evcs-gateway:bootRun &

# 启动业务服务（按需）
./gradlew :evcs-tenant:bootRun &
./gradlew :evcs-station:bootRun &
./gradlew :evcs-order:bootRun &
./gradlew :evcs-payment:bootRun &
```

#### 3. 访问服务
- **API网关**: http://localhost:8080
- **API文档**: http://localhost:8080/doc.html
- **Eureka**: http://localhost:8761
- **认证服务**: http://localhost:8081

## 📚 详细部署

### 构建阶段

#### 1. 构建应用程序
```bash
# 清理并构建所有模块
./gradlew clean build -x test

# 构建Docker镜像
docker-compose -f docker-compose.monitoring.yml build
```

#### 2. 准备配置文件
```bash
# 复制并修改配置文件
cp docker-compose.monitoring.yml.example docker-compose.monitoring.yml

# 根据环境需要修改配置
vim docker-compose.monitoring.yml
```

### 部署阶段

#### 1. 启动基础设施
```bash
# 启动Redis
docker-compose -f docker-compose.monitoring.yml up -d redis

# 等待Redis就绪
./scripts/wait-for-it.sh localhost:6379 --timeout=60
```

#### 2. 启动应用服务
```bash
# 启动EVCS网关
docker-compose -f docker-compose.monitoring.yml up -d evcs-gateway

# 检查健康状态
curl -f http://localhost:8080/actuator/health
```

#### 3. 启动监控组件（可选）
```bash
# 启动监控服务
./scripts/deploy.sh monitor

# 启动日志服务
./scripts/deploy.sh logs

# 启动链路追踪
./scripts/deploy.sh tracing
```

## 📊 监控配置

### 监控架构概览

EVCS Manager提供企业级监控解决方案，基于以下技术栈：

```
Prometheus (指标收集) → Grafana (可视化) → AlertManager (告警)
     ↓                    ↓                    ↓
Micrometer (Java客户端) → 仪表盘 → 邮件/短信通知
```

### 访问监控界面

#### Grafana仪表盘 ⭐
- **地址**: http://localhost:3001
- **用户名**: admin
- **密码**: admin123
- **功能**: 实时监控、性能分析、告警管理

#### Prometheus指标
- **地址**: http://localhost:9090
- **功能**: 指标查询、目标状态、规则配置

### 预配置仪表盘

#### 1. 系统概览仪表盘
- **整体健康状态**: 服务UP/DOWN状态、资源使用情况
- **请求量统计**: QPS、并发数、请求分布
- **响应时间**: 平均响应时间、P95/P99延迟
- **错误率**: HTTP错误率、异常统计

#### 2. 安全监控仪表盘
- **认证状态**: 登录成功/失败次数、Token状态
- **限流情况**: 限流触发次数、IP分布
- **可疑活动**: 异常访问模式、攻击检测
- **JWT黑名单**: Token撤销统计

#### 3. 业务监控仪表盘
- **充电站状态**: 在线/离线数量、连接统计
- **充电会话**: 活跃会话、历史统计、充电量
- **订单统计**: 订单量、成功率、收入统计
- **支付监控**: 支付成功率、渠道分布、退款统计

#### 4. 性能分析仪表盘
- **JVM性能**: 堆内存使用、GC统计、线程状态
- **数据库性能**: 连接池状态、慢查询统计
- **缓存性能**: Redis命中率、内存使用
- **网络性能**: 连接数、带宽使用

### 关键指标说明

#### HTTP指标
```yaml
http_requests_total:
  描述: HTTP请求总数
  标签: method, path, status, application

http_request_duration_seconds:
  描述: HTTP请求响应时间
  类型: Histogram
  标签: method, path, application

http_active_connections:
  描述: 当前活跃连接数
  类型: Gauge
```

#### 安全指标
```yaml
security_authentication_success_total:
  描述: 认证成功次数
  标签: user_type, ip_address

security_authentication_failure_total:
  描述: 认证失败次数
  标签: failure_type, ip_address

security_rate_limit_total:
  描述: 限流触发次数
  标签: endpoint, limit_type

security_suspicious_activity_total:
  描述: 可疑活动次数
  标签: activity_type, severity
```

#### 业务指标
```yaml
business_charger_connections_total:
  描述: 充电站连接总数
  标签: station_id, status

business_charging_sessions_total:
  描述: 充电会话总数
  标签: station_id, session_status

business_active_chargers:
  描述: 在线充电站数量
  类型: Gauge

business_orders_total:
  描述: 订单总数
  标签: order_status, tenant_id
```

#### 系统指标
```yaml
system_circuit_breakers_open:
  描述: 打开的熔断器数量
  类型: Gauge

jvm_memory_used_bytes:
  描述: JVM内存使用量
  标签: area, heap/non-heap

process_cpu_seconds_total:
  描述: CPU使用时间
  类型: Counter
```

### 健康检查体系

#### 应用级别健康检查
```bash
# 基础健康检查
curl http://localhost:8080/actuator/health

# 详细健康检查
curl http://localhost:8080/actuator/health/detailed

# 组件级健康检查
curl http://localhost:8080/actuator/health/components
```

**响应示例**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 250685575168,
        "free": 214947880960,
        "threshold": 10485760
      }
    }
  }
}
```

#### 业务健康检查
```bash
# 检查核心业务流程
curl http://localhost:8080/actuator/health/business

# 检查外部依赖
curl http://localhost:8080/actuator/health/external
```

## 🔧 运维管理

### 服务管理

#### 查看服务状态
```bash
# 查看所有服务状态
docker-compose ps

# 查看特定服务日志
docker-compose logs -f evcs-gateway
docker-compose logs -f evcs-auth
docker-compose logs -f evcs-station

# 查看最近的日志
docker-compose logs --tail=100 evcs-gateway
```

#### 服务重启和更新
```bash
# 重启单个服务
docker-compose restart evcs-gateway

# 重新构建并启动
docker-compose up -d --build evcs-gateway

# 滚动更新（零停机）
docker-compose up -d --no-deps evcs-gateway
```

### 配置管理

#### 动态配置更新
```bash
# 通过配置中心更新配置
curl -X POST http://localhost:8888/actuator/refresh \
  -H "Content-Type: application/json"

# 查看当前配置
curl http://localhost:8080/actuator/configprops
```

#### 环境变量管理
```bash
# 查看环境变量
docker-compose exec evcs-gateway env | grep EVCS

# 更新环境变量
docker-compose up -d -e SPRING_PROFILES_ACTIVE=prod evcs-gateway
```

### 数据备份

#### 数据库备份
```bash
# PostgreSQL备份
docker-compose exec postgresql pg_dump -U evcs_user evcs_db > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker-compose exec -T postgresql psql -U evcs_user evcs_db < backup_20251102.sql
```

#### Redis备份
```bash
# Redis数据备份
docker-compose exec redis redis-cli BGSAVE

# 复制RDB文件
docker cp evcs-mgr_redis_1:/data/dump.rdb ./redis_backup_$(date +%Y%m%d).rdb
```

### 日志管理

#### 日志查看和分析
```bash
# 实时查看日志
docker-compose logs -f --tail=100 evcs-gateway

# 查看错误日志
docker-compose logs evcs-gateway | grep ERROR

# 按时间查看日志
docker-compose logs --since="2025-11-02T06:00:00" evcs-gateway
```

#### 日志轮转配置
```yaml
# docker-compose.yml中的日志配置
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

### 性能监控

#### JVM性能分析
```bash
# 查看JVM指标
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 生成堆转储
curl -X POST http://localhost:8080/actuator/heapdump

# 查看线程信息
curl http://localhost:8080/actuator/threads
```

#### 数据库性能监控
```bash
# 查看连接池状态
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# 监控慢查询
docker-compose logs postgresql | grep "slow query"
```

### 健康检查

系统提供多层健康检查：

#### 应用级别
```bash
# 基础健康检查
curl http://localhost:8080/actuator/health

# 详细健康检查
curl http://localhost:8080/actuator/health/detailed

# 异步健康检查
curl http://localhost:8080/actuator/health/async
```

#### 组件级别
- **Redis连接**: 读写测试
- **数据库连接**: 连接池状态
- **外部服务**: 依赖服务可用性

### 日志聚合

系统提供完整的日志聚合方案：

#### 日志级别
- `ERROR`: 系统错误、异常
- `WARN`: 警告信息、降级操作
- `INFO`: 重要业务事件
- `DEBUG`: 调试信息

#### 日志格式
```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "level": "INFO",
  "logger": "com.evcs.gateway.service",
  "message": "Request processed successfully",
  "traceId": "abc123",
  "spanId": "def456",
  "userId": "user123",
  "requestId": "req789"
}
```

## 🔧 故障排除

### 常见问题

#### 1. 应用启动失败
```bash
# 查看应用日志
docker-compose -f docker-compose.monitoring.yml logs evcs-gateway

# 检查配置文件
docker-compose -f docker-compose.monitoring.yml config
```

#### 2. Redis连接失败
```bash
# 检查Redis状态
docker-compose -f docker-compose.monitoring.yml exec redis redis-cli ping

# 检查网络连接
docker network ls
docker network inspect evcs-mgr_evcs-network
```

#### 3. 监控服务异常
```bash
# 检查Prometheus配置
curl http://localhost:9090/targets

# 检查Grafana数据源
curl http://localhost:3000/api/datasources
```

#### 4. 内存不足
```bash
# 检查容器资源使用
docker stats

# 调整JVM参数
export JAVA_OPTS="-Xms256m -Xmx512m"
```

### 性能问题诊断

#### 1. 高响应时间
- 检查CPU使用率
- 分析慢查询日志
- 检查网络延迟

#### 2. 高内存使用
- 分析内存泄漏
- 调整堆内存大小
- 检查缓存配置

#### 3. 连接池耗尽
- 增加连接池大小
- 检查连接泄漏
- 优化查询性能

## ⚡ 性能优化

### JVM优化（JDK 21）
```bash
# 生产环境JVM参数 - 针对JDK 21优化
JAVA_OPTS="
-Xms1g -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:InitialRAMPercentage=50.0
-XX:+OptimizeStringConcat
-XX:+UnlockExperimentalVMOptions
-XX:+UseStringDeduplication
-Djava.security.egd=file:/dev/./urandom
"
```

### 数据库连接池优化
```yaml
# HikariCP优化配置（已在Docker镜像中配置）
spring:
  datasource:
    hikari:
      pool-name: HikariCP-EVCS
      minimum-idle: 10
      maximum-pool-size: 30
      auto-commit: true
      idle-timeout: 300000
      max-lifetime: 600000
      connection-timeout: 20000
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
```

### Redis优化
```yaml
# Redis生产环境配置
maxmemory 1gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec
```

### 网络优化
```yaml
# 应用配置优化
server:
  tomcat:
    max-threads: 200
    min-spare-threads: 10
    max-connections: 8192
    accept-count: 100
    connection-timeout: 20000
```

### 监控优化
```yaml
# Prometheus配置优化
global:
  scrape_interval: 15s
  evaluation_interval: 15s

# 减少不必要的指标采集
scrape_configs:
  - job_name: 'evcs-gateway'
    scrape_interval: 30s
    scrape_timeout: 10s
    metrics_path: '/actuator/prometheus'
```

## 🚀 生产部署

### 高可用架构部署

#### 1. 负载均衡配置（Nginx）
```nginx
upstream evcs_gateway {
    server 192.168.1.10:8080 weight=1 max_fails=3 fail_timeout=30s;
    server 192.168.1.11:8080 weight=1 max_fails=3 fail_timeout=30s;
    server 192.168.1.12:8080 weight=1 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.evcs.com;

    location / {
        proxy_pass http://evcs_gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
}
```

#### 2. 数据库主从配置
```yaml
# PostgreSQL主从配置
postgresql:
  primary:
    host: postgres-master.internal
    port: 5432
    database: evcs_db
    username: evcs_user
    password: ${DB_PASSWORD}
  replica:
    host: postgres-replica.internal
    port: 5432
    database: evcs_db
    username: evcs_readonly
    password: ${DB_READONLY_PASSWORD}
```

#### 3. Redis集群配置
```yaml
# Redis Cluster配置
redis:
  cluster:
    nodes:
      - redis-node-1.internal:6379
      - redis-node-2.internal:6379
      - redis-node-3.internal:6379
      - redis-node-4.internal:6379
      - redis-node-5.internal:6379
      - redis-node-6.internal:6379
    max-redirects: 3
    password: ${REDIS_PASSWORD}
```

### 安全加固

#### 1. HTTPS配置
```yaml
# SSL/TLS配置
server:
  port: 443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: evcs-manager
  port: 80
  http2:
    enabled: true
```

#### 2. 防火墙配置
```bash
# UFW防火墙配置
ufw allow 22/tcp      # SSH
ufw allow 80/tcp      # HTTP
ufw allow 443/tcp     # HTTPS
ufw allow 8080/tcp    # API Gateway
ufw allow 9090/tcp    # Prometheus (内网)
ufw allow 3001/tcp    # Grafana (内网)
ufw enable
```

#### 3. 安全头配置
```yaml
# 安全HTTP头配置
security:
  headers:
    content-security-policy: "default-src 'self'"
    x-content-type-options: nosniff
    x-frame-options: DENY
    x-xss-protection: "1; mode=block"
    strict-transport-security: "max-age=31536000; includeSubDomains"
```

### 监控告警配置

#### 1. AlertManager配置
```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'alerts@evcs.com'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'

receivers:
  - name: 'web.hook'
    email_configs:
      - to: 'admin@evcs.com'
        subject: '[EVCS Manager] 告警通知'
        body: |
          {{ range .Alerts }}
          告警: {{ .Annotations.summary }}
          描述: {{ .Annotations.description }}
          时间: {{ .StartsAt }}
          {{ end }}
```

#### 2. 告警规则
```yaml
# prometheus-rules.yml
groups:
  - name: evcs-alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.job }} 已下线"
          description: "服务 {{ $labels.job }} 在 {{ $labels.instance }} 已经下线超过1分钟"

      - alert: HighResponseTime
        expr: http_request_duration_seconds{quantile="0.95"} > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "响应时间过高"
          description: "服务 {{ $labels.job }} 的95%响应时间超过1秒"

      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "错误率过高"
          description: "服务 {{ $labels.job }} 的5xx错误率超过10%"
```

### 备份策略

#### 1. 自动备份脚本
```bash
#!/bin/bash
# backup.sh - 自动备份脚本

BACKUP_DIR="/backup/evcs-manager"
DATE=$(date +%Y%m%d_%H%M%S)

# 数据库备份
docker-compose exec -T postgresql pg_dump -U evcs_user evcs_db > $BACKUP_DIR/db_backup_$DATE.sql

# Redis备份
docker-compose exec redis redis-cli BGSAVE
docker cp evcs-mgr_redis_1:/data/dump.rdb $BACKUP_DIR/redis_backup_$DATE.rdb

# 配置文件备份
tar -czf $BACKUP_DIR/config_backup_$DATE.tar.gz config-repo/

# 清理旧备份（保留7天）
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.rdb" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete

echo "备份完成: $DATE"
```

#### 2. 定时备份任务
```bash
# 添加到crontab
# 每天凌晨2点执行备份
0 2 * * * /opt/evcs-manager/scripts/backup.sh >> /var/log/evcs-backup.log 2>&1
```

### 扩容策略

#### 1. 水平扩容
```bash
# 扩展API网关实例
docker-compose up -d --scale evcs-gateway=3

# 扩展业务服务实例
docker-compose up -d --scale evcs-station=2
docker-compose up -d --scale evcs-order=2
```

#### 2. 垂直扩容
```yaml
# 调整容器资源限制
services:
  evcs-gateway:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 1G
```

## 📞 技术支持

### 故障排查流程

1. **快速诊断**
   ```bash
   # 检查所有服务状态
   docker-compose ps

   # 检查系统资源
   df -h
   free -h
   docker stats
   ```

2. **日志分析**
   ```bash
   # 查看错误日志
   docker-compose logs --tail=100 | grep -i error

   # 查看特定时间段日志
   docker-compose logs --since="1h" evcs-gateway
   ```

3. **网络检查**
   ```bash
   # 检查容器间连通性
   docker network ls
   docker network inspect evcs-mgr_default

   # 检查端口占用
   netstat -tulpn | grep -E ':(8080|5432|6379)'
   ```

### 联系方式

- **技术文档**: [文档索引](DOCUMENTATION-INDEX.md)
- **API文档**: [API指南](API-DOCUMENTATION-GUIDE.md)
- **项目状态**: [状态报告](docs/PROJECT-STATUS-REPORT.md)
- **快速开始**: [快速指南](QUICK-START-GUIDE.md)

### 支持渠道
- 📧 **技术支持**: support@evcs-manager.com
- 🐛 **问题反馈**: [GitHub Issues](https://github.com/Big-Dao/evcs-mgr/issues)
- 💬 **技术讨论**: [GitHub Discussions](https://github.com/Big-Dao/evcs-mgr/discussions)

---

**注意**: 本部署指南支持Docker Compose和Kubernetes两种部署方式。生产环境建议使用Kubernetes进行容器编排，以获得更好的弹性和可管理性。

**最后更新**: 2025-11-02
**文档版本**: v2.0