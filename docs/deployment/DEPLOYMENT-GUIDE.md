# EVCS充电站管理系统 - 统一部署指南

> **版本**: v1.2 | **最后更新**: 2025-11-10 | **维护者**: DevOps 团队 | **状态**: 活跃
>
> 📋 **唯一权威部署指引**：覆盖本地、测试、生产、演示环境流程
>
> 📋 **本文档替代所有其他部署指南，作为唯一的权威部署文档**

## 🎯 概述

本文档提供EVCS充电站管理平台的完整部署方案，支持从小规模开发到企业级生产环境的各种部署需求。

### 🏗️ 系统架构

#### 微服务组件
```
evcs-gateway (8080)     - API网关，路由与安全防护（统一鉴权）
evcs-auth (8081)        - 认证授权服务，JWT + RBAC + 多租户上下文
evcs-station (8082)     - 充电站管理，设备控制与状态同步
evcs-order (8083)       - 订单管理，计费方案，演示订单基线
evcs-payment (8084)     - 支付服务，支付宝/微信沙箱（集成中）
evcs-protocol (8085)    - 协议处理，OCPP / 云快充事件流
evcs-tenant (8086)     - 租户管理，多租户隔离
evcs-monitoring (8087) - 监控服务，健康检查
evcs-config (8888)     - 配置中心，Git配置
evcs-eureka (8761)     - 服务注册中心
evcs-common            - 公共组件，共享工具类
```

#### 演示数据准备
- **脚本位置**: `sql/demo-order-data.sql`（tenant_id = 1001）
- **macOS / Linux**:
  ```bash
  cat sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr
  ```
- **Windows PowerShell**:
  ```powershell
  Get-Content sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr
  ```
- **用途**: 导入星辰能源平台计费方案及 5 条订单样本，供前端演示 / 联调使用。
- **注意**: 脚本会同步更新计费与订单序列值，执行完即可继续创建新数据。

#### 基础设施
```
PostgreSQL (5432)      - 主数据库
Redis (6379)          - 缓存和会话存储
RabbitMQ (5672/15672)  - 消息队列
Nginx (80/443)        - 负载均衡（可选）
```

## 🚀 部署方案

### 方案一：核心开发环境（推荐 ⭐）

**适用场景**: 日常开发、功能测试、小规模演示
**资源需求**: ~2GB内存，~10GB存储
**启动时间**: 2-3分钟

```bash
# 启动核心服务
docker-compose -f docker-compose.core-dev.yml up -d

# 检查服务状态
docker-compose -f docker-compose.core-dev.yml ps

# 查看服务日志
docker-compose -f docker-compose.core-dev.yml logs -f

# 停止服务
docker-compose -f docker-compose.core-dev.yml down
```

**包含服务**:
- ✅ evcs-gateway (API网关)
- ✅ evcs-auth (认证服务)
- ✅ evcs-eureka (服务注册)
- ✅ evcs-config (配置中心)
- ✅ PostgreSQL + Redis + RabbitMQ

**访问地址**:
- 🚪 API网关: http://localhost:8080
- 🔐 认证服务: http://localhost:8081
- 📡 服务注册中心: http://localhost:8761
- ⚙️ 配置中心: http://localhost:8888

### 方案二：完整生产环境

**适用场景**: 生产环境、完整功能测试
**资源需求**: ~4GB内存，~20GB存储
**启动时间**: 5-8分钟

```bash
# 启动所有服务
docker-compose up -d

# 添加监控服务（可选）
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# 检查所有服务状态
docker-compose ps

# 健康检查
curl http://localhost:8080/actuator/health
```

**包含服务**: 所有11个微服务 + 基础设施 + 监控

### 方案三：自定义环境

根据需要选择不同的docker-compose文件：

```bash
# 基础服务 + 核心业务
docker-compose -f docker-compose.yml -f docker-compose.core-services.yml up -d

# 添加外部集成
docker-compose -f docker-compose.yml -f docker-compose.integration.yml up -d

# 添加完整监控
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

## 🔧 环境配置

### 必需的环境变量

创建 `.env` 文件：

```bash
# 数据库配置
POSTGRES_DB=evcs
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password

# Redis配置
REDIS_PASSWORD=your_redis_password

# JWT配置
JWT_SECRET=your_jwt_secret_key_at_least_32_characters

# 外部服务配置（可选）
ALIPAY_APP_ID=your_alipay_app_id
ALIPAY_PRIVATE_KEY=your_alipay_private_key
WECHAT_APP_ID=your_wechat_app_id
WECHAT_MCH_ID=your_wechat_mch_id
```

### JVM优化配置

**小规模业务优化**（核心开发环境）：
```yaml
environment:
  JAVA_OPTS: >
    -Xms256m -Xmx512m
    -XX:+UseContainerSupport
    -XX:MaxRAMPercentage=60.0
    -server
