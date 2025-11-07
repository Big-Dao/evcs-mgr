# EVCS Docker配置使用指南

> **版本**: v1.0 | **更新日期**: 2025-11-07
>
> 📋 **本文档说明所有Docker配置文件的用途和使用方法**

## 🎯 配置文件概览

EVCS项目提供了多个Docker配置文件，支持不同的部署场景：

| 配置文件 | 用途 | 资源需求 | 适用场景 |
|----------|------|----------|----------|
| `docker-compose.yml` | 生产环境 | ~4GB内存 | 完整功能部署 |
| `docker-compose.core-dev.yml` | 核心开发环境 | ~2GB内存 | 日常开发测试 |
| `docker-compose.monitoring.yml` | 监控服务 | ~1GB内存 | 可选监控栈 |
| `docker-compose.dev.yml` | 开发环境 | ~3GB内存 | 开发调试 |
| `docker-compose.test.yml` | 测试环境 | ~2GB内存 | 自动化测试 |

## 🚀 推荐配置

### 1. 核心开发环境（推荐 ⭐）

```bash
# 启动核心服务：基础设施 + 认证 + 网关
docker-compose -f docker-compose.core-dev.yml up -d

# 查看服务状态
docker-compose -f docker-compose.core-dev.yml ps

# 查看日志
docker-compose -f docker-compose.core-dev.yml logs -f
```

**包含服务**:
- ✅ PostgreSQL (数据库)
- ✅ Redis (缓存)
- ✅ RabbitMQ (消息队列)
- ✅ evcs-eureka (服务注册)
- ✅ evcs-config (配置中心)
- ✅ evcs-gateway (API网关)
- ✅ evcs-auth (认证服务)

### 2. 完整生产环境

```bash
# 启动所有服务
docker-compose up -d

# 添加监控服务
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

### 3. 添加监控栈

```bash
# 在核心开发环境基础上添加监控
docker-compose -f docker-compose.core-dev.yml -f docker-compose.monitoring.yml up -d

# 访问监控界面
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

## 📋 配置文件详细说明

### docker-compose.core-dev.yml ⭐

**用途**: 核心开发环境，推荐日常使用
**特点**: 轻量级、启动快、功能完整

```yaml
# 主要配置特点
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: evcs
      POSTGRES_USER: evcs
      POSTGRES_PASSWORD: evcs123

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass evcs123

  rabbitmq:
    image: rabbitmq:3-management-alpine

  evcs-eureka:
    image: evcs-mgr/eureka-service:latest
    environment:
      EUREKA_SERVER_URL: http://evcs-eureka:8761/eureka

  evcs-config:
    image: evcs-mgr/config-service:latest
    environment:
      EUREKA_SERVER_URL: http://evcs-eureka:8761/eureka

  evcs-gateway:
    image: evcs-mgr/auth-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/evcs
      EUREKA_SERVER_URL: http://evcs-eureka:8761/eureka

  evcs-auth:
    image: evcs-mgr/auth-service:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/evcs
      EUREKA_SERVER_URL: http://evcs-eureka:8761/eureka
```

### docker-compose.yml

**用途**: 生产环境完整部署
**特点**: 全功能、高可用、监控就绪

包含所有11个微服务：
- evcs-gateway, evcs-auth, evcs-station, evcs-order
- evcs-payment, evcs-protocol, evcs-tenant, evcs-monitoring
- evcs-config, evcs-eureka
- 所有基础设施服务

### docker-compose.monitoring.yml

**用途**: 监控服务栈
**特点**: 可叠加使用，独立监控

**监控组件**:
- Prometheus (指标收集)
- Grafana (可视化)
- Jaeger (链路追踪)
- AlertManager (告警)

## 🔧 环境变量配置

创建 `.env` 文件：

```bash
# 数据库配置
POSTGRES_DB=evcs
POSTGRES_USER=evcs
POSTGRES_PASSWORD=evcs123

# Redis配置
REDIS_PASSWORD=evcs123

# JWT配置
JWT_SECRET=your_jwt_secret_key_minimum_32_characters

# 外部服务配置（可选）
ALIPAY_APP_ID=your_alipay_app_id
WECHAT_APP_ID=your_wechat_app_id
```

## 🛠️ 常用操作

### 启动和停止

```bash
# 启动服务
docker-compose -f docker-compose.core-dev.yml up -d

# 停止服务
docker-compose -f docker-compose.core-dev.yml down

# 停止并删除数据卷（谨慎使用）
docker-compose -f docker-compose.core-dev.yml down -v

# 重新构建并启动
docker-compose -f docker-compose.core-dev.yml up --build -d
```

### 查看和调试

