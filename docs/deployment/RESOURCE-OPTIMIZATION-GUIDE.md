# EVCS Manager 资源优化指南

> **版本**: v1.1 | **最后更新**: 2025-11-10 | **维护者**: DevOps 团队 | **状态**: 活跃
>
> 💡 **用途**: 针对 Docker / Kubernetes / JVM / 数据库 / 缓存的资源调优方案

本文档提供全面的资源优化策略，帮助在不同部署环境中最大化资源利用率。

## 目录

- [优化概述](#优化概述)
- [Docker 资源优化](#docker-资源优化)
- [Kubernetes 资源优化](#kubernetes-资源优化)
- [JVM 参数调优](#jvm-参数调优)
- [数据库优化](#数据库优化)
- [缓存优化](#缓存优化)
- [监控和诊断](#监控和诊断)
- [最佳实践](#最佳实践)

## 优化概述

### 优化目标

1. **减少内存占用**: 降低总体内存使用量 30-50%
2. **优化CPU使用**: 提高CPU利用率和响应性能
3. **快速启动**: 减少容器启动时间 40-60%
4. **资源弹性**: 支持动态扩缩容
5. **成本效益**: 降低基础设施成本

### 部署环境配置矩阵

| 环境 | 配置文件 | 内存需求 | CPU需求 | 适用场景 |
|------|----------|----------|---------|----------|
| 开发测试 | `docker-compose.minimal.yml` | 2-4GB | 2-4核 | 本地开发、功能测试 |
| 小规模生产 | `docker-compose.optimized.yml` | 6-8GB | 4-6核 | 小型充电站、试点项目 |
| 大规模生产 | `docker-compose.yml` | 12-16GB | 8-12核 | 大型充电站网络 |
| Kubernetes | `k8s/optimized-resource-quota.yaml` | 可扩展 | 可扩展 | 云原生部署 |

## Docker 资源优化

### 1. 优化配置文件选择

#### 最小配置 (docker-compose.minimal.yml)
```yaml
# 仅包含核心服务，适用于资源受限环境
services:
  - postgres (1GB RAM)
  - redis (128MB RAM)
  - eureka (128MB RAM)
  - config-server (128MB RAM)
  - gateway (256MB RAM)
  - auth-service (256MB RAM)
  - station-service (256MB RAM)
```

**总资源需求**: ~2.5GB 内存, 2-4 CPU核心

#### 优化配置 (docker-compose.optimized.yml)
```yaml
# 包含完整服务栈，但优化了资源配置
x-java-env: &java-env
  JAVA_OPTS: >
    -Xms128m -Xmx384m
    -XX:+UseContainerSupport
    -XX:MaxRAMPercentage=65.0
    -XX:+UseG1GC
    -XX:+UseStringDeduplication
```

**总资源需求**: ~6-8GB 内存, 4-6 CPU核心

### 2. JVM 容器优化

#### 内存优化参数
```bash
# 容器感知的JVM配置
-Xms128m                    # 初始堆大小
-Xmx384m                    # 最大堆大小
-XX:+UseContainerSupport    # 启用容器感知
-XX:MaxRAMPercentage=65.0   # 使用65%容器内存
-XX:InitialRAMPercentage=35.0 # 初始35%容器内存
-XX:+UseG1GC               # 使用G1垃圾收集器
-XX:+UseStringDeduplication # 字符串去重
-XX:MinHeapFreeRatio=20    # 最小堆空闲比例
-XX:MaxHeapFreeRatio=40    # 最大堆空闲比例
```

#### CPU优化参数
```bash
# CPU优化
-server                                 # 服务器模式
-XX:+OptimizeStringConcat              # 字符串拼接优化
-Djava.security.egd=file:/dev/./urandom # 加快随机数生成
-Dspring.jmx.enabled=false             # 禁用JMX减少开销
```

### 3. 服务特定优化

#### PostgreSQL 优化
```yaml
postgres:
  command: >
    postgres
    -c shared_preload_libraries=pg_stat_statements
    -c max_connections=100              # 减少连接数
    -c shared_buffers=256MB             # 缓冲区大小
    -c effective_cache_size=768MB       # 有效缓存大小
    -c maintenance_work_mem=64MB        # 维护工作内存
    -c checkpoint_completion_target=0.9 # 检查点完成目标
```

#### Redis 优化
```yaml
redis:
  command: >
    redis-server
    --appendonly yes                     # AOF持久化
    --maxmemory 256mb                   # 限制内存使用
    --maxmemory-policy allkeys-lru      # LRU淘汰策略
    --tcp-keepalive 60                  # TCP保活
```

## Kubernetes 资源优化

### 1. 资源配额配置

```yaml
# 命名空间级别的资源限制
apiVersion: v1
kind: ResourceQuota
metadata:
  name: evcs-mgr-quota
spec:
  hard:
    requests.cpu: "8"          # 8核CPU请求
    requests.memory: 16Gi      # 16GB内存请求
    limits.cpu: "16"           # 16核CPU限制
    limits.memory: 32Gi        # 32GB内存限制
    pods: "25"                 # 最大Pod数量
```

### 2. 容器资源限制

```yaml
# 单个容器的资源配置
resources:
  requests:                    # 资源请求（保证分配）
    cpu: "0.3"                 # 0.3核CPU
    memory: "256Mi"           # 256MB内存
  limits:                      # 资源限制（最大使用）
    cpu: "0.8"                 # 0.8核CPU
    memory: "512Mi"           # 512MB内存
```

### 3. HPA (水平Pod自动扩缩容)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodPodAutoscaler
metadata:
  name: gateway-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: gateway
  minReplicas: 2
  maxReplicas: 8
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## JVM 参数调优

### 1. 内存调优

#### 堆内存分配
```bash
# 根据容器大小调整
-Xms128m -Xmx384m     # 512MB容器
-Xms192m -Xmx512m     # 768MB容器
-Xms256m -Xmx768m     # 1GB容器
```

#### 非堆内存优化
```bash
# 元空间和直接内存优化
-XX:MetaspaceSize=64m
-XX:MaxMetaspaceSize=256m
-XX:MaxDirectMemorySize=128m
```

### 2. 垃圾收集优化

#### G1GC 配置
```bash
-XX:+UseG1GC                    # 启用G1GC
-XX:MaxGCPauseMillis=200        # 最大暂停时间200ms
-XX:G1HeapRegionSize=8m         # G1区域大小
-XX:+UseStringDeduplication     # 字符串去重
```

#### 低延迟配置
```bash
-XX:+UnlockExperimentalVMOptions
-XX:+UseZGC                     # ZGC低延迟收集器
-XX:+ZGenerational              # 分代ZGC
```

### 3. Spring Boot 优化

#### 应用配置
```yaml
spring:
  jmx:
    enabled: false              # 禁用JMX
  main:
    banner-mode: off           # 关闭启动横幅
  output:
    ansi:
      enabled: never           # 禁用ANSI颜色
```

#### 数据库连接池优化
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20    # 减少连接池大小
      minimum-idle: 5         # 最小空闲连接
      idle-timeout: 300000    # 空闲超时
      max-lifetime: 1200000   # 连接最大生命周期
```

## 数据库优化

### 1. PostgreSQL 参数调优

#### 内存配置
```sql
-- shared_buffers 应为系统内存的 25%
SET shared_buffers = '256MB';

-- effective_cache_size 应为系统内存的 50-75%
SET effective_cache_size = '768MB';

-- work_mem 用于排序和哈希操作
SET work_mem = '4MB';

-- maintenance_work_mem 用于维护操作
SET maintenance_work_mem = '64MB';
```

#### 连接优化
```sql
-- 根据应用并发数调整
SET max_connections = 100;

-- 检查点优化
SET checkpoint_completion_target = 0.9;
SET wal_buffers = '16MB';
```

### 2. 索引优化

```sql
-- 分析表统计信息
ANALYZE;

-- 查看慢查询
SELECT query, mean_time, calls
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- 重建索引
REINDEX DATABASE evcs_mgr;
```

## 缓存优化

### 1. Redis 内存管理

#### 内存策略
```bash
# 最大内存限制
maxmemory 256mb

# 淘汰策略
maxmemory-policy allkeys-lru

# 过期策略
save 900 1      # 15分钟内有1个key变化就保存
save 300 10     # 5分钟内有10个key变化就保存
save 60 10000   # 1分钟内有10000个key变化就保存
```

#### 连接优化
```bash
# TCP优化
tcp-keepalive 60
timeout 300

# 客户端连接限制
maxclients 100
```

### 2. Spring Cache 优化

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000      # 10分钟默认过期
      cache-null-values: false  # 不缓存null值
      key-prefix: evcs:         # 键前缀
```

## 监控和诊断

### 1. 资源监控脚本

使用提供的资源监控工具：

```bash
# 实时监控
./scripts/docker/resource-monitor.sh monitor

# 资源统计
./scripts/docker/resource-monitor.sh stats

# 分析优化建议
./scripts/docker/resource-monitor.sh analyze

# 自动优化
./scripts/docker/resource-monitor.sh optimize
```

### 2. JVM 监控

#### JMX 参数
```bash
# 启用JMX监控（仅在生产环境调试时使用）
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

#### 内存分析
```bash
# 生成堆转储
jmap -dump:live,format=b,file=heap.hprof <pid>

# GC日志分析
-XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps
-Xloggc:/app/logs/gc.log
```

### 3. 容器监控

#### Docker Stats
```bash
# 查看容器资源使用
docker stats --no-stream

# 持续监控
docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"
```

#### cAdvisor
```yaml
# 使用cAdvisor进行详细监控
version: '3.8'
services:
  cadvisor:
    image: gcr.io/cadvisor/cadvisor:latest
    ports:
      - "8080:8080"
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:ro
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
      - /dev/disk/:/dev/disk:ro
```

## 最佳实践

### 1. 部署建议

#### 开发环境
- 使用 `docker-compose.minimal.yml`
- 启用调试模式
- 较低的资源限制
- 快速启动优先

#### 测试环境
- 使用 `docker-compose.optimized.yml`
- 启用性能监控
- 模拟生产配置
- 自动化测试集成

#### 生产环境
- 使用完整的 `docker-compose.yml`
- 严格的资源限制
- 健康检查配置
- 日志收集和分析

### 2. 资源管理

#### 容器资源分配原则
1. **requests 保证**: 设置合理的资源请求，确保容器获得足够资源
2. **limits 限制**: 设置资源上限，防止单个容器占用过多资源
3. **监控告警**: 设置资源使用率告警，及时发现问题
4. **弹性伸缩**: 根据负载自动调整实例数量

#### 内存优化原则
1. **JVM调优**: 合理设置堆大小和垃圾收集参数
2. **连接池**: 数据库和Redis连接池大小适中
3. **缓存策略**: 合理设置缓存过期时间和淘汰策略
4. **内存泄漏**: 定期检查和修复内存泄漏

### 3. 性能调优流程

#### 1. 基准测试
```bash
# 建立性能基准
./scripts/performance-test.sh baseline
```

#### 2. 监控分析
```bash
# 持续监控性能指标
./scripts/docker/resource-monitor.sh monitor > metrics.log
```

#### 3. 优化实施
```bash
# 应用优化配置
./scripts/docker/resource-monitor.sh optimize
```

#### 4. 效果验证
```bash
# 验证优化效果
./scripts/performance-test.sh compare
```

### 4. 故障排查

#### 常见问题及解决方案

**内存溢出 (OOM)**
```bash
# 症状: 容器频繁重启
# 解决: 增加内存限制或优化应用
docker-compose up -d --scale service=1
```

**CPU使用率过高**
```bash
# 症状: 响应时间变长
# 解决: 增加CPU核心或优化代码
docker stats --format "table {{.Container}}\t{{.CPUPerc}}"
```

**启动缓慢**
```bash
# 症状: 容器启动时间过长
# 解决: 优化JVM参数和应用配置
docker logs <container_name>
```

## 总结

通过实施这些优化策略，可以实现：

- ✅ **内存使用减少 30-50%**
- ✅ **启动时间缩短 40-60%**
- ✅ **CPU利用率提升 20-30%**
- ✅ **资源弹性扩缩容**
- ✅ **运维成本降低**

选择合适的配置文件，定期监控资源使用，并根据实际负载调整参数，是保持系统高效运行的关键。

---

**最后更新**: 2025-11-08
**维护者**: EVCS Manager 团队
**版本**: v1.0
