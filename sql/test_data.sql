-- ============================================
-- 充电站管理平台 - 测试数据
-- ============================================

-- 注意：租户ID=1 和用户 admin 已经在 init.sql 中创建

-- ============================================
-- 1. 添加测试用户
-- ============================================
INSERT INTO sys_user (id, username, password, real_name, phone, email, gender, status, user_type, tenant_id, create_by, update_by)
VALUES 
    (2, 'operator1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '运营员1', '13800138001', 'operator1@evcs.com', 1, 1, 2, 1, 1, 1),
    (3, 'operator2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '运营员2', '13800138002', 'operator2@evcs.com', 0, 1, 2, 1, 1, 1),
    (4, 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', '13800138003', 'test@evcs.com', 2, 1, 2, 1, 1, 1)
ON CONFLICT DO NOTHING;

-- ============================================
-- 2. 添加充电站
-- ============================================
INSERT INTO charging_station (station_id, tenant_id, station_code, station_name, station_type, province, city, district, address, latitude, longitude, operator_name, operator_phone, service_hours, parking_fee, service_fee, payment_methods, facilities, status, create_by, update_by)
VALUES 
    (1, 1, 'ST001', '市中心充电站', 1, '广东省', '深圳市', '南山区', '科技园南路15号', 22.5431, 113.9420, '深圳充电运营', '0755-12345678', '24小时', 5.00, 0.60, ARRAY[1,2,3], '{"wifi": true, "restaurant": true, "toilet": true, "convenience_store": true}'::jsonb, 1, 1, 1),
    (2, 1, 'ST002', '商业中心充电站', 1, '广东省', '深圳市', '福田区', '华强北路88号', 22.5470, 114.0850, '深圳充电运营', '0755-12345678', '06:00-24:00', 8.00, 0.60, ARRAY[1,2,3], '{"wifi": true, "restaurant": true, "toilet": true}'::jsonb, 1, 1, 1),
    (3, 1, 'ST003', '住宅区充电站', 1, '广东省', '深圳市', '龙岗区', '龙城街道中心城小区', 22.7200, 114.2500, '深圳充电运营', '0755-12345678', '24小时', 3.00, 0.50, ARRAY[1,2], '{"wifi": false, "toilet": true}'::jsonb, 1, 1, 1),
    (4, 1, 'ST004', '高速服务区充电站', 1, '广东省', '深圳市', '宝安区', 'G4京港澳高速深圳服务区', 22.6500, 113.8800, '深圳充电运营', '0755-12345678', '24小时', 0.00, 0.80, ARRAY[1,2,3,4], '{"wifi": true, "restaurant": true, "toilet": true, "convenience_store": true, "rest_area": true}'::jsonb, 1, 1, 1),
    (5, 1, 'ST005', '办公楼充电站', 2, '广东省', '深圳市', '南山区', '科技大厦地下停车场', 22.5380, 113.9350, '深圳充电运营', '0755-12345678', '07:00-22:00', 10.00, 0.50, ARRAY[1,2], '{"wifi": true, "toilet": false}'::jsonb, 1, 1, 1)
ON CONFLICT DO NOTHING;

-- ============================================
-- 3. 添加充电桩
-- ============================================
INSERT INTO charger (charger_id, tenant_id, station_id, charger_code, charger_name, charger_type, brand, model, manufacturer, production_date, operation_date, rated_power, input_voltage, output_voltage_range, output_current_range, gun_count, gun_types, supported_protocols, status, create_by, update_by)
VALUES 
    -- 市中心充电站 (8个桩)
    (1, 1, 1, 'ST001-DC-001', '1号快充桩', 1, '特来电', 'TLD-120kW', '特锐德电气', '2023-06-01', '2023-07-15', 120.00, 380, '200-750V', '0-250A', 2, 'CCS,CHAdeMO', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (2, 1, 1, 'ST001-DC-002', '2号快充桩', 1, '特来电', 'TLD-120kW', '特锐德电气', '2023-06-01', '2023-07-15', 120.00, 380, '200-750V', '0-250A', 2, 'CCS,CHAdeMO', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (3, 1, 1, 'ST001-DC-003', '3号快充桩', 1, '星星充电', 'XC-90kW', '万帮数字能源', '2023-05-15', '2023-07-15', 90.00, 380, '200-750V', '0-200A', 1, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 2, 1, 1),
    (4, 1, 1, 'ST001-AC-001', '4号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-06-10', '2023-07-15', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (5, 1, 1, 'ST001-AC-002', '5号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-06-10', '2023-07-15', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (6, 1, 1, 'ST001-DC-004', '6号超充桩', 1, '蔚来', 'NIO-180kW', '蔚来能源', '2023-07-01', '2023-07-15', 180.00, 380, '200-1000V', '0-500A', 2, 'CCS', '{"ocpp": "2.0"}'::jsonb, 2, 1, 1),
    (7, 1, 1, 'ST001-DC-005', '7号快充桩', 1, '特来电', 'TLD-120kW', '特锐德电气', '2023-06-15', '2023-07-15', 120.00, 380, '200-750V', '0-250A', 2, 'CCS,CHAdeMO', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (8, 1, 1, 'ST001-AC-003', '8号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-06-10', '2023-07-15', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 0, 1, 1),
    
    -- 商业中心充电站 (6个桩)
    (9, 1, 2, 'ST002-DC-001', '1号快充桩', 1, '星星充电', 'XC-120kW', '万帮数字能源', '2023-05-20', '2023-06-30', 120.00, 380, '200-750V', '0-250A', 2, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 2, 1, 1),
    (10, 1, 2, 'ST002-DC-002', '2号快充桩', 1, '星星充电', 'XC-120kW', '万帮数字能源', '2023-05-20', '2023-06-30', 120.00, 380, '200-750V', '0-250A', 2, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (11, 1, 2, 'ST002-DC-003', '3号快充桩', 1, '特来电', 'TLD-90kW', '特锐德电气', '2023-06-01', '2023-06-30', 90.00, 380, '200-750V', '0-200A', 1, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (12, 1, 2, 'ST002-AC-001', '4号慢充桩', 2, '南方电网', 'CSG-11kW', '南网能源', '2023-05-25', '2023-06-30', 11.00, 220, '220V', '0-48A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (13, 1, 2, 'ST002-AC-002', '5号慢充桩', 2, '南方电网', 'CSG-11kW', '南网能源', '2023-05-25', '2023-06-30', 11.00, 220, '220V', '0-48A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (14, 1, 2, 'ST002-DC-004', '6号快充桩', 1, '星星充电', 'XC-120kW', '万帮数字能源', '2023-05-20', '2023-06-30', 120.00, 380, '200-750V', '0-250A', 2, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    
    -- 住宅区充电站 (10个桩，主要是慢充)
    (15, 1, 3, 'ST003-AC-001', '1号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-04-15', '2023-05-20', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (16, 1, 3, 'ST003-AC-002', '2号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-04-15', '2023-05-20', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (17, 1, 3, 'ST003-AC-003', '3号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-04-15', '2023-05-20', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 2, 1, 1),
    (18, 1, 3, 'ST003-AC-004', '4号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-04-15', '2023-05-20', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (19, 1, 3, 'ST003-AC-005', '5号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-04-15', '2023-05-20', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (20, 1, 3, 'ST003-AC-006', '6号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-04-20', '2023-05-20', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 0, 1, 1),
    (21, 1, 3, 'ST003-DC-001', '7号快充桩', 1, '星星充电', 'XC-60kW', '万帮数字能源', '2023-05-01', '2023-05-20', 60.00, 380, '200-750V', '0-150A', 1, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (22, 1, 3, 'ST003-DC-002', '8号快充桩', 1, '星星充电', 'XC-60kW', '万帮数字能源', '2023-05-01', '2023-05-20', 60.00, 380, '200-750V', '0-150A', 1, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (23, 1, 3, 'ST003-AC-007', '9号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-04-20', '2023-05-20', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (24, 1, 3, 'ST003-AC-008', '10号慢充桩', 2, '普天', 'PT-7kW', '普天新能源', '2023-04-20', '2023-05-20', 7.00, 220, '220V', '0-32A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    
    -- 高速服务区充电站 (12个桩，全是快充/超充)
    (25, 1, 4, 'ST004-DC-001', '1号超充桩', 1, '蔚来', 'NIO-180kW', '蔚来能源', '2023-07-01', '2023-08-01', 180.00, 380, '200-1000V', '0-500A', 2, 'CCS', '{"ocpp": "2.0"}'::jsonb, 2, 1, 1),
    (26, 1, 4, 'ST004-DC-002', '2号超充桩', 1, '蔚来', 'NIO-180kW', '蔚来能源', '2023-07-01', '2023-08-01', 180.00, 380, '200-1000V', '0-500A', 2, 'CCS', '{"ocpp": "2.0"}'::jsonb, 2, 1, 1),
    (27, 1, 4, 'ST004-DC-003', '3号快充桩', 1, '特来电', 'TLD-120kW', '特锐德电气', '2023-06-20', '2023-08-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS,CHAdeMO', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (28, 1, 4, 'ST004-DC-004', '4号快充桩', 1, '特来电', 'TLD-120kW', '特锐德电气', '2023-06-20', '2023-08-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS,CHAdeMO', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (29, 1, 4, 'ST004-DC-005', '5号快充桩', 1, '星星充电', 'XC-120kW', '万帮数字能源', '2023-06-15', '2023-08-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (30, 1, 4, 'ST004-DC-006', '6号快充桩', 1, '星星充电', 'XC-120kW', '万帮数字能源', '2023-06-15', '2023-08-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (31, 1, 4, 'ST004-DC-007', '7号快充桩', 1, '特来电', 'TLD-120kW', '特锐德电气', '2023-06-20', '2023-08-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS,CHAdeMO', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 2, 1, 1),
    (32, 1, 4, 'ST004-DC-008', '8号快充桩', 1, '特来电', 'TLD-120kW', '特锐德电气', '2023-06-20', '2023-08-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS,CHAdeMO', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (33, 1, 4, 'ST004-DC-009', '9号快充桩', 1, '星星充电', 'XC-120kW', '万帮数字能源', '2023-06-15', '2023-08-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (34, 1, 4, 'ST004-DC-010', '10号快充桩', 1, '星星充电', 'XC-120kW', '万帮数字能源', '2023-06-15', '2023-08-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1),
    (35, 1, 4, 'ST004-DC-011', '11号超充桩', 1, '蔚来', 'NIO-180kW', '蔚来能源', '2023-07-01', '2023-08-01', 180.00, 380, '200-1000V', '0-500A', 2, 'CCS', '{"ocpp": "2.0"}'::jsonb, 1, 1, 1),
    (36, 1, 4, 'ST004-DC-012', '12号超充桩', 1, '蔚来', 'NIO-180kW', '蔚来能源', '2023-07-01', '2023-08-01', 180.00, 380, '200-1000V', '0-500A', 2, 'CCS', '{"ocpp": "2.0"}'::jsonb, 1, 1, 1),
    
    -- 办公楼充电站 (4个桩，慢充为主)
    (37, 1, 5, 'ST005-AC-001', '1号慢充桩', 2, '南方电网', 'CSG-11kW', '南网能源', '2023-05-10', '2023-06-15', 11.00, 220, '220V', '0-48A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (38, 1, 5, 'ST005-AC-002', '2号慢充桩', 2, '南方电网', 'CSG-11kW', '南网能源', '2023-05-10', '2023-06-15', 11.00, 220, '220V', '0-48A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 1, 1, 1),
    (39, 1, 5, 'ST005-AC-003', '3号慢充桩', 2, '南方电网', 'CSG-11kW', '南网能源', '2023-05-10', '2023-06-15', 11.00, 220, '220V', '0-48A', 1, 'Type2', '{"ocpp": "1.6"}'::jsonb, 0, 1, 1),
    (40, 1, 5, 'ST005-DC-001', '4号快充桩', 1, '星星充电', 'XC-60kW', '万帮数字能源', '2023-05-15', '2023-06-15', 60.00, 380, '200-750V', '0-150A', 1, 'CCS', '{"ocpp": "1.6", "gb": "2015"}'::jsonb, 1, 1, 1)
ON CONFLICT DO NOTHING;

-- ============================================
-- 4. 添加计费方案
-- ============================================
INSERT INTO billing_plan (id, tenant_id, station_id, name, code, status, is_default, create_by, update_by)
VALUES 
    (1, 1, NULL, '标准计费方案', 'STANDARD', 1, 1, 1, 1),
    (2, 1, 1, '市中心专用方案', 'ST001_SPECIAL', 1, 0, 1, 1),
    (3, 1, 4, '高速服务区方案', 'ST004_HIGHWAY', 1, 1, 1, 1)
ON CONFLICT DO NOTHING;

-- 标准计费方案时段 (峰谷平)
INSERT INTO billing_plan_segment (tenant_id, plan_id, segment_index, start_time, end_time, energy_price, service_fee, create_by, update_by)
VALUES 
    -- 标准方案
    (1, 1, 1, '00:00', '08:00', 0.4500, 0.6000, 1, 1),  -- 谷时
    (1, 1, 2, '08:00', '12:00', 0.8800, 0.6000, 1, 1),  -- 峰时
    (1, 1, 3, '12:00', '18:00', 0.6500, 0.6000, 1, 1),  -- 平时
    (1, 1, 4, '18:00', '22:00', 0.9200, 0.6000, 1, 1),  -- 峰时
    (1, 1, 5, '22:00', '24:00', 0.5000, 0.6000, 1, 1),  -- 谷时
    
    -- 市中心专用方案
    (1, 2, 1, '00:00', '07:00', 0.5000, 0.7000, 1, 1),
    (1, 2, 2, '07:00', '10:00', 1.0000, 0.7000, 1, 1),
    (1, 2, 3, '10:00', '14:00', 0.7500, 0.7000, 1, 1),
    (1, 2, 4, '14:00', '20:00', 0.9500, 0.7000, 1, 1),
    (1, 2, 5, '20:00', '24:00', 0.6000, 0.7000, 1, 1),
    
    -- 高速服务区方案 (较高价格)
    (1, 3, 1, '00:00', '24:00', 1.2000, 0.8000, 1, 1)  -- 全天统一价
ON CONFLICT DO NOTHING;

-- ============================================
-- 5. 添加充电订单 (今日订单)
-- ============================================
-- 获取今天的日期
DO $$
DECLARE
    today_start TIMESTAMP := date_trunc('day', CURRENT_TIMESTAMP);
    today_08 TIMESTAMP := today_start + INTERVAL '8 hours';
    today_09 TIMESTAMP := today_start + INTERVAL '9 hours';
    today_10 TIMESTAMP := today_start + INTERVAL '10 hours';
    today_11 TIMESTAMP := today_start + INTERVAL '11 hours';
    today_12 TIMESTAMP := today_start + INTERVAL '12 hours';
    today_13 TIMESTAMP := today_start + INTERVAL '13 hours';
    today_14 TIMESTAMP := today_start + INTERVAL '14 hours';
    today_15 TIMESTAMP := today_start + INTERVAL '15 hours';
    today_16 TIMESTAMP := today_start + INTERVAL '16 hours';
BEGIN
    -- 已完成的充电订单
    INSERT INTO charging_order (tenant_id, station_id, charger_id, session_id, user_id, start_time, end_time, energy, duration, amount, billing_plan_id, payment_trade_id, paid_time, status, create_by, update_by)
    VALUES 
        (1, 1, 1, 'SESSION001', 2, today_08, today_08 + INTERVAL '45 minutes', 35.50, 45, 53.25, 1, 'PAY2025012901', today_08 + INTERVAL '50 minutes', 11, 2, 2),
        (1, 1, 2, 'SESSION002', 3, today_08 + INTERVAL '15 minutes', today_08 + INTERVAL '60 minutes', 42.30, 45, 63.45, 1, 'PAY2025012902', today_08 + INTERVAL '65 minutes', 11, 3, 3),
        (1, 1, 3, 'SESSION003', 4, today_09, today_09 + INTERVAL '50 minutes', 38.80, 50, 58.20, 1, 'PAY2025012903', today_09 + INTERVAL '55 minutes', 11, 4, 4),
        (1, 2, 9, 'SESSION004', 2, today_09 + INTERVAL '30 minutes', today_10 + INTERVAL '20 minutes', 45.20, 50, 67.80, 1, 'PAY2025012904', today_10 + INTERVAL '25 minutes', 11, 2, 2),
        (1, 2, 10, 'SESSION005', 3, today_10, today_10 + INTERVAL '55 minutes', 50.10, 55, 75.15, 1, 'PAY2025012905', today_11 + INTERVAL '5 minutes', 11, 3, 3),
        (1, 3, 21, 'SESSION006', 4, today_11, today_11 + INTERVAL '40 minutes', 28.60, 40, 42.90, 1, 'PAY2025012906', today_11 + INTERVAL '45 minutes', 11, 4, 4),
        (1, 4, 27, 'SESSION007', 2, today_12, today_12 + INTERVAL '35 minutes', 55.20, 35, 99.36, 3, 'PAY2025012907', today_12 + INTERVAL '40 minutes', 11, 2, 2),
        (1, 4, 28, 'SESSION008', 3, today_12 + INTERVAL '20 minutes', today_12 + INTERVAL '55 minutes', 48.90, 35, 87.92, 3, 'PAY2025012908', today_13 + INTERVAL '5 minutes', 11, 3, 3),
        (1, 1, 5, 'SESSION009', 4, today_14, today_14 + INTERVAL '3 hours', 18.50, 180, 27.75, 1, 'PAY2025012909', today_14 + INTERVAL '190 minutes', 11, 4, 4),
        (1, 1, 7, 'SESSION010', 2, today_15, today_15 + INTERVAL '48 minutes', 44.30, 48, 66.45, 1, 'PAY2025012910', today_15 + INTERVAL '55 minutes', 11, 2, 2),
        
        -- 正在充电的订单
        (1, 1, 1, 'SESSION011', 3, today_16, NULL, NULL, NULL, NULL, 1, NULL, NULL, 1, 3, 3),
        (1, 2, 10, 'SESSION012', 4, today_16 + INTERVAL '10 minutes', NULL, NULL, NULL, NULL, 1, NULL, NULL, 1, 4, 4),
        (1, 4, 25, 'SESSION013', 2, today_16 + INTERVAL '5 minutes', NULL, NULL, NULL, NULL, 3, NULL, NULL, 1, 2, 2)
    ON CONFLICT DO NOTHING;
END $$;

-- ============================================
-- 6. 更新统计数据
-- ============================================
-- 更新充电站统计
UPDATE charging_station SET 
    total_sessions = 10,
    total_energy = 407.40,
    total_revenue = 641.23
WHERE station_id = 1;

UPDATE charging_station SET 
    total_sessions = 2,
    total_energy = 95.30,
    total_revenue = 142.95
WHERE station_id = 2;

UPDATE charging_station SET 
    total_sessions = 1,
    total_energy = 28.60,
    total_revenue = 42.90
WHERE station_id = 3;

UPDATE charging_station SET 
    total_sessions = 2,
    total_energy = 104.10,
    total_revenue = 187.28
WHERE station_id = 4;

-- 更新充电桩统计
UPDATE charger SET 
    total_charging_sessions = 1,
    total_charging_energy = 35.50,
    total_charging_time = 45,
    last_session_time = CURRENT_TIMESTAMP - INTERVAL '8 hours'
WHERE charger_id IN (1);

UPDATE charger SET 
    total_charging_sessions = 1,
    total_charging_energy = 42.30,
    total_charging_time = 45,
    last_session_time = CURRENT_TIMESTAMP - INTERVAL '8 hours'
WHERE charger_id = 2;

UPDATE charger SET 
    total_charging_sessions = 1,
    total_charging_energy = 38.80,
    total_charging_time = 50,
    last_session_time = CURRENT_TIMESTAMP - INTERVAL '7 hours'
WHERE charger_id = 3;

-- ============================================
-- 7. 验证数据
-- ============================================
-- 查看租户信息
SELECT 'Tenants:' as info, COUNT(*) as count FROM sys_tenant WHERE deleted = 0;

-- 查看用户信息  
SELECT 'Users:' as info, COUNT(*) as count FROM sys_user WHERE deleted = 0;

-- 查看充电站信息
SELECT 'Stations:' as info, COUNT(*) as count FROM charging_station WHERE deleted = 0;

-- 查看充电桩信息
SELECT 'Chargers:' as info, COUNT(*) as count FROM charger WHERE deleted = 0;

-- 按状态统计充电桩
SELECT 'Chargers by status:' as info, 
       status,
       CASE status 
           WHEN 0 THEN '离线'
           WHEN 1 THEN '空闲'
           WHEN 2 THEN '充电中'
           WHEN 3 THEN '故障'
       END as status_name,
       COUNT(*) as count
FROM charger 
WHERE deleted = 0
GROUP BY status
ORDER BY status;

-- 查看订单信息
SELECT 'Orders:' as info, COUNT(*) as count FROM charging_order WHERE deleted = 0;

-- 今日订单统计
SELECT 'Today Orders:' as info, 
       COUNT(*) as total_orders,
       SUM(energy) as total_energy,
       SUM(amount) as total_revenue
FROM charging_order 
WHERE deleted = 0 
  AND start_time >= date_trunc('day', CURRENT_TIMESTAMP)
  AND status IN (1, 11);

-- 查看计费方案
SELECT 'Billing Plans:' as info, COUNT(*) as count FROM billing_plan WHERE deleted = 0;
