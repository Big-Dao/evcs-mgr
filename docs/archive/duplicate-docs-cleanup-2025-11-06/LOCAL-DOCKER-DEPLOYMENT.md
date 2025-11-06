# EVCS Manager 本地 Docker 部署指南

## 📋 当前部署状态

### ✅ 已成功部署的服务

| 服务 | 状态 | 端口 | 访问地址 |
|------|------|------|---------|
| PostgreSQL | 🟢 健康运行 | 5432 | localhost:5432 |
| Redis | 🟢 健康运行 | 6379 | localhost:6379 |
| RabbitMQ | 🟢 健康运行 | 5672, 15672 | http://localhost:15672 |
| Eureka | 🟢 健康运行 | 8761 | http://localhost:8761 |

### 🔧 待部署的应用服务

- Auth Service (认证服务)
- Gateway (API网关)
- Tenant Service (租户服务)
- Station Service (充电站服务)
- Order Service (订单服务)
- Payment Service (支付服务)
- Protocol Service (协议服务)
- Monitoring Service (监控服务)

---

## 🚀 快速部署方案

### 方案 1：分步部署（推荐-稳定）

逐个构建和启动服务，便于排查问题：

```powershell
# 1. 基础设施（已完成）✅
docker-compose up -d postgres redis rabbitmq

# 2. 注册中心（已完成）✅
docker-compose build eureka
docker-compose up -d eureka

# 3. 配置中心
docker-compose build config-server
docker-compose up -d config-server

# 4. API 网关
docker-compose build gateway
docker-compose up -d gateway

# 5. 认证服务
docker-compose build auth-service
docker-compose up -d auth-service

# 6. 业务服务
docker-compose build tenant-service station-service order-service payment-service protocol-service
docker-compose up -d tenant-service station-service order-service payment-service protocol-service

# 7. 监控服务
docker-compose build monitoring-service
docker-compose up -d monitoring-service
```

### 方案 2：一键部署（快速但可能需要较长时间）

```powershell
# 构建并启动所有服务（需要 10-15 分钟）
docker-compose up -d --build
```

### 方案 3：使用已构建的 JAR（最快）

由于 JAR 文件已经构建好，可以使用预构建模式：

```powershell
# 检查所有 JAR 文件
ls evcs-*/build/libs/*.jar

# 如果所有 JAR 都存在，直接启动
docker-compose up -d
```

---

## 📊 部署进度监控

### 1. 查看所有容器状态

```powershell
docker-compose ps
```

### 2. 查看特定服务日志

```powershell
# 实时查看日志
docker-compose logs -f [服务名]

# 例如：查看 Gateway 日志
docker-compose logs -f gateway

# 查看最近 50 行日志
docker logs evcs-gateway --tail 50
```

### 3. 健康检查

```powershell
# 检查所有服务健康状态
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 使用健康检查脚本
.\scripts\health-check.sh
```

---

## 🔍 服务启动顺序

**重要：** 按以下顺序启动以确保依赖关系正确：

1. **基础设施层** (0-30秒)
   - PostgreSQL
   - Redis
   - RabbitMQ

2. **服务注册层** (30-60秒)
   - Eureka (注册中心)
   - Config Server (配置中心)

3. **网关层** (60-90秒)
   - Gateway (API网关)

4. **核心服务层** (90-150秒)
   - Auth Service (认证)
   - Tenant Service (租户)

5. **业务服务层** (150-240秒)
   - Station Service (充电站)
   - Order Service (订单)
   - Payment Service (支付)
   - Protocol Service (协议)

6. **监控层** (240-300秒)
   - Monitoring Service

---

## 📁 配置文件位置

| 文件 | 说明 |
|------|------|
| `docker-compose.yml` | 主配置文件（完整部署） |
| `docker-compose.local.yml` | 本地开发配置（仅基础设施） |
| `docker-compose.dev.yml` | 开发环境配置 |
| `docker-compose.test.yml` | 测试环境配置 |

---

## 🛠️ 常见问题排查

### 问题 1：服务启动失败

```powershell
# 查看详细错误日志
docker-compose logs [服务名] | Select-String "ERROR|Exception"

# 重启特定服务
docker-compose restart [服务名]
```

### 问题 2：端口冲突

```powershell
# 检查端口占用
netstat -ano | findstr "8761"
netstat -ano | findstr "5432"

# 修改 docker-compose.yml 中的端口映射
```

### 问题 3：数据库连接失败

```powershell
# 检查 PostgreSQL 健康状态
docker exec evcs-postgres pg_isready -U postgres

# 手动连接测试
docker exec -it evcs-postgres psql -U postgres -d evcs_mgr
```

### 问题 4：Eureka 连接超时

```powershell
# 确认 Eureka 已启动
curl http://localhost:8761/eureka/apps

# 检查网络连接
docker network inspect evcs-mgr_evcs-network
```

---

## 🎯 下一步操作

### 推荐操作流程

1. **继续部署剩余服务**
   ```powershell
   # 构建并启动网关
   docker-compose build gateway
   docker-compose up -d gateway
   
   # 等待 30 秒后启动业务服务
   Start-Sleep -Seconds 30
   docker-compose build tenant-service station-service
   docker-compose up -d tenant-service station-service
   ```

2. **验证服务注册**
   ```powershell
   # 访问 Eureka 控制台
   # http://localhost:8761
   
   # 应该看到所有已启动的服务
   ```

3. **测试API可用性**
   ```powershell
   # 测试网关健康检查
   curl http://localhost:8080/actuator/health
   
   # 测试租户服务
   curl http://localhost:8083/actuator/health
   ```

4. **配置前端管理界面**
   ```powershell
   # 构建前端
   cd evcs-admin
   npm install
   npm run build
   
   # 或使用 Docker 部署
   docker build -t evcs-admin:latest ./evcs-admin
   docker run -d -p 80:80 evcs-admin:latest
   ```

---

## 📈 监控访问地址

部署完成后可访问：

- **Eureka 控制台**: http://localhost:8761
- **RabbitMQ 管理**: http://localhost:15672 (guest/guest)
- **Prometheus**: http://localhost:9090 (需启动 monitoring-service)
- **Grafana**: http://localhost:3000 (admin/admin)
- **API 网关**: http://localhost:8080
- **前端管理**: http://localhost (需单独部署)

---

## 🔧 维护命令

```powershell
# 停止所有服务
docker-compose down

# 停止并删除数据卷（重置数据）
docker-compose down -v

# 查看资源使用情况
docker stats

# 清理未使用的镜像和容器
docker system prune -a

# 重新构建特定服务
docker-compose build --no-cache [服务名]
```

---

## 📚 相关文档

- [Docker 快速启动指南](../DOCKER-QUICKSTART.md)
- [开发者指南](../docs/DEVELOPER-GUIDE.md)
- [Docker 构建修复](../docs/development/DOCKER-BUILD-FIX.md)
- [健康检查脚本](./health-check.sh)

---

**部署时间**: 2025-10-28
**当前阶段**: 基础设施 + Eureka ✅
**下一步**: 部署 Gateway 和业务服务
