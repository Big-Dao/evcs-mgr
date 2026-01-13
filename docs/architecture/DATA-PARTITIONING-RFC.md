# EVCS 海量数据处理方案 RFC

> **版本**: v1.1  
> **创建日期**: 2026-01-13  
> **维护者**: 数据架构组  
> **状态**: 草稿

---

## 1. 概述

### 1.1 背景

随着 EVCS 充电站管理系统运行时间增长，订单、支付、充电曲线等业务数据将持续累积至海量规模。当前系统尚未实现完整的海量数据处理机制，需要提前规划表分区、数据归档、读写分离、缓存策略等，以保障系统长期稳定运行。

### 1.2 目标

1. 建立基于 PostgreSQL 原生表分区的海量数据处理机制
2. 确保分区对应用层透明，无需修改业务代码
3. 定义数据保留策略与归档方案
4. 建立读写分离与缓存策略，优化查询性能
5. 规范慢查询治理与连接池管理
6. 完善灾备与恢复机制
7. 为未来可能的分库分表预留扩展点

### 1.3 范围

- **IN SCOPE**: 表分区设计、自动分区管理、数据归档策略、读写分离、缓存策略、慢查询治理、连接池管理、灾备恢复
- **OUT OF SCOPE**: 分库分表（ShardingSphere）、跨数据中心复制

---

## 2. 数据增长预估

### 2.1 业务规模基线

基于当前测试数据和业务预估：

| 指标 | 当前规模 | 年增量预估 | 5年累计 |
|------|----------|------------|---------|
| 运营商数量 | 18 | +5/年 | 43 |
| 充电站数量 | 200+ | +100/年 | 700+ |
| 充电桩数量 | 4,000+ | +2,000/年 | 14,000+ |
| 日订单量 | 5,000 | +20%/年 | 12,500 |
| 年订单量 | 180万 | - | 900万 |

### 2.2 表级数据增长分析

| 表名 | 日增量 | 年增量 | 5年累计 | 增长级别 |
|------|--------|--------|---------|----------|
| `charger_connector_curve_point` | 100万+ | 3.6亿+ | 18亿+ | 极高 |
| `user_behavior_event` | 10万+ | 3600万+ | 1.8亿+ | 极高 |
| `charging_order` | 5,000 | 180万 | 900万 | 高 |
| `payment_order` | 5,000 | 180万 | 900万 | 高 |
| `charger_connector_session` | 5,000 | 180万 | 900万 | 高 |
| `balance_transaction` | 10,000 | 360万 | 1800万 | 高 |
| `points_transaction` | 10,000 | 360万 | 1800万 | 高 |
| `user_login_log` | 20,000 | 720万 | 3600万 | 中高 |
| `user_message` | 15,000 | 540万 | 2700万 | 中高 |
| `user_coupon` | 2,000 | 72万 | 360万 | 中 |

---

## 3. 系统表分区优先级

### 3.1 分区优先级矩阵

基于数据增长速度和业务重要性，将 51 张表划分为 4 个优先级：

