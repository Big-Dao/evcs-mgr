# P4 Week 2-4 性能优化详细执行计划

> **制定日期**: 2025-10-23  
> **执行周期**: Week 2-4 (2025-10-28 ~ 2025-11-15)  
> **负责人**: Backend Team  
> **状态**: 待执行

---

## 📋 执行概览

### 优化目标

基于 GraalVM 评估报告的结论，我们选择继续使用 OpenJDK 21，并执行以下**高 ROI 优化方案**：

| 优化项 | 当前状态 | 目标状态 | 预期收益 | 工时 |
|--------|---------|---------|---------|------|
| **JVM GC 调优** | ~200ms 暂停 | <100ms | 用户体验提升 50% | 2-3天 |
| **连接池优化** | 未监控 | 60-80% 使用率 | 避免连接泄漏，稳定性↑ | 1-2天 |
| **Redis 集群化** | 单点故障风险 | 99.9%+ 可用性 | 规避单点，可用性↑ | 3-5天 |

**总投入**: 6-10 工作日  
**ROI 评估**: 高（提升稳定性与用户体验，成本可控）

---

## Week 2: JVM 调优 + 连接池优化

### 📅 时间线：2025-10-28 ~ 11-01

---

### Day 1（Monday）：JVM 配置评估与监控准备

#### 🎯 目标
- 了解当前 JVM 配置现状
- 建立性能基线
- 配置监控工具

#### 📝 任务清单

**1. 当前配置审计**（1小时）
```bash
# 检查各服务当前 JVM 参数
docker exec evcs-gateway java -XX:+PrintFlagsFinal -version | grep -E 'UseG1GC|UseZGC|MaxHeapSize'
docker exec evcs-order java -XX:+PrintFlagsFinal -version | grep -E 'UseG1GC|UseZGC|MaxHeapSize'

# 记录到文档
cat > docs/jvm-baseline.md <<EOF
# JVM 配置基线 (2025-10-28)

## 当前配置
- Heap: -Xms256m -Xmx512m
- GC: 默认（可能是 G1GC）
- 其他参数: 无

## 观察到的问题
- GC 暂停时间: 未监控
- 吞吐量: 未基准测试
EOF
```

**2. 安装 JFR（Java Flight Recorder）监控**（2小时）
```yaml
# 更新 docker-compose.yml，添加 JFR 参数
environment:
  JAVA_OPTS: >
    -Xms256m -Xmx512m
    -XX:StartFlightRecording=dumponexit=true,filename=/app/logs/flight.jfr
    -XX:FlightRecorderOptions=stackdepth=128
volumes:
  - ./logs/jfr:/app/logs  # 挂载 JFR 输出目录
```

**3. 配置 JVM 指标导出**（1小时）
```yaml
# application.yml 添加 JVM metrics
management:
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

**4. 设计性能测试场景**（2小时）

创建 JMeter 测试脚本：
```bash
# performance-tests/jvm-tuning-test.jmx
测试场景 1: 订单创建高并发（500 TPS）
- 并发用户: 100
- 持续时间: 10 分钟
- 监控指标: 响应时间、GC 暂停

测试场景 2: 订单查询混合负载（1000 TPS）
- 并发用户: 200
- 持续时间: 10 分钟
- 监控指标: 吞吐量、内存使用

测试场景 3: 充电状态更新峰值（2000 TPS）
- 并发用户: 500
- 持续时间: 5 分钟
- 监控指标: CPU、GC 频率
```

**交付物**:
- ✅ `docs/jvm-baseline.md` - JVM 配置基线文档
- ✅ JFR 监控配置完成
- ✅ Prometheus 指标导出配置
- ✅ JMeter 测试脚本

---

### Day 2-3（Tuesday - Wednesday）：GC 参数调优与验证

#### 🎯 目标
- 测试 G1GC 与 ZGC 性能表现
- 找到最优配置组合
- 建立性能基准报告

#### 📝 任务清单

**1. G1GC 参数调优测试**（Day 2 上午，3小时）

**测试组 A：保守配置**
```yaml
environment:
  JAVA_OPTS: >
    -Xms256m -Xmx512m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=100
    -XX:G1HeapRegionSize=16m
