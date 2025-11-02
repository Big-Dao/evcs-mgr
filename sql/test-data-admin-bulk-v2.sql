-- ============================================================================
-- 为 admin 用户生成大量测试数据
-- 用户: admin (ID: 1)
-- 租户: 系统租户 (ID: 1, tenant_code: SYSTEM)
-- 生成时间: 2025-10-31
-- 
-- 数据量:
-- - 充电站: 50个
-- - 充电桩: ~400个 (每个站8-30个桩不等)
-- - 计费方案: 30个
-- - 充电订单: 150个
-- ============================================================================

-- ============================================================================
-- 1. 充电站数据生成函数
-- ============================================================================
DO $$
DECLARE
    station_names TEXT[] := ARRAY[
        '南山科技园站', '福田CBD站', '罗湖商业区站', '宝安中心站', '龙华新区站',
        '龙岗中心城站', '坪山高铁站', '光明新区站', '前海自贸区站', '蛇口港站',
        '盐田港区站', '大鹏新区站', '深圳北站', '深圳机场站', '欢乐谷站',
        '天河CBD站', '珠江新城站', '白云机场站', '番禺广场站', '广州南站',
        '黄埔区站', '海珠商圈站', '荔湾老城站', '增城新区站', '从化温泉站',
        '中关村站', '国贸CBD站', '首都机场站', '亦庄开发区站', '西单商圈站',
        '望京商务区站', '通州副中心站', '石景山万达站', '陆家嘴金融区站', '南京路步行街站',
        '虹桥枢纽站', '浦东机场站', '静安寺商圈站', '张江高科站', '徐家汇商圈站',
        '杨浦大学城站', '天府广场站', '春熙路商圈站', '双流机场站', '高新区站',
        '宽窄巷子站', '西湖景区站', '滨江高新区站', '萧山机场站', '武林广场站'
    ];
    cities TEXT[] := ARRAY['深圳市', '广州市', '北京市', '上海市', '成都市', '杭州市'];
    districts TEXT[] := ARRAY['南山区', '福田区', '罗湖区', '宝安区', '龙华区', '龙岗区', '天河区', '海淀区', '浦东新区'];
    i INTEGER;
    station_code_val VARCHAR(50);
    station_name_val VARCHAR(200);
    city_val VARCHAR(50);
    district_val VARCHAR(50);
BEGIN
    FOR i IN 1..50 LOOP
        station_code_val := 'T1-' || LPAD(i::TEXT, 4, '0');
        station_name_val := '系统租户 - ' || station_names[i];
        city_val := cities[(i % 6) + 1];
        district_val := districts[(i % 9) + 1];
        
        INSERT INTO charging_station (
            tenant_id, station_code, station_name, station_type, address,
            province, city, district, longitude, latitude, status,
            operator_name, operator_phone, service_hours, parking_fee, facilities,
            create_time, update_time, create_by, update_by, deleted
        ) VALUES (
            1,
            station_code_val,
            station_name_val,
            CASE WHEN i % 3 = 0 THEN 2 ELSE 1 END,  -- 1/3 是小区站，2/3 是公共站
            city_val || district_val || '某某街道' || (100 + i) || '号',
            CASE 
                WHEN city_val IN ('深圳市', '广州市') THEN '广东省'
                WHEN city_val = '北京市' THEN '北京市'
                WHEN city_val = '上海市' THEN '上海市'
                WHEN city_val = '成都市' THEN '四川省'
                ELSE '浙江省'
            END,
            city_val,
            district_val,
            113.0 + (i % 10) * 0.1,  -- longitude
            22.5 + (i % 8) * 0.05,   -- latitude
            1,  -- 运营中
            '系统运营商',
            '400-' || LPAD((8000000 + i)::TEXT, 7, '0'),
            CASE WHEN i % 5 = 0 THEN '06:00-23:00' ELSE '00:00-24:00' END,
            CASE WHEN i % 3 = 0 THEN 0.00 ELSE (i % 3) * 5.00 END,
            '["wifi", "休息室", "便利店", "洗手间"]'::jsonb,
            NOW() - (INTERVAL '1 day' * (90 - i)),
            NOW() - (INTERVAL '1 hour' * (i % 24)),
            1,
            1,
            0
        );
    END LOOP;
END $$;