```
┌─────────────────────────────────────────────────────────────────┐
│                      分区优先级矩阵                               │
├─────────────────────────────────────────────────────────────────┤
│  P0 必须分区（7张）— 数据量达 100万 前实施                        │
│  ├── charger_connector_curve_point  → 按天，保留 90 天            │
│  ├── user_behavior_event            → 按天，保留 90 天            │
│  ├── charging_order                 → 按月，保留 24 个月          │
│  ├── payment_order                  → 按月，保留 24 个月          │
│  ├── charger_connector_session      → 按月，保留 12 个月          │
│  ├── balance_transaction            → 按月，保留 24 个月          │
│  └── points_transaction             → 按月，保留 24 个月          │
├─────────────────────────────────────────────────────────────────┤
│  P1 建议分区（9张）— 数据量达 500万 前实施                        │
│  ├── user_login_log                 → 按月                        │
│  ├── user_message                   → 按月                        │
│  ├── user_coupon                    → 按月                        │
│  ├── campaign_participation         → 按月                        │
│  ├── user_task_progress             → 按月                        │
│  ├── user_sign_in                   → 按月                        │
│  ├── station_review                 → 按季度                      │
│  ├── tenant_audit_log               → 按季度                      │
│  └── user_profile_snapshot          → 按月                        │
├─────────────────────────────────────────────────────────────────┤
│  P2 可选分区（6张）— 按需实施                                     │
│  ├── complaint / complaint_record   → 按季度                      │
│  ├── reconciliation_task / _exception → 按月                      │
│  ├── user_invitation                → 按季度                      │
│  └── phone_bindchange_log           → 按季度                      │
├─────────────────────────────────────────────────────────────────┤
│  P3 无需分区（29张）— 配置/主数据表                               │
│  └── sys_*, charging_station, charger, charging_user 等          │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 P0 表分区详细设计

#### 3.2.1 `charger_connector_curve_point`（充电曲线采样点）

**特点**: 时序数据，每秒采样，增长最快

```sql
-- 改为分区表（按天）
CREATE TABLE charger_connector_curve_point (
    charger_connector_curve_point_id BIGSERIAL,
    tenant_id BIGINT NOT NULL,
    charger_id BIGINT NOT NULL,
    connector_no INTEGER NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    sample_time TIMESTAMP NOT NULL,
    voltage NUMERIC(10,2),
    current_value NUMERIC(10,2),
    power NUMERIC(12,4),
    soc NUMERIC(5,2),
    energy NUMERIC(12,4),
    duration_seconds BIGINT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0,
    PRIMARY KEY (charger_connector_curve_point_id, sample_time)
) PARTITION BY RANGE (sample_time);

-- 自动创建每日分区（示例）
CREATE TABLE charger_connector_curve_point_2026_01_13
    PARTITION OF charger_connector_curve_point
    FOR VALUES FROM ('2026-01-13') TO ('2026-01-14');
```

**保留策略**: 在线 90 天，之后归档到对象存储

#### 3.2.2 `user_behavior_event`（用户行为事件）

**特点**: 事件流数据，高频写入

```sql
CREATE TABLE user_behavior_event (
    id BIGSERIAL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    event_data JSONB,
    device_type VARCHAR(20),
    ip VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, event_time)
) PARTITION BY RANGE (event_time);
```

**保留策略**: 在线 90 天，归档后保留 1 年

#### 3.2.3 `charging_order`（充电订单）

**特点**: 核心业务表，需长期查询

```sql
CREATE TABLE charging_order (
    id BIGSERIAL,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT,
    charger_id BIGINT,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    energy DECIMAL(12,4),
    duration BIGINT,
    amount DECIMAL(12,4),
    billing_plan_id BIGINT,
    coupon_id BIGINT,
    discount_amount DECIMAL(12,4) DEFAULT 0,
    pay_amount DECIMAL(12,4),
    payment_trade_id VARCHAR(100),
    paid_time TIMESTAMP,
    status INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0,
    PRIMARY KEY (id, start_time)
) PARTITION BY RANGE (start_time);

-- 按月创建分区
CREATE TABLE charging_order_2026_01
    PARTITION OF charging_order
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

**保留策略**: 在线 24 个月，归档后保留 7 年（财务合规）

#### 3.2.4 `payment_order`（支付订单）

**分区策略**: 与 `charging_order` 一致，按月分区

**保留策略**: 在线 24 个月，归档后保留 7 年

#### 3.2.5 `charger_connector_session`（充电会话）

**分区策略**: 按月分区，基于 `start_time`

**保留策略**: 在线 12 个月，归档后保留 3 年

#### 3.2.6 `balance_transaction` / `points_transaction`（余额/积分流水）

**分区策略**: 按月分区，基于 `create_time`

**保留策略**: 在线 24 个月，归档后保留 7 年

---

## 4. 自动分区管理

### 4.1 分区自动创建函数

```sql
-- 通用分区创建函数（按月）
CREATE OR REPLACE FUNCTION create_monthly_partition(
    p_table_name TEXT,
    p_partition_column TEXT,
    p_year INTEGER,
    p_month INTEGER
) RETURNS VOID AS $$
DECLARE
    v_partition_name TEXT;
    v_start_date DATE;
    v_end_date DATE;
BEGIN
    v_partition_name := p_table_name || '_' || p_year || '_' || LPAD(p_month::TEXT, 2, '0');
    v_start_date := make_date(p_year, p_month, 1);
    v_end_date := v_start_date + INTERVAL '1 month';
    
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
        v_partition_name, p_table_name, v_start_date, v_end_date
    );
    
    RAISE NOTICE '分区 % 创建成功', v_partition_name;
END;
$$ LANGUAGE plpgsql;

-- 通用分区创建函数（按天）
CREATE OR REPLACE FUNCTION create_daily_partition(
    p_table_name TEXT,
    p_partition_column TEXT,
    p_date DATE
) RETURNS VOID AS $$
DECLARE
    v_partition_name TEXT;
BEGIN
    v_partition_name := p_table_name || '_' || to_char(p_date, 'YYYY_MM_DD');
    
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
        v_partition_name, p_table_name, p_date, p_date + 1
    );
    
    RAISE NOTICE '分区 % 创建成功', v_partition_name;
END;
$$ LANGUAGE plpgsql;
```

