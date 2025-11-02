-- ============================================================================
-- Admin 用户大量测试数据生成 - 最终版本
-- 用户: admin (ID: 1), 租户: 系统租户 (ID: 1)
-- 数据量: 站点50 + 充电桩400 + 计费方案30 + 订单150
-- ============================================================================

\echo '=== 开始生成 admin 用户测试数据 ==='

-- 1. 生成50个充电站
\echo '生成充电站...'
INSERT INTO charging_station (
    tenant_id, station_code, station_name, station_type, address,
    province, city, district, longitude, latitude, status,
    operator_name, operator_phone, service_hours, parking_fee, facilities,
    create_time, update_time, create_by, update_by, deleted
)
SELECT 
    1,
    'T1-ST-' || LPAD(gs::TEXT, 3, '0'),
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
    '400-' || (8000000 + gs)::TEXT,
    CASE WHEN gs % 5 = 0 THEN '06:00-23:00' ELSE '00:00-24:00' END,
    CASE WHEN gs % 3 = 0 THEN 0.00 ELSE (gs % 3) * 5.00 END,
    '["wifi", "休息室"]'::jsonb,
    NOW() - (gs || ' days')::INTERVAL,
    NOW() - ((gs % 10) || ' hours')::INTERVAL,
    1, 1, 0
FROM generate_series(1, 50) AS gs;

SELECT COUNT(*) || ' 个充电站已创建' FROM charging_station WHERE tenant_id = 1 AND station_code LIKE 'T1-ST-%';

-- 2. 生成充电桩（每个站8个，共400个）
\echo '生成充电桩...'
-- 先删除租户1的旧充电桩
DELETE FROM charger WHERE tenant_id = 1;

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
WHERE cs.tenant_id = 1 AND cs.station_code LIKE 'T1-ST-%';

SELECT COUNT(*) || ' 个充电桩已创建' FROM charger WHERE tenant_id = 1 AND charger_code LIKE 'T1-ST-%';

-- 3. 生成30个计费方案
\echo '生成计费方案...'
INSERT INTO billing_plan (
    tenant_id, code, name, status, is_default, priority,
    create_time, update_time, create_by, update_by, deleted
)
SELECT 
    1,
    'T1-PLAN-' || LPAD(gs::TEXT, 3, '0'),
    '系统计费方案 - ' || (ARRAY['标准方案', '优惠方案', '峰谷方案', 'VIP方案', '快充方案',
                                 '慢充方案', '夜间方案', '节假日方案', '企业方案', '特惠方案'])[1 + (gs % 10)] || gs::TEXT,
    1,
    CASE WHEN gs = 1 THEN 1 ELSE 0 END,
    gs,
    NOW() - ((gs * 2) || ' days')::INTERVAL,
    NOW() - ((gs % 7) || ' days')::INTERVAL,
    1, 1, 0
FROM generate_series(1, 30) AS gs;

SELECT COUNT(*) || ' 个计费方案已创建' FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1-PLAN-%';

-- 4. 生成计费时段 (每个方案3个时段)
\echo '生成计费时段...'
INSERT INTO billing_plan_segment (
    plan_id, segment_index, start_time, end_time, energy_price, service_fee,
    create_time, update_time, create_by, update_by, deleted
)
SELECT 
    bp.id,
    segment_index,
    start_time,
    end_time,
    energy_price,
    service_fee,
    NOW() - INTERVAL '30 days',
    NOW(),
    1,
    1,
    0
FROM billing_plan bp
CROSS JOIN (
    VALUES 
        (1, '00:00', '08:00', 0.50, 0.30),
        (2, '08:00', '18:00', 0.80, 0.40),
        (3, '18:00', '23:59', 1.20, 0.50)
) AS segments(segment_index, start_time, end_time, energy_price, service_fee)
WHERE bp.tenant_id = 1 AND bp.code LIKE 'T1-PLAN-%';