```

**生产环境配置**：
```yaml
environment:
  JAVA_OPTS: >
    -Xms1g -Xmx2g
    -XX:+UseG1GC
    -XX:+UseContainerSupport
    -XX:MaxRAMPercentage=75.0
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:HeapDumpPath=/app/dumps/
```

## 📊 部署验证

### 健康检查

```bash
# 检查所有服务健康状态
for service in gateway auth station order payment protocol tenant monitoring config eureka; do
  echo "=== $service service ==="
  curl -s http://localhost:808${service:0:1}/actuator/health || echo "Service not responding"
done
```

### 功能测试

```bash
# 1. 测试网关路由
curl http://localhost:8080/api/auth/test

# 2. 测试认证服务
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# 3. 检查服务注册
curl http://localhost:8761/eureka/apps

# 4. 测试配置中心
curl http://localhost:8888/evcs-auth/dev
```

## 🛠️ 故障排除

### 常见问题

#### 1. 服务启动失败
```bash
# 查看详细日志
docker-compose logs [service-name]

# 检查端口占用
netstat -tulpn | grep [port]

# 重新构建镜像
docker-compose build [service-name]
```

#### 2. 内存不足
```bash
# 监控内存使用
docker stats

# 调整JVM参数
export JAVA_OPTS="-Xms128m -Xmx256m"
docker-compose up -d
```

#### 3. 数据库连接失败
```bash
# 检查数据库状态
docker-compose exec postgres pg_isready

# 重置数据库
docker-compose down -v
docker-compose up -d postgres
```

#### 4. 服务注册失败
```bash
# 检查Eureka状态
curl http://localhost:8761/eureka/status

# 重启服务注册中心
docker-compose restart evcs-eureka
```

### 性能调优

#### 数据库优化
```sql
-- 创建索引
CREATE INDEX CONCURRENTLY idx_orders_tenant_id ON orders(tenant_id);
CREATE INDEX CONCURRENTLY idx_stations_status ON stations(status);

-- 分析查询性能
EXPLAIN ANALYZE SELECT * FROM orders WHERE tenant_id = 1;
```

#### 缓存配置
```yaml
# application.yml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5分钟
      cache-null-values: false
```

## 📈 监控和日志

### 启用监控

```bash
# 启动完整监控栈
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# 访问监控界面
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
# Jaeger: http://localhost:16686
```

### 日志配置

```yaml
# application.yml
logging:
  level:
    com.evcs: INFO
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 🔒 安全配置

### 生产环境安全清单

- [ ] 更改所有默认密码
- [ ] 配置HTTPS证书
- [ ] 启用防火墙规则
- [ ] 定期备份数据库
- [ ] 配置日志轮转
- [ ] 启用安全监控
- [ ] 更新依赖包版本

### SSL/TLS配置

```nginx
# nginx.conf
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://evcs-gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 🚀 生产部署最佳实践

### 1. 环境准备
```bash
# 安装Docker和Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# 配置Docker镜像加速
sudo mkdir -p /etc/docker
echo '{"registry-mirrors": ["https://mirror.ccs.tencentyun.com"]}' | sudo tee /etc/docker/daemon.json
```

### 2. 数据持久化
```yaml
# docker-compose.override.yml
volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
  rabbitmq_data:
    driver: local
```

### 3. 备份策略
```bash
#!/bin/bash
# backup.sh
DATE=$(date +%Y%m%d_%H%M%S)

# 数据库备份
docker-compose exec postgres pg_dump -U postgres evcs > backup_${DATE}.sql

# 配置文件备份
tar -czf config_backup_${DATE}.tar.gz .env docker-compose*.yml
```

### 4. 滚动更新
```bash
# 零停机更新
docker-compose up -d --no-deps evcs-auth
docker-compose up -d --no-deps evcs-gateway
# ... 逐个更新其他服务
```

## 📞 支持和维护

### 获取帮助
- 📖 [项目文档](README.md)
- 🐛 [问题反馈](https://github.com/your-org/evcs-mgr/issues)
- 💬 [技术讨论](https://github.com/your-org/evcs-mgr/discussions)

### 版本更新
```bash
# 拉取最新代码
git pull origin main

# 重新构建镜像
docker-compose build

# 滚动更新服务
docker-compose up -d
```

---

**注意**: 本文档会持续更新，请定期查看最新版本。如有问题，请参考故障排除章节或提交Issue。