### 4.2 分区预创建定时任务

```sql
-- 每日执行：为未来 7 天创建每日分区
CREATE OR REPLACE FUNCTION maintain_daily_partitions() RETURNS VOID AS $$
DECLARE
    v_date DATE;
    v_tables TEXT[] := ARRAY['charger_connector_curve_point', 'user_behavior_event'];
    v_table TEXT;
BEGIN
    FOREACH v_table IN ARRAY v_tables LOOP
        FOR i IN 0..7 LOOP
            v_date := CURRENT_DATE + i;
            PERFORM create_daily_partition(v_table, 'sample_time', v_date);
        END LOOP;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 每月 1 号执行：为未来 3 个月创建月度分区
CREATE OR REPLACE FUNCTION maintain_monthly_partitions() RETURNS VOID AS $$
DECLARE
    v_year INTEGER;
    v_month INTEGER;
    v_tables TEXT[] := ARRAY[
        'charging_order', 'payment_order', 'charger_connector_session',
        'balance_transaction', 'points_transaction', 'user_login_log',
        'user_message', 'user_coupon', 'campaign_participation',
        'user_task_progress', 'user_sign_in', 'user_profile_snapshot'
    ];
    v_table TEXT;
BEGIN
    FOREACH v_table IN ARRAY v_tables LOOP
        FOR i IN 0..3 LOOP
            v_year := EXTRACT(YEAR FROM (CURRENT_DATE + (i || ' month')::INTERVAL));
            v_month := EXTRACT(MONTH FROM (CURRENT_DATE + (i || ' month')::INTERVAL));
            PERFORM create_monthly_partition(v_table, 'create_time', v_year, v_month);
        END LOOP;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
```

### 4.3 pg_cron 调度配置

```sql
-- 安装 pg_cron 扩展
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- 每日凌晨 2 点维护每日分区
SELECT cron.schedule('maintain_daily_partitions', '0 2 * * *', 'SELECT maintain_daily_partitions()');

-- 每月 1 号凌晨 3 点维护月度分区
SELECT cron.schedule('maintain_monthly_partitions', '0 3 1 * *', 'SELECT maintain_monthly_partitions()');
```

---

## 5. 数据归档策略

### 5.1 归档保留策略

| 数据类型 | 在线保留 | 归档保留 | 归档方式 |
|----------|----------|----------|----------|
| 充电曲线 | 90 天 | 1 年 | Parquet → 对象存储 |
| 行为事件 | 90 天 | 1 年 | Parquet → 对象存储 |
| 充电订单 | 24 个月 | 7 年 | 归档表 → 对象存储 |
| 支付订单 | 24 个月 | 7 年 | 归档表 |
| 充电会话 | 12 个月 | 3 年 | 归档表 |
| 流水记录 | 24 个月 | 7 年 | 归档表 |
| 审计日志 | 12 个月 | 永久 | 归档表 |
| 登录日志 | 6 个月 | 2 年 | 归档表 |

### 5.2 归档执行函数

```sql
-- 归档旧分区（示例：充电订单）
CREATE OR REPLACE FUNCTION archive_old_partitions(
    p_table_name TEXT,
    p_retention_months INTEGER
) RETURNS INTEGER AS $$
DECLARE
    v_cutoff_date DATE;
    v_partition RECORD;
    v_count INTEGER := 0;
BEGIN
    v_cutoff_date := (CURRENT_DATE - (p_retention_months || ' months')::INTERVAL)::DATE;
    
    FOR v_partition IN
        SELECT inhrelid::regclass::text AS partition_name
        FROM pg_inherits
        WHERE inhparent = p_table_name::regclass
    LOOP
        -- 检查分区是否早于截止日期
        IF v_partition.partition_name ~ '_\d{4}_\d{2}$' THEN
            -- 导出到归档表（或对象存储）
            EXECUTE format('
                INSERT INTO %I_archive 
                SELECT * FROM %I',
                p_table_name, v_partition.partition_name
            );
            
            -- 删除旧分区
            EXECUTE format('DROP TABLE IF EXISTS %I', v_partition.partition_name);
            v_count := v_count + 1;
        END IF;
    END LOOP;
    
    RETURN v_count;
END;
$$ LANGUAGE plpgsql;
```

