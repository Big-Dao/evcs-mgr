# EVCS 系统资源规划指南

> **版本**: v1.0  
> **创建日期**: 2026-01-13  
> **维护者**: 运维/架构团队  
> **状态**: 已发布

---

## 1. 概述

本文档定义 EVCS 充电站管理系统在不同业务规模下的资源配置基线，涵盖计算、存储、网络等方面的规划。

### 1.1 适用范围

- Docker Compose 开发/测试环境
- Kubernetes 生产环境
- 容量规划与扩展指导

### 1.2 业务规模定义

| 规模等级 | 租户数 | 充电站 | 充电桩 | 日订单量 | 并发用户 |
|----------|--------|--------|--------|----------|----------|
| **S 小型** | ≤ 10 | ≤ 100 | ≤ 1,000 | ≤ 2,000 | ≤ 100 |
| **M 中型** | ≤ 50 | ≤ 500 | ≤ 5,000 | ≤ 10,000 | ≤ 300 |
| **L 大型** | ≤ 100 | ≤ 1,000 | ≤ 15,000 | ≤ 30,000 | ≤ 1,000 |
| **XL 超大** | > 100 | > 1,000 | > 15,000 | > 30,000 | > 1,000 |

---

## 2. 基础设施资源规划

### 2.1 PostgreSQL 数据库

#### 资源配置

| 规模 | CPU | 内存 | 存储 | shared_buffers | work_mem | max_connections |
|------|-----|------|------|----------------|----------|-----------------|
| **S** | 1 核 | 2 GB | 50 GB SSD | 512 MB | 16 MB | 100 |
| **M** | 2 核 | 4 GB | 200 GB SSD | 1 GB | 32 MB | 200 |
| **L** | 4 核 | 8 GB | 500 GB SSD | 2 GB | 64 MB | 300 |
| **XL** | 8 核 | 16 GB | 1 TB SSD | 4 GB | 128 MB | 500 |

#### 高可用配置

| 规模 | 架构 | 说明 |
|------|------|------|
| **S** | 单节点 | 定期备份 |
| **M** | 主 + 只读副本 | 报表查询走副本 |
| **L** | 主 + 2 只读副本 | 读写分离 |
| **XL** | 主 + 3 只读副本 + PgBouncer | 连接池中间件 |

#### PostgreSQL 配置模板

```ini
# 中型规模 (M) 配置
max_connections = 200
shared_buffers = 1GB
effective_cache_size = 3GB
maintenance_work_mem = 256MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 32MB
min_wal_size = 1GB
max_wal_size = 4GB
max_worker_processes = 4
max_parallel_workers_per_gather = 2
max_parallel_workers = 4
max_parallel_maintenance_workers = 2

# 慢查询日志
log_min_duration_statement = 1000
statement_timeout = 30000
```

---

### 2.2 Redis 缓存

#### 资源配置

| 规模 | CPU | 内存 | maxmemory | maxmemory-policy |
|------|-----|------|-----------|------------------|
| **S** | 0.5 核 | 512 MB | 400 MB | allkeys-lru |
| **M** | 1 核 | 1 GB | 800 MB | allkeys-lru |
| **L** | 2 核 | 2 GB | 1.6 GB | allkeys-lru |
| **XL** | 4 核 | 4 GB | 3.2 GB | volatile-lru |

#### 内存分配预估

| 用途 | S | M | L | XL |
|------|---|---|---|-----|
| 用户会话 | 50 MB | 100 MB | 200 MB | 400 MB |
| 站点/桩缓存 | 100 MB | 200 MB | 400 MB | 800 MB |
| 计费方案 | 20 MB | 50 MB | 100 MB | 200 MB |
| 分布式锁 | 10 MB | 20 MB | 50 MB | 100 MB |
| 热点数据 | 100 MB | 200 MB | 400 MB | 800 MB |
| **总计** | **280 MB** | **570 MB** | **1.15 GB** | **2.3 GB** |

#### 高可用配置

| 规模 | 架构 |
|------|------|
| **S** | 单节点 |
| **M** | 主从复制 |
| **L** | Sentinel 哨兵模式 |
| **XL** | Redis Cluster 集群 |

---

### 2.3 RabbitMQ 消息队列

#### 资源配置

| 规模 | CPU | 内存 | 磁盘 | 队列数预估 |
|------|-----|------|------|------------|
| **S** | 0.5 核 | 512 MB | 10 GB | ~20 |
| **M** | 1 核 | 1 GB | 50 GB | ~50 |
| **L** | 2 核 | 2 GB | 100 GB | ~100 |
| **XL** | 4 核 | 4 GB | 200 GB | ~200 |

#### 高可用配置

| 规模 | 架构 |
|------|------|
| **S** | 单节点 |
| **M** | 镜像队列 (2 节点) |
| **L** | 集群 (3 节点) |
| **XL** | 集群 (3+ 节点) + 联邦 |

