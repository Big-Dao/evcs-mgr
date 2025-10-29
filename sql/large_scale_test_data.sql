-- ============================================
-- 充电站管理平台 - 大规模测试数据
-- 2个平台方 + 多个运营商 + 大量充电站和充电桩
-- ============================================

-- 清理现有测试数据（保留admin用户和默认租户）
DELETE FROM charging_order WHERE tenant_id != 1;
DELETE FROM charger WHERE tenant_id != 1;
DELETE FROM charging_station WHERE tenant_id != 1;
DELETE FROM sys_user WHERE id > 1 AND tenant_id != 1;
DELETE FROM sys_tenant WHERE id > 1;

-- 重置序列（如果需要）
-- SELECT setval('sys_tenant_id_seq', 1, false);

-- ============================================
-- 1. 创建平台方租户（2个）
-- ============================================
INSERT INTO sys_tenant (id, tenant_code, tenant_name, parent_id, ancestors, contact_person, contact_phone, contact_email, address, tenant_type, status, max_users, max_stations, max_chargers, tenant_id, create_by, update_by)
VALUES 
    (100, 'PLATFORM_A', '华南充电平台', NULL, '100', '张经理', '13800100001', 'platform_a@evcs.com', '广东省深圳市南山区科技园', 1, 1, 1000, 500, 50000, 100, 1, 1),
    (200, 'PLATFORM_B', '华东充电平台', NULL, '200', '李经理', '13800200001', 'platform_b@evcs.com', '上海市浦东新区张江高科技园区', 1, 1, 1000, 500, 50000, 200, 1, 1)
ON CONFLICT (id) DO UPDATE SET
    tenant_name = EXCLUDED.tenant_name,
    update_time = CURRENT_TIMESTAMP;

-- ============================================
-- 2. 创建运营商租户
-- ============================================

-- 华南平台运营商（8个）
INSERT INTO sys_tenant (id, tenant_code, tenant_name, parent_id, ancestors, contact_person, contact_phone, contact_email, address, tenant_type, status, max_users, max_stations, max_chargers, tenant_id, create_by, update_by)
VALUES 
    (101, 'OP_A_001', '深圳特来电运营', 100, '100,101', '王运营', '13801010001', 'op001@platform_a.com', '深圳市南山区', 2, 1, 100, 50, 5000, 101, 100, 100),
    (102, 'OP_A_002', '深圳星星充电', 100, '100,102', '刘运营', '13801010002', 'op002@platform_a.com', '深圳市福田区', 2, 1, 100, 50, 5000, 102, 100, 100),
    (103, 'OP_A_003', '广州南方电网', 100, '100,103', '陈运营', '13801010003', 'op003@platform_a.com', '广州市天河区', 2, 1, 100, 50, 5000, 103, 100, 100),
    (104, 'OP_A_004', '东莞普天充电', 100, '100,104', '赵运营', '13801010004', 'op004@platform_a.com', '东莞市南城区', 2, 1, 100, 50, 5000, 104, 100, 100),
    (105, 'OP_A_005', '佛山蔚来能源', 100, '100,105', '孙运营', '13801010005', 'op005@platform_a.com', '佛山市南海区', 2, 1, 100, 50, 5000, 105, 100, 100),
    (106, 'OP_A_006', '珠海小鹏充电', 100, '100,106', '周运营', '13801010006', 'op006@platform_a.com', '珠海市香洲区', 2, 1, 100, 50, 5000, 106, 100, 100),
    (107, 'OP_A_007', '惠州比亚迪充电', 100, '100,107', '吴运营', '13801010007', 'op007@platform_a.com', '惠州市惠城区', 2, 1, 100, 50, 5000, 107, 100, 100),
    (108, 'OP_A_008', '中山理想充电', 100, '100,108', '郑运营', '13801010008', 'op008@platform_a.com', '中山市东区', 2, 1, 100, 50, 5000, 108, 100, 100)