---

## 6. 应用层适配

### 6.1 查询透明性

**关键点**: 分区对应用层完全透明

- 应用代码无需修改，仍然查询父表
- PostgreSQL 自动路由到正确分区
- `WHERE` 条件中包含分区键时，自动进行分区裁剪

```java
// 应用代码无需变化
@Select("SELECT * FROM charging_order WHERE tenant_id = #{tenantId} AND start_time BETWEEN #{start} AND #{end}")
List<ChargingOrder> findByTimeRange(@Param("tenantId") Long tenantId, 
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
```

### 6.2 查询优化建议

1. **强制时间范围**: 查询大表时必须带时间范围条件
2. **分区键优先**: `WHERE` 条件中优先使用分区键
3. **避免跨分区 JOIN**: 尽量在同分区内完成关联查询

```java
// 推荐：带时间范围
List<Order> orders = orderMapper.findByTimeRange(tenantId, startTime, endTime);

// 不推荐：无时间范围（可能扫描所有分区）
List<Order> orders = orderMapper.findAll(tenantId);
```

---

## 7. 实施计划

### Phase 1: P0 表分区（Week 1-2）

| 任务 | 预计工时 | 负责人 |
|------|----------|--------|
| 创建分区管理函数 | 1天 | DBA |
| 迁移 `charger_connector_curve_point` | 2天 | DBA |
| 迁移 `user_behavior_event` | 1天 | DBA |
| 迁移 `charging_order` / `payment_order` | 2天 | DBA |
| 迁移 `charger_connector_session` | 1天 | DBA |
| 迁移流水表 | 1天 | DBA |
| 配置 pg_cron 定时任务 | 0.5天 | DBA |
| 验证与性能测试 | 1.5天 | QA |

### Phase 2: P1 表分区（Week 3-4）

| 任务 | 预计工时 |
|------|----------|
| 迁移 9 张 P1 表 | 3天 |
| 归档策略实施 | 2天 |
| 监控告警配置 | 1天 |

### Phase 3: 归档机制（Week 5-6）

| 任务 | 预计工时 |
|------|----------|
| 归档表创建 | 1天 |
| 归档定时任务 | 2天 |
| 对象存储集成 | 3天 |
| 归档数据查询接口 | 2天 |

---

## 8. 监控与告警

### 8.1 监控指标

| 指标 | 告警阈值 | 说明 |
|------|----------|------|
| 分区数量 | > 100 | 单表分区过多 |
| 单分区大小 | > 10GB | 分区过大 |
| 分区创建失败 | = 1 | 自动创建失败 |
| 归档延迟 | > 7 天 | 归档任务延迟 |

### 8.2 Prometheus 指标

```sql
-- 分区统计视图
CREATE VIEW v_partition_stats AS
SELECT 
    parent.relname AS table_name,
    COUNT(*) AS partition_count,
    pg_size_pretty(SUM(pg_relation_size(child.oid))) AS total_size
FROM pg_inherits
JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
JOIN pg_class child ON pg_inherits.inhrelid = child.oid
GROUP BY parent.relname
ORDER BY SUM(pg_relation_size(child.oid)) DESC;
```

---

## 9. 读写分离方案

### 9.1 架构设计

```
┌─────────────────┐     流复制      ┌─────────────────┐
│     主库        │───────────────▶│    只读副本      │
│  (读写操作)     │                 │  (报表/导出)     │
└─────────────────┘                 └─────────────────┘
        │                                   │
        ▼                                   ▼
   业务写入/实时查询               统计报表/批量导出/跨租户分析
```

### 9.2 实施阶段

| 阶段 | 数据量 | 方案 |
|------|--------|------|
| 当前 | < 1000万 | 单库 + 分区 |
| 中期 | 1000万-1亿 | 读写分离 + TimescaleDB（时序数据） |
| 远期 | > 1亿 | ShardingSphere 分库分表 |

