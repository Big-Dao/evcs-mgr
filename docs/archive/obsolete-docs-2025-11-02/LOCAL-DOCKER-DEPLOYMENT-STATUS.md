# 本地 Docker 部署状态

## 部署日期
2025-10-28 21:42

## 部署策略
采用**本地构建 + 简单 Docker 打包**策略：
1. 使用 Gradle 在本地构建所有 JAR 文件（快速、有缓存）
2. 使用简单的 Dockerfile 将 JAR 复制到镜像（秒级完成）
3. 避免了 Docker 多阶段构建的慢速问题（从 3-5 分钟/服务 → 数秒）

## 当前状态

### ✅ 已完成
1. **构建工具**
   - `scripts/build-local-images.ps1` - 本地镜像构建脚本
   - 支持增量构建（只构建指定服务）
   - 自动生成 docker-compose.local-images.yml 部署文件

2. **Docker Profile 配置**
   - ✅ evcs-station - 已有 `application-docker.yml`
   - ✅ evcs-tenant - 已有 `application-docker.yml`
   - ✅ evcs-order - 新增 `application-docker.yml`
   - ✅ evcs-payment - 新增 `application-docker.yml`
   - ✅ evcs-protocol - 新增 `application-docker.yml`
   - ✅ evcs-gateway - 新增 `application-docker.yml`（无数据库）
   - ✅ evcs-auth - 新增 `application-docker.yml`
   - ✅ evcs-config - 新增 `application-docker.yml`（无数据库）

3. **业务指标修复**
   - ✅ 修复 `BusinessMetrics` 重复注册问题
   - ✅ 添加指标存在性检查
   - ✅ 重新构建 evcs-common、evcs-station、evcs-order、evcs-monitoring

4. **Docker 镜像**
   - ✅ 所有 10 个服务的镜像已构建完成
   ```
   evcs-eureka:latest
   evcs-config:latest
   evcs-gateway:latest
   evcs-auth:latest
   evcs-tenant:latest
   evcs-station:latest
   evcs-order:latest
   evcs-payment:latest
   evcs-protocol:latest
   evcs-monitoring:latest
   ```

5. **基础设施服务** - 全部健康
   - ✅ PostgreSQL (evcs-postgres) - healthy
   - ✅ Redis (evcs-redis) - healthy
   - ✅ RabbitMQ (evcs-rabbitmq) - healthy
   - ✅ Eureka (evcs-eureka) - healthy

6. **业务服务** - 部分健康
   - ✅ Payment (evcs-payment) - healthy
   - ✅ Protocol (evcs-protocol) - healthy

### ⚠️ 进行中（最后检查时间 21:39）
   - ⏳ Auth (evcs-auth) - starting
   - ⏳ Config (evcs-config) - starting  
   - ⏳ Station (evcs-station) - starting
   - ⏳ Order (evcs-order) - starting
   - ⏳ Monitoring (evcs-monitoring) - starting

### ⚠️ 需要重启（新镜像已构建）
   - 🔄 Gateway (evcs-gateway) - 需要使用新镜像重启
   - 🔄 Auth (evcs-auth) - 需要使用新镜像重启
   - 🔄 Config (evcs-config) - 需要使用新镜像重启

### ❓ 状态异常（需要调查）
   - ⚠️ Tenant (evcs-tenant) - 显示 unhealthy，但日志显示已启动（端口 8081 而非 8083）

## 下一步操作

### 立即执行
1. 重启 gateway、auth、config 使用新镜像：
   ```powershell
   # 修改 docker-compose profile 为 docker
   # 然后执行
   docker-compose -f docker-compose.local-images.yml up -d --force-recreate gateway auth config
   ```

2. 等待所有服务启动完成（约 1-2 分钟）：
   ```powershell
   docker-compose -f docker-compose.local-images.yml ps
   ```

3. 验证服务健康状态：
   ```powershell
   docker-compose -f docker-compose.local-images.yml ps --format json | ConvertFrom-Json | Select-Object Name, Health | Sort-Object Health
   ```

### 问题排查
1. **Tenant 端口问题**
   - 配置文件指定 8083，但实际监听 8081
   - 需要检查 `evcs-tenant/src/main/resources/application-docker.yml` 的端口配置

2. **健康检查**
   - 确认所有服务的 actuator health endpoint 正常响应
   - 检查 docker-compose.local-images.yml 中的 healthcheck 配置

### 待办事项
1. ☐ 完成所有服务部署并验证健康
2. ☐ 测试服务间调用（gateway → 各微服务）
3. ☐ 验证 Eureka 服务注册
4. ☐ 推送代码到远程仓库（网络恢复后）
5. ☐ 继续 P4 Week 3 工作：配置 Prometheus 告警规则

## 使用说明

### 构建镜像
```powershell
# 构建所有服务
.\scripts\build-local-images.ps1

# 跳过 Gradle 构建（JAR 已存在）
.\scripts\build-local-images.ps1 -SkipBuild

# 只构建指定服务
.\scripts\build-local-images.ps1 -Services station,order,payment
```

### 部署服务
```powershell
# 启动所有服务
docker-compose -f docker-compose.local-images.yml up -d

# 查看状态
docker-compose -f docker-compose.local-images.yml ps

# 查看日志
docker-compose -f docker-compose.local-images.yml logs -f [服务名]

# 停止所有服务
docker-compose -f docker-compose.local-images.yml down
```

## 已知问题

1. **PowerShell exitcode 问题**
   - Gradle 构建成功但 `$LASTEXITCODE` 不为 0
   - 临时方案：通过输出检测 "BUILD SUCCESSFUL" 判断成功

2. **Compose 文件覆盖**
   - `build-local-images.ps1 -Services` 会覆盖整个 docker-compose.local-images.yml
   - 每次都需要手动修改 profile 从 `local` 改为 `docker`
   - TODO: 修改脚本默认使用 docker profile

3. **Health check 端口**
   - Tenant 服务端口配置不一致（8081 vs 8083）
   - 需要核对并统一端口配置

## Git 提交记录
```
commit [hash]
Date: 2025-10-28 21:42

feat(docker): Add docker profile configs and fix metrics registration

- Add application-docker.yml for order, payment, protocol, gateway, auth, config services
- Fix BusinessMetrics duplicate registration issue by checking existing metrics
- Create build-local-images.ps1 script for fast local Docker image building
- Update docker-compose.local-images.yml with proper profile configuration
```

## 参考文档
- [Docker 构建修复文档](../docs/deployment/DOCKER-BUILD-FIX.md)
- [Docker 构建修复总结](../docs/deployment/DOCKER-BUILD-FIX-SUMMARY.md)
- [业务指标文档](../docs/monitoring/BUSINESS-METRICS.md)
