# EVCS 充电站管理系统 — 统一部署指南

版本：v1.3｜最后更新：2025-12-05｜维护：DevOps 团队｜状态：活跃

本指南覆盖本地开发、测试与生产环境的统一部署流程。快速体验请参见 `docs/deployment/quick-start.md`。

## 概述
支持从小规模开发到生产环境的多场景部署：核心微服务、基础设施（PostgreSQL/Redis/RabbitMQ）、注册与配置中心。

### 系统架构概览
- 网关（`evcs-gateway`）：路由与统一鉴权
- 认证（`evcs-auth`）：JWT + RBAC，多租户上下文
- 注册中心（`evcs-eureka`）与配置中心（`evcs-config`）
- 站点/订单/支付/协议等业务微服务
- 基础设施：PostgreSQL、Redis、RabbitMQ、可选 Nginx

### 演示数据准备
- 脚本位置：`sql/demo-order-data.sql`（示例租户 `tenant_id = 1001`）
- 导入方法见下方“数据导入与验证”

## 部署方案

### 方案一：核心开发环境（推荐）
适用场景：日常开发、功能测试、小规模演示
- 资源需求：~2GB 内存、~10GB 存储
- 启动时间：约 2–3 分钟

```
# 启动核心服务
docker compose -f docker-compose.core-dev.yml up -d

# 查看状态
docker compose -f docker-compose.core-dev.yml ps

# 网关健康检查
curl http://localhost:8080/actuator/health
```

包含服务：
- evcs-gateway、evcs-auth、evcs-eureka、evcs-config
- PostgreSQL、Redis、RabbitMQ

访问地址：
- API 网关: `http://localhost:8080`
- 认证服务: `http://localhost:8081`
- 注册中心: `http://localhost:8761`
- 配置中心: `http://localhost:8888`

### 方案二：完整生产环境
适用场景：生产部署、完整功能测试
- 资源需求：~4GB 内存、~20GB 存储
- 启动时间：约 5–8 分钟

```
# 启动全部服务
docker compose up -d

# 可选：叠加监控
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# 查看状态
docker compose ps

# 健康检查
curl http://localhost:8080/actuator/health
```

包含服务：所有微服务 + 基础设施 + 监控

### 方案三：自定义环境组合
按需选择不同 Compose 文件：
```
# 基础服务 + 核心业务
docker compose -f docker-compose.yml -f docker-compose.core-services.yml up -d

# 外部集成
docker compose -f docker-compose.yml -f docker-compose.integration.yml up -d

# 完整监控
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

## 环境配置

### 必需的环境变量（`.env`）
```
# 数据库配置
POSTGRES_DB=evcs
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password

# Redis 配置
REDIS_PASSWORD=your_redis_password

# JWT 配置
JWT_SECRET=your_jwt_secret_key_at_least_32_characters

# 外部服务配置（可选）
...
```

## 数据导入与验证（可选）
```
# macOS/Linux
cat sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr

# Windows PowerShell
Get-Content sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr
```

示例调用：
```
# 获取 JWT（替换用户名/密码/tenantId）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin.east","password":"password","tenantId":1001}'

# 查询订单列表（携带租户/用户标识与令牌）
curl "http://localhost:8080/api/order/list?current=1&size=10" \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-Id: 1001" \
  -H "X-User-Id: 1010"
```

## 维护与升级

### 数据持久化（示例）
```
# docker-compose.override.yml
volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
  rabbitmq_data:
    driver: local
```

### 备份策略
```
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)

# 数据库备份
docker compose exec postgres pg_dump -U postgres evcs > backup_${DATE}.sql

# 配置文件备份
tar -czf config_backup_${DATE}.tar.gz .env docker-compose*.yml
```

### 滚动更新
```
# 零停机更新核心服务
docker compose up -d --no-deps evcs-auth
docker compose up -d --no-deps evcs-gateway
# 逐个更新其它服务
```

## 支持与参考
- 文档首页：`docs/README.md`
- 运维索引：`docs/operations/README.md`
- 故障排查：`docs/troubleshooting/README.md`

## 优化建议（合并摘要）
- Docker：镜像分层与缓存、multi-stage 构建、按需裁剪依赖、基础镜像及时拉取
- 资源：CPU/内存配额、连接池与线程池参数基线、限流与熔断、日志采样与等级控制、缓存 TTL 与命中监控

更多细节参见归档：
- `docs/archive/deployment-docs-cleanup-2025-12-05/DOCKER-OPTIMIZATION.md`
- `docs/archive/deployment-docs-cleanup-2025-12-05/RESOURCE-OPTIMIZATION-GUIDE.md`
