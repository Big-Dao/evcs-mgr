# Week 2 Day 4 计划 - 数据库与应用层优化

**日期**: 2025-10-26  
**前置条件**: Day 3完成GC优化，确认Station瓶颈在数据库查询

---

## 执行摘要

### 计划调整

**原计划**: 连接池优化（HikariCP参数调优）  
**新计划**: **数据库查询优化为主，连接池为辅**

**调整原因**:
- Day 3测试显示Station平均响应838ms（P50仅270ms）
- 说明存在大量极端慢请求（可能>2000ms）
- 慢查询比连接池等待影响更大
- 连接池配置（max-20）在低并发下不是瓶颈

---

## 工作目标

### 上午：诊断与分析（09:00-12:00）

**目标1: 识别Station慢查询** 🔴
- [ ] 启用Hibernate SQL日志
- [ ] 执行性能测试，收集SQL语句
- [ ] 分析慢查询Top 5（按执行时间）
- [ ] 检查N+1查询问题

**目标2: 数据库索引分析** 🔴
- [ ] 检查`charging_station`表索引
- [ ] 检查`charging_pile`表索引
- [ ] 使用EXPLAIN ANALYZE验证查询计划
- [ ] 识别缺失或未使用的索引

**目标3: 连接池监控** 🟡
- [ ] 监控HikariCP连接池等待时间
- [ ] 检查连接泄漏（leak detection）
- [ ] 分析连接使用率峰值

---

## 下午：优化实施（13:00-18:00）

### 阶段1: 数据库优化（13:00-15:30）

**任务1: 修复慢查询**
```java
// 示例：优化充电站列表查询
// 优化前（可能的N+1）
List<ChargingStation> stations = stationService.findByTenantId(tenantId);
for (ChargingStation station : stations) {
    station.getPiles(); // 懒加载触发N+1
}

// 优化后（使用JOIN FETCH）
@Query("SELECT s FROM ChargingStation s LEFT JOIN FETCH s.piles WHERE s.tenantId = :tenantId")
List<ChargingStation> findByTenantIdWithPiles(@Param("tenantId") Long tenantId);
```

**任务2: 添加缺失索引**
```sql
-- 检查当前索引
SELECT * FROM pg_indexes WHERE tablename = 'charging_station';

-- 添加复合索引（如有需要）
CREATE INDEX idx_station_tenant_status ON charging_station(tenant_id, status);
CREATE INDEX idx_pile_station_status ON charging_pile(station_id, status);
```

**任务3: 优化MyBatis查询**
- 检查是否有SELECT * （减少字段）
- 启用二级缓存（如适用）
- 使用批量操作替代循环查询

---

### 阶段2: 连接池调优（15:30-16:30）

**配置调整**:
```yaml
# evcs-station/src/main/resources/application-docker.yml
spring:
  datasource:
    hikari:
      minimum-idle: 10           # 5 → 10（增加常驻连接）
      maximum-pool-size: 30      # 20 → 30（提高并发能力）
      connection-timeout: 20000  # 30s → 20s（快速失败）
      idle-timeout: 300000       # 5分钟（回收空闲连接）
      max-lifetime: 600000       # 10分钟（强制重连）
      leak-detection-threshold: 60000  # 启用泄漏检测
```

**监控指标**:
```bash
# 连接池活跃连接数
curl -s http://localhost:8082/actuator/metrics/hikaricp.connections.active | jq

# 连接等待时间
curl -s http://localhost:8082/actuator/metrics/hikaricp.connections.pending | jq

# 连接使用率
curl -s http://localhost:8082/actuator/metrics/hikaricp.connections.usage | jq
```

---

### 阶段3: 验证测试（16:30-18:00）

**测试1: 基准对比**
```powershell
# 执行相同负载测试
.\simple-baseline.ps1 -Requests 500 -Concurrency 25

# 期望结果
# Station TPS: 1.14 → 3.0+
# 平均响应: 838ms → <300ms
# P99延迟: 419ms → <350ms
```

**测试2: 压力测试**
```powershell
# 更高并发
.\simple-baseline.ps1 -Requests 1000 -Concurrency 50

# 验证连接池不满载
```

**测试3: 长稳定性测试（可选）**
```powershell
# 30分钟测试
.\simple-long-test.ps1

# 检查：
# - 无连接泄漏
# - TPS稳定
# - 内存无增长
```

---

## 技术方案

### 1. 启用SQL日志

**application-docker.yml**:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        # 慢查询日志（>200ms）
        session:
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 200
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**收集慢查询**:
```bash
# 运行测试并收集日志
docker-compose restart station-service
.\simple-baseline.ps1 -Requests 500 -Concurrency 25
docker logs evcs-station --tail 1000 > station-sql-logs.txt

# 分析慢查询
grep -E "SELECT|UPDATE|INSERT|DELETE" station-sql-logs.txt | sort | uniq -c | sort -rn
```

---

### 2. 数据库查询优化技巧

**JOIN FETCH vs 懒加载**:
```java
// ❌ 触发N+1查询
@OneToMany(mappedBy = "station", fetch = FetchType.LAZY)
private List<ChargingPile> piles;

// 在循环中访问
stations.forEach(s -> s.getPiles().size()); // N+1!

// ✅ 使用JOIN FETCH
@Query("SELECT DISTINCT s FROM ChargingStation s LEFT JOIN FETCH s.piles WHERE s.tenantId = :tenantId")
List<ChargingStation> findWithPiles(@Param("tenantId") Long tenantId);
```

**批量查询**:
```java
// ❌ 循环查询
for (Long stationId : stationIds) {
    ChargingStation station = stationRepository.findById(stationId);
}

// ✅ 批量查询
List<ChargingStation> stations = stationRepository.findAllById(stationIds);
```

