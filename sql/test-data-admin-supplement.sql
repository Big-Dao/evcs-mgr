-- ============================================================================
-- 为 admin 用户补充测试数据至目标数量
-- 当前状态: 站点5, 充电桩0, 计费方案30, 订单168
-- 目标状态: 站点50+, 充电桩400+, 计费方案30+, 订单150+
-- ============================================================================

\echo '=== 开始为 admin 用户补充测试数据 ==='

-- 1. 补充45个充电站 (从当前5个补到50个)
\echo '补充充电站至50个...'
INSERT INTO charging_station (
    tenant_id, station_code, station_name, station_type, address,
    province, city, district, longitude, latitude, status,
    operator_name, operator_phone, service_hours, parking_fee, facilities,
    create_time, update_time, create_by, update_by, deleted
)
SELECT 
    1,
    'SYS-ST-' || LPAD(gs::TEXT, 3, '0'),
    '系统 - ' || (ARRAY['机场充电站', '高铁站充电站', 'CBD商圈站', '科技园区站', '住宅小区站', 
                         '商业中心站', '工业园区站', '服务区站', '景区站点', '公共停车场站'])[1 + (gs % 10)] || gs::TEXT,
    CASE WHEN gs % 3 = 0 THEN 2 ELSE 1 END,
    (ARRAY['深圳市', '广州市', '北京市', '上海市', '成都市', '杭州市'])[1 + (gs % 6)] || '某某区某某路' || (100 + gs)::TEXT || '号',
    CASE gs % 6
        WHEN 0 THEN '广东省'
        WHEN 1 THEN '广东省'
        WHEN 2 THEN '北京市'
        WHEN 3 THEN '上海市'
        WHEN 4 THEN '四川省'
        ELSE '浙江省'
    END,
    (ARRAY['深圳市', '广州市', '北京市', '上海市', '成都市', '杭州市'])[1 + (gs % 6)],
    (ARRAY['南山区', '福田区', '朝阳区', '浦东新区', '天河区'])[1 + (gs % 5)],
    113.0 + (gs % 10) * 0.15,
    22.5 + (gs % 8) * 0.08,
    1,
    '系统运营商',
    '400-' || (9000000 + gs)::TEXT,
    CASE WHEN gs % 5 = 0 THEN '06:00-23:00' ELSE '00:00-24:00' END,
    CASE WHEN gs % 3 = 0 THEN 0.00 ELSE (gs % 3) * 5.00 END,
    '["wifi", "休息室", "便利店"]'::jsonb,
    NOW() - (gs || ' days')::INTERVAL,
    NOW() - ((gs % 10) || ' hours')::INTERVAL,
    1, 1, 0
FROM generate_series(1, 45) AS gs;

SELECT COUNT(*) || ' 个充电站（总数）' FROM charging_station WHERE tenant_id = 1;

-- 2. 为所有站点生成充电桩（每站8个）
\echo '为所有站点生成充电桩...'
INSERT INTO charger (
    tenant_id, station_id, charger_code, charger_name, charger_type,
    brand, model, manufacturer, rated_power, input_voltage,
    output_voltage_range, output_current_range, gun_count, gun_types,
    supported_protocols, status, enabled,
    create_time, update_time, create_by, update_by, deleted
)
SELECT 
    1,
    cs.station_id,
    cs.station_code || '-C' || LPAD(gs::TEXT, 2, '0'),
    REPLACE(cs.station_name, '系统 - ', '') || '-' || gs::TEXT || '号' ||
        CASE WHEN gs <= 5 THEN '快充' ELSE '慢充' END,
    CASE WHEN gs <= 5 THEN 1 ELSE 2 END,
    (ARRAY['特来电', '星星充电', '云快充', 'ABB'])[1 + (gs % 4)],
    CASE WHEN gs <= 5 THEN 'DC-' || (120 + gs * 10)::TEXT || 'kW'
         ELSE 'AC-' || (7 + (gs % 2) * 4)::TEXT || 'kW'
    END,
    '制造商' || (gs % 4)::TEXT,
    CASE WHEN gs <= 5 THEN 120.0 + gs * 10 ELSE 7.0 + (gs % 2) * 4 END,
    CASE WHEN gs <= 5 THEN 380 ELSE 220 END,
    CASE WHEN gs <= 5 THEN '200-1000V' ELSE '220V' END,
    CASE WHEN gs <= 5 THEN '0-500A' ELSE '0-32A' END,
    CASE WHEN gs <= 5 THEN 2 ELSE 1 END,
    CASE WHEN gs <= 5 THEN 'CCS,CHAdeMO' ELSE 'Type 2' END,
    '{"gb": "2015", "ocpp": "1.6"}'::jsonb,
    CASE 
        WHEN gs % 10 = 0 THEN 3
        WHEN gs % 6 = 0 THEN 2
        ELSE 1
    END,
    1,
    NOW() - ((cs.station_id % 60) || ' days')::INTERVAL,
    NOW() - ((gs % 20) || ' hours')::INTERVAL,
    1, 1, 0
FROM charging_station cs
CROSS JOIN generate_series(1, 8) AS gs
WHERE cs.tenant_id = 1;

SELECT COUNT(*) || ' 个充电桩（总数）' FROM charger WHERE tenant_id = 1;

-- 3. 检查计费方案（应该已有30个）
SELECT COUNT(*) || ' 个计费方案（总数）' FROM billing_plan WHERE tenant_id = 1;

-- 4. 检查计费时段
SELECT COUNT(*) || ' 个计费时段（总数）' 
FROM billing_plan_segment bps
JOIN billing_plan bp ON bps.plan_id = bp.id
WHERE bp.tenant_id = 1;

-- 5. 检查订单（应该已有150+）
SELECT COUNT(*) || ' 个充电订单（总数）' FROM charging_order WHERE tenant_id = 1;

-- 最终统计
\echo ''
\echo '=== 数据补充完成，最终统计 ==='
SELECT 
    '充电站' as 类型, 
    COUNT(*) as 数量 
FROM charging_station 
WHERE tenant_id = 1
UNION ALL
SELECT 
    '充电桩' as 类型, 
    COUNT(*) as 数量 
FROM charger 
WHERE tenant_id = 1
UNION ALL
SELECT 
    '计费方案' as 类型, 
    COUNT(*) as 数量 
FROM billing_plan 
WHERE tenant_id = 1
UNION ALL
SELECT 
    '计费时段' as 类型, 
    COUNT(*) as 数量 
FROM billing_plan_segment bps
JOIN billing_plan bp ON bps.plan_id = bp.id
WHERE bp.tenant_id = 1
UNION ALL
SELECT 
    '充电订单' as 类型, 
    COUNT(*) as 数量 
FROM charging_order 
WHERE tenant_id = 1;

-- 订单状态分布
\echo ''
\echo '订单状态分布:'
SELECT 
    CASE status
        WHEN 0 THEN '已取消'
        WHEN 1 THEN '充电中'
        WHEN 10 THEN '待支付'
        WHEN 11 THEN '已支付'
        ELSE '其他'
    END as 订单状态,
    COUNT(*) as 数量,
    ROUND(COUNT(*) * 100.0 / 
        NULLIF((SELECT COUNT(*) FROM charging_order WHERE tenant_id = 1), 0), 1) 
    as 百分比
FROM charging_order 
WHERE tenant_id = 1
GROUP BY status
ORDER BY status;

\echo ''
\echo '=== 完成！admin 用户现在拥有充足的测试数据 ==='
