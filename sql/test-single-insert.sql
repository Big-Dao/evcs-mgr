-- 测试单个站点插入
INSERT INTO charging_station (
    tenant_id, station_code, station_name, station_type, address,
    province, city, district, longitude, latitude, status,
    operator_name, operator_phone, service_hours, parking_fee, facilities,
    create_time, update_time, create_by, update_by, deleted
) VALUES (
    1, 'T1-TEST-001', '测试站点001', 1, '深圳市南山区测试路1号',
    '广东省', '深圳市', '南山区', 113.938, 22.543, 1,
    '系统运营商', '400-8000001', '00:00-24:00', 0.00, '["wifi"]'::jsonb,
    NOW(), NOW(), 1, 1, 0
);

SELECT * FROM charging_station WHERE station_code = 'T1-TEST-001';
