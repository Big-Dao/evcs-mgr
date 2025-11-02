# Week 2 Day 2 - G1GC 低延迟优化方案

**日期**: 2025-10-25  
**目标**: GC暂停时间 P99 < 100ms，响应时间降低 26%

---

## 1. 基线回顾

### Day 1 性能基线（默认G1GC）

| 指标 | Order | Station | Gateway |
|------|-------|---------|---------|
| TPS | 3.57 | 3.59 | 0.31* |
| P50延迟 | 270ms | 272ms | 270ms |
| P99延迟 | 314ms | 316ms | 540ms |
| 错误率 | 0% | 0% | 0.2% |

*注：Gateway的TPS异常是测试脚本问题，实际延迟与直连相当（280ms vs 286ms）

### 当前JVM配置

```yaml
JAVA_OPTS: >
  -Xms256m 
  -Xmx512m
  # 默认使用G1GC，无显式GC参数
  -XX:StartFlightRecording=dumponexit=true,filename=/tmp/flight.jfr
  -XX:FlightRecorderOptions=stackdepth=128
```

---

## 2. 优化方案：G1GC低延迟配置

### 优化目标

- ✅ GC暂停时间 P99: < 100ms（预计从 150-250ms 降低）
- ✅ 响应时间 P50: < 200ms（从 270ms 降低 26%）
- ✅ TPS: 提升 30-50%（从 3.6 提升至 5+）
- ✅ Full GC次数: 0（保持）

### 新增JVM参数

```yaml
JAVA_OPTS: >
  -Xms512m                              # 堆初始大小（提升至最大值，减少扩容）
  -Xmx512m                              # 堆最大大小
  -XX:+UseG1GC                          # 显式启用G1GC（虽然Java 21默认）
  -XX:MaxGCPauseMillis=50               # 目标最大暂停时间 50ms（低延迟）
  -XX:G1HeapRegionSize=4M               # Region大小4MB（适合512MB堆）
  -XX:InitiatingHeapOccupancyPercent=45 # 堆占用45%时触发并发标记（提早GC）
  -XX:G1ReservePercent=15               # 预留15%堆空间（避免Full GC）
  -XX:+ParallelRefProcEnabled           # 并行处理引用对象（加速GC）
  -XX:StartFlightRecording=dumponexit=true,filename=/tmp/flight.jfr
  -XX:FlightRecorderOptions=stackdepth=128
```

### 参数说明

| 参数 | 作用 | 优化效果 |
|------|------|---------|
| `-Xms512m` | 初始堆=最大堆 | 避免运行时扩容开销 |
| `-XX:MaxGCPauseMillis=50` | 目标暂停时间50ms | 降低延迟峰值 |
| `-XX:G1HeapRegionSize=4M` | Region大小4MB | 平衡吞吐量与延迟 |
| `-XX:InitiatingHeapOccupancyPercent=45` | 45%堆占用触发GC | 更积极的并发标记，避免Full GC |
| `-XX:G1ReservePercent=15` | 预留15%空间 | 防止晋升失败导致Full GC |
| `-XX:+ParallelRefProcEnabled` | 并行引用处理 | 加速Reference对象处理 |

---

## 3. 实施步骤

### Step 1: 备份当前配置

```bash
cd c:\Users\andyz\Projects\evcs-mgr
cp docker-compose.yml docker-compose.yml.backup-day1
```

### Step 2: 修改 docker-compose.yml

找到 `x-java-env` anchor：

```yaml
x-java-env: &java-env
  JAVA_OPTS: >
    -Xms512m -Xmx512m
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=50
    -XX:G1HeapRegionSize=4M
    -XX:InitiatingHeapOccupancyPercent=45
    -XX:G1ReservePercent=15
    -XX:+ParallelRefProcEnabled
    -XX:StartFlightRecording=dumponexit=true,filename=/tmp/flight.jfr
    -XX:FlightRecorderOptions=stackdepth=128
  SPRING_PROFILES_ACTIVE: local
  SPRING_FLYWAY_BASELINE_ON_MIGRATE: "true"
  SPRING_FLYWAY_ENABLED: "true"
  CONFIG_SERVER_URL: "http://config-server:8888"
  EUREKA_SERVER_URL: "http://eureka:8761/eureka/"
```

### Step 3: 重启服务

```bash
# 停止所有服务
docker-compose down

# 启动服务（应用新配置）
docker-compose up -d

# 等待服务就绪（2-3分钟）
Start-Sleep -Seconds 180

# 验证服务健康
docker ps --filter "name=evcs-" --format "table {{.Names}}\t{{.Status}}"
```

### Step 4: 验证JVM参数生效

```bash
# 检查Order服务的JVM参数
docker exec evcs-order ps aux | Select-String "MaxGCPauseMillis"

# 应显示: -XX:MaxGCPauseMillis=50
```

### Step 5: 执行对比测试