```

**测试组 B：激进配置**
```yaml
environment:
  JAVA_OPTS: >
    -Xms256m -Xmx512m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=50
    -XX:InitiatingHeapOccupancyPercent=45
    -XX:G1HeapRegionSize=16m
    -XX:+UseStringDeduplication
```

**测试组 C：平衡配置**
```yaml
environment:
  JAVA_OPTS: >
    -Xms256m -Xmx512m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=75
    -XX:InitiatingHeapOccupancyPercent=50
    -XX:G1HeapRegionSize=8m
    -XX:+UseStringDeduplication
    -XX:+OptimizeStringConcat
```

**执行步骤**:
```bash
# 每组配置执行
for config in A B C; do
  echo "Testing G1GC Config $config"
  
  # 1. 更新配置
  cp docker-compose-g1gc-$config.yml docker-compose.yml
  docker-compose restart evcs-order evcs-station
  
  # 2. 等待服务稳定
  sleep 60
  
  # 3. 执行性能测试
  jmeter -n -t performance-tests/jvm-tuning-test.jmx \
    -l results/g1gc-$config-results.jtl \
    -e -o results/g1gc-$config-report
  
  # 4. 收集 JFR 数据
  docker cp evcs-order:/app/logs/flight.jfr ./results/g1gc-$config-flight.jfr
  
  # 5. 分析 GC 日志
  docker logs evcs-order 2>&1 | grep -E 'GC|pause' > results/g1gc-$config-gc.log
done
```

**2. ZGC 低延迟配置测试**（Day 2 下午，3小时）

**测试组 D：ZGC 默认配置**
```yaml
environment:
  JAVA_OPTS: >
    -Xms256m -Xmx512m
    -XX:+UseZGC
    -XX:ZAllocationSpikeTolerance=5
```

**测试组 E：ZGC 优化配置**
```yaml
environment:
  JAVA_OPTS: >
    -Xms256m -Xmx512m
    -XX:+UseZGC
    -XX:ZAllocationSpikeTolerance=2
    -XX:ZCollectionInterval=5
```

**执行步骤**（同上）

**3. 性能数据分析**（Day 3 上午，4小时）

创建分析脚本：
```python
# scripts/analyze_jvm_performance.py
import pandas as pd
import matplotlib.pyplot as plt

# 读取 JMeter 结果
configs = ['g1gc-A', 'g1gc-B', 'g1gc-C', 'zgc-D', 'zgc-E']
results = []

for config in configs:
    df = pd.read_csv(f'results/{config}-results.jtl')
    results.append({
        'config': config,
        'avg_response_time': df['elapsed'].mean(),
        'p99_response_time': df['elapsed'].quantile(0.99),
        'throughput': len(df) / (df['timeStamp'].max() - df['timeStamp'].min()) * 1000,
        'error_rate': df['success'].value_counts(normalize=True).get(False, 0)
    })

# 生成对比报告
report_df = pd.DataFrame(results)
print(report_df.to_markdown(index=False))

# 生成图表
fig, axes = plt.subplots(2, 2, figsize=(12, 10))
report_df.plot(x='config', y='avg_response_time', kind='bar', ax=axes[0,0], title='平均响应时间')
report_df.plot(x='config', y='p99_response_time', kind='bar', ax=axes[0,1], title='P99 响应时间')
report_df.plot(x='config', y='throughput', kind='bar', ax=axes[1,0], title='吞吐量 (TPS)')
report_df.plot(x='config', y='error_rate', kind='bar', ax=axes[1,1], title='错误率')
plt.savefig('results/jvm-performance-comparison.png')
```

**4. 选择最优配置**（Day 3 下午，2小时）

**决策矩阵**:
| 指标 | 权重 | G1GC-A | G1GC-B | G1GC-C | ZGC-D | ZGC-E |
|------|------|--------|--------|--------|-------|-------|
| P99 响应时间 | 40% | ? | ? | ? | ? | ? |
| 吞吐量 | 30% | ? | ? | ? | ? | ? |
| 稳定性 | 20% | ? | ? | ? | ? | ? |
| 资源占用 | 10% | ? | ? | ? | ? | ? |
| **总分** | 100% | ? | ? | ? | ? | ? |

**5. 应用最优配置到生产**（Day 3 下午，1小时）

```bash
# 更新所有服务的 docker-compose.yml
# 示例：假设 G1GC-C 获胜

