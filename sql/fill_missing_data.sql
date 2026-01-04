-- =====================================================
-- 补全测试数据脚本
-- 目的：确保系统中有足够的数据用于演示和测试
-- 包含：基础租户/用户、站点、充电桩、计费方案、订单
-- =====================================================

-- 0. 重置序列 (防止手动插入导致序列不同步)
SELECT setval('billing_plan_id_seq', COALESCE((SELECT MAX(id) FROM billing_plan), 1));
SELECT setval('charging_station_station_id_seq', COALESCE((SELECT MAX(station_id) FROM charging_station), 1));
SELECT setval('charger_charger_id_seq', COALESCE((SELECT MAX(charger_id) FROM charger), 1));
SELECT setval('charging_order_id_seq', COALESCE((SELECT MAX(id) FROM charging_order), 1));

-- 1. 确保基础租户存在
INSERT INTO sys_tenant (id, tenant_code, tenant_name, parent_id, ancestors, tenant_type, status, tenant_id)
VALUES (2, 'OPERATOR_BJ', '北京城市运营商', 1, '1', 2, 1, 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_tenant (id, tenant_code, tenant_name, parent_id, ancestors, tenant_type, status, tenant_id)
VALUES (3, 'MALL_PARTNER', '朝阳大悦城商户', 2, '1,2', 3, 1, 3)
ON CONFLICT (id) DO NOTHING;

-- 2. 确保基础用户存在
INSERT INTO sys_user (username, login_identifier, password, real_name, user_type, status, tenant_id)
SELECT 'operator', 'operator@bj', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '运营商管理员', 1, 1, 2
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE login_identifier = 'operator@bj');

INSERT INTO sys_user (username, login_identifier, password, real_name, user_type, status, tenant_id)
SELECT 'merchant', 'merchant@mall', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '商户管理员', 1, 1, 3
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE login_identifier = 'merchant@mall');

-- 2.1 关联角色
INSERT INTO sys_user_role (user_id, role_id, tenant_id)
SELECT id, 3, 2 FROM sys_user WHERE username = 'operator'
AND NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = sys_user.id AND role_id = 3);

INSERT INTO sys_user_role (user_id, role_id, tenant_id)
SELECT id, 4, 3 FROM sys_user WHERE username = 'merchant'
AND NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = sys_user.id AND role_id = 4);

-- 3. 批量生成充电站 (为系统租户生成 20 个站点)
INSERT INTO charging_station (
    tenant_id, station_code, station_name, station_type, address,
    province, city, district, longitude, latitude, status,
    operator_name, operator_phone, service_hours, parking_fee, facilities,
    create_time, update_time, create_by, update_by, deleted
)
SELECT 
    1,
    'SYS-ST-' || LPAD(gs::TEXT, 3, '0'),
    '演示站-' || (ARRAY['商业中心', '交通枢纽', '居民区', '办公区'])[1 + (gs % 4)] || '-' || gs::TEXT,
    CASE WHEN gs % 2 = 0 THEN 1 ELSE 2 END,
    '演示地址 ' || gs || ' 号',
    '北京市',
    '北京市',
    (ARRAY['朝阳区', '海淀区', '丰台区', '东城区'])[1 + (gs % 4)],
    116.40 + (random() * 0.1),
    39.90 + (random() * 0.1),
    1,
    '系统运营',
    '400-123-4567',
    '24小时',
    5.00,
    '{"wifi": true}'::jsonb,
    NOW(),
    NOW(),
    1, 1, 0
FROM generate_series(1, 20) AS gs
ON CONFLICT (station_code, tenant_id) DO NOTHING;

-- 4. 批量生成充电桩 (每个站点 5 个桩)
INSERT INTO charger (
    tenant_id, station_id, charger_code, charger_name, charger_type,
    brand, model, rated_power, status, create_by
)
SELECT 
    s.tenant_id,
    s.station_id,
    s.station_code || '-CH-' || LPAD(gs::TEXT, 2, '0'),
    s.station_name || '-桩' || gs,
    CASE WHEN gs % 2 = 0 THEN 1 ELSE 2 END, -- 1:DC, 2:AC
    '演示品牌',
    'Model-X',
    CASE WHEN gs % 2 = 0 THEN 120.0 ELSE 7.0 END,
    1, -- 空闲
    1
FROM charging_station s
CROSS JOIN generate_series(1, 5) AS gs
WHERE s.tenant_id = 1 AND s.station_code LIKE 'SYS-ST-%'
ON CONFLICT (charger_code, tenant_id) DO NOTHING;

-- 5. 确保计费方案存在
INSERT INTO billing_plan (tenant_id, name, code, status, is_default, create_by)
SELECT 1, '通用计费方案', 'PLAN_GEN_001', 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM billing_plan WHERE code = 'PLAN_GEN_001' AND tenant_id = 1);

-- 关联计费时段 (如果方案是新插入的)
INSERT INTO billing_plan_segment (plan_id, segment_index, start_time, end_time, energy_price, service_fee)
SELECT id, 1, '00:00', '24:00', 1.0, 0.5
FROM billing_plan 
WHERE code = 'PLAN_GEN_001' AND tenant_id = 1
AND NOT EXISTS (SELECT 1 FROM billing_plan_segment WHERE plan_id = billing_plan.id);

-- 6. 批量生成订单 (过去 30 天，每天约 5 单)
-- 先清理可能存在的旧数据（为了重新生成完整数据）
DELETE FROM charging_order 
WHERE session_id LIKE 'SESS-' || to_char(NOW(), 'YYYYMMDD') || '-%';

WITH generated_data AS (
    SELECT
        1 as tenant_id,
        s.station_id,
        (SELECT charger_id FROM charger WHERE station_id = s.station_id LIMIT 1) as charger_id,
        'SESS-' || to_char(NOW(), 'YYYYMMDD') || '-' || gs as session_id,
        1 + (gs % 4) as user_id, -- Users 1-4
        NOW() - (gs % 30 || ' days')::interval - (random() * 10 || ' hours')::interval as start_time,
        60 as duration,
        20.0 + (random() * 30) as energy,
        30.0 + (random() * 40) as amount,
        gs
    FROM charging_station s
    CROSS JOIN generate_series(1, 150) AS gs
    WHERE s.tenant_id = 1 AND s.station_code LIKE 'SYS-ST-%'
    LIMIT 150
)
INSERT INTO charging_order (
    tenant_id, station_id, charger_id, session_id, user_id,
    start_time, end_time, energy, duration, amount,
    billing_plan_id, coupon_id, discount_amount, pay_amount,
    payment_trade_id, paid_time, status,
    create_time, update_time, create_by, update_by, deleted, version
)
SELECT
    tenant_id, station_id, charger_id, session_id, user_id,
    start_time,
    start_time + (duration || ' minutes')::interval,
    energy,
    duration,
    amount,
    (SELECT id FROM billing_plan WHERE code = 'PLAN_GEN_001' LIMIT 1),
    NULL,
    0.00,
    amount, -- pay_amount = amount
    'TRADE-' || session_id,
    start_time + (duration || ' minutes')::interval,
    11, -- Paid
    start_time,
    start_time + (duration || ' minutes')::interval,
    1,
    1,
    0,
    0
FROM generated_data
WHERE NOT EXISTS (
    SELECT 1 FROM charging_order WHERE session_id = generated_data.session_id
);

