# EVCS充电站管理系统 - 部署指南

## 📋 目录

- [系统概述](#系统概述)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [详细部署](#详细部署)
- [监控配置](#监控配置)
- [故障排除](#故障排除)
- [性能优化](#性能优化)

## 🔧 系统概述

EVCS充电站管理系统是一个基于Spring Cloud Gateway的微服务架构系统，包含以下核心组件：

- **API网关** (evcs-gateway): 统一入口、安全认证、限流熔断
- **协议服务** (evcs-protocol): OCPP协议支持、WebSocket通信
- **充电站服务** (evcs-charger): 充电站管理、状态监控
- **用户服务** (evcs-user): 用户管理、认证授权
- **支付服务** (evcs-payment): 支付处理、账单管理

## 📦 环境要求

### 基础环境
- **Java**: JDK 21+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **内存**: 最小4GB，推荐8GB+
- **磁盘**: 最小10GB可用空间

### 网络要求
- **端口**: 8080 (API网关), 8089 (OCPP WebSocket)
- **监控端口**: 9090 (Prometheus), 3000 (Grafana)
- **数据库端口**: 6379 (Redis), 5432 (PostgreSQL)

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone <repository-url>
cd evcs-mgr
```

### 2. 一键部署（推荐）
```bash
# 完整部署（包含监控组件）
./scripts/deploy.sh full

# 或基础部署（仅应用服务）
./scripts/deploy.sh basic
```

### 3. 验证部署
```bash
# 检查服务状态
curl http://localhost:8080/actuator/health

# 查看服务列表
docker-compose -f docker-compose.monitoring.yml ps
```

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

### Prometheus指标

系统暴露以下关键指标：

#### HTTP指标
- `http_requests_total`: HTTP请求总数
- `http_request_duration_seconds`: HTTP请求响应时间
- `http_active_connections`: 活跃连接数

#### 安全指标
- `security_authentication_success_total`: 认证成功次数
- `security_authentication_failure_total`: 认证失败次数
- `security_rate_limit_total`: 限流触发次数
- `security_suspicious_activity_total`: 可疑活动次数

#### 业务指标
- `business_charger_connections_total`: 充电站连接总数
- `business_charging_sessions_total`: 充电会话总数
- `business_active_chargers`: 在线充电站数量

#### 系统指标
- `system_circuit_breakers_open`: 打开的熔断器数量
- `jvm_memory_used_bytes`: JVM内存使用量
- `process_cpu_seconds_total`: CPU使用时间

### Grafana仪表盘

访问 `http://localhost:3000` (admin/admin123) 查看预配置的仪表盘：

1. **系统概览**: 整体健康状态、请求量、响应时间
2. **安全监控**: 认证状态、限流情况、可疑活动
3. **业务指标**: 充电站状态、充电会话统计
4. **性能分析**: 响应时间分布、错误率、吞吐量

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

### JVM优化
```bash
# 生产环境JVM参数
JAVA_OPTS="
-Xms1g -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:+OptimizeStringConcat
-Djava.security.egd=file:/dev/./urandom
"
```

### Redis优化
```yaml
# redis.conf优化配置
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
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
```

## 📞 技术支持

如果遇到部署问题，请按以下步骤排查：

1. **检查日志**: 查看相关服务的详细日志
2. **验证配置**: 确认配置文件正确性
3. **网络检查**: 验证容器间网络连通性
4. **资源监控**: 检查系统资源使用情况

更多技术文档请参考：
- [API文档](./API.md)
- [开发指南](./DEVELOPMENT.md)
- [架构设计](./ARCHITECTURE.md)

---

**注意**: 本部署指南基于Docker Compose，生产环境建议使用Kubernetes进行容器编排。