# docker-compose.yml
x-java-env: &java-env
  JAVA_OPTS: >
    -Xms256m -Xmx512m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=75
    -XX:InitiatingHeapOccupancyPercent=50
    -XX:G1HeapRegionSize=8m
    -XX:+UseStringDeduplication
    -XX:+OptimizeStringConcat
    -Djava.security.egd=file:/dev/./urandom
```

**交付物**:
- ✅ 5组配置的性能测试报告
- ✅ JFR 分析数据
- ✅ `results/jvm-performance-comparison.png` - 性能对比图表
- ✅ `docs/JVM-TUNING-REPORT.md` - 完整调优报告
- ✅ 更新后的 `docker-compose.yml`

---

### Day 4（Thursday）：数据库连接池优化

#### 🎯 目标
- 配置 Hikari 连接池监控
- 优化连接池参数
- 避免连接泄漏

#### 📝 任务清单

**1. 配置 Hikari 监控指标**（2小时）

```yaml
# evcs-common/src/main/resources/application.yml
spring:
  datasource:
    hikari:
      # 连接池配置
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 600000        # 10分钟
      max-lifetime: 1800000        # 30分钟
      
      # 监控配置
      register-mbeans: true
      metric-registry: hikariMetricRegistry
      
      # 泄漏检测
      leak-detection-threshold: 60000  # 60秒

# Micrometer 指标导出
management:
  metrics:
    enable:
      hikaricp: true
    export:
      prometheus:
        enabled: true
```

**2. 添加连接池监控到 Grafana**（2小时）

创建 Grafana 仪表盘配置：
```json
// monitoring/grafana/dashboards/hikari-connection-pool.json
{
  "dashboard": {
    "title": "HikariCP Connection Pool Monitoring",
    "panels": [
      {
        "title": "Active Connections",
        "targets": [
          {
            "expr": "hikaricp_connections_active{application=\"evcs-order\"}"
          }
        ]
      },
      {
        "title": "Pending Threads",
        "targets": [
          {
            "expr": "hikaricp_connections_pending{application=\"evcs-order\"}"
          }
        ]
      },
      {
        "title": "Connection Timeout Count",
        "targets": [
          {
            "expr": "rate(hikaricp_connections_timeout_total[5m])"
          }
        ]
      },
      {
        "title": "Connection Usage (%)",
        "targets": [
          {
            "expr": "(hikaricp_connections_active / hikaricp_connections_max) * 100"
          }
        ]
      }
    ]
  }
}
```

**3. 压力测试连接池**（2小时）

```bash
# 模拟高并发场景
jmeter -n -t performance-tests/connection-pool-stress.jmx \
  -Jusers=500 \
  -Jduration=600 \
  -l results/connection-pool-stress.jtl

