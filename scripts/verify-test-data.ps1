# 测试数据验证脚本
# 用于快速查看导入的测试数据统计

Write-Host "`n========================================"
Write-Host "充电站管理平台 - 数据统计"
Write-Host "========================================`n"

# 连接数据库并查询
$dbContainer = "evcs-postgres"
$dbUser = "postgres"
$dbName = "evcs_mgr"

# 1. 租户统计
Write-Host "【租户统计】"
docker exec -i $dbContainer psql -U $dbUser -d $dbName -t -c @"
SELECT 
    CASE tenant_type WHEN 1 THEN '平台方' ELSE '运营商' END || ': ' || COUNT(*) || ' 个'
FROM sys_tenant 
WHERE deleted=0 AND id >= 100
GROUP BY tenant_type 
ORDER BY tenant_type;
"@

# 2. 充电站统计
Write-Host "`n【充电站统计】"
docker exec -i $dbContainer psql -U $dbUser -d $dbName -t -c @"
SELECT 
    '总数: ' || COUNT(*) || ' 个, ' ||
    '公共站: ' || COUNT(*) FILTER(WHERE station_type=1) || ' 个, ' ||
    '专用站: ' || COUNT(*) FILTER(WHERE station_type=2) || ' 个'
FROM charging_station 
WHERE deleted=0 AND tenant_id >= 100;
"@

# 3. 充电桩统计
Write-Host "`n【充电桩统计】"
docker exec -i $dbContainer psql -U $dbUser -d $dbName -t -c @"
SELECT 
    '总数: ' || COUNT(*) || ' 个'
FROM charger 
WHERE deleted=0 AND tenant_id >= 100;

SELECT 
    '  ' || 
    CASE charger_type WHEN 1 THEN 'DC快充' ELSE 'AC慢充' END || ': ' || 
    COUNT(*) || ' 个'
FROM charger 
WHERE deleted=0 AND tenant_id >= 100
GROUP BY charger_type 
ORDER BY charger_type;

SELECT 
    '  ' || 
    CASE status 
        WHEN 0 THEN '离线'
        WHEN 1 THEN '空闲'
        WHEN 2 THEN '充电中'
        WHEN 3 THEN '故障'
    END || ': ' || 
    COUNT(*) || ' 个 (' || 
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM charger WHERE deleted=0 AND tenant_id>=100), 1) || '%)'
FROM charger 
WHERE deleted=0 AND tenant_id >= 100
GROUP BY status 
ORDER BY status;
"@

# 4. 用户统计
Write-Host "`n【用户统计】"
docker exec -i $dbContainer psql -U $dbUser -d $dbName -t -c @"
SELECT '总数: ' || COUNT(*) || ' 个'
FROM sys_user 
WHERE deleted=0 AND tenant_id >= 100;
"@

# 5. 今日订单统计
Write-Host "`n【今日订单统计】"
docker exec -i $dbContainer psql -U $dbUser -d $dbName -t -c @"
SELECT 
    '订单总数: ' || COUNT(*) || ' 笔'
FROM charging_order 
WHERE deleted=0 
  AND tenant_id >= 100
  AND start_time >= date_trunc('day', CURRENT_TIMESTAMP);

SELECT 
    '  ' ||
    CASE status 
        WHEN 1 THEN '充电中'
        WHEN 11 THEN '已支付'
        ELSE '其他'
    END || ': ' || 
    COUNT(*) || ' 笔'
FROM charging_order 
WHERE deleted=0 
  AND tenant_id >= 100
  AND start_time >= date_trunc('day', CURRENT_TIMESTAMP)
GROUP BY status 
ORDER BY status;

SELECT 
    '今日充电量: ' || ROUND(COALESCE(SUM(energy), 0)::numeric, 2) || ' kWh'
FROM charging_order 
WHERE deleted=0 
  AND tenant_id >= 100
  AND start_time >= date_trunc('day', CURRENT_TIMESTAMP)
  AND status = 11;

SELECT 
    '今日收入: ¥' || ROUND(COALESCE(SUM(amount), 0)::numeric, 2)
FROM charging_order 
WHERE deleted=0 
  AND tenant_id >= 100
  AND start_time >= date_trunc('day', CURRENT_TIMESTAMP)
  AND status = 11;
"@

# 6. TOP 5 运营商
Write-Host "`n【TOP 5 运营商（按充电桩数量）】"
docker exec -i $dbContainer psql -U $dbUser -d $dbName -c @"
SELECT 
    t.tenant_name as 运营商名称,
    (SELECT COUNT(*) FROM charging_station WHERE tenant_id=t.id AND deleted=0) as 充电站,
    (SELECT COUNT(*) FROM charger WHERE tenant_id=t.id AND deleted=0) as 充电桩,
    (SELECT COUNT(*) FROM charging_order WHERE tenant_id=t.id AND deleted=0 
     AND start_time >= date_trunc('day', CURRENT_TIMESTAMP)) as 今日订单
FROM sys_tenant t
WHERE t.tenant_type=2 AND t.deleted=0 AND t.id >= 100
ORDER BY 充电桩 DESC
LIMIT 5;
"@

Write-Host "`n========================================"
Write-Host "数据验证完成！"
Write-Host "========================================`n"