### 9.3 应用层适配

```java
// 使用 @Transactional(readOnly = true) 路由到只读库
@Transactional(readOnly = true)
public List<OrderStatistics> getStatistics(Long tenantId, LocalDate date) {
    return orderMapper.selectStatistics(tenantId, date);
}
```

---

## 10. 缓存策略

### 10.1 缓存层级

| 层级 | 数据类型 | TTL | 失效策略 |
|------|----------|-----|----------|
| L1 本地缓存 | 计费方案、系统配置 | 5分钟 | 变更事件广播 |
| L2 Redis | 站点/桩信息、用户会话 | 30分钟 | 主动失效 |
| L3 预计算 | 订单统计、报表数据 | 1小时 | 定时刷新 |

### 10.2 缓存键设计

```
# 站点信息
station:{tenantId}:{stationId}

# 用户会话
user:session:{userId}

# 订单统计（预计算）
order:stats:{tenantId}:{date}
```

### 10.3 缓存穿透/雪崩防护

| 问题 | 解决方案 |
|------|----------|
| 缓存穿透 | 布隆过滤器 + 空值缓存（TTL 1分钟） |
| 缓存雪崩 | TTL 随机化（±10%）+ 限流降级 |
| 缓存击穿 | 互斥锁（Redisson）+ 热点数据预加载 |

---

## 11. 慢查询治理

### 11.1 强制规范

| 规范 | 说明 |
|------|------|
| **强制时间范围** | 订单/日志/流水查询必须带 `start_time`/`end_time` |
| **分页上限** | 单次最多 1000 条，禁止无界查询 |
| **查询超时** | `statement_timeout = 30s` |
| **禁止全表扫描** | 关键表必须有覆盖索引 |

### 11.2 PostgreSQL 配置

```sql
-- 慢查询日志
ALTER SYSTEM SET log_min_duration_statement = 1000; -- 超过1秒记录
ALTER SYSTEM SET log_statement = 'none'; -- 不记录所有语句
ALTER SYSTEM SET statement_timeout = '30s'; -- 查询超时

SELECT pg_reload_conf();
```

### 11.3 慢查询监控

```sql
-- 查看当前慢查询
SELECT pid, now() - pg_stat_activity.query_start AS duration, query
FROM pg_stat_activity
WHERE state != 'idle' 
  AND now() - pg_stat_activity.query_start > interval '5 seconds';

-- 查看最耗时的查询（需要 pg_stat_statements 扩展）
SELECT query, calls, total_time, mean_time, rows
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

---

## 12. 连接池与 PgBouncer

### 12.1 HikariCP 配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30          # 最大连接数
      minimum-idle: 10               # 最小空闲连接
      connection-timeout: 30000      # 获取连接超时(ms)
      idle-timeout: 600000           # 空闲连接回收(10分钟)
      max-lifetime: 1800000          # 连接最大生命周期(30分钟)
      leak-detection-threshold: 60000 # 连接泄漏检测(1分钟)
```

### 12.2 PgBouncer 部署（超高并发）

当连接数超过数据库承载能力时，引入 PgBouncer：

```ini
[databases]
evcs = host=postgres port=5432 dbname=evcs

[pgbouncer]
pool_mode = transaction          # 事务模式
max_client_conn = 1000           # 最大客户端连接
default_pool_size = 50           # 每个数据库的连接池大小
reserve_pool_size = 10           # 预留连接
reserve_pool_timeout = 3         # 预留连接超时(秒)
```

---

## 13. 批量操作优化

### 13.1 批量插入

```java
// 推荐：使用 COPY 或批量 VALUES
@Insert("<script>" +
    "INSERT INTO charging_order (tenant_id, station_id, ...) VALUES " +
    "<foreach collection='list' item='item' separator=','>" +
    "(#{item.tenantId}, #{item.stationId}, ...)" +
    "</foreach>" +
    "</script>")
void batchInsert(@Param("list") List<ChargingOrder> orders);
```

### 13.2 流式导出

```java
// 使用游标分批导出，避免内存溢出
@Options(fetchSize = 1000)
@Select("SELECT * FROM charging_order WHERE tenant_id = #{tenantId} AND start_time BETWEEN #{start} AND #{end}")
Cursor<ChargingOrder> exportOrders(@Param("tenantId") Long tenantId, 
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);
```

