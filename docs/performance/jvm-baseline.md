# JVM 配置基线报告

> **创建日期**: 2025-10-23  
> **目的**: 建立性能优化前的配置基线  
> **状态**: 准备阶段

---

## 1. 当前配置审计

### 1.1 Docker Compose 配置

**文件**: `docker-compose.yml`

```yaml
x-java-env: &java-env
  JAVA_OPTS: "-Xms256m -Xmx512m -Dspring.config.import=optional:configserver:http://config-server:8888"
```

**分析**:
- ✅ 堆内存: 256MB ~ 512MB（针对容器环境已优化）
- ⚠️ GC 策略: 未显式配置（使用 JVM 默认）
- ⚠️ GC 日志: 未启用
- ⚠️ JFR 监控: 未配置
- ⚠️ 性能调优参数: 无

### 1.2 预期的默认 JVM 配置

基于 Java 21 + 容器环境，预期的默认配置：

| 参数 | 预期值 | 说明 |
|------|--------|------|
| **GC 算法** | G1GC | Java 9+ 默认 GC |
| **MaxHeapSize** | 512m | 由 -Xmx 指定 |
| **InitialHeapSize** | 256m | 由 -Xms 指定 |
| **MaxGCPauseMillis** | 200ms | G1GC 默认目标 |
| **ParallelGCThreads** | ~2-4 | 基于容器 CPU 限制 |

---

## 2. 观察到的问题（基于代码审查）

### 2.1 缺少 GC 监控

**问题**: 
- 无法查看 GC 暂停时间
- 无法分析 GC 频率和效率
- 无法验证内存使用模式

**影响**: 
- 无法建立性能基线
- 无法识别性能瓶颈
- 无法验证优化效果

### 2.2 缺少 JFR 性能分析

**问题**: 
- 无法进行热点分析
- 无法追踪内存分配
- 无法分析线程行为

**影响**: 
- 问题排查困难
- 优化方向不明确

### 2.3 GC 参数未针对工作负载调优

**问题**: 
- 使用默认 200ms GC 暂停目标（对实时业务偏高）
- 未配置字符串去重（节省内存）
- 未优化堆区域大小

**潜在优化空间**: 
- GC 暂停时间可降低至 <100ms
- 内存占用可通过字符串去重降低 5-10%

---

## 3. 性能基线（待测量）

### 3.1 需要测量的指标

| 指标 | 测量方法 | 目标 |
|------|---------|------|
| **GC 暂停时间 (P99)** | JFR + Prometheus | 建立基线 → 优化至 <100ms |
| **吞吐量 (TPS)** | JMeter 压测 | 建立基线 → 提升 10%+ |
| **内存使用** | Prometheus metrics | 建立基线 → 维持 |
| **CPU 使用率** | Prometheus metrics | 建立基线 → 维持 |
| **响应时间 (P99)** | JMeter 压测 | 建立基线 → 降低至 <200ms |

### 3.2 测试场景设计

**场景 1: 订单创建高并发**
- 目标 TPS: 500
- 并发用户: 100
- 持续时间: 10 分钟
- 关注指标: GC 暂停、响应时间

**场景 2: 订单查询混合负载**
- 目标 TPS: 1000
- 并发用户: 200
- 持续时间: 10 分钟
- 关注指标: 吞吐量、内存使用

**场景 3: 充电状态更新峰值**
- 目标 TPS: 2000
- 并发用户: 500
- 持续时间: 5 分钟
- 关注指标: CPU、GC 频率

---

## 4. 下一步行动

### 4.1 准备阶段（本周）

- [x] 创建 JVM 基线文档
- [ ] 配置 JFR 监控（docker-compose.yml）
- [ ] 配置 Prometheus JVM metrics（application.yml）
- [ ] 创建 JMeter 测试脚本
- [ ] 安装测试工具（JMeter、JDK Mission Control）

### 4.2 Week 2 Day 1（2025-10-28）

- [ ] 启动服务并验证 JFR 配置
- [ ] 执行基线性能测试
- [ ] 收集 JFR 数据并分析
- [ ] 记录当前性能指标

### 4.3 Week 2 Day 2-3（2025-10-29 ~ 10-30）

- [ ] 测试 G1GC 不同参数组合
- [ ] 测试 ZGC 低延迟配置
- [ ] 对比性能数据
- [ ] 选择最优配置

---

## 5. 配置优化方向（预规划）

### 5.1 G1GC 优化参数（待测试）

```yaml
# 保守配置
JAVA_OPTS: >
  -Xms256m -Xmx512m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=100
  -XX:G1HeapRegionSize=16m

# 激进配置
JAVA_OPTS: >
  -Xms256m -Xmx512m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=50
  -XX:InitiatingHeapOccupancyPercent=45
  -XX:G1HeapRegionSize=16m
  -XX:+UseStringDeduplication

# 平衡配置
JAVA_OPTS: >
  -Xms256m -Xmx512m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=75
  -XX:InitiatingHeapOccupancyPercent=50
  -XX:G1HeapRegionSize=8m
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
```

### 5.2 ZGC 低延迟配置（待测试）

```yaml
# ZGC 默认配置
JAVA_OPTS: >
  -Xms256m -Xmx512m
  -XX:+UseZGC
  -XX:ZAllocationSpikeTolerance=5

# ZGC 优化配置
JAVA_OPTS: >
  -Xms256m -Xmx512m
  -XX:+UseZGC
  -XX:ZAllocationSpikeTolerance=2
  -XX:ZCollectionInterval=5
```

---

## 6. 预期优化效果

| 指标 | 当前（估计） | 目标 | 提升 |
|------|-------------|------|------|
| GC 暂停时间 (P99) | ~200ms | <100ms | 50%+ |
| 吞吐量 (TPS) | 基线 | +10% | 10%+ |
| 响应时间 (P99) | 基线 | <200ms | 视基线而定 |
| 内存占用 | 256-512MB | 维持 | 0% |
| CPU 使用率 | 基线 | 维持 | 0% |

---

## 7. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 配置不当导致频繁 Full GC | 高 | 充分测试 + 回滚方案 |
| 内存溢出 | 高 | 监控 + 告警 |
| 性能下降 | 中 | 对比基线 + 回滚 |
| 测试影响生产 | 低 | 使用测试环境 |

---

## 8. 参考资料

- [Java 21 GC Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [G1GC Best Practices](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [ZGC Overview](https://wiki.openjdk.org/display/zgc/Main)
- [JFR and JDK Mission Control](https://docs.oracle.com/javacomponents/jmc.htm)

---

**文档状态**: ✅ 基线已建立  
**下一步**: 配置监控工具  
**负责人**: Backend Team