SELECT COUNT(*) || ' 个计费时段已创建' FROM billing_plan_segment 
WHERE plan_id IN (SELECT id FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1-PLAN-%');

-- 5. 生成150个充电订单
\echo '生成充电订单...'
INSERT INTO charging_order (
    tenant_id, session_id, user_id, station_id, charger_id, billing_plan_id,
    start_time, end_time, duration, energy, amount, status,
    payment_trade_id, paid_time,
    create_time, update_time, create_by, update_by, deleted
)
SELECT 
    1,
    'T1-SS-' || LPAD(gs::TEXT, 6, '0'),
    1,
    (SELECT station_id FROM charging_station WHERE tenant_id = 1 AND station_code LIKE 'T1-ST-%' 
     ORDER BY station_id LIMIT 1 OFFSET (gs % 50)),
    (SELECT charger_id FROM charger WHERE tenant_id = 1 AND charger_code LIKE 'T1-ST-%' 
     ORDER BY charger_id LIMIT 1 OFFSET (gs % 400)),
    (SELECT id FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1-PLAN-%' 
     ORDER BY id LIMIT 1 OFFSET (gs % 30)),
    NOW() - ((gs % 30) || ' days')::INTERVAL - ((gs % 24) || ' hours')::INTERVAL,
    CASE 
        WHEN gs % 20 = 0 THEN NOW() - ((gs % 30) || ' days')::INTERVAL - ((gs % 24) || ' hours')::INTERVAL + INTERVAL '3 minutes'
        WHEN gs % 10 = 0 THEN NULL
        ELSE NOW() - ((gs % 30) || ' days')::INTERVAL - ((gs % 24) || ' hours')::INTERVAL + ((30 + gs % 180) || ' minutes')::INTERVAL
    END,
    CASE 
        WHEN gs % 20 = 0 THEN 3
        WHEN gs % 10 = 0 THEN EXTRACT(EPOCH FROM (NOW() - (NOW() - ((gs % 30) || ' days')::INTERVAL - ((gs % 24) || ' hours')::INTERVAL)))::BIGINT / 60
        ELSE 30 + (gs % 180)
    END,
    CASE 
        WHEN gs % 20 = 0 THEN 0.0
        WHEN gs % 10 = 0 THEN (gs % 40 + 10)::NUMERIC
        ELSE (gs % 60 + 20)::NUMERIC
    END,
    CASE 
        WHEN gs % 20 = 0 THEN 0.0
        WHEN gs % 10 = 0 THEN 0.0
        WHEN gs % 7 = 0 THEN ((gs % 50 + 15)::NUMERIC * 1.25)
        ELSE ((gs % 60 + 20)::NUMERIC * 1.32)
    END,
    CASE 
        WHEN gs % 20 = 0 THEN 0
        WHEN gs % 10 = 0 THEN 1
        WHEN gs % 7 = 0 THEN 10
        ELSE 11
    END,
    CASE 
        WHEN gs % 20 = 0 OR gs % 10 = 0 OR gs % 7 = 0 THEN NULL
        ELSE 'PAY-' || gs::TEXT
    END,
    CASE 
        WHEN gs % 20 = 0 OR gs % 10 = 0 OR gs % 7 = 0 THEN NULL
        ELSE NOW() - ((gs % 30) || ' days')::INTERVAL - ((gs % 24) || ' hours')::INTERVAL + ((30 + gs % 180 + 1) || ' minutes')::INTERVAL
    END,
    NOW() - ((gs % 30) || ' days')::INTERVAL - ((gs % 24) || ' hours')::INTERVAL,
    COALESCE(
        NOW() - ((gs % 30) || ' days')::INTERVAL - ((gs % 24) || ' hours')::INTERVAL + ((30 + gs % 180) || ' minutes')::INTERVAL,
        NOW()
    ),
    1, 1, 0
FROM generate_series(1, 150) AS gs;

SELECT COUNT(*) || ' 个充电订单已创建' FROM charging_order WHERE tenant_id = 1 AND session_id LIKE 'T1-SS-%';

-- 统计汇总
\echo '=== 数据生成完成，统计如下 ==='

SELECT 
    '充电站' as 类型, 
    COUNT(*) as 数量 
FROM charging_station 
WHERE tenant_id = 1 AND station_code LIKE 'T1-ST-%'
UNION ALL
SELECT 
    '充电桩' as 类型, 
    COUNT(*) as 数量 
FROM charger 
WHERE tenant_id = 1 AND charger_code LIKE 'T1-ST-%'
UNION ALL
SELECT 
    '计费方案' as 类型, 
    COUNT(*) as 数量 
FROM billing_plan 
WHERE tenant_id = 1 AND code LIKE 'T1-PLAN-%'
UNION ALL
SELECT 
    '计费时段' as 类型, 
    COUNT(*) as 数量 
FROM billing_plan_segment 
WHERE plan_id IN (SELECT id FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1-PLAN-%')
UNION ALL
SELECT 
    '充电订单' as 类型, 
    COUNT(*) as 数量 
FROM charging_order 
WHERE tenant_id = 1 AND session_id LIKE 'T1-SS-%';

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
        NULLIF((SELECT COUNT(*) FROM charging_order WHERE tenant_id = 1 AND session_id LIKE 'T1-SS-%'), 0), 1) 
    as 百分比
FROM charging_order 
WHERE tenant_id = 1 AND session_id LIKE 'T1-SS-%'
GROUP BY status
ORDER BY status;