---

## 3. 微服务资源规划

### 3.1 基础服务

| 服务 | S 规模 | M 规模 | L 规模 | XL 规模 |
|------|--------|--------|--------|---------|
| **Eureka** | 256 MB | 512 MB | 512 MB × 2 | 512 MB × 3 |
| **Config Server** | 256 MB | 512 MB | 512 MB | 512 MB × 2 |
| **Gateway** | 512 MB | 768 MB | 1 GB × 2 | 1 GB × 3 |

### 3.2 业务服务

| 服务 | S 规模 | M 规模 | L 规模 | XL 规模 | 说明 |
|------|--------|--------|--------|---------|------|
| **Auth** | 384 MB | 512 MB | 768 MB | 768 MB × 2 | 认证服务 |
| **Tenant** | 256 MB | 384 MB | 512 MB | 512 MB × 2 | 租户管理 |
| **Station** | 384 MB | 512 MB | 768 MB × 2 | 768 MB × 3 | 设备管理 |
| **Order** | 512 MB | 768 MB | 1 GB × 2 | 1 GB × 3 | 订单热点 |
| **Payment** | 384 MB | 512 MB | 768 MB | 768 MB × 2 | 支付服务 |
| **Protocol** | 512 MB | 1 GB | 1.5 GB × 2 | 2 GB × 3 | 长连接服务 |
| **Monitoring** | 256 MB | 384 MB | 512 MB | 512 MB × 2 | 监控服务 |

### 3.3 JVM 配置模板

```yaml
# S 规模 (小型)
JAVA_OPTS: "-Xms128m -Xmx256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=65.0"

# M 规模 (中型)
JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=65.0"

# L 规模 (大型)
JAVA_OPTS: "-Xms384m -Xmx768m -XX:+UseContainerSupport -XX:MaxRAMPercentage=65.0 -XX:+UseG1GC"

# XL 规模 (超大型)
JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseContainerSupport -XX:MaxRAMPercentage=65.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
```

---

## 4. HikariCP 连接池规划

### 4.1 按服务差异化配置

| 服务 | S 规模 | M 规模 | L 规模 | XL 规模 | 理由 |
|------|--------|--------|--------|---------|------|
| Auth | 10 | 15 | 20 | 30 | 读多写少 |
| Tenant | 5 | 10 | 15 | 20 | 低频访问 |
| Station | 15 | 25 | 35 | 50 | 设备状态 |
| Order | 20 | 40 | 60 | 80 | 订单热点 |
| Payment | 10 | 20 | 30 | 40 | 支付操作 |
| Protocol | 20 | 50 | 80 | 100 | 高并发事件 |
| Monitoring | 5 | 10 | 15 | 20 | 定时任务 |
| **总计** | **85** | **170** | **255** | **340** | - |

### 4.2 PostgreSQL max_connections 配置

| 规模 | 服务连接 | 预留连接 | 总计 |
|------|----------|----------|------|
| **S** | 85 | 15 | **100** |
| **M** | 170 | 30 | **200** |
| **L** | 255 | 45 | **300** |
| **XL** | 340 | 60 | **400+** |

### 4.3 连接池配置模板

```yaml
# application.yml
spring:
  datasource:
    hikari:
      # 通用配置
      connection-timeout: 20000      # 获取连接超时 20s
      idle-timeout: 300000           # 空闲连接超时 5min
      max-lifetime: 1200000          # 连接最大生命周期 20min
      leak-detection-threshold: 60000 # 泄漏检测 1min
      
      # 按服务配置 (示例: Order Service L 规模)
      maximum-pool-size: 60
      minimum-idle: 20
```

---

## 5. Protocol Service 特殊规划

### 5.1 长连接资源估算

| 连接数 | 内存预估 | JVM 堆 | 容器内存 | 副本数 |
|--------|----------|--------|----------|--------|
| 1,000 | 300 MB | 512 MB | 768 MB | 1 |
| 5,000 | 800 MB | 1 GB | 1.5 GB | 1 |
| 10,000 | 1.5 GB | 1.5 GB | 2 GB | 2 |
| 15,000 | 2 GB | 2 GB | 3 GB | 2 |
| 30,000 | 4 GB | 3 GB | 5 GB | 3 |

### 5.2 Netty 配置优化

```java
// 大规模长连接配置
EventLoopGroup bossGroup = new NioEventLoopGroup(2);
EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.group(bossGroup, workerGroup)
    .channel(NioServerSocketChannel.class)
    .option(ChannelOption.SO_BACKLOG, 1024)
    .option(ChannelOption.SO_REUSEADDR, true)
    .childOption(ChannelOption.SO_KEEPALIVE, true)
    .childOption(ChannelOption.TCP_NODELAY, true)
    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
```

---

## 6. 存储规划

### 6.1 PostgreSQL 存储增长预估