```bash
# 查看服务状态
docker-compose -f docker-compose.core-dev.yml ps

# 查看日志
docker-compose -f docker-compose.core-dev.yml logs -f evcs-auth

# 进入容器调试
docker-compose -f docker-compose.core-dev.yml exec evcs-auth bash

# 查看资源使用情况
docker stats
```

### 数据管理

```bash
# 备份数据库
docker-compose -f docker-compose.core-dev.yml exec postgres pg_dump -U evcs evcs > backup.sql

# 恢复数据库
docker-compose -f docker-compose.core-dev.yml exec -T postgres psql -U evcs evcs < backup.sql

# 清理未使用的镜像和容器
docker system prune -f
```

## 🚨 故障排除

### 常见问题

#### 1. 端口冲突
```bash
# 检查端口占用
netstat -tulpn | grep 8080

# 修改docker-compose.yml中的端口映射
ports:
  - "8081:8080"  # 将外部端口改为8081
```

#### 2. 服务启动失败
```bash
# 查看详细日志
docker-compose -f docker-compose.core-dev.yml logs [service-name]

# 重新构建特定服务
docker-compose -f docker-compose.core-dev.yml build [service-name]

# 重启服务
docker-compose -f docker-compose.core-dev.yml restart [service-name]
```

#### 3. 内存不足
```bash
# 监控内存使用
docker stats

# 调整JVM参数
environment:
  JAVA_OPTS: "-Xms128m -Xmx256m"

# 使用更轻量的配置
docker-compose -f docker-compose.core-dev.yml up -d  # 而不是完整的docker-compose.yml
```

#### 4. 数据库连接问题
```bash
# 检查数据库状态
docker-compose -f docker-compose.core-dev.yml exec postgres pg_isready

# 重置数据库
docker-compose -f docker-compose.core-dev.yml down -v
docker-compose -f docker-compose.core-dev.yml up -d postgres
```

## 📊 性能优化

### JVM调优

```yaml
# 生产环境JVM参数
environment:
  JAVA_OPTS: >
    -Xms1g
    -Xmx2g
    -XX:+UseG1GC
    -XX:+UseContainerSupport
    -XX:MaxRAMPercentage=75.0
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:HeapDumpPath=/app/dumps/
```

### 数据库优化

```yaml
# PostgreSQL优化
environment:
  POSTGRES_SHARED_PRELOAD_LIBRARIES: pg_stat_statements
  POSTGRES_MAX_CONNECTIONS: 200
  POSTGRES_SHARED_BUFFERS: 256MB
  POSTGRES_EFFECTIVE_CACHE_SIZE: 1GB
```

### Redis优化

```yaml
# Redis优化
command: >
  redis-server
  --maxmemory 512mb
  --maxmemory-policy allkeys-lru
  --requirepass evcs123
```

## 🔒 安全配置

### 生产环境安全清单

- [ ] 更改所有默认密码
- [ ] 使用强密码策略
- [ ] 启用SSL/TLS加密
- [ ] 配置防火墙规则
- [ ] 限制网络访问
- [ ] 定期更新镜像
- [ ] 启用安全扫描

### 网络安全

```yaml
# 创建自定义网络
networks:
  evcs-network:
    driver: bridge
    internal: true  # 内部网络，不访问外网
```

## 📈 监控和日志

### 启用监控

```bash
# 启动监控栈
docker-compose -f docker-compose.core-dev.yml -f docker-compose.monitoring.yml up -d

# 访问监控界面
# Grafana: http://localhost:3000
# Prometheus: http://localhost:9090
# Jaeger: http://localhost:16686
```

### 日志配置

```yaml
# 日志驱动配置
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

## 🧪 测试配置

### 自动化测试

```bash
# 启动测试环境
docker-compose -f docker-compose.test.yml up -d

# 运行测试
./gradlew test

# 清理测试环境
docker-compose -f docker-compose.test.yml down -v
```

### 集成测试

```bash
# 启动完整环境进行集成测试
docker-compose -f docker-compose.yml up -d

# 运行集成测试
./gradlew integrationTest

# 清理环境
docker-compose down -v
```

## 🔄 持续集成

### GitHub Actions示例

```yaml
name: Docker Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Start services
      run: docker-compose -f docker-compose.test.yml up -d

    - name: Wait for services
      run: sleep 30

    - name: Run tests
      run: ./gradlew test

    - name: Stop services
      run: docker-compose -f docker-compose.test.yml down -v
```

## 📚 相关文档

- [统一部署指南](DEPLOYMENT-GUIDE.md)
- [故障排除指南](docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md)
- [AI编程助手规范](AI-ASSISTANTS-INDEX.md)
- [项目编码标准](PROJECT-CODING-STANDARDS.md)

---

**注意**: 推荐使用 `docker-compose.core-dev.yml` 进行日常开发，它提供了最佳的功能性和资源使用平衡。