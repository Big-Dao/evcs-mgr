-- EVCS性能优化索引脚本
-- 创建日期: 2025-10-25
-- 目的: 优化Station服务数据库查询性能

-- ============================================
-- 充电桩表 (charger) 索引
-- ============================================

-- 租户隔离索引（最重要，MyBatis Plus租户拦截器会用到）
CREATE INDEX IF NOT EXISTS idx_charger_tenant ON charger(tenant_id);

-- 充电站关联索引（查询某站点下的所有充电桩）
CREATE INDEX IF NOT EXISTS idx_charger_station ON charger(station_id);

-- 复合索引：站点+状态（常用查询：查找某站点下在线/空闲的充电桩）
CREATE INDEX IF NOT EXISTS idx_charger_station_status ON charger(station_id, status);

-- 复合索引：租户+状态+删除标记（租户下所有在线充电桩）
CREATE INDEX IF NOT EXISTS idx_charger_tenant_status ON charger(tenant_id, status, deleted);

-- ============================================
-- 充电订单表 (charging_order) 索引优化
-- ============================================

-- 租户隔离索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_order_tenant ON charging_order(tenant_id);

-- 复合索引：租户+状态（查询某租户的进行中订单）
CREATE INDEX IF NOT EXISTS idx_order_tenant_status ON charging_order(tenant_id, status);

-- 充电桩关联索引（查询某充电桩的历史订单）
CREATE INDEX IF NOT EXISTS idx_order_charger ON charging_order(charger_id);

-- 创建时间索引（按时间范围查询订单）
CREATE INDEX IF NOT EXISTS idx_order_start_time ON charging_order(start_time);

-- ============================================
-- 索引使用情况检查
-- ============================================

-- 查看索引使用统计
SELECT 
    s.schemaname,
    s.relname AS tablename,
    s.indexrelname AS indexname,
    s.idx_scan AS index_scans,
    s.idx_tup_read AS tuples_read,
    s.idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes s
WHERE s.schemaname = 'public'
AND s.relname IN ('charging_station', 'charger', 'charging_order')
ORDER BY s.relname, s.idx_scan DESC;

-- 查看表扫描统计（发现缺失索引）
SELECT 
    s.schemaname,
    s.relname AS tablename,
    s.seq_scan AS sequential_scans,
    s.seq_tup_read AS rows_read_sequentially,
    s.idx_scan AS index_scans,
    s.idx_tup_fetch AS rows_fetched_by_index,
    s.n_tup_ins AS rows_inserted,
    s.n_tup_upd AS rows_updated,
    s.n_tup_del AS rows_deleted
FROM pg_stat_user_tables s
WHERE s.schemaname = 'public'
AND s.relname IN ('charging_station', 'charger', 'charging_order')
ORDER BY s.seq_scan DESC;