```bash
cd performance-tests

# 执行测试（与Day 1相同参数）
.\simple-baseline.ps1 -Requests 500 -Concurrency 25

# 测试结果将保存至: results/simple-baseline-20251025-HHMMSS.json
```

### Step 6: 收集JFR文件

```bash
# 导出JFR文件（优化后）
docker cp evcs-order:/tmp/flight.jfr logs/jfr/g1gc-optimized-order.jfr
docker cp evcs-station:/tmp/flight.jfr logs/jfr/g1gc-optimized-station.jfr
docker cp evcs-gateway:/tmp/flight.jfr logs/jfr/g1gc-optimized-gateway.jfr
```

---

## 4. 预期效果

### 性能提升预测

| 指标 | 基线 (Day 1) | 目标 (Day 2) | 提升幅度 |
|------|-------------|-------------|---------|
| **TPS** | 3.6 | 5.0+ | +39% |
| **P50延迟** | 270ms | <200ms | -26% |
| **P90延迟** | 290ms | <250ms | -14% |
| **P99延迟** | 390ms | <300ms | -23% |
| **GC暂停P99** | 150-250ms* | <100ms | -40% |
| **错误率** | 0.1% | <0.1% | 保持 |

*基于512MB堆的典型G1GC表现

### 风险评估

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| GC暂停时间不降反升 | 低 | 中 | MaxGCPauseMillis过小导致频繁GC，调整至75ms |
| 吞吐量下降 | 中 | 低 | 低延迟会牺牲少量吞吐量，可接受 |
| 服务启动失败 | 极低 | 高 | 保留备份配置，快速回滚 |

---

## 5. 分析方法

### 5.1 JFR对比分析

使用JDK Mission Control打开两个JFR文件：

```bash
# 基线
jmc logs/jfr/baseline-order.jfr

# 优化后
jmc logs/jfr/g1gc-optimized-order.jfr
```

**关键对比点**:

1. **GC暂停时间** (`Java Application → Garbage Collections`)
   - 对比暂停时间分布图
   - 检查P99是否 <100ms

2. **GC频率**
   - 对比每分钟GC次数
   - 目标: <10次/分钟

3. **Heap使用**
   - 对比堆使用率曲线
   - 检查是否有Full GC

4. **吞吐量影响**
   - 对比GC总耗时占比
   - 目标: <5%

### 5.2 Prometheus指标对比

访问 http://localhost:9090，执行以下查询：

**GC暂停时间对比**:
```promql
histogram_quantile(0.99, 
  rate(jvm_gc_pause_seconds_bucket{service="evcs-order"}[5m])
)
```

**GC次数对比**:
```promql
rate(jvm_gc_pause_seconds_count{service="evcs-order"}[5m]) * 60
```

**Heap使用率**:
```promql
jvm_memory_used_bytes{area="heap",service="evcs-order"} / 
jvm_memory_max_bytes{area="heap",service="evcs-order"}
```

---

## 6. 成功标准

### 必须满足（Go/No-Go）

- ✅ 服务启动成功，所有容器健康
- ✅ 错误率 ≤ 0.5%（不能因优化导致稳定性下降）
- ✅ JVM参数验证生效（`MaxGCPauseMillis=50`）

### 优化目标（期望达成）

- 🎯 P99响应时间 < 300ms（基线：390ms）
- 🎯 P50响应时间 < 200ms（基线：270ms）
- 🎯 TPS提升 > 30%（基线：3.6 → 目标：4.7+）
- 🎯 GC暂停P99 < 100ms（基于JFR）

### 拉伸目标（最佳结果）

- 🏆 P99响应时间 < 250ms（提升 36%）
- 🏆 TPS提升 > 50%（达到 5.4+）
- 🏆 GC暂停P99 < 75ms

---

## 7. 回滚计划

如果优化效果不佳或出现问题：

```bash
# 立即回滚
docker-compose down
cp docker-compose.yml.backup-day1 docker-compose.yml
docker-compose up -d
```

**回滚触发条件**:
- 错误率 > 1%
- 服务无法启动
- P99响应时间 > 500ms（性能恶化）

---

## 8. 下一步（Day 3）

根据Day 2结果决定：

**场景1: Day 2效果显著**（P99<100ms，TPS提升>30%）
- ✅ 采用G1GC低延迟配置
- ⏭️ Day 3跳过ZGC测试，直接进入Day 4连接池优化

**场景2: Day 2效果中等**（P99=100-150ms，TPS提升10-30%）
- ⚠️ Day 3测试ZGC配置（可能更优）
- 📊 对比G1GC vs ZGC，选择最优方案

**场景3: Day 2效果不佳**（P99>150ms或性能下降）
- 🔄 回滚至基线配置
- 🔍 调整参数（如MaxGCPauseMillis=75ms）重新测试

---

**编制**: GitHub Copilot  
**计划日期**: 2025-10-25  
**预计执行时间**: 1-2小时
