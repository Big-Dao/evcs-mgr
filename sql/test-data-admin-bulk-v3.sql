-- ============================================================================
-- 为 admin 用户生成大量测试数据 (简化且正确的版本)
-- 用户: admin (ID: 1), 租户: 系统租户 (ID: 1)
-- ============================================================================

-- 清理已有测试数据 (可选)
-- DELETE FROM charging_order WHERE tenant_id = 1 AND session_id LIKE 'T1-SS-%';
-- DELETE FROM charger WHERE tenant_id = 1 AND charger_code LIKE 'T1-%-C%';
-- DELETE FROM billing_plan_segment WHERE plan_id IN (SELECT id FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1-PLAN-%');
-- DELETE FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1-PLAN-%';
-- DELETE FROM charging_station WHERE tenant_id = 1 AND station_code LIKE 'T1-ST-%';

-- ============================================================================
-- 1. 充电站数据 (50个)
-- ============================================================================
DO $$
DECLARE
    i INTEGER;
BEGIN
    FOR i IN 1..50 LOOP
        INSERT INTO charging_station (
            tenant_id, station_code, station_name, station_type, address,
            province, city, district, longitude, latitude, status,
            operator_name, operator_phone, service_hours, parking_fee, facilities,
            create_time, update_time, create_by, update_by, deleted
        ) VALUES (
            1,
            'T1-ST-' || LPAD(i::TEXT, 3, '0'),
            '系统 - ' || CASE i % 10
                WHEN 0 THEN '机场充电站'
                WHEN 1 THEN '高铁站充电站'
                WHEN 2 THEN 'CBD商圈站'
                WHEN 3 THEN '科技园区站'
                WHEN 4 THEN '住宅小区站'
                WHEN 5 THEN '商业中心站'
                WHEN 6 THEN '工业园区站'
                WHEN 7 THEN '服务区站'
                WHEN 8 THEN '景区站点'
                ELSE '公共停车场站'
            END || i::TEXT || '号',
            CASE WHEN i % 3 = 0 THEN 2 ELSE 1 END,
            CASE i % 6
                WHEN 0 THEN '深圳市'
                WHEN 1 THEN '广州市'
                WHEN 2 THEN '北京市'
                WHEN 3 THEN '上海市'
                WHEN 4 THEN '成都市'
                ELSE '杭州市'
            END || '某某区某某街道' || (100 + i)::TEXT || '号',
            CASE i % 6
                WHEN 0 THEN '广东省'
                WHEN 1 THEN '广东省'
                WHEN 2 THEN '北京市'
                WHEN 3 THEN '上海市'
                WHEN 4 THEN '四川省'
                ELSE '浙江省'
            END,
            CASE i % 6
                WHEN 0 THEN '深圳市'
                WHEN 1 THEN '广州市'
                WHEN 2 THEN '北京市'
                WHEN 3 THEN '上海市'
                WHEN 4 THEN '成都市'
                ELSE '杭州市'
            END,
            CASE i % 5
                WHEN 0 THEN '南山区'
                WHEN 1 THEN '福田区'
                WHEN 2 THEN '朝阳区'
                WHEN 3 THEN '浦东新区'
                ELSE '天河区'
            END,
            113.0 + (i % 10) * 0.15,
            22.5 + (i % 8) * 0.08,
            1,
            '系统运营商',
            '400-' || (8000000 + i)::TEXT,
            CASE WHEN i % 5 = 0 THEN '06:00-23:00' ELSE '00:00-24:00' END,
            CASE WHEN i % 3 = 0 THEN 0.00 ELSE (i % 3) * 5.00 END,
            '["wifi", "休息室", "便利店"]'::jsonb,
            NOW() - (i || ' days')::INTERVAL,
            NOW() - (i % 10 || ' hours')::INTERVAL,
            1, 1, 0
        );
    END LOOP;
    RAISE NOTICE '已生成 50 个充电站';
END $$;

-- ============================================================================
-- 2. 充电桩数据 (每个站8个桩，共400个)
-- ============================================================================
DO $$
DECLARE
    station_rec RECORD;
    i INTEGER;
BEGIN
    FOR station_rec IN 
        SELECT station_id, station_code, station_name 
        FROM charging_station 
        WHERE tenant_id = 1 AND station_code LIKE 'T1-ST-%'
        ORDER BY station_id
    LOOP
        FOR i IN 1..8 LOOP
            INSERT INTO charger (
                tenant_id, station_id, charger_code, charger_name, charger_type,
                brand, model, manufacturer, rated_power, input_voltage,
                output_voltage_range, output_current_range, gun_count, gun_types,
                supported_protocols, status, enabled,
                create_time, update_time, create_by, update_by, deleted
            ) VALUES (
                1,
                station_rec.station_id,
                station_rec.station_code || '-C' || LPAD(i::TEXT, 2, '0'),
                REPLACE(station_rec.station_name, '系统 - ', '') || '-' || i::TEXT || '号' ||
                    CASE WHEN i <= 5 THEN '快充' ELSE '慢充' END,
                CASE WHEN i <= 5 THEN 1 ELSE 2 END,
                CASE i % 4
                    WHEN 0 THEN '特来电'
                    WHEN 1 THEN '星星充电'
                    WHEN 2 THEN '云快充'
                    ELSE 'ABB'
                END,
                CASE 
                    WHEN i <= 5 THEN 'DC-' || (120 + i * 10)::TEXT || 'kW'
                    ELSE 'AC-' || (7 + i % 2 * 4)::TEXT || 'kW'
                END,
                '制造商' || (i % 4)::TEXT,
                CASE WHEN i <= 5 THEN 120.0 + i * 10 ELSE 7.0 + (i % 2) * 4 END,
                CASE WHEN i <= 5 THEN 380 ELSE 220 END,
                CASE WHEN i <= 5 THEN '200-1000V' ELSE '220V' END,
                CASE WHEN i <= 5 THEN '0-500A' ELSE '0-32A' END,
                CASE WHEN i <= 5 THEN 2 ELSE 1 END,
                CASE WHEN i <= 5 THEN 'CCS,CHAdeMO' ELSE 'Type 2' END,
                '{"gb": "2015", "ocpp": "1.6"}'::jsonb,
                CASE 
                    WHEN i % 10 = 0 THEN 3  -- 维护中
                    WHEN i % 6 = 0 THEN 2   -- 充电中
                    ELSE 1                   -- 运营中
                END,
                1,
                NOW() - (station_rec.station_id % 60 || ' days')::INTERVAL,
                NOW() - (i % 20 || ' hours')::INTERVAL,
                1, 1, 0
            );
        END LOOP;
    END LOOP;
    RAISE NOTICE '已生成约 400 个充电桩';
END $$;

-- ============================================================================
-- 3. 计费方案 (30个)
-- ============================================================================
DO $$
DECLARE
    i INTEGER;
BEGIN
    FOR i IN 1..30 LOOP
        INSERT INTO billing_plan (
            tenant_id, code, name, status, is_default, priority,
            create_time, update_time, create_by, update_by, deleted
        ) VALUES (
            1,
            'T1-PLAN-' || LPAD(i::TEXT, 3, '0'),
            '系统计费方案 - ' || CASE i % 10
                WHEN 0 THEN '标准方案'
                WHEN 1 THEN '优惠方案'
                WHEN 2 THEN '峰谷方案'
                WHEN 3 THEN 'VIP方案'
                WHEN 4 THEN '快充方案'
                WHEN 5 THEN '慢充方案'
                WHEN 6 THEN '夜间方案'
                WHEN 7 THEN '节假日方案'
                WHEN 8 THEN '企业方案'
                ELSE '特惠方案'
            END || i::TEXT,
            1,
            CASE WHEN i = 1 THEN 1 ELSE 0 END,
            i,
            NOW() - (i * 2 || ' days')::INTERVAL,
            NOW() - (i % 7 || ' days')::INTERVAL,
            1, 1, 0
        );
    END LOOP;
    RAISE NOTICE '已生成 30 个计费方案';
END $$;

-- ============================================================================
-- 4. 计费时段 (每个方案3个时段，共90个)
-- ============================================================================
DO $$
DECLARE
    plan_rec RECORD;
BEGIN
    FOR plan_rec IN 
        SELECT id FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1-PLAN-%'
    LOOP
        -- 谷时段
        INSERT INTO billing_plan_segment (
            plan_id, name, start_time, end_time, energy_price, service_fee,
            create_time, update_time, deleted
        ) VALUES (
            plan_rec.id, '谷时', '00:00:00', '08:00:00',
            0.50 + (plan_rec.id % 10) * 0.03, 
            0.30 + (plan_rec.id % 5) * 0.02,
            NOW() - INTERVAL '30 days', NOW(), 0
        );
        
        -- 平时段
        INSERT INTO billing_plan_segment (
            plan_id, name, start_time, end_time, energy_price, service_fee,
            create_time, update_time, deleted
        ) VALUES (
            plan_rec.id, '平时', '08:00:00', '18:00:00',
            0.80 + (plan_rec.id % 10) * 0.03, 
            0.40 + (plan_rec.id % 5) * 0.02,
            NOW() - INTERVAL '30 days', NOW(), 0
        );
        
        -- 峰时段
        INSERT INTO billing_plan_segment (
            plan_id, name, start_time, end_time, energy_price, service_fee,
            create_time, update_time, deleted
        ) VALUES (
            plan_rec.id, '峰时', '18:00:00', '23:59:59',
            1.20 + (plan_rec.id % 10) * 0.03, 
            0.50 + (plan_rec.id % 5) * 0.02,
            NOW() - INTERVAL '30 days', NOW(), 0
        );
    END LOOP;
    RAISE NOTICE '已生成 90 个计费时段';
END $$;

-- ============================================================================
-- 5. 充电订单 (150个)
-- ============================================================================
DO $$
DECLARE
    i INTEGER;
    station_ids BIGINT[];
    charger_ids BIGINT[];
    plan_ids BIGINT[];
    station_id_val BIGINT;
    charger_id_val BIGINT;
    plan_id_val BIGINT;
    status_val INTEGER;
    start_time_val TIMESTAMP;
    end_time_val TIMESTAMP;
    duration_val BIGINT;
    energy_val NUMERIC(12,4);
    amount_val NUMERIC(12,4);
BEGIN
    -- 获取ID数组
    SELECT ARRAY_AGG(station_id) INTO station_ids 
    FROM charging_station WHERE tenant_id = 1 AND station_code LIKE 'T1-ST-%';
    
    SELECT ARRAY_AGG(charger_id) INTO charger_ids 
    FROM charger WHERE tenant_id = 1 AND charger_code LIKE 'T1-ST-%';
    
    SELECT ARRAY_AGG(id) INTO plan_ids 
    FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1-PLAN-%';
    
    FOR i IN 1..150 LOOP
        -- 随机选择
        station_id_val := station_ids[(i % array_length(station_ids, 1)) + 1];
        charger_id_val := charger_ids[(i % array_length(charger_ids, 1)) + 1];
        plan_id_val := plan_ids[(i % array_length(plan_ids, 1)) + 1];
        
        -- 生成时间
        start_time_val := NOW() - (i % 30 || ' days')::INTERVAL - (i % 24 || ' hours')::INTERVAL;
        
        -- 订单状态: 70% 已完成(11), 15% 待支付(10), 10% 充电中(1), 5% 已取消(0)
        IF i % 20 = 0 THEN
            status_val := 0;  -- 已取消
            end_time_val := start_time_val + INTERVAL '3 minutes';
            duration_val := 3;
            energy_val := 0;
            amount_val := 0;
        ELSIF i % 10 = 0 THEN
            status_val := 1;  -- 充电中
            end_time_val := NULL;
            duration_val := EXTRACT(EPOCH FROM (NOW() - start_time_val))::BIGINT / 60;
            energy_val := (i % 40 + 10)::NUMERIC;
            amount_val := 0;
        ELSIF i % 7 = 0 THEN
            status_val := 10;  -- 待支付
            duration_val := 45 + (i % 120);
            end_time_val := start_time_val + (duration_val || ' minutes')::INTERVAL;
            energy_val := (i % 50 + 15)::NUMERIC;
            amount_val := energy_val * 1.25;
        ELSE
            status_val := 11;  -- 已完成已支付
            duration_val := 30 + (i % 180);
            end_time_val := start_time_val + (duration_val || ' minutes')::INTERVAL;
            energy_val := (i % 60 + 20)::NUMERIC;
            amount_val := energy_val * 1.32;
        END IF;
        
        INSERT INTO charging_order (
            tenant_id, session_id, user_id, station_id, charger_id, billing_plan_id,
            start_time, end_time, duration, energy, amount, status,
            payment_trade_id, paid_time,
            create_time, update_time, create_by, update_by, deleted
        ) VALUES (
            1,
            'T1-SS-' || LPAD(i::TEXT, 6, '0'),
            1,
            station_id_val,
            charger_id_val,
            plan_id_val,
            start_time_val,
            end_time_val,
            duration_val,
            energy_val,
            amount_val,
            status_val,
            CASE WHEN status_val = 11 THEN 'PAY-' || i::TEXT ELSE NULL END,
            CASE WHEN status_val = 11 THEN end_time_val + INTERVAL '1 minute' ELSE NULL END,
            start_time_val,
            COALESCE(end_time_val, NOW()),
            1, 1, 0
        );
    END LOOP;
    RAISE NOTICE '已生成 150 个充电订单';
END $$;

-- ============================================================================
-- 统计信息
-- ============================================================================
SELECT '=== Admin 用户测试数据生成完成 ===' as 信息;

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