ON CONFLICT (id) DO UPDATE SET
    tenant_name = EXCLUDED.tenant_name,
    update_time = CURRENT_TIMESTAMP;

-- 华东平台运营商（10个）
INSERT INTO sys_tenant (id, tenant_code, tenant_name, parent_id, ancestors, contact_person, contact_phone, contact_email, address, tenant_type, status, max_users, max_stations, max_chargers, tenant_id, create_by, update_by)
VALUES 
    (201, 'OP_B_001', '上海国家电网', 200, '200,201', '钱运营', '13802010001', 'op001@platform_b.com', '上海市浦东新区', 2, 1, 100, 50, 5000, 201, 200, 200),
    (202, 'OP_B_002', '杭州星星充电', 200, '200,202', '冯运营', '13802010002', 'op002@platform_b.com', '杭州市西湖区', 2, 1, 100, 50, 5000, 202, 200, 200),
    (203, 'OP_B_003', '南京特来电', 200, '200,203', '朱运营', '13802010003', 'op003@platform_b.com', '南京市鼓楼区', 2, 1, 100, 50, 5000, 203, 200, 200),
    (204, 'OP_B_004', '苏州蔚来能源', 200, '200,204', '卫运营', '13802010004', 'op004@platform_b.com', '苏州市工业园区', 2, 1, 100, 50, 5000, 204, 200, 200),
    (205, 'OP_B_005', '无锡小鹏充电', 200, '200,205', '蒋运营', '13802010005', 'op005@platform_b.com', '无锡市滨湖区', 2, 1, 100, 50, 5000, 205, 200, 200),
    (206, 'OP_B_006', '宁波理想充电', 200, '200,206', '沈运营', '13802010006', 'op006@platform_b.com', '宁波市鄞州区', 2, 1, 100, 50, 5000, 206, 200, 200),
    (207, 'OP_B_007', '温州比亚迪充电', 200, '200,207', '韩运营', '13802010007', 'op007@platform_b.com', '温州市鹿城区', 2, 1, 100, 50, 5000, 207, 200, 200),
    (208, 'OP_B_008', '嘉兴普天充电', 200, '200,208', '杨运营', '13802010008', 'op008@platform_b.com', '嘉兴市南湖区', 2, 1, 100, 50, 5000, 208, 200, 200),
    (209, 'OP_B_009', '南通极狐充电', 200, '200,209', '朱运营', '13802010009', 'op009@platform_b.com', '南通市崇川区', 2, 1, 100, 50, 5000, 209, 200, 200),
    (210, 'OP_B_010', '常州问界充电', 200, '200,210', '秦运营', '13802010010', 'op010@platform_b.com', '常州市武进区', 2, 1, 100, 50, 5000, 210, 200, 200)
ON CONFLICT (id) DO UPDATE SET
    tenant_name = EXCLUDED.tenant_name,
    update_time = CURRENT_TIMESTAMP;

-- ============================================
-- 3. 使用PL/pgSQL生成充电站和充电桩数据
-- ============================================

