# EVCS Manager Docker 部署指南

## 目录结构

```
evcs-mgr/
├── docker-compose.yml          # 完整部署配置（生产环境）
├── docker-compose.dev.yml      # 开发环境配置（包含应用服务）
├── docker-compose.local.yml    # 本地开发配置（仅基础设施）
├── evcs-tenant/Dockerfile      # 租户服务Docker镜像
├── evcs-station/Dockerfile     # 充电站服务Docker镜像
└── scripts/
    ├── start-local.ps1         # 启动本地基础设施
    ├── start-dev.ps1           # 启动完整开发环境
    └── stop-all.ps1            # 停止所有服务
```

## 快速开始

### 方式一：本地开发（推荐）

仅启动基础设施服务（PostgreSQL、Redis、RabbitMQ），应用服务在IDE中运行：

```powershell
# 启动基础设施
docker-compose -f docker-compose.local.yml up -d

# 查看服务状态
docker-compose -f docker-compose.local.yml ps

# 停止服务
docker-compose -f docker-compose.local.yml down
```

然后在IDE中运行应用：
- evcs-tenant: `.\gradlew :evcs-tenant:bootRun`
- evcs-station: `.\gradlew :evcs-station:bootRun`

### 方式二：完整Docker环境

构建并启动所有服务（包括应用服务）：

```powershell
# 构建镜像并启动
docker-compose -f docker-compose.dev.yml up --build -d

# 查看日志
docker-compose -f docker-compose.dev.yml logs -f tenant-service
docker-compose -f docker-compose.dev.yml logs -f station-service

# 停止服务
docker-compose -f docker-compose.dev.yml down
```

## 服务访问地址

### 基础设施服务

| 服务 | 地址 | 用户名/密码 |
|------|------|-------------|
| PostgreSQL | localhost:5432 | postgres/postgres |
| Redis | localhost:6379 | (无密码) |
| RabbitMQ | localhost:5672 | guest/guest |
| RabbitMQ管理界面 | http://localhost:15672 | guest/guest |
| Adminer (数据库管理) | http://localhost:8090 | - |

### 应用服务

| 服务 | 端口 | 健康检查 |
|------|------|----------|
| 租户服务 | 8081 | http://localhost:8081/actuator/health |
| 充电站服务 | 8082 | http://localhost:8082/actuator/health |

## 数据库初始化

数据库SQL脚本会在容器首次启动时自动执行：

1. `sql/init.sql` - 基础表结构
2. `sql/charging_station_tables.sql` - 充电站相关表

如需重新初始化数据库：

```powershell
# 删除数据卷
docker-compose -f docker-compose.local.yml down -v

# 重新启动
docker-compose -f docker-compose.local.yml up -d
```

## 常用命令

### 查看日志

```powershell
# 查看所有服务日志
docker-compose -f docker-compose.local.yml logs

# 查看特定服务日志
docker-compose -f docker-compose.local.yml logs postgres
docker-compose -f docker-compose.local.yml logs redis
docker-compose -f docker-compose.local.yml logs rabbitmq

# 实时跟踪日志
docker-compose -f docker-compose.local.yml logs -f
```

### 重启服务

```powershell
# 重启所有服务
docker-compose -f docker-compose.local.yml restart

# 重启特定服务
docker-compose -f docker-compose.local.yml restart postgres
```

### 清理环境

```powershell
# 停止并删除容器
docker-compose -f docker-compose.local.yml down

# 停止并删除容器和数据卷
docker-compose -f docker-compose.local.yml down -v

# 清理所有未使用的资源
docker system prune -a
```

## 故障排查

### 1. 端口被占用

如果端口已被占用，可以修改docker-compose文件中的端口映射：

```yaml
ports:
  - "15432:5432"  # 将本地端口改为15432
```

### 2. 容器无法启动

查看容器日志：

```powershell
docker logs evcs-postgres
docker logs evcs-redis
docker logs evcs-rabbitmq
```

### 3. 数据库连接失败

确保数据库容器健康：

```powershell
docker-compose -f docker-compose.local.yml ps
```

等待健康检查通过（HEALTH状态）。

### 4. 应用服务连接失败

检查网络连接：

```powershell
docker network ls
docker network inspect evcs-mgr_evcs-network
```

## 开发建议

1. **本地开发**：使用 `docker-compose.local.yml` 启动基础设施，在IDE中运行应用
2. **集成测试**：使用 `docker-compose.dev.yml` 启动完整环境
3. **生产部署**：使用 `docker-compose.yml` 并配置适当的环境变量

## 环境变量

应用服务支持以下环境变量：

```bash
# 数据库配置
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/evcs_mgr
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Redis配置
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=

# RabbitMQ配置
SPRING_RABBITMQ_HOST=rabbitmq
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest

# JVM配置
JAVA_OPTS=-Xms256m -Xmx512m

# Spring Profile
SPRING_PROFILES_ACTIVE=docker
```

## 性能优化

### 数据库

```yaml
environment:
  POSTGRES_SHARED_BUFFERS: 256MB
  POSTGRES_EFFECTIVE_CACHE_SIZE: 1GB
  POSTGRES_MAX_CONNECTIONS: 100
```

### 应用服务

```yaml
environment:
  JAVA_OPTS: -Xms512m -Xmx1024m -XX:+UseG1GC
```

## 监控

### 应用健康检查

```powershell
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### 容器资源使用

```powershell
docker stats
```

## 更新应用

```powershell
# 重新构建镜像
docker-compose -f docker-compose.dev.yml build tenant-service

# 重启服务
docker-compose -f docker-compose.dev.yml up -d tenant-service
```
