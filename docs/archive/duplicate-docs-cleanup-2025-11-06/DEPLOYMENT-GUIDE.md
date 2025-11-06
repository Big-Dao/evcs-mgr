# EVCS Manager 部署指南

## 概述

本文档提供 EVCS Manager 系统在生产环境和测试环境的完整部署指南，包括 Docker Compose 和 Kubernetes 两种部署方式。

**版本**: v1.2.0  
**更新日期**: 2025-10-28  
**项目阶段**: P4 Week 2 完成 - 性能优化与测试修复完成  
**构建状态**: ✅ 所有测试通过 (168/168), 覆盖率 96%

---

## 目录

- [系统要求](#系统要求)
- [Docker Compose 部署](#docker-compose-部署)
- [Kubernetes 部署](#kubernetes-部署)
- [配置参数说明](#配置参数说明)
- [数据库迁移](#数据库迁移)
- [健康检查](#健康检查)
- [故障排查](#故障排查)

---

## 系统要求

### 硬件要求（最小配置）

**开发环境**:
- CPU: 4核
- 内存: 8GB
- 磁盘: 50GB SSD

**生产环境**:
- CPU: 8核
- 内存: 16GB
- 磁盘: 200GB SSD
- 网络: 100Mbps

### 软件要求

- **Java**: OpenJDK 21+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+ (Docker Compose部署)
- **Kubernetes**: 1.24+ (K8s部署)
- **Helm**: 3.0+ (K8s部署)
- **PostgreSQL**: 15+
- **Redis**: 7+
- **RabbitMQ**: 3.11+
- **Gradle**: 8.5+ (本地构建)

### 性能配置建议（基于 P4 Week 2 优化成果）

**JVM 配置**:
- 堆内存：固定 512MB (-Xms512m -Xmx512m)
- GC策略：G1GC，最大暂停时间 100ms
- 连接池：HikariCP max-pool=30, min-idle=10

**数据库索引**:
- 已优化复合索引 (tenant_id + status)
- 查询响应时间提升 68%

---

## Docker Compose 部署

### 1. 准备工作

#### 1.1 克隆代码仓库

```bash
git clone https://github.com/Big-Dao/evcs-mgr.git
cd evcs-mgr
```

#### 1.2 配置环境变量

创建 `.env` 文件：

```bash
# 数据库配置
POSTGRES_USER=evcs_user
POSTGRES_PASSWORD=SecurePassword123!
POSTGRES_DB=evcs_db

# Redis配置
REDIS_PASSWORD=RedisPassword123!

# RabbitMQ配置
RABBITMQ_USER=evcs_mq
RABBITMQ_PASSWORD=MqPassword123!

# 应用配置
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=YourJwtSecretKeyHere123456789!
```

### 2. 构建服务

```bash
# 构建所有服务镜像
./gradlew build
./gradlew dockerBuild

# 或者使用 Docker Compose 构建
docker-compose build
```

### 3. 启动服务

#### 3.1 启动基础设施（数据库、缓存、消息队列）

```bash
docker-compose -f docker-compose.yml up -d postgres redis rabbitmq
```

等待约30秒，确保基础设施服务完全启动。

#### 3.2 初始化数据库

```bash
# 执行数据库迁移脚本
docker-compose exec postgres psql -U $POSTGRES_USER -d $POSTGRES_DB -f /sql/init.sql
```

#### 3.3 启动应用服务

```bash
# 启动所有应用服务
docker-compose -f docker-compose.yml up -d
```

### 4. 验证部署

```bash
# 检查服务状态
docker-compose ps

# 查看日志
docker-compose logs -f evcs-gateway

# 健康检查
curl http://localhost:8080/actuator/health
```

### 5. 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（谨慎使用）
docker-compose down -v
```

---

## Kubernetes 部署

### 1. 准备 Kubernetes 集群

确保已有可用的 Kubernetes 集群，并配置了 `kubectl`:

```bash
# 验证集群连接
kubectl cluster-info
kubectl get nodes
```

### 2. 创建命名空间

```bash
kubectl create namespace evcs-prod
```

### 3. 配置 Secrets

#### 3.1 创建数据库密钥

```bash
kubectl create secret generic postgres-secret \
  --from-literal=username=evcs_user \
  --from-literal=password=SecurePassword123! \
  --namespace=evcs-prod
```

#### 3.2 创建 Redis 密钥

```bash
kubectl create secret generic redis-secret \
  --from-literal=password=RedisPassword123! \
  --namespace=evcs-prod
```

#### 3.3 创建 RabbitMQ 密钥

```bash
kubectl create secret generic rabbitmq-secret \
  --from-literal=username=evcs_mq \
  --from-literal=password=MqPassword123! \
  --namespace=evcs-prod
```

#### 3.4 创建应用配置密钥

```bash
kubectl create secret generic app-secret \
  --from-literal=jwt-secret=YourJwtSecretKeyHere123456789! \
  --namespace=evcs-prod
```

### 4. 使用 Helm 部署

#### 4.1 添加 Helm 仓库

```bash
# 添加 Bitnami 仓库（用于 PostgreSQL、Redis、RabbitMQ）
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

#### 4.2 部署 PostgreSQL

```bash
helm install evcs-postgres bitnami/postgresql \
  --namespace evcs-prod \
  --set auth.username=evcs_user \
  --set auth.password=SecurePassword123! \
  --set auth.database=evcs_db \
  --set primary.persistence.size=50Gi
```

#### 4.3 部署 Redis

```bash
helm install evcs-redis bitnami/redis \
  --namespace evcs-prod \
  --set auth.password=RedisPassword123! \
  --set master.persistence.size=10Gi
```

#### 4.4 部署 RabbitMQ

```bash
helm install evcs-rabbitmq bitnami/rabbitmq \
  --namespace evcs-prod \
  --set auth.username=evcs_mq \
  --set auth.password=MqPassword123! \
  --set persistence.size=20Gi
```

#### 4.5 部署应用服务

创建 `k8s/evcs-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: evcs-gateway
  namespace: evcs-prod
spec:
  replicas: 3
  selector:
    matchLabels:
      app: evcs-gateway
  template:
    metadata:
      labels:
        app: evcs-gateway
    spec:
      containers:
      - name: evcs-gateway
        image: evcs-mgr/evcs-gateway:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://evcs-postgres-postgresql:5432/evcs_db"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
        - name: SPRING_REDIS_HOST
          value: "evcs-redis-master"
        - name: SPRING_REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: password
        - name: SPRING_RABBITMQ_HOST
          value: "evcs-rabbitmq"
        - name: SPRING_RABBITMQ_USERNAME
          valueFrom:
            secretKeyRef:
              name: rabbitmq-secret
              key: username
        - name: SPRING_RABBITMQ_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rabbitmq-secret
              key: password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secret
              key: jwt-secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: evcs-gateway
  namespace: evcs-prod
spec:
  type: LoadBalancer
  selector:
    app: evcs-gateway
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

应用配置：

```bash
kubectl apply -f k8s/evcs-deployment.yaml
```

### 5. 配置 Ingress（可选）

创建 `k8s/evcs-ingress.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: evcs-ingress
  namespace: evcs-prod
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.evcs-mgr.com
    secretName: evcs-tls-secret
  rules:
  - host: api.evcs-mgr.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: evcs-gateway
            port:
              number: 80
```

应用 Ingress 配置：

```bash
kubectl apply -f k8s/evcs-ingress.yaml
```

### 6. 验证部署

```bash
# 查看所有 Pod 状态
kubectl get pods -n evcs-prod

# 查看服务状态
kubectl get svc -n evcs-prod

# 查看日志
kubectl logs -f deployment/evcs-gateway -n evcs-prod

# 健康检查
kubectl exec -it deployment/evcs-gateway -n evcs-prod -- curl http://localhost:8080/actuator/health
```

---

## 配置参数说明

### 环境变量

| 变量名 | 说明 | 默认值 | 是否必需 |
|--------|------|--------|----------|
| `SPRING_PROFILES_ACTIVE` | 运行环境配置 | `dev` | 是 |
| `SPRING_DATASOURCE_URL` | 数据库连接URL | - | 是 |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | - | 是 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | - | 是 |
| `SPRING_REDIS_HOST` | Redis主机地址 | `localhost` | 是 |
| `SPRING_REDIS_PASSWORD` | Redis密码 | - | 否 |
| `SPRING_RABBITMQ_HOST` | RabbitMQ主机地址 | `localhost` | 是 |
| `SPRING_RABBITMQ_USERNAME` | RabbitMQ用户名 | - | 是 |
| `SPRING_RABBITMQ_PASSWORD` | RabbitMQ密码 | - | 是 |
| `JWT_SECRET` | JWT签名密钥 | - | 是 |
| `EVCS_TENANT_ENABLED` | 是否启用多租户 | `true` | 否 |
| `LOGGING_LEVEL_ROOT` | 日志级别 | `INFO` | 否 |

### JVM参数

推荐生产环境JVM参数：

```bash
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/heapdump.hprof \
  -Dspring.profiles.active=prod"
```

### 应用配置文件

主配置文件 `application-prod.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
  
  rabbitmq:
    listener:
      simple:
        concurrency: 5
        max-concurrency: 20
        prefetch: 10

server:
  port: 8080
  tomcat:
    threads:
      max: 200
      min-spare: 10
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

---

## 数据库迁移

### 初始化数据库

```bash
# Docker Compose环境
docker-compose exec postgres psql -U evcs_user -d evcs_db -f /sql/init.sql

# Kubernetes环境
kubectl exec -it evcs-postgres-postgresql-0 -n evcs-prod -- \
  psql -U evcs_user -d evcs_db -f /sql/init.sql
```

### 迁移脚本位置

数据库迁移脚本位于 `sql/` 目录：

- `sql/init.sql`: 初始化脚本（创建表结构）
- `sql/V1.0.1__add_payment_tables.sql`: 版本1.0.1迁移脚本
- `sql/V1.0.2__add_indexes.sql`: 版本1.0.2迁移脚本

### 回滚脚本

每个迁移脚本都有对应的回滚脚本：

```bash
# 回滚示例
docker-compose exec postgres psql -U evcs_user -d evcs_db \
  -f /sql/rollback/R1.0.1__rollback_payment_tables.sql
```

---

## 健康检查

### 健康检查端点

- **Liveness Probe**: `/actuator/health/liveness`
  - 检查应用是否存活
  - 失败时Kubernetes会重启Pod

- **Readiness Probe**: `/actuator/health/readiness`
  - 检查应用是否就绪
  - 失败时Kubernetes会移除Pod流量

### 健康检查响应示例

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
        "version": "6.2.7"
      }
    },
    "rabbit": {
      "status": "UP",
      "details": {
        "version": "3.11.0"
      }
    }
  }
}
```

### 监控指标

访问 `/actuator/prometheus` 获取Prometheus格式的监控指标。

---

## 故障排查

### 常见问题FAQ

#### 1. 应用无法连接数据库

**现象**: 日志显示 `Connection refused` 或 `timeout`

**排查步骤**:
```bash
# 检查数据库服务是否运行
docker-compose ps postgres
kubectl get pods -l app=postgres -n evcs-prod

# 检查网络连通性
docker-compose exec evcs-gateway ping postgres
kubectl exec -it deployment/evcs-gateway -n evcs-prod -- ping evcs-postgres-postgresql

# 检查数据库日志
docker-compose logs postgres
kubectl logs evcs-postgres-postgresql-0 -n evcs-prod
```

**解决方案**:
- 确认数据库服务已启动
- 检查数据库连接URL配置
- 验证用户名和密码正确

#### 2. Redis连接失败

**现象**: `ERR Client sent AUTH, but no password is set`

**排查步骤**:
```bash
# 测试Redis连接
docker-compose exec redis redis-cli ping
kubectl exec -it evcs-redis-master-0 -n evcs-prod -- redis-cli -a RedisPassword123! ping
```

**解决方案**:
- 如果Redis未设置密码，删除配置中的password参数
- 确认密码配置正确

#### 3. 服务启动慢或超时

**现象**: Pod一直处于 `CrashLoopBackOff` 状态

**排查步骤**:
```bash
# 查看Pod事件
kubectl describe pod <pod-name> -n evcs-prod

# 查看容器日志
kubectl logs <pod-name> -n evcs-prod --previous
```

**解决方案**:
- 增加 `initialDelaySeconds` 和 `timeoutSeconds`
- 检查资源限制是否过低
- 查看应用日志确认启动失败原因

#### 4. 内存不足 (OOM)

**现象**: Pod被Killed，日志显示 `OutOfMemoryError`

**排查步骤**:
```bash
# 查看Pod资源使用
kubectl top pods -n evcs-prod

# 查看heap dump（如果配置了）
kubectl cp evcs-prod/<pod-name>:/logs/heapdump.hprof ./heapdump.hprof
```

**解决方案**:
- 增加Pod内存限制
- 调整JVM内存参数
- 使用MAT工具分析heap dump

---

## 性能优化建议

### 1. 数据库优化（已实施 ✅）

**P4 Week 2 优化成果**:
- ✅ 复合索引优化：(tenant_id + status) 提升查询性能 68%
- ✅ HikariCP 连接池：max-pool=30, min-idle=10, 连接泄漏检测
- ✅ 定期执行 `VACUUM ANALYZE` 维护统计信息

**建议配置**:
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
```

### 2. Redis优化

**集群化部署**（Week 3-4 计划）:
- 使用Redis Cluster提高可用性（目标：99.9%+）
- 配置maxmemory-policy为allkeys-lru
- 启用AOF持久化

### 3. 应用优化（已实施 ✅）

**JVM 参数优化**:
```bash
JAVA_OPTS: >
  -Xms512m -Xmx512m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=100
  -XX:+ParallelRefProcEnabled
  -XX:+UseStringDeduplication
  -XX:StartFlightRecording=dumponexit=true,filename=/tmp/flight.jfr
```

**性能指标**（Station Service 优化后）:
- TPS: 3.79 (提升 232%)
- 响应时间: 264ms (降低 68%)
- 错误率: 0%

**其他优化**:
- ✅ 启用HTTP/2
- ✅ 启用Gzip压缩
- 配置适当的线程池大小（根据CPU核数调整）

---

## 安全建议

1. **使用强密码**: 所有服务密码至少16位，包含大小写字母、数字和特殊字符
2. **启用TLS/SSL**: 生产环境必须使用HTTPS
3. **定期更新**: 及时更新系统和依赖包，修复安全漏洞
4. **最小权限原则**: 数据库用户只授予必要的权限
5. **网络隔离**: 使用防火墙和安全组限制访问
6. **日志审计**: 记录所有关键操作和访问

---

## 备份与恢复

### 数据库备份

```bash
# 备份
docker-compose exec postgres pg_dump -U evcs_user evcs_db > backup.sql

# 恢复
docker-compose exec -T postgres psql -U evcs_user evcs_db < backup.sql
```

### 定期备份策略

建议配置定时任务（cron）：

```bash
# 每天凌晨2点备份
0 2 * * * /path/to/backup-script.sh
```

---

## 更新日志

### v1.2.0 (2025-10-28)
- ✅ 更新性能配置建议（基于 P4 Week 2 优化成果）
- ✅ 添加 JVM 优化参数（G1GC, MaxGCPauseMillis=100ms）
- ✅ 更新数据库连接池配置（HikariCP 优化）
- ✅ 添加性能指标数据（Station Service TPS +232%）
- ✅ 更新软件版本要求（PostgreSQL 15+, Redis 7+）
- ✅ 添加健康检查最佳实践

### v1.1.0 (2025-10-20)
- 添加测试环境快速部署指南
- 更新 Kubernetes 部署配置
- 添加监控告警配置示例

### v1.0.0 (2025-10-12)
- 初始版本发布
- Docker Compose 部署指南
- Kubernetes 部署指南
- 故障排查指南

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0.0 | 2025-10-12 | 初始版本 |

---

## 支持与联系

**技术支持**: ops@evcs-mgr.com  
**文档反馈**: docs@evcs-mgr.com  
**紧急联系**: +86-400-xxx-xxxx