DO $$
DECLARE
    v_tenant_id BIGINT;
    v_tenant_code VARCHAR(50);
    v_tenant_name VARCHAR(100);
    v_station_count INTEGER;
    v_station_id BIGINT;
    v_station_code VARCHAR(64);
    v_charger_count INTEGER;
    v_charger_id BIGINT;
    v_charger_code VARCHAR(64);
    v_charger_type INTEGER;
    v_status INTEGER;
    v_total_stations INTEGER := 0;
    v_total_chargers INTEGER := 0;
    
    -- 城市和地址模板
    v_cities VARCHAR(50)[] := ARRAY['深圳市', '广州市', '东莞市', '佛山市', '珠海市', '惠州市', '中山市', '江门市', 
                                     '上海市', '杭州市', '南京市', '苏州市', '无锡市', '宁波市', '温州市', '嘉兴市', '南通市', '常州市'];
    v_districts VARCHAR(50)[] := ARRAY['南山区', '福田区', '罗湖区', '宝安区', '龙岗区', '龙华区', '坪山区', '盐田区',
                                        '天河区', '越秀区', '海珠区', '荔湾区', '白云区', '黄埔区', '番禺区', '花都区',
                                        '浦东新区', '黄浦区', '静安区', '徐汇区', '长宁区', '普陀区', '虹口区', '杨浦区',
                                        '西湖区', '拱墅区', '江干区', '下城区', '上城区', '滨江区', '萧山区', '余杭区'];
    v_street_types VARCHAR(50)[] := ARRAY['科技园', '商业中心', '住宅区', '工业园', '高新区', '开发区', 'CBD', '软件园'];
    
    v_city VARCHAR(50);
    v_district VARCHAR(50);
    v_street VARCHAR(100);
    
    v_brands VARCHAR(50)[] := ARRAY['特来电', '星星充电', '南方电网', '普天', '蔚来', '小鹏', '比亚迪', '理想', '国家电网', '极狐', '问界'];
    v_brand VARCHAR(50);