# 监控连接池指标
watch -n 1 "curl -s http://localhost:8083/actuator/metrics/hikaricp.connections.active | jq"
```

**4. 验证泄漏检测**（1小时）

```java
// 创建一个故意泄漏的测试用例
@Test
void testConnectionLeakDetection() {
    // 模拟忘记关闭连接
    Connection conn = dataSource.getConnection();
    // 不调用 conn.close()
    
    // 等待超过 leak-detection-threshold (60秒)
    Thread.sleep(65000);
    
    // 检查日志是否有泄漏警告
    // Expected: "Connection leak detection triggered"
}
```

**交付物**:
- ✅ 优化后的 Hikari 配置
- ✅ Grafana 连接池监控仪表盘
- ✅ 连接池压力测试报告
- ✅ 泄漏检测验证报告

---

### Day 5（Friday）：Redis 集群方案设计

#### 🎯 目标
- 评估 Redis Sentinel vs Cluster
- 设计高可用架构
- 制定迁移计划

#### 📝 任务清单

**1. 方案对比分析**（2小时）

| 对比维度 | Redis Sentinel | Redis Cluster |
|---------|---------------|---------------|
| **架构复杂度** | ⭐⭐ 简单 | ⭐⭐⭐⭐ 复杂 |
| **可用性** | 99.9% | 99.99% |
| **写性能** | 单主 | 多主（分片） |
| **读性能** | 主+从 | 主+从（多分片） |
| **客户端兼容** | ⭐⭐⭐⭐⭐ 透明 | ⭐⭐⭐ 需特殊客户端 |
| **运维成本** | ⭐⭐ 低 | ⭐⭐⭐⭐ 高 |
| **适用场景** | 中小规模 | 大规模分布式 |
| **推荐度** | ⭐⭐⭐⭐ | ⭐⭐⭐ |

**2. 设计 Redis Sentinel 架构**（3小时）

```yaml
# docker-compose-redis-sentinel.yml
services:
  redis-master:
    image: redis:7-alpine
    command: redis-server --appendonly yes --masterauth redis123 --requirepass redis123
    volumes:
      - redis-master-data:/data

  redis-slave-1:
    image: redis:7-alpine
    command: redis-server --appendonly yes --slaveof redis-master 6379 --masterauth redis123 --requirepass redis123
    volumes:
      - redis-slave-1-data:/data

  redis-slave-2:
    image: redis:7-alpine
    command: redis-server --appendonly yes --slaveof redis-master 6379 --masterauth redis123 --requirepass redis123
    volumes:
      - redis-slave-2-data:/data

  redis-sentinel-1:
    image: redis:7-alpine
    command: >
      redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./redis/sentinel.conf:/etc/redis/sentinel.conf

  redis-sentinel-2:
    image: redis:7-alpine
    command: >
      redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./redis/sentinel.conf:/etc/redis/sentinel.conf

  redis-sentinel-3:
    image: redis:7-alpine
    command: >
      redis-sentinel /etc/redis/sentinel.conf
    volumes:
      - ./redis/sentinel.conf:/etc/redis/sentinel.conf
```

**Sentinel 配置文件**:
```conf
# redis/sentinel.conf
port 26379
sentinel monitor mymaster redis-master 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 10000
sentinel auth-pass mymaster redis123
```

**3. 制定迁移计划**（2小时）

```markdown
# Redis Sentinel 迁移计划

## Phase 1: 测试环境验证（Week 3 Day 1-2）
1. 搭建 Sentinel 集群（1主2从3哨兵）
2. 验证故障自动切换
3. 性能测试对比

## Phase 2: 应用配置调整（Week 3 Day 3）
1. 更新 Spring Boot 配置
```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - redis-sentinel-1:26379
          - redis-sentinel-2:26379
          - redis-sentinel-3:26379
      password: redis123
```
2. 更新 Lettuce 客户端配置
3. 本地测试验证

## Phase 3: 灰度切换（Week 3 Day 4-5）
1. **双写验证**（1天）
   - 新老 Redis 同时写入
   - 对比数据一致性
   - 监控错误率

2. **流量切换**（0.5天）
   - 切换读流量到新集群
   - 监控性能指标
   - 准备回滚方案

3. **完全迁移**（0.5天）
   - 关闭双写
   - 下线旧 Redis
   - 清理配置