---

## 14. 索引管理

### 14.1 索引维护

```sql
-- 查看索引使用情况
SELECT schemaname, relname, indexrelname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;

-- 查看索引膨胀
SELECT schemaname, tablename, indexname, 
       pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC;

-- 在线重建索引（不阻塞写入）
REINDEX INDEX CONCURRENTLY idx_order_tenant_status;
```

### 14.2 索引清理策略

| 条件 | 操作 |
|------|------|
| `idx_scan = 0` 超过 30 天 | 考虑删除 |
| 索引膨胀 > 50% | `REINDEX CONCURRENTLY` |
| 重复索引 | 保留最优，删除冗余 |

---

## 15. 灾备与恢复

### 15.1 备份策略

| 类型 | 频率 | 保留 | 工具 |
|------|------|------|------|
| 物理全量备份 | 每日 | 7 天 | `pg_basebackup` |
| 逻辑备份（核心表） | 每日 | 30 天 | `pg_dump` |
| WAL 归档 | 持续 | 7 天 | `archive_command` |

### 15.2 恢复目标

| 指标 | 目标值 |
|------|--------|
| RPO（数据丢失容忍） | < 1 分钟 |
| RTO（恢复时间目标） | < 30 分钟 |

### 15.3 备份脚本示例

```bash
#!/bin/bash
# 每日物理备份
BACKUP_DIR="/backup/pg/$(date +%Y-%m-%d)"
mkdir -p $BACKUP_DIR
pg_basebackup -h localhost -U postgres -D $BACKUP_DIR -Ft -z -P

# 保留 7 天
find /backup/pg -type d -mtime +7 -exec rm -rf {} \;
```

---

## 16. 数据压缩

### 16.1 PostgreSQL TOAST 压缩

PostgreSQL 默认对大字段（> 2KB）自动 TOAST 压缩，无需额外配置。

### 16.2 归档数据压缩

| 数据类型 | 格式 | 压缩率 |
|----------|------|--------|
| 充电曲线 | Parquet + Zstd | ~10:1 |
| 订单归档 | CSV + Gzip | ~5:1 |
| 日志归档 | JSON + Gzip | ~8:1 |

---

## 17. 监控告警增强

### 17.1 数据库健康指标

| 指标 | 告警阈值 | 说明 |
|------|----------|------|
| 连接数使用率 | > 80% | 考虑扩容或引入 PgBouncer |
| 表膨胀率 | > 50% | 执行 VACUUM FULL |
| 死元组比例 | > 20% | 检查 autovacuum 配置 |
| 慢查询数/分钟 | > 10 | 查询优化 |
| WAL 堆积 | > 10GB | 检查复制延迟 |
| 磁盘使用率 | > 85% | 扩容或清理归档 |

### 17.2 Prometheus 采集配置

```yaml
# postgres_exporter 配置
scrape_configs:
  - job_name: 'postgresql'
    static_configs:
      - targets: ['postgres-exporter:9187']
```

---

## 18. 风险与应对

| 风险 | 等级 | 应对措施 |
|------|------|----------|
| 分区迁移期间服务中断 | 🔴 | 低峰期执行；分批迁移；回滚方案 |
| 分区自动创建失败 | 🟠 | 提前 7 天创建；告警通知；手动补救 |
| 归档数据丢失 | 🔴 | 双重备份；归档前校验；对象存储版本控制 |
| 跨分区查询性能下降 | 🟠 | 强制时间范围；查询改写指导 |
| 连接池耗尽 | 🟠 | 监控连接数；引入 PgBouncer |
| 缓存雪崩 | 🟠 | TTL 随机化；限流降级 |

---

## 19. 相关文档

- [数据库设计规范](../development/DATABASE-DESIGN-STANDARDS.md)
- [C端用户模块 RFC](./EVCS-USER-MODULE-RFC.md)
- [项目编码规范](../overview/PROJECT-CODING-STANDARDS.md)
- [部署指南](../deployment/DEPLOYMENT-GUIDE.md)
- [监控指南](../operations/MONITORING-GUIDE.md)

---

## 20. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.1 | 新增读写分离、缓存策略、慢查询治理、连接池、灾备等章节 |
| 2026-01-13 | v1.0 | 初始版本，完成 51 张表分区分析 |