BEGIN
    -- 遍历所有运营商租户
    FOR v_tenant_id, v_tenant_code, v_tenant_name IN 
        SELECT id, tenant_code, tenant_name 
        FROM sys_tenant 
        WHERE tenant_type = 2 AND deleted = 0 AND id >= 100
        ORDER BY id
    LOOP
        -- 随机生成每个运营商的充电站数量（2-24个）
        v_station_count := 2 + floor(random() * 23)::INTEGER;
        
        RAISE NOTICE '运营商: % (ID: %), 生成 % 个充电站', v_tenant_name, v_tenant_id, v_station_count;
        
        -- 生成充电站
        FOR i IN 1..v_station_count LOOP
            v_station_id := v_tenant_id * 1000 + i;
            v_station_code := 'ST' || LPAD(v_tenant_id::TEXT, 3, '0') || '-' || LPAD(i::TEXT, 3, '0');
            
            -- 随机选择城市、区域和街道
            v_city := v_cities[1 + floor(random() * array_length(v_cities, 1))::INTEGER];
            v_district := v_districts[1 + floor(random() * array_length(v_districts, 1))::INTEGER];
            v_street := v_street_types[1 + floor(random() * array_length(v_street_types, 1))::INTEGER] || i::TEXT || '号';
            
            -- 插入充电站
            INSERT INTO charging_station (
                station_id, tenant_id, station_code, station_name, station_type,
                province, city, district, address, 
                latitude, longitude,
                operator_name, operator_phone, service_hours,
                parking_fee, service_fee, 
                payment_methods, facilities, status,
                create_by, update_by
            ) VALUES (
                v_station_id, v_tenant_id, v_station_code,
                v_tenant_name || '-' || v_district || '站点' || i,
                CASE WHEN random() < 0.8 THEN 1 ELSE 2 END, -- 80%公共站，20%专用站
                CASE WHEN v_tenant_id < 200 THEN '广东省' ELSE '江苏省' END,
                v_city, v_district, v_street,
                22.5 + (random() * 0.5), 113.9 + (random() * 0.5), -- 随机坐标
                v_tenant_name, '400-' || LPAD((v_tenant_id % 10000)::TEXT, 7, '0'), '24小时',
                (2 + floor(random() * 8))::NUMERIC(8,2), -- 停车费2-10元
                (0.4 + random() * 0.6)::NUMERIC(8,2), -- 服务费0.4-1.0元
                ARRAY[1,2,3], '{"wifi": true, "toilet": true}'::jsonb, 1,
                v_tenant_id, v_tenant_id
            );
            
            v_total_stations := v_total_stations + 1;
            
            -- 随机生成每个充电站的充电桩数量（2-120个）
            -- 小站点(30%): 2-10个桩
            -- 中站点(50%): 10-40个桩
            -- 大站点(20%): 40-120个桩
            CASE 
                WHEN random() < 0.3 THEN v_charger_count := 2 + floor(random() * 9)::INTEGER;
                WHEN random() < 0.8 THEN v_charger_count := 10 + floor(random() * 31)::INTEGER;
                ELSE v_charger_count := 40 + floor(random() * 81)::INTEGER;
            END CASE;
            
            -- 生成充电桩
            FOR j IN 1..v_charger_count LOOP
                v_charger_id := v_station_id * 1000 + j;
                v_charger_code := v_station_code || '-' || 
                                 CASE WHEN random() < 0.6 THEN 'DC' ELSE 'AC' END || 
                                 '-' || LPAD(j::TEXT, 3, '0');
                
                -- 60%快充，40%慢充
                v_charger_type := CASE WHEN random() < 0.6 THEN 1 ELSE 2 END;
                
                -- 状态分布: 10%离线，60%空闲，25%充电中，5%故障
                CASE 
                    WHEN random() < 0.10 THEN v_status := 0;
                    WHEN random() < 0.70 THEN v_status := 1;
                    WHEN random() < 0.95 THEN v_status := 2;
                    ELSE v_status := 3;
                END CASE;
                
                -- 随机选择品牌
                v_brand := v_brands[1 + floor(random() * array_length(v_brands, 1))::INTEGER];
                
                -- 插入充电桩
                INSERT INTO charger (
                    charger_id, tenant_id, station_id, charger_code, charger_name,
                    charger_type, brand, model, manufacturer,
                    production_date, operation_date,
                    rated_power, input_voltage, output_voltage_range, output_current_range,
                    gun_count, gun_types, supported_protocols,
                    status, last_heartbeat,
                    total_charging_sessions, total_charging_energy, total_charging_time,
                    enabled, create_by, update_by
                ) VALUES (
                    v_charger_id, v_tenant_id, v_station_id, v_charger_code,
                    v_charger_code,
                    v_charger_type, v_brand, 
                    v_brand || '-' || CASE v_charger_type WHEN 1 THEN '120kW' ELSE '7kW' END,
                    v_brand,
                    CURRENT_DATE - (random() * 730)::INTEGER, -- 2年内生产
                    CURRENT_DATE - (random() * 365)::INTEGER, -- 1年内投运
                    CASE v_charger_type WHEN 1 THEN (60 + random() * 120)::NUMERIC(8,2) ELSE (7 + random() * 4)::NUMERIC(8,2) END,
                    CASE v_charger_type WHEN 1 THEN 380 ELSE 220 END,
                    CASE v_charger_type WHEN 1 THEN '200-750V' ELSE '220V' END,
                    CASE v_charger_type WHEN 1 THEN '0-250A' ELSE '0-32A' END,
                    CASE v_charger_type WHEN 1 THEN 2 ELSE 1 END,
                    CASE v_charger_type WHEN 1 THEN 'CCS,CHAdeMO' ELSE 'Type2' END,
                    '{"ocpp": "1.6", "gb": "2015"}'::jsonb,
                    v_status,
                    CASE WHEN v_status != 0 THEN CURRENT_TIMESTAMP - (random() * 3600)::INTEGER * INTERVAL '1 second' ELSE NULL END,
                    floor(random() * 500)::BIGINT, -- 历史充电次数
                    (random() * 50000)::NUMERIC(12,2), -- 历史充电量
                    floor(random() * 100000)::BIGINT, -- 历史充电时长
                    1, v_tenant_id, v_tenant_id
                );
                
                v_total_chargers := v_total_chargers + 1;
                
                -- 每1000个充电桩输出一次进度
                IF v_total_chargers % 1000 = 0 THEN
                    RAISE NOTICE '已生成 % 个充电站, % 个充电桩', v_total_stations, v_total_chargers;
                END IF;
            END LOOP;
            
        END LOOP;
    END LOOP;
    
    RAISE NOTICE '数据生成完成！总计: % 个充电站, % 个充电桩', v_total_stations, v_total_chargers;