-- ============================================================================
-- 2. 充电桩数据 (每个站生成8-30个桩)
-- ============================================================================
DO $$
DECLARE
    station_rec RECORD;
    charger_count INTEGER;
    i INTEGER;
    charger_type_val INTEGER;
    rated_power_val NUMERIC(10,2);
    charger_status INTEGER;
    brand_val VARCHAR(50);
    brands TEXT[] := ARRAY['特来电', '星星充电', '云快充', 'ABB', '蔚来', '小鹏'];
BEGIN
    FOR station_rec IN 
        SELECT station_id, station_code, station_name 
        FROM charging_station 
        WHERE tenant_id = 1 AND station_code LIKE 'T1-%'
        ORDER BY station_id
    LOOP
        -- 每个站8-30个桩
        charger_count := 8 + (station_rec.station_id % 23);
        
        FOR i IN 1..charger_count LOOP
            -- 60% 直流快充, 40% 交流慢充
            IF i <= (charger_count * 0.6) THEN
                charger_type_val := 1;  -- DC
                rated_power_val := CASE 
                    WHEN i % 3 = 0 THEN 180.00
                    WHEN i % 3 = 1 THEN 120.00
                    ELSE 90.00
                END;
            ELSE
                charger_type_val := 2;  -- AC
                rated_power_val := CASE 
                    WHEN i % 2 = 0 THEN 11.00
                    ELSE 7.00
                END;
            END IF;
            
            -- 75% 运营中, 20% 充电中, 5% 维护中
            charger_status := CASE 
                WHEN i % 20 = 0 THEN 3  -- 维护中
                WHEN i % 5 = 0 OR i % 7 = 0 THEN 2  -- 充电中
                ELSE 1  -- 运营中
            END;
            
            brand_val := brands[(i % 6) + 1];
            
            INSERT INTO charger (
                tenant_id, station_id, charger_code, charger_name, charger_type,
                brand, model, manufacturer, rated_power, input_voltage,
                output_voltage_range, output_current_range, gun_count, gun_types,
                supported_protocols, status, enabled,
                create_time, update_time, create_by, update_by, deleted
            ) VALUES (
                1,
                station_rec.station_id,
                station_rec.station_code || '-C' || LPAD(i::TEXT, 3, '0'),
                REPLACE(station_rec.station_name, '系统租户 - ', '') || i || '号' ||
                    CASE WHEN charger_type_val = 1 THEN '快充' ELSE '慢充' END,
                charger_type_val,
                brand_val,
                CASE charger_type_val
                    WHEN 1 THEN 'DC-' || rated_power_val::TEXT || 'kW'
                    ELSE 'AC-' || rated_power_val::TEXT || 'kW'
                END,
                brand_val || '科技有限公司',
                rated_power_val,
                CASE charger_type_val WHEN 1 THEN 380 ELSE 220 END,
                CASE charger_type_val
                    WHEN 1 THEN '200-1000V'
                    ELSE '220V'
                END,
                CASE charger_type_val
                    WHEN 1 THEN '0-500A'
                    ELSE '0-32A'
                END,
                CASE charger_type_val WHEN 1 THEN 2 ELSE 1 END,
                CASE charger_type_val
                    WHEN 1 THEN 'CCS,CHAdeMO'
                    ELSE 'Type 2'
                END,
                '{"gb": "2015", "ocpp": "1.6"}'::jsonb,
                charger_status,
                1,
                NOW() - (INTERVAL '1 day' * ((station_rec.station_id % 80) + 10)),
                NOW() - (INTERVAL '1 hour' * (i % 24)),
                1,
                1,
                0
            );
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 3. 计费方案数据 (30个)
-- ============================================================================
DO $$
DECLARE
    plan_types TEXT[] := ARRAY[
        '标准计费', '峰谷电价', '快充专属', '慢充优惠', '夜间优惠',
        '科技园专属', '机场高速', '自贸区优惠', '工业区物流', '住宅区居民',
        'CBD商务', '机场周边', '新区住宅', '开发区企业', '商圈夜间',
        'VIP会员', '车队企业', '节假日促销', '应急充电', '绿色能源',
        '学生优惠', '老年优惠', '新能源出租', '网约车专属', '公交专线',
        '物流配送', '环卫车辆', '公务用车', '分时租赁', '共享汽车'
    ];
    i INTEGER;
    plan_code_val VARCHAR(64);
    plan_name_val VARCHAR(100);
    is_default_val INTEGER;
