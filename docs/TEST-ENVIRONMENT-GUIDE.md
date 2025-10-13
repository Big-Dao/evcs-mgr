# 测试环境部署指南

## 概述

本文档提供 EVCS Manager 测试环境的完整部署和使用指南。测试环境用于集成测试、人工测试和演示验证。

**版本**: v1.0.0  
**更新日期**: 2025-10-12

---

## 目录

- [快速开始](#快速开始)
- [环境要求](#环境要求)
- [部署步骤](#部署步骤)
- [健康检查](#健康检查)
- [冒烟测试](#冒烟测试)
- [服务访问](#服务访问)
- [故障排查](#故障排查)
- [清理环境](#清理环境)

---

## 快速开始

### Linux/macOS

```bash
# 1. 启动测试环境
./scripts/start-test.sh

# 2. 运行健康检查
./scripts/health-check.sh

# 3. 运行冒烟测试
./scripts/smoke-test.sh

# 4. 停止测试环境
./scripts/stop-test.sh
```

### Windows PowerShell

```powershell
# 1. 启动测试环境
docker compose -f docker-compose.test.yml up --build -d

# 2. 查看服务状态
docker compose -f docker-compose.test.yml ps

# 3. 停止测试环境
docker compose -f docker-compose.test.yml down
```

---

## 环境要求

### 硬件要求

- **CPU**: 4核或以上
- **内存**: 8GB或以上
- **磁盘**: 20GB可用空间

### 软件要求

- **Docker**: 20.10+ 
- **Docker Compose**: 2.0+
- **Java**: OpenJDK 21+ (仅构建时需要)
- **Bash**: 4.0+ (Linux/macOS)
- **curl**: 用于健康检查和API测试

### 端口要求

确保以下端口未被占用：

| 端口 | 服务 | 说明 |
|------|------|------|
| 5432 | PostgreSQL | 数据库服务 |
| 6379 | Redis | 缓存服务 |
| 5672 | RabbitMQ | 消息队列AMQP端口 |
| 15672 | RabbitMQ | 管理界面 |
| 8081 | 租户服务 | 租户管理API |
| 8082 | 充电站服务 | 充电站管理API |
| 8090 | Adminer | 数据库管理工具 |

---

## 部署步骤

### 1. 准备代码

```bash
# 克隆仓库
git clone https://github.com/Big-Dao/evcs-mgr.git
cd evcs-mgr
```

### 2. 构建应用（可选）

如果需要最新代码，先构建：

```bash
# 设置Java环境（如果使用非系统默认版本）
export JAVA_HOME=/path/to/jdk21
export PATH=$JAVA_HOME/bin:$PATH

# 构建应用
./gradlew build -x test --no-daemon
```

### 3. 启动测试环境

#### 方式一：使用脚本（推荐）

```bash
# 给脚本执行权限
chmod +x scripts/*.sh

# 启动测试环境
./scripts/start-test.sh
```

脚本会：
1. ✅ 检查Docker状态
2. ✅ 验证配置文件
3. ✅ 询问是否重新构建镜像
4. ✅ 停止旧环境
5. ✅ 启动新环境
6. ✅ 显示访问信息

#### 方式二：手动启动

```bash
# 停止旧环境
docker compose -f docker-compose.test.yml down

# 启动新环境（不重新构建）
docker compose -f docker-compose.test.yml up -d

# 或者重新构建并启动
docker compose -f docker-compose.test.yml up --build -d
```

### 4. 等待服务就绪

```bash
# 查看服务启动状态
docker compose -f docker-compose.test.yml ps

# 查看服务日志
docker compose -f docker-compose.test.yml logs -f
```

等待所有服务状态变为 `healthy` 或 `running`，通常需要1-2分钟。

---

## 健康检查

### 自动健康检查

运行健康检查脚本：

```bash
./scripts/health-check.sh
```

脚本会检查：
- ✅ 容器运行状态
- ✅ 数据库连接
- ✅ Redis连接
- ✅ RabbitMQ连接
- ✅ 应用服务健康状态

### 手动健康检查

#### 基础设施服务

```bash
# PostgreSQL
docker exec evcs-postgres-test pg_isready -U evcs_test -d evcs_mgr_test

# Redis
docker exec evcs-redis-test redis-cli -a test_redis_123 PING

# RabbitMQ
curl -u evcs_test:test_mq_123 http://localhost:15672/api/overview
```

#### 应用服务

```bash
# 租户服务
curl http://localhost:8081/actuator/health

# 充电站服务
curl http://localhost:8082/actuator/health
```

期望输出：
```json
{
  "status": "UP"
}
```

---

## 冒烟测试

### 运行完整冒烟测试

```bash
./scripts/smoke-test.sh
```

测试内容包括：
1. **基础设施测试** (3项)
   - PostgreSQL连接
   - Redis连接
   - RabbitMQ连接

2. **应用服务测试** (2项)
   - 租户服务健康检查
   - 充电站服务健康检查

3. **Actuator端点测试** (2项)
   - 租户服务Info端点
   - 充电站服务Info端点

4. **数据库结构测试** (4项)
   - sys_tenant表
   - sys_user表
   - charging_station表
   - charger表

5. **服务依赖测试** (2项)
   - 租户服务数据库连接
   - 充电站服务数据库连接

### 手动冒烟测试

#### 测试租户服务

```bash
# 健康检查
curl http://localhost:8081/actuator/health

# 服务信息
curl http://localhost:8081/actuator/info
```

#### 测试充电站服务

```bash
# 健康检查
curl http://localhost:8082/actuator/health

# 服务信息
curl http://localhost:8082/actuator/info
```

---

## 服务访问

### 基础设施服务

#### PostgreSQL数据库

```bash
# 命令行连接
docker exec -it evcs-postgres-test psql -U evcs_test -d evcs_mgr_test

# 或使用Adminer web界面
# 浏览器访问: http://localhost:8090
# 服务器: postgres
# 用户名: evcs_test
# 密码: test_password_123
# 数据库: evcs_mgr_test
```

#### Redis缓存

```bash
# 连接Redis
docker exec -it evcs-redis-test redis-cli -a test_redis_123

# 查看所有键
KEYS *

# 查看信息
INFO
```

#### RabbitMQ消息队列

```bash
# Web管理界面
# 浏览器访问: http://localhost:15672
# 用户名: evcs_test
# 密码: test_mq_123
```

### 应用服务

#### 租户服务API

```bash
# 基础URL
http://localhost:8081

# 健康检查
curl http://localhost:8081/actuator/health

# API文档（如果启用了Swagger）
http://localhost:8081/swagger-ui.html
```

#### 充电站服务API

```bash
# 基础URL
http://localhost:8082

# 健康检查
curl http://localhost:8082/actuator/health

# API文档（如果启用了Swagger）
http://localhost:8082/swagger-ui.html
```

---

## 故障排查

### 常见问题

#### 1. 服务启动失败

**症状**: 容器频繁重启或退出

**排查步骤**:

```bash
# 查看容器状态
docker compose -f docker-compose.test.yml ps

# 查看失败服务的日志
docker compose -f docker-compose.test.yml logs tenant-service
docker compose -f docker-compose.test.yml logs station-service

# 查看最近的日志
docker compose -f docker-compose.test.yml logs --tail=100 tenant-service
```

**常见原因**:
- 数据库连接失败（等待数据库就绪）
- 端口被占用（更改端口或停止冲突服务）
- 内存不足（增加Docker内存限制）

#### 2. 数据库连接失败

**症状**: 应用日志显示数据库连接错误

**排查步骤**:

```bash
# 检查PostgreSQL是否运行
docker compose -f docker-compose.test.yml ps postgres

# 检查PostgreSQL日志
docker compose -f docker-compose.test.yml logs postgres

# 手动测试连接
docker exec evcs-postgres-test pg_isready -U evcs_test
```

**解决方案**:
- 等待数据库完全启动（通常需要10-30秒）
- 检查数据库配置是否正确
- 验证网络连接

#### 3. 端口冲突

**症状**: 启动时提示端口已被占用

**排查步骤**:

```bash
# Linux/macOS
sudo lsof -i :8081
sudo lsof -i :5432

# Windows
netstat -ano | findstr :8081
```

**解决方案**:
- 停止占用端口的服务
- 或修改 `docker-compose.test.yml` 中的端口映射

#### 4. 健康检查失败

**症状**: 容器运行但健康检查失败

**排查步骤**:

```bash
# 检查服务日志
docker logs evcs-tenant-test
docker logs evcs-station-test

# 手动访问健康检查端点
curl -v http://localhost:8081/actuator/health
```

**解决方案**:
- 增加健康检查的等待时间
- 检查应用配置是否正确
- 验证依赖服务是否就绪

### 查看详细日志

```bash
# 所有服务日志
docker compose -f docker-compose.test.yml logs

# 特定服务日志
docker compose -f docker-compose.test.yml logs tenant-service

# 实时跟踪日志
docker compose -f docker-compose.test.yml logs -f tenant-service

# 最近N行日志
docker compose -f docker-compose.test.yml logs --tail=100 station-service
```

### 进入容器调试

```bash
# 进入租户服务容器
docker exec -it evcs-tenant-test /bin/sh

# 进入数据库容器
docker exec -it evcs-postgres-test /bin/bash

# 进入Redis容器
docker exec -it evcs-redis-test /bin/sh
```

---

## 清理环境

### 停止服务（保留数据）

```bash
# 使用脚本
./scripts/stop-test.sh

# 或手动停止
docker compose -f docker-compose.test.yml down
```

### 完全清理（删除数据）

```bash
# 停止并删除所有数据
docker compose -f docker-compose.test.yml down -v

# 清理Docker资源
docker system prune -f
docker volume prune -f
```

### 重置环境

```bash
# 1. 完全清理
docker compose -f docker-compose.test.yml down -v

# 2. 重新启动
./scripts/start-test.sh
```

---

## CI/CD集成

测试环境可以集成到CI/CD流程中：

### GitHub Actions

项目已包含 `.github/workflows/test-environment.yml` 工作流：

```yaml
# 触发条件
- push到main/develop分支
- pull request到main/develop分支
- 手动触发

# 执行步骤
1. 构建应用
2. 启动测试环境
3. 运行健康检查
4. 运行冒烟测试
5. 收集日志（失败时）
6. 清理环境
```

### 本地运行CI流程

```bash
# 模拟CI环境
export CI=true

# 运行完整流程
./scripts/start-test.sh && \
./scripts/health-check.sh && \
./scripts/smoke-test.sh && \
./scripts/stop-test.sh
```

---

## 性能建议

### Docker资源配置

推荐的Docker Desktop资源配置：

```
CPU: 4核或以上
内存: 8GB或以上
交换内存: 2GB
磁盘: 20GB可用空间
```

### 服务资源限制

可以在 `docker-compose.test.yml` 中调整资源限制：

```yaml
services:
  tenant-service:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

---

## 安全说明

**⚠️ 重要**: 测试环境使用的是测试凭证，不要在生产环境使用！

测试环境凭证：
- PostgreSQL: `evcs_test / test_password_123`
- Redis: `test_redis_123`
- RabbitMQ: `evcs_test / test_mq_123`

生产环境请使用强密码并通过环境变量或密钥管理系统配置。

---

## 相关文档

- [部署指南](./DEPLOYMENT-GUIDE.md) - 生产环境部署
- [开发者指南](./DEVELOPER-GUIDE.md) - 开发环境搭建
- [测试指南](./TESTING-GUIDE.md) - 测试框架使用
- [API文档](./API-DOCUMENTATION.md) - API接口文档

---

## 支持与反馈

如有问题或建议，请：
1. 查看[故障排查](#故障排查)章节
2. 搜索项目Issues
3. 创建新Issue并提供详细信息

---

**文档更新**: 2025-10-12  
**适用版本**: v1.0.0+