END $$;

-- ============================================
-- 4. 生成运营商用户
-- ============================================
DO $$
DECLARE
    v_tenant_id BIGINT;
    v_user_count INTEGER;
    v_user_id BIGINT;
BEGIN
    FOR v_tenant_id IN 
        SELECT id FROM sys_tenant WHERE tenant_type = 2 AND deleted = 0 AND id >= 100
    LOOP
        -- 每个运营商3-8个用户
        v_user_count := 3 + floor(random() * 6)::INTEGER;
        
        FOR i IN 1..v_user_count LOOP
            v_user_id := v_tenant_id * 100 + i;
            
            INSERT INTO sys_user (
                id, username, password, real_name, phone, email,
                gender, status, user_type, tenant_id,
                create_by, update_by
            ) VALUES (
                v_user_id,
                'user_' || v_tenant_id || '_' || i,
                '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', -- password
                '用户' || v_tenant_id || '-' || i,
                '138' || LPAD((v_tenant_id * 100 + i)::TEXT, 8, '0'),
                'user_' || v_tenant_id || '_' || i || '@evcs.com',
                floor(random() * 3)::INTEGER,
                1, 2, v_tenant_id,
                v_tenant_id, v_tenant_id
            )
            ON CONFLICT (id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- ============================================
-- 5. 生成今日充电订单（随机生成部分充电桩的订单）
-- ============================================
DO $$
DECLARE
    v_charger RECORD;
    v_order_id BIGINT := 1000000;
    v_session_id VARCHAR(64);
    v_today_start TIMESTAMP := date_trunc('day', CURRENT_TIMESTAMP);
    v_start_time TIMESTAMP;
    v_end_time TIMESTAMP;
    v_duration INTEGER;
    v_energy NUMERIC(12,4);
    v_amount NUMERIC(12,4);
    v_order_count INTEGER := 0;
BEGIN
    -- 随机抽取20%的充电桩生成今日订单（状态为空闲或充电中的桩）
    FOR v_charger IN 
        SELECT charger_id, tenant_id, station_id, charger_type, rated_power
        FROM charger 
        WHERE status IN (1, 2) 
        AND random() < 0.2
        LIMIT 5000 -- 最多5000个订单
    LOOP
        v_order_id := v_order_id + 1;
        v_session_id := 'SESSION' || LPAD(v_order_id::TEXT, 10, '0');
        
        -- 随机生成今日某个时间开始充电
        v_start_time := v_today_start + (random() * 16)::INTEGER * INTERVAL '1 hour';
        
        -- 充电时长: 快充20-60分钟，慢充2-8小时
        v_duration := CASE 
            WHEN v_charger.charger_type = 1 THEN 20 + floor(random() * 41)::INTEGER
            ELSE 120 + floor(random() * 361)::INTEGER
        END;
        
        v_end_time := v_start_time + v_duration * INTERVAL '1 minute';
        
        -- 充电量 = 功率 * 时长 * 效率(0.85-0.95)
        v_energy := (v_charger.rated_power * v_duration / 60.0 * (0.85 + random() * 0.1))::NUMERIC(12,4);
        
        -- 金额 = 充电量 * (电价0.5-1.2 + 服务费0.4-0.8)
        v_amount := (v_energy * (0.9 + random() * 0.7))::NUMERIC(12,4);
        
        -- 80%已完成已支付，20%充电中
        INSERT INTO charging_order (
            id, tenant_id, station_id, charger_id, session_id,
            user_id, start_time, end_time,
            energy, duration, amount,
            status, create_by, update_by
        ) VALUES (
            v_order_id, v_charger.tenant_id, v_charger.station_id, v_charger.charger_id,
            v_session_id,
            v_charger.tenant_id * 100 + 1, -- 使用第一个用户
            v_start_time,
            CASE WHEN random() < 0.8 THEN v_end_time ELSE NULL END,
            CASE WHEN random() < 0.8 THEN v_energy ELSE NULL END,
            CASE WHEN random() < 0.8 THEN v_duration ELSE NULL END,
            CASE WHEN random() < 0.8 THEN v_amount ELSE NULL END,
            CASE WHEN random() < 0.8 THEN 11 ELSE 1 END, -- 11=已支付，1=充电中
            v_charger.tenant_id, v_charger.tenant_id
        );
        
        v_order_count := v_order_count + 1;
        
        IF v_order_count % 500 = 0 THEN
            RAISE NOTICE '已生成 % 个订单', v_order_count;
        END IF;
    END LOOP;
    
    RAISE NOTICE '订单生成完成！总计: % 个订单', v_order_count;
END $$;

-- ============================================
-- 6. 统计信息
-- ============================================
SELECT '=== 数据统计 ===' as info;

SELECT '租户信息:' as category, 
       COUNT(*) FILTER (WHERE tenant_type = 1) as platform_count,
       COUNT(*) FILTER (WHERE tenant_type = 2) as operator_count,
       COUNT(*) as total_tenants
FROM sys_tenant WHERE deleted = 0;

SELECT '充电站统计:' as category,
       COUNT(*) as total_stations,
       COUNT(*) FILTER (WHERE station_type = 1) as public_stations,
       COUNT(*) FILTER (WHERE station_type = 2) as private_stations
FROM charging_station WHERE deleted = 0;

SELECT '充电桩统计:' as category,
       COUNT(*) as total_chargers,
       COUNT(*) FILTER (WHERE charger_type = 1) as dc_chargers,
       COUNT(*) FILTER (WHERE charger_type = 2) as ac_chargers,
       COUNT(*) FILTER (WHERE status = 0) as offline,
       COUNT(*) FILTER (WHERE status = 1) as available,
       COUNT(*) FILTER (WHERE status = 2) as charging,
       COUNT(*) FILTER (WHERE status = 3) as fault
FROM charger WHERE deleted = 0;

SELECT '用户统计:' as category,
       COUNT(*) as total_users
FROM sys_user WHERE deleted = 0;

SELECT '订单统计:' as category,
       COUNT(*) as total_orders,
       COUNT(*) FILTER (WHERE status = 11) as paid_orders,
       COUNT(*) FILTER (WHERE status = 1) as charging_orders,
       SUM(energy) as total_energy,
       SUM(amount) as total_revenue
FROM charging_order 
WHERE deleted = 0 
  AND start_time >= date_trunc('day', CURRENT_TIMESTAMP);

-- 各运营商数据统计
SELECT '各运营商统计:' as info;
SELECT 
    t.tenant_name as operator_name,
    COUNT(DISTINCT cs.station_id) as stations,
    COUNT(DISTINCT c.charger_id) as chargers,
    COUNT(DISTINCT co.id) as today_orders,
    COALESCE(SUM(co.energy), 0)::NUMERIC(12,2) as today_energy,
    COALESCE(SUM(co.amount), 0)::NUMERIC(12,2) as today_revenue
FROM sys_tenant t
LEFT JOIN charging_station cs ON t.id = cs.tenant_id AND cs.deleted = 0
LEFT JOIN charger c ON t.id = c.tenant_id AND c.deleted = 0
LEFT JOIN charging_order co ON t.id = co.tenant_id 
    AND co.deleted = 0 
    AND co.start_time >= date_trunc('day', CURRENT_TIMESTAMP)
WHERE t.tenant_type = 2 AND t.deleted = 0 AND t.id >= 100
GROUP BY t.id, t.tenant_name
ORDER BY t.id;