BEGIN
    FOR i IN 1..30 LOOP
        plan_code_val := 'T1_PLAN_' || LPAD(i::TEXT, 3, '0');
        plan_name_val := '系统 - ' || plan_types[i];
        is_default_val := CASE WHEN i = 1 THEN 1 ELSE 0 END;
        
        INSERT INTO billing_plan (
            tenant_id, code, name, status, is_default, priority,
            create_time, update_time, create_by, update_by, deleted
        ) VALUES (
            1,
            plan_code_val,
            plan_name_val,
            1,  -- 启用
            is_default_val,
            i,
            NOW() - (INTERVAL '1 day' * (90 - i * 2)),
            NOW() - (INTERVAL '1 day' * (i % 30)),
            1,
            1,
            0
        );
    END LOOP;
END $$;

-- ============================================================================
-- 4. 计费方案时段 (每个方案3个时段)
-- ============================================================================
DO $$
DECLARE
    plan_rec RECORD;
    energy_price_base NUMERIC(8,2);
    service_fee_base NUMERIC(8,2);
BEGIN
    FOR plan_rec IN 
        SELECT id FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1_PLAN_%'
    LOOP
        energy_price_base := 0.50 + (plan_rec.id % 10) * 0.05;
        service_fee_base := 0.30 + (plan_rec.id % 5) * 0.02;
        
        -- 谷时段 (00:00-08:00)
        INSERT INTO billing_plan_segment (
            plan_id, name, start_time, end_time, energy_price, service_fee,
            create_time, update_time, deleted
        ) VALUES (
            plan_rec.id, '谷时', '00:00:00', '08:00:00',
            energy_price_base, service_fee_base,
            NOW() - INTERVAL '60 days', NOW(), 0
        );
        
        -- 平时段 (08:00-18:00)
        INSERT INTO billing_plan_segment (
            plan_id, name, start_time, end_time, energy_price, service_fee,
            create_time, update_time, deleted
        ) VALUES (
            plan_rec.id, '平时', '08:00:00', '18:00:00',
            energy_price_base + 0.30, service_fee_base + 0.10,
            NOW() - INTERVAL '60 days', NOW(), 0
        );
        
        -- 峰时段 (18:00-24:00)
        INSERT INTO billing_plan_segment (
            plan_id, name, start_time, end_time, energy_price, service_fee,
            create_time, update_time, deleted
        ) VALUES (
            plan_rec.id, '峰时', '18:00:00', '23:59:59',
            energy_price_base + 0.70, service_fee_base + 0.20,
            NOW() - INTERVAL '60 days', NOW(), 0
        );
    END LOOP;
END $$;

-- ============================================================================
-- 5. 充电订单数据 (150个)
-- ============================================================================
DO $$
DECLARE
    i INTEGER;
    station_ids BIGINT[];
    charger_ids BIGINT[];
    plan_ids BIGINT[];
    selected_station_id BIGINT;
    selected_charger_id BIGINT;
    selected_plan_id BIGINT;
    order_status INTEGER;
    start_time_val TIMESTAMP;
    end_time_val TIMESTAMP;
    charging_duration_val INTEGER;
    energy_consumed_val NUMERIC(10,2);
    energy_cost_val NUMERIC(10,2);
    service_cost_val NUMERIC(10,2);
    total_cost_val NUMERIC(10,2);
    session_id_val VARCHAR(64);
    payment_method_val INTEGER;