| 数据类型 | 单条大小 | 年增量 | 年存储 | 5年存储 |
|----------|----------|--------|--------|---------|
| 订单 | ~2 KB | 180万 | ~4 GB | ~20 GB |
| 支付 | ~1 KB | 180万 | ~2 GB | ~10 GB |
| 充电曲线 | ~200 B | 3.6亿 | ~70 GB | ~350 GB |
| 行为事件 | ~500 B | 3600万 | ~18 GB | ~90 GB |
| 日志/审计 | ~1 KB | 1000万 | ~10 GB | ~50 GB |
| **总计** | - | - | **~104 GB/年** | **~520 GB** |

### 6.2 存储配置建议

| 规模 | 数据盘 | 备份盘 | IOPS | 说明 |
|------|--------|--------|------|------|
| **S** | 50 GB SSD | 100 GB | 3000 | 本地 SSD |
| **M** | 200 GB SSD | 400 GB | 6000 | 云 SSD |
| **L** | 500 GB SSD | 1 TB | 10000 | 高性能 SSD |
| **XL** | 1 TB SSD | 2 TB | 20000+ | NVMe SSD |

---

## 7. 网络规划

### 7.1 带宽估算

| 场景 | 计算 | 带宽需求 |
|------|------|----------|
| API 请求 (1000 QPS × 10KB) | 10 MB/s | 100 Mbps |
| 协议通信 (15000 连接 × 1KB/s) | 15 MB/s | 150 Mbps |
| 数据库同步 | 5 MB/s | 50 Mbps |
| **总计** | **30 MB/s** | **300 Mbps** |

### 7.2 网络配置

| 规模 | 内网带宽 | 公网带宽 |
|------|----------|----------|
| **S** | 100 Mbps | 10 Mbps |
| **M** | 500 Mbps | 50 Mbps |
| **L** | 1 Gbps | 100 Mbps |
| **XL** | 10 Gbps | 500 Mbps |

---

## 8. Kubernetes HPA 配置

### 8.1 自动扩缩容配置

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 1
  maxReplicas: 5
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
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Pods
        value: 2
        periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Pods
        value: 1
        periodSeconds: 120
```

### 8.2 各服务 HPA 参数

| 服务 | minReplicas | maxReplicas | CPU 阈值 | 内存阈值 |
|------|-------------|-------------|----------|----------|
| Gateway | 1 | 5 | 70% | 80% |
| Order | 1 | 5 | 70% | 80% |
| Protocol | 2 | 6 | 60% | 75% |
| Station | 1 | 3 | 70% | 80% |
| Payment | 1 | 3 | 70% | 80% |
| Auth | 1 | 3 | 70% | 80% |

---

## 9. 监控告警阈值

### 9.1 资源告警

| 指标 | 警告阈值 | 严重阈值 | 说明 |
|------|----------|----------|------|
| CPU 使用率 | > 70% | > 85% | 持续 5 分钟 |
| 内存使用率 | > 75% | > 90% | 持续 5 分钟 |
| 磁盘使用率 | > 70% | > 85% | - |
| 连接池使用率 | > 70% | > 90% | - |
| 网络带宽 | > 60% | > 80% | 持续 10 分钟 |

### 9.2 业务告警

| 指标 | 警告阈值 | 严重阈值 |
|------|----------|----------|
| API P99 延迟 | > 500ms | > 1000ms |
| 错误率 | > 1% | > 5% |
| 消息积压 | > 1000 | > 10000 |
| 数据库连接等待 | > 100ms | > 500ms |

---

## 10. 当前环境配置更新清单

### 10.1 K8s 立即更新（P0）

| 组件 | 当前配置 | 建议配置 | 变更 |
|------|----------|----------|------|
| PostgreSQL | 1.5 GB | 4 GB | +2.5 GB |
| Redis | 256 MB | 1 GB | +768 MB |
| RabbitMQ | 256 MB | 512 MB | +256 MB |
| Protocol Service | 512 MB | 1 GB | +512 MB |
| Gateway | 512 MB | 768 MB | +256 MB |
| Order Service | 512 MB | 768 MB | +256 MB |

### 10.2 连接池差异化配置（P1）

| 服务 | 当前 | 建议 |
|------|------|------|
| Auth | 30 | 15 |
| Tenant | 30 | 10 |
| Station | 30 | 25 |
| Order | 30 | 40 |
| Payment | 30 | 20 |
| Protocol | 30 | 50 |
| Monitoring | 30 | 10 |

---

## 11. 相关文档

- [海量数据处理方案 RFC](../architecture/DATA-PARTITIONING-RFC.md)
- [系统架构风险审计报告](../architecture/RISK-AUDIT-REPORT.md)
- [部署指南](DEPLOYMENT-GUIDE.md)
- [监控指南](../operations/MONITORING-GUIDE.md)

---

## 12. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本，定义 S/M/L/XL 四级资源规划 |
