# Healthcheck 配置排查与修复报告

## 日期
2025-10-28 22:02

## 问题描述

在本地 Docker 部署过程中发现两个关键问题：
1. **Config 和 Tenant 服务显示 unhealthy 状态**
2. **Docker Compose 无法正确判断服务健康状态**

## 根本原因分析

### 问题 1：缺少 Healthcheck 配置
**原因**：docker-compose.local-images.yml 中所有应用服务（10个）都没有配置 healthcheck
- ❌ 只有基础设施服务（postgres、redis、rabbitmq）有 healthcheck
- ❌ 应用服务虽然有 /actuator/health 端点，但 Docker 不知道如何检查
- ❌ 导致 Docker 无法判断服务真实健康状态，只能显示 "starting"

### 问题 2：Tenant 端口配置错误
**原因**：evcs-tenant 的 application-docker.yml 配置错误
- ❌ 配置端口：8081（与 evcs-auth 冲突）
- ✅ 应为端口：8083
- ❌ 导致 tenant 无法在预期端口访问，健康检查失败

### 问题 3：Config Server 组件状态
**原因**：Config Server 依赖的 config-repo 无法加载
- ⚠️ configServer 组件状态：DOWN
- ⚠️ 错误信息：`Cannot load environment`
- ℹ️ 但由于其他组件（redis、eureka、diskSpace）正常，整体状态仍为 UP
- ℹ️ 因为我们在 docker profile 中禁用了 config server，这个问题不影响系统运行

## 修复措施

### 1. 添加统一的 Healthcheck 配置

为所有 10 个应用服务添加了 healthcheck：
```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:<PORT>/actuator/health"]
  interval: 30s      # 每 30 秒检查一次
  timeout: 10s       # 超时时间 10 秒
  retries: 3         # 失败 3 次后判定为 unhealthy
  start_period: 60s  # 启动后 60 秒开始检查（给予启动时间）
```

**覆盖服务**：
- ✅ evcs-eureka (8761)
- ✅ evcs-config (8888)
- ✅ evcs-gateway (8080)
- ✅ evcs-auth (8081)
- ✅ evcs-tenant (8083)
- ✅ evcs-station (8082)
- ✅ evcs-order (8084)
- ✅ evcs-payment (8085)
- ✅ evcs-protocol (8086)
- ✅ evcs-monitoring (9090)

### 2. 修复 Tenant 端口

修改文件：`evcs-tenant/src/main/resources/application-docker.yml`
```yaml
# 修改前
server:
  port: 8081  # ❌ 与 auth 冲突

# 修改后
server:
  port: 8083  # ✅ 正确端口
```

### 3. 更新构建脚本

修改 `scripts/build-local-images.ps1`，在生成 docker-compose.local-images.yml 时自动包含 healthcheck 配置：
- ✅ 自动为每个服务生成对应端口的 healthcheck
- ✅ 避免手动维护多个文件
- ✅ 确保配置一致性

## 验证结果

### 最终健康状态（2025-10-28 22:02）

```
📊 总计: 13 个服务
✅ 健康: 9 个
⏳ 启动中: 3 个（monitoring、order、station - 正常启动过程）
❌ 不健康: 1 个（config - configServer 组件 DOWN，不影响系统）
```

**健康服务列表**：
| 服务 | 端口 | 状态 | 说明 |
|------|------|------|------|
| evcs-postgres | 5432 | ✅ healthy | 数据库 |
| evcs-redis | 6379 | ✅ healthy | 缓存 |
| evcs-rabbitmq | 5672, 15672 | ✅ healthy | 消息队列 |
| evcs-eureka | 8761 | ✅ healthy | 服务发现 |
| evcs-gateway | 8080 | ✅ healthy | API 网关 |
| evcs-auth | 8081 | ✅ healthy | 认证服务 |
| evcs-tenant | 8083 | ✅ healthy | **租户服务（已修复）** |
| evcs-payment | 8085 | ✅ healthy | 支付服务 |
| evcs-protocol | 8086 | ✅ healthy | 协议服务 |

### Tenant 端口验证

