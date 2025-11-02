-- 为 platform_admin_a 用户（租户ID: 100）生成完整测试数据
-- 用户ID: 100001

-- 1. 创建充电站数据
INSERT INTO charging_station (station_id, tenant_id, station_code, station_name, station_type, address, 
    province, city, district, longitude, latitude, status, operator_name, 
    operator_phone, service_hours, parking_fee, facilities, create_time, update_time, create_by, 
    update_by, deleted) 
VALUES 
    -- 租户100的第一个充电站
    (101, 100, 'T100-ST001', '平台A - 科技园充电站', 1, '深圳市南山区科技园南路15号', 
     '广东省', '深圳市', '南山区', 113.9382, 22.5431, 1, '平台A运营', 
     '0755-88888888', '00:00-24:00', 5.00, 
     '["WiFi", "休息区", "咖啡厅", "便利店", "洗手间"]'::jsonb, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    -- 租户100的第二个充电站
    (102, 100, 'T100-ST002', '平台A - 商业中心站', 1, '深圳市福田区福华路123号', 
     '广东省', '深圳市', '福田区', 114.0579, 22.5431, 1, '平台A运营', 
     '0755-99999999', '06:00-23:00', 10.00, 
     '["WiFi", "休息区", "餐厅", "充电宝租借"]'::jsonb, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    -- 租户100的第三个充电站
    (103, 100, 'T100-ST003', '平台A - 住宅区站点', 2, '深圳市宝安区宝安大道888号', 
     '广东省', '深圳市', '宝安区', 113.8839, 22.5549, 1, '平台A运营', 
     '0755-77777777', '00:00-24:00', 0.00, 
     '["WiFi", "监控", "照明"]'::jsonb, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0)
ON CONFLICT (station_id) DO NOTHING;

-- 2. 创建充电桩数据
INSERT INTO charger (charger_id, tenant_id, station_id, charger_code, charger_name, charger_type, brand, model, 
    manufacturer, production_date, operation_date, rated_power, input_voltage, output_voltage_range, 
    output_current_range, gun_count, gun_types, supported_protocols, status, total_charging_sessions, 
    total_charging_energy, total_charging_time, enabled, create_time, update_time, create_by, update_by, deleted) 
VALUES 
    -- 科技园充电站的充电桩
    (1001, 100, 101, 'T100-ST001-DC-001', '科技园1号快充', 1, '特来电', 'TLD-180kW', '特锐德电气', 
     '2024-01-15', '2024-02-01', 180.00, 380, '200-1000V', '0-500A', 2, 'CCS,CHAdeMO', 
     '{"gb": "2015", "ocpp": "1.6"}'::jsonb, 1, 156, 8234.50, 12450, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    (1002, 100, 101, 'T100-ST001-DC-002', '科技园2号快充', 1, '星星充电', 'XC-120kW', '万帮数字能源', 
     '2024-01-15', '2024-02-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS,CHAdeMO', 
     '{"gb": "2015", "ocpp": "1.6"}'::jsonb, 1, 143, 7456.80, 11230, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    (1003, 100, 101, 'T100-ST001-DC-003', '科技园3号快充', 1, '蔚来', 'NIO-180kW', '蔚来能源', 
     '2024-02-01', '2024-02-15', 180.00, 380, '200-1000V', '0-500A', 2, 'CCS', 
     '{"ocpp": "2.0"}'::jsonb, 2, 89, 5234.20, 8450, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    (1004, 100, 101, 'T100-ST001-AC-001', '科技园4号慢充', 2, '普天', 'PT-11kW', '普天新能源', 
     '2024-01-20', '2024-02-01', 11.00, 220, '220V', '0-48A', 1, 'Type2', 
     '{"ocpp": "1.6"}'::jsonb, 1, 234, 3456.70, 25600, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    (1005, 100, 101, 'T100-ST001-AC-002', '科技园5号慢充', 2, '普天', 'PT-7kW', '普天新能源', 
     '2024-01-20', '2024-02-01', 7.00, 220, '220V', '0-32A', 1, 'Type2', 
     '{"ocpp": "1.6"}'::jsonb, 1, 198, 2345.60, 21400, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    -- 商业中心站的充电桩
    (1006, 100, 102, 'T100-ST002-DC-001', '商业中心1号快充', 1, '特来电', 'TLD-120kW', '特锐德电气', 
     '2024-02-10', '2024-03-01', 120.00, 380, '200-750V', '0-250A', 2, 'CCS,CHAdeMO', 
     '{"gb": "2015", "ocpp": "1.6"}'::jsonb, 1, 267, 14567.80, 18920, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    (1007, 100, 102, 'T100-ST002-DC-002', '商业中心2号快充', 1, '星星充电', 'XC-90kW', '万帮数字能源', 
     '2024-02-10', '2024-03-01', 90.00, 380, '200-750V', '0-200A', 1, 'CCS', 
     '{"gb": "2015", "ocpp": "1.6"}'::jsonb, 1, 189, 9876.50, 14560, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    (1008, 100, 102, 'T100-ST002-AC-001', '商业中心3号慢充', 2, '南方电网', 'CSG-11kW', '南网能源', 
     '2024-02-15', '2024-03-01', 11.00, 220, '220V', '0-48A', 1, 'Type2', 
     '{"ocpp": "1.6"}'::jsonb, 1, 456, 6789.40, 42300, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    -- 住宅区站点的充电桩
    (1009, 100, 103, 'T100-ST003-AC-001', '住宅区1号慢充', 2, '普天', 'PT-7kW', '普天新能源', 
     '2024-03-01', '2024-03-15', 7.00, 220, '220V', '0-32A', 1, 'Type2', 
     '{"ocpp": "1.6"}'::jsonb, 1, 345, 4567.80, 38900, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    (1010, 100, 103, 'T100-ST003-AC-002', '住宅区2号慢充', 2, '普天', 'PT-7kW', '普天新能源', 
     '2024-03-01', '2024-03-15', 7.00, 220, '220V', '0-32A', 1, 'Type2', 
     '{"ocpp": "1.6"}'::jsonb, 1, 312, 3987.60, 35600, 1, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0)
ON CONFLICT (charger_id) DO NOTHING;

-- 3. 创建计费方案
INSERT INTO billing_plan (id, tenant_id, station_id, name, code, status, is_default, priority, 
    create_time, update_time, create_by, update_by, deleted) 
VALUES 
    (1001, 100, NULL, '平台A - 标准计费方案', 'T100_STANDARD', 1, 1, 0, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    (1002, 100, 101, '平台A - 科技园专属方案', 'T100_ST001_SPECIAL', 1, 0, 0, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
     
    (1003, 100, 102, '平台A - 商业中心优惠方案', 'T100_ST002_DISCOUNT', 1, 1, 0, 
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0)
ON CONFLICT (id) DO NOTHING;

-- 4. 创建计费分段
INSERT INTO billing_plan_segment (id, tenant_id, plan_id, start_time, end_time, energy_price, service_fee, 
    create_time, update_time, create_by, update_by, deleted) 
VALUES 
    -- 标准方案分段
    (10001, 100, 1001, '00:00', '08:00', 0.55, 0.40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    (10002, 100, 1001, '08:00', '12:00', 0.85, 0.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    (10003, 100, 1001, '12:00', '18:00', 0.70, 0.45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    (10004, 100, 1001, '18:00', '22:00', 0.95, 0.55, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    (10005, 100, 1001, '22:00', '24:00', 0.55, 0.40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    
    -- 科技园专属方案分段
    (10006, 100, 1002, '00:00', '08:00', 0.50, 0.35, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    (10007, 100, 1002, '08:00', '18:00', 0.75, 0.45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    (10008, 100, 1002, '18:00', '24:00', 0.65, 0.40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    
    -- 商业中心优惠方案分段
    (10009, 100, 1003, '00:00', '10:00', 0.60, 0.38, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    (10010, 100, 1003, '10:00', '22:00', 0.80, 0.48, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0),
    (10011, 100, 1003, '22:00', '24:00', 0.58, 0.35, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100001, 100001, 0)
ON CONFLICT (id) DO NOTHING;

-- 5. 创建充电订单（最近30天的订单）
INSERT INTO charging_order (id, tenant_id, station_id, charger_id, session_id, user_id, start_time, end_time, 
    energy, duration, amount, billing_plan_id, payment_trade_id, paid_time, status, 
    create_time, update_time, create_by, update_by, deleted) 
VALUES 
    -- 已完成并支付的订单
    (100001, 100, 101, 1001, 'T100_SESSION_001', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '5 days' - INTERVAL '2 hours', 
     CURRENT_TIMESTAMP - INTERVAL '5 days' - INTERVAL '1 hour', 
     45.80, 58, 68.70, 1001, 'PAY_T100_001', CURRENT_TIMESTAMP - INTERVAL '5 days', 11, 
     CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 100001, 100001, 0),
     
    (100002, 100, 101, 1002, 'T100_SESSION_002', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '4 days' - INTERVAL '3 hours', 
     CURRENT_TIMESTAMP - INTERVAL '4 days' - INTERVAL '2 hours', 
     38.50, 52, 57.75, 1002, 'PAY_T100_002', CURRENT_TIMESTAMP - INTERVAL '4 days', 11, 
     CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days', 100001, 100001, 0),
     
    (100003, 100, 102, 1006, 'T100_SESSION_003', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '3 days' - INTERVAL '4 hours', 
     CURRENT_TIMESTAMP - INTERVAL '3 days' - INTERVAL '3 hours', 
     52.30, 61, 78.45, 1003, 'PAY_T100_003', CURRENT_TIMESTAMP - INTERVAL '3 days', 11, 
     CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days', 100001, 100001, 0),
     
    (100004, 100, 101, 1004, 'T100_SESSION_004', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '2 days' - INTERVAL '5 hours', 
     CURRENT_TIMESTAMP - INTERVAL '2 days' - INTERVAL '1 hour', 
     28.90, 245, 34.68, 1001, 'PAY_T100_004', CURRENT_TIMESTAMP - INTERVAL '2 days', 11, 
     CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days', 100001, 100001, 0),
     
    (100005, 100, 103, 1009, 'T100_SESSION_005', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '8 hours', 
     CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '2 hours', 
     35.60, 360, 40.32, 1001, 'PAY_T100_005', CURRENT_TIMESTAMP - INTERVAL '1 day', 11, 
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day', 100001, 100001, 0),
     
    -- 充电中的订单
    (100006, 100, 102, 1007, 'T100_SESSION_006', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '45 minutes', NULL, 
     18.50, 45, NULL, 1003, NULL, NULL, 1, 
     CURRENT_TIMESTAMP - INTERVAL '45 minutes', CURRENT_TIMESTAMP - INTERVAL '45 minutes', 100001, 100001, 0),
     
    -- 待支付的订单
    (100007, 100, 101, 1003, 'T100_SESSION_007', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '6 hours', 
     CURRENT_TIMESTAMP - INTERVAL '5 hours', 
     62.30, 58, 93.45, 1002, NULL, NULL, 10, 
     CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '6 hours', 100001, 100001, 0),
     
    -- 更多历史订单
    (100008, 100, 102, 1008, 'T100_SESSION_008', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '7 days' - INTERVAL '2 hours', 
     CURRENT_TIMESTAMP - INTERVAL '7 days' - INTERVAL '30 minutes', 
     15.80, 90, 18.96, 1003, 'PAY_T100_008', CURRENT_TIMESTAMP - INTERVAL '7 days', 11, 
     CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '7 days', 100001, 100001, 0),
     
    (100009, 100, 103, 1010, 'T100_SESSION_009', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '8 days' - INTERVAL '10 hours', 
     CURRENT_TIMESTAMP - INTERVAL '8 days' - INTERVAL '4 hours', 
     32.40, 360, 36.72, 1001, 'PAY_T100_009', CURRENT_TIMESTAMP - INTERVAL '8 days', 11, 
     CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days', 100001, 100001, 0),
     
    (100010, 100, 101, 1001, 'T100_SESSION_010', 100001, 
     CURRENT_TIMESTAMP - INTERVAL '10 days' - INTERVAL '3 hours', 
     CURRENT_TIMESTAMP - INTERVAL '10 days' - INTERVAL '2 hours', 
     48.90, 65, 73.35, 1001, 'PAY_T100_010', CURRENT_TIMESTAMP - INTERVAL '10 days', 11, 
     CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days', 100001, 100001, 0)
ON CONFLICT (id) DO NOTHING;

-- 打印完成信息
SELECT '测试数据生成完成！' as message;
SELECT '租户ID: 100' as info;
SELECT '用户ID: 100001 (platform_admin_a)' as info;
SELECT '充电站数量: 3' as info;
SELECT '充电桩数量: 10' as info;
SELECT '计费方案数量: 3' as info;
SELECT '充电订单数量: 10' as info;