**分页查询**:
```java
// ❌ 一次性加载全部
List<ChargingStation> all = stationRepository.findAll();

// ✅ 分页加载
Page<ChargingStation> page = stationRepository.findAll(PageRequest.of(0, 50));
```

---

### 3. 索引优化策略

**检查现有索引**:
```sql
-- PostgreSQL索引查看
SELECT 
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
AND tablename IN ('charging_station', 'charging_pile', 'charging_order');

-- 索引使用率
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,  -- 使用次数
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

**添加关键索引**:
```sql
-- 租户隔离索引（最重要）
CREATE INDEX IF NOT EXISTS idx_station_tenant ON charging_station(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pile_tenant ON charging_pile(tenant_id);
CREATE INDEX IF NOT EXISTS idx_order_tenant ON charging_order(tenant_id);

-- 常用查询索引
CREATE INDEX IF NOT EXISTS idx_station_status ON charging_station(status) WHERE status = 'ONLINE';
CREATE INDEX IF NOT EXISTS idx_pile_station_status ON charging_pile(station_id, status);

-- 复合索引（覆盖多条件查询）
CREATE INDEX IF NOT EXISTS idx_station_tenant_status ON charging_station(tenant_id, status);
```

---

### 4. MyBatis优化

**使用LambdaQueryWrapper避免SELECT \***:
```java
// ❌ 查询所有字段
List<ChargingStation> stations = stationMapper.selectList(
    Wrappers.lambdaQuery(ChargingStation.class)
        .eq(ChargingStation::getTenantId, tenantId)
);

// ✅ 只查询需要的字段
List<ChargingStation> stations = stationMapper.selectList(
    Wrappers.lambdaQuery(ChargingStation.class)
        .select(ChargingStation::getId, ChargingStation::getName, ChargingStation::getStatus)
        .eq(ChargingStation::getTenantId, tenantId)
);
```

**批量操作**:
```java
// ❌ 循环插入
for (ChargingPile pile : piles) {
    pileMapper.insert(pile);
}

// ✅ 批量插入
pileService.saveBatch(piles, 100); // MyBatis Plus批量保存
```

---

## 成功标准

### 性能指标

**Station服务**:
- TPS: 1.14 → **3.0+** (提升2.6倍)
- 平均响应时间: 838ms → **<300ms** (降低64%)
- P99延迟: 419ms → **<350ms**
- 错误率: 保持0%

**连接池健康度**:
- 活跃连接数峰值: <25（最大30）
- 等待时间: <10ms
- 无连接泄漏警告

---

## 回滚计划

### 如果优化失败

**方案1: 回滚数据库更改**
```sql
-- 删除新增索引
DROP INDEX IF EXISTS idx_station_tenant_status;
DROP INDEX IF EXISTS idx_pile_station_status;
```

**方案2: 回滚连接池配置**
```bash
git checkout evcs-station/src/main/resources/application-docker.yml
docker-compose restart station-service
```

**方案3: 回滚代码优化**
```bash
git revert HEAD
./gradlew :evcs-station:build
docker-compose restart station-service
```

---

## 预期输出

### 文档

- [ ] `docs/week2-day4-database-optimization-report.md`（数据库优化报告）
- [ ] `docs/week2-day4-summary.md`（Day 4工作总结）
- [ ] `sql/performance-indexes.sql`（性能索引脚本）

### 配置更改

- [ ] `evcs-station/src/main/resources/application-docker.yml`（连接池配置）
- [ ] `sql/init.sql`（添加性能索引）

### 测试结果

- [ ] `performance-tests/results/day4-baseline-*.json`（优化前基线）
- [ ] `performance-tests/results/day4-optimized-*.json`（优化后对比）

---

## 风险评估

### 高风险 🔴

**索引过多导致写入性能下降**
- 缓解：只添加必要索引，监控写入TPS
- 回滚：立即删除新增索引

**连接池增大导致数据库压力**
- 缓解：逐步增加（20→25→30），监控数据库CPU
- 回滚：恢复原配置（max-20）

### 中风险 🟡

**JOIN FETCH导致笛卡尔积**
- 缓解：使用DISTINCT，限制深度（只fetch一层）
- 回滚：使用懒加载 + 批量查询

**慢查询日志影响性能**
- 缓解：仅在测试时启用，生产关闭
- 回滚：设置`show-sql: false`

---

## 时间线

| 时间 | 任务 | 负责人 | 输出 |
|------|------|--------|------|
| 09:00-10:00 | 启用SQL日志，收集查询 | Agent | station-sql-logs.txt |
| 10:00-11:00 | 分析慢查询，识别N+1 | Agent | 慢查询Top 5列表 |
| 11:00-12:00 | 检查索引，生成优化方案 | Agent | performance-indexes.sql |
| 13:00-14:30 | 修复慢查询，添加索引 | Agent | 代码提交 |
| 14:30-15:30 | 调整连接池配置 | Agent | 配置提交 |
| 15:30-16:30 | 执行性能测试 | Agent | day4-optimized-*.json |
| 16:30-17:30 | 压力测试，稳定性验证 | Agent | 测试报告 |
| 17:30-18:00 | 生成Day 4总结 | Agent | day4-summary.md |

---

## 下一步（Day 5）

### 如果Day 4成功
- [ ] 进一步压力测试（1000并发）
- [ ] 长稳定性测试（8小时）
- [ ] Week 2总结报告

### 如果Day 4效果有限
- [ ] 引入Redis缓存（充电站列表）
- [ ] 读写分离（主从复制）
- [ ] 分布式缓存（集群模式）

---

**计划编制**: GitHub Copilot  
**优先级**: 🔴 高优先级（Station是核心业务服务）  
**预计工作量**: 1天（8小时）  
**状态**: 📋 待执行 🚀