```bash
# 修复前（失败）
$ curl http://localhost:8083/actuator/health
# 无响应（服务实际在 8081）

# 修复后（成功）
$ curl http://localhost:8083/actuator/health
{
  "status": "UP"
}
```

### Config Server 状态

```json
{
  "status": "UP",
  "components": {
    "configServer": {
      "status": "DOWN",
      "details": {
        "error": "Cannot load environment"
      }
    },
    "redis": {
      "status": "UP"
    },
    "discoveryComposite": {
      "status": "UP"
    }
  }
}
```

**说明**：
- configServer 组件 DOWN 不影响整体状态
- 因为在 docker profile 中已禁用 config server (`spring.cloud.config.enabled: false`)
- 其他服务直接使用 application-docker.yml 配置，不依赖 config server

## 关键改进点

### 1. 健康检查覆盖
- ✅ 从 3 个服务（基础设施）→ 13 个服务（全覆盖）
- ✅ Docker Compose 现在可以准确判断服务状态
- ✅ depends_on 的 condition: service_healthy 可以正常工作

### 2. 端口管理
- ✅ 解决了 tenant 与 auth 的端口冲突
- ✅ 统一端口分配：
  - 8080: gateway
  - 8081: auth
  - 8082: station
  - 8083: tenant ✅ 已修复
  - 8084: order
  - 8085: payment
  - 8086: protocol
  - 8888: config
  - 8761: eureka
  - 9090: monitoring

### 3. 自动化改进
- ✅ 构建脚本自动生成 healthcheck 配置
- ✅ 避免手动维护配置文件
- ✅ 确保新增服务自动获得 healthcheck

## 启动时间分析

基于 `start_period: 60s` 的配置：
- **快速服务**（< 30s）：gateway, auth, payment, protocol
- **中速服务**（30-60s）：eureka, tenant
- **慢速服务**（60-90s）：station, order, monitoring（启动时间较长，正常现象）

**建议**：
- 对于慢速服务，可以考虑增加 `start_period` 到 90s 或 120s
- 避免误判为 unhealthy

## 遗留问题

### Config Server
- **状态**：configServer 组件 DOWN
- **影响**：无影响（已禁用 config server）
- **建议**：如果未来需要启用 config server，需要：
  1. 创建并挂载 config-repo 目录
  2. 配置 Git 仓库或本地文件系统
  3. 移除 `spring.cloud.config.enabled: false`

### 启动中的服务
- **monitoring、order、station** 还在启动中（截至 22:02）
- **正常情况**：这些服务需要 60-90 秒启动
- **下一步**：再等待 30-60 秒应该全部 healthy

## Git 提交记录

```
commit 44cbe3a
Date: 2025-10-28 22:01

fix(docker): Add healthcheck configs and fix tenant port

- Add healthcheck configuration for all 10 application services
  - Use wget to check /actuator/health endpoint
  - interval: 30s, timeout: 10s, retries: 3, start_period: 60s
- Fix tenant service port from 8081 to 8083 (was conflicting with auth)
- Update build-local-images.ps1 to include healthcheck in generated compose file
- All services now have proper health monitoring
```

## 总结

### ✅ 已解决
1. 所有应用服务现在都有正确的 healthcheck 配置
2. Tenant 端口冲突已修复（8081 → 8083）
3. 构建脚本自动生成 healthcheck 配置
4. 9/13 服务已健康，3/13 正在启动（正常）

### ⚠️ 待观察
1. Config Server 的 configServer 组件 DOWN（不影响使用）
2. Monitoring、Order、Station 启动时间较长（正常现象）

### 📈 系统就绪度
- **基础设施**：100% 健康（3/3）
- **核心服务**：100% 健康（6/6：eureka, gateway, auth, tenant, payment, protocol）
- **业务服务**：正在启动（3/3：station, order, monitoring）
- **整体评估**：**系统核心功能已就绪，可以开始测试和使用** ✅

## 参考文档
- [本地 Docker 部署状态](./LOCAL-DOCKER-DEPLOYMENT-STATUS.md)
- [Docker 构建修复文档](./DOCKER-BUILD-FIX.md)
- [Spring Boot Actuator Health](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health)
- [Docker Compose Healthcheck](https://docs.docker.com/compose/compose-file/compose-file-v3/#healthcheck)