## Phase 4: 监控与验收（Week 4 Day 1）
1. 配置 Sentinel 监控告警
2. 执行故障演练
3. 编写运维文档
```

**4. 编写技术方案文档**（1小时）

```bash
# 创建 docs/REDIS-SENTINEL-MIGRATION.md
touch docs/REDIS-SENTINEL-MIGRATION.md
```

**交付物**:
- ✅ Redis Sentinel vs Cluster 对比报告
- ✅ `docker-compose-redis-sentinel.yml` - Sentinel 集群配置
- ✅ `docs/REDIS-SENTINEL-MIGRATION.md` - 迁移方案文档
- ✅ Week 3 详细执行计划

---

## Week 3: Redis 集群化实施

### 📅 时间线：2025-11-04 ~ 11-08

（详细计划见上述 Day 5 制定的迁移方案）

---

## Week 4: 性能压测与监控完善

### 📅 时间线：2025-11-11 ~ 11-15

### 目标
- 建立性能基准
- 验证优化效果
- 完善监控告警

### 主要任务

**1. JMeter 压测执行**（2天）
- 订单创建压测（目标 >= 500 TPS）
- 订单查询压测（目标 >= 1000 TPS）
- 充电状态更新压测（目标 >= 2000 TPS）

**2. 性能瓶颈分析**（1天）
- 使用 JFR 分析热点方法
- 使用 Arthas 实时诊断
- 数据库慢查询优化

**3. Prometheus 告警规则配置**（1天）
```yaml
# monitoring/prometheus/alerts.yml
groups:
  - name: evcs_performance
    rules:
      - alert: HighGCPauseTime
        expr: jvm_gc_pause_seconds_max > 0.1
        for: 5m
        annotations:
          summary: "GC 暂停时间过长 ({{ $value }}s)"
      
      - alert: HighConnectionPoolUsage
        expr: (hikaricp_connections_active / hikaricp_connections_max) > 0.9
        for: 5m
        annotations:
          summary: "连接池使用率过高 ({{ $value }}%)"
      
      - alert: RedisMasterDown
        expr: redis_up{role="master"} == 0
        for: 1m
        annotations:
          summary: "Redis 主节点宕机"
```

**4. 运维文档更新**（1天）
- 性能调优手册
- 故障排查流程
- 监控指标说明

---

## 📊 验收标准

### Week 2 验收（2025-11-01）

| 指标 | 目标 | 验收方式 |
|------|------|---------|
| GC 暂停时间 | <100ms | JFR 报告 + JMeter P99 |
| 连接池使用率 | 60-80% | Grafana 仪表盘 |
| 吞吐量提升 | >= 10% | 对比基线测试 |
| 文档完整性 | 100% | Review 检查 |

### Week 3 验收（2025-11-08）

| 指标 | 目标 | 验收方式 |
|------|------|---------|
| Redis 可用性 | 99.9%+ | 故障演练 |
| 故障恢复时间 | <30秒 | Sentinel 自动切换 |
| 数据一致性 | 100% | 双写对比验证 |
| 监控覆盖 | 100% | Grafana 仪表盘 |

### Week 4 验收（2025-11-15）

| 指标 | 目标 | 验收方式 |
|------|------|---------|
| 订单创建 TPS | >= 500 | JMeter 报告 |
| 订单查询 TPS | >= 1000 | JMeter 报告 |
| P99 响应时间 | < 200ms | JMeter 报告 |
| 错误率 | < 0.1% | JMeter 报告 |

---

## 📝 文档交付物清单

- ✅ `docs/jvm-baseline.md` - JVM 配置基线
- ✅ `docs/JVM-TUNING-REPORT.md` - JVM 调优报告
- ✅ `docs/REDIS-SENTINEL-MIGRATION.md` - Redis 迁移方案
- ✅ `docs/PERFORMANCE-BENCHMARK-REPORT.md` - 性能基准报告
- ✅ `monitoring/grafana/dashboards/hikari-connection-pool.json` - 连接池仪表盘
- ✅ `monitoring/prometheus/alerts.yml` - 告警规则
- ✅ `performance-tests/jvm-tuning-test.jmx` - JVM 调优测试脚本
- ✅ `performance-tests/connection-pool-stress.jmx` - 连接池压测脚本

---

## ⚠️ 风险与缓解

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| JVM 配置不当导致频繁 Full GC | 高 | 中 | 充分测试 + 保留回滚配置 |
| Redis 迁移过程数据丢失 | 高 | 低 | 双写验证 + 定时备份 |
| 性能压测影响生产环境 | 中 | 低 | 使用独立测试环境 |
| 连接池泄漏未被检测到 | 中 | 中 | 配置泄漏检测 + 监控告警 |

---

## 🚀 下一步（Week 5+）

完成性能优化后，团队将转向：
- Week 5-8: 前端开发（管理后台 + 小程序）
- Week 9-10: 协议与支付集成完善
- Week 11-12: 运维监控 + 灰度上线

---

**计划状态**: ✅ 已制定，待执行  
**最后更新**: 2025-10-23  
**负责人**: Backend Team + DevOps