BEGIN
    -- 获取所有站点、充电桩和计费方案 ID
    SELECT ARRAY_AGG(station_id) INTO station_ids FROM charging_station WHERE tenant_id = 1 AND station_code LIKE 'T1-%';
    SELECT ARRAY_AGG(charger_id) INTO charger_ids FROM charger WHERE tenant_id = 1 LIMIT 400;
    SELECT ARRAY_AGG(id) INTO plan_ids FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1_PLAN_%';
    
    FOR i IN 1..150 LOOP
        -- 随机选择站点、充电桩和计费方案
        selected_station_id := station_ids[(i % array_length(station_ids, 1)) + 1];
        selected_charger_id := charger_ids[(i % array_length(charger_ids, 1)) + 1];
        selected_plan_id := plan_ids[(i % array_length(plan_ids, 1)) + 1];
        
        -- 生成时间（最近30天）
        start_time_val := NOW() - INTERVAL '1 day' * (i % 30) - INTERVAL '1 hour' * (i % 24);
        
        -- 订单状态: 70% 已完成已支付, 15% 已完成待支付, 10% 充电中, 5% 已取消
        IF i % 20 = 0 THEN
            -- 已取消
            order_status := 0;
            end_time_val := start_time_val + INTERVAL '5 minutes';
            charging_duration_val := 5;
            energy_consumed_val := 0;
            energy_cost_val := 0;
            service_cost_val := 0;
            total_cost_val := 0;
            payment_method_val := NULL;
        ELSIF i % 10 = 0 THEN
            -- 充电中
            order_status := 1;
            end_time_val := NULL;
            charging_duration_val := EXTRACT(EPOCH FROM (NOW() - start_time_val))::INTEGER / 60;
            energy_consumed_val := (i % 50) + 5.5;
            energy_cost_val := energy_consumed_val * 0.85;
            service_cost_val := energy_consumed_val * 0.42;
            total_cost_val := 0;
            payment_method_val := NULL;
        ELSIF i % 7 = 0 THEN
            -- 已完成待支付
            order_status := 10;
            charging_duration_val := 45 + (i % 180);
            end_time_val := start_time_val + (charging_duration_val || ' minutes')::INTERVAL;
            energy_consumed_val := (i % 60) + 10.5;
            energy_cost_val := energy_consumed_val * 0.88;
            service_cost_val := energy_consumed_val * 0.44;
            total_cost_val := energy_cost_val + service_cost_val;
            payment_method_val := NULL;
        ELSE
            -- 已完成已支付
            order_status := 11;
            charging_duration_val := 30 + (i % 240);
            end_time_val := start_time_val + (charging_duration_val || ' minutes')::INTERVAL;
            energy_consumed_val := (i % 70) + 15.8;
            energy_cost_val := energy_consumed_val * 0.90;
            service_cost_val := energy_consumed_val * 0.45;
            total_cost_val := energy_cost_val + service_cost_val;
            payment_method_val := (i % 3) + 1;  -- 1=支付宝, 2=微信, 3=余额
        END IF;
        
        session_id_val := 'T1_SESSION_' || LPAD(i::TEXT, 6, '0');
        
        INSERT INTO charging_order (
            tenant_id, session_id, user_id, station_id, charger_id, billing_plan_id,
            start_time, end_time, duration, energy, amount, status,
            payment_trade_id, paid_time, create_time, update_time, create_by, update_by, deleted
        ) VALUES (
            1,
            session_id_val,
            1,  -- admin用户
            selected_station_id,
            selected_charger_id,
            selected_plan_id,
            start_time_val,
            end_time_val,
            charging_duration_val,
            energy_consumed_val,
            total_cost_val,
            order_status,
            CASE WHEN order_status = 11 THEN 'PAY_' || session_id_val ELSE NULL END,
            CASE WHEN order_status = 11 THEN end_time_val + INTERVAL '2 minutes' ELSE NULL END,
            start_time_val,
            COALESCE(end_time_val, NOW()),
            1,
            1,
            0
        );
    END LOOP;
END $$;

-- ============================================================================
-- 数据生成完成 - 显示统计信息
-- ============================================================================

SELECT '=== Admin 用户测试数据生成完成 ===' as 标题;

SELECT '充电站' as 类型, COUNT(*) as 数量 
FROM charging_station 
WHERE tenant_id = 1 AND station_code LIKE 'T1-%'
UNION ALL
SELECT '充电桩' as 类型, COUNT(*) as 数量 
FROM charger 
WHERE tenant_id = 1 AND charger_code LIKE 'T1-%'
UNION ALL
SELECT '计费方案' as 类型, COUNT(*) as 数量 
FROM billing_plan 
WHERE tenant_id = 1 AND code LIKE 'T1_PLAN_%'
UNION ALL
SELECT '计费时段' as 类型, COUNT(*) as 数量 
FROM billing_plan_segment 
WHERE plan_id IN (SELECT id FROM billing_plan WHERE tenant_id = 1 AND code LIKE 'T1_PLAN_%')
UNION ALL
SELECT '充电订单' as 类型, COUNT(*) as 数量 
FROM charging_order 
WHERE tenant_id = 1 AND session_id LIKE 'T1_SESSION_%';

-- 订单状态分布
SELECT 
    CASE status
        WHEN 0 THEN '已取消'
        WHEN 1 THEN '充电中'
        WHEN 10 THEN '已完成待支付'
        WHEN 11 THEN '已完成已支付'
        ELSE '其他'
    END as 订单状态,
    COUNT(*) as 数量,
    ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM charging_order WHERE tenant_id = 1 AND session_id LIKE 'T1_SESSION_%'), 0), 2) as 百分比
FROM charging_order 
WHERE tenant_id = 1 AND session_id LIKE 'T1_SESSION_%'
GROUP BY status
ORDER BY status;
