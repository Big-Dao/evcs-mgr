-- =====================================================
-- 03-full-test-data.sql
-- 完善的测试数据初始化脚本
-- 包含：租户层级、多用户、计费方案、订单数据、支付数据
-- =====================================================

-- =====================================================
-- 1. DDL (确保表存在，如果服务未启动)
-- =====================================================

-- 计费方案表
CREATE TABLE IF NOT EXISTS billing_plan (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64),
    status INTEGER DEFAULT 1, -- 0-禁用 1-启用
    is_default INTEGER DEFAULT 0, -- 0-否 1-是
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_plan_code_tenant ON billing_plan(tenant_id, code) WHERE code IS NOT NULL AND deleted = 0;

-- 计费时段表
CREATE TABLE IF NOT EXISTS billing_plan_segment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    plan_id BIGINT NOT NULL REFERENCES billing_plan(id) ON DELETE CASCADE,
    segment_index INTEGER,
    start_time VARCHAR(8) NOT NULL, -- HH:mm
    end_time VARCHAR(8) NOT NULL,   -- HH:mm
    energy_price DECIMAL(10,4) NOT NULL DEFAULT 0,
    service_fee DECIMAL(10,4) NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_plan_segment_plan_idx ON billing_plan_segment(plan_id, segment_index);

-- 充电订单表
CREATE TABLE IF NOT EXISTS charging_order (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT,
    charger_id BIGINT,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    energy DECIMAL(12,4),       -- kWh
    duration BIGINT,            -- minutes
    amount DECIMAL(12,4),       -- total amount
    billing_plan_id BIGINT,
    coupon_id BIGINT,           -- 优惠券ID
    discount_amount DECIMAL(12,4) DEFAULT 0, -- 优惠金额
    pay_amount DECIMAL(12,4),   -- 实付金额
    payment_trade_id VARCHAR(100),
    paid_time TIMESTAMP,
    status INTEGER DEFAULT 0,   -- 0-created, 1-completed, 2-cancelled, 10-to_pay, 11-paid
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_tenant ON charging_order(tenant_id, status, deleted);

-- 支付订单表
CREATE TABLE IF NOT EXISTS payment_order (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    trade_no VARCHAR(64) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status INTEGER NOT NULL DEFAULT 0,
    out_trade_no VARCHAR(64),
    paid_time TIMESTAMP,
    idempotent_key VARCHAR(64),
    description VARCHAR(255),
    pay_params TEXT,
    pay_url VARCHAR(512),
    refund_amount DECIMAL(10, 2),
    refund_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0
);

-- =====================================================
-- 2. 租户与用户数据
-- =====================================================

-- 插入二级租户 (运营商)
INSERT INTO sys_tenant (id, tenant_code, tenant_name, parent_id, ancestors, tenant_type, status, tenant_id)
VALUES (2, 'OPERATOR_BJ', '北京城市运营商', 1, '1', 2, 1, 2)
ON CONFLICT (id) DO NOTHING;

-- 插入三级租户 (商户)
INSERT INTO sys_tenant (id, tenant_code, tenant_name, parent_id, ancestors, tenant_type, status, tenant_id)
VALUES (3, 'MALL_PARTNER', '朝阳大悦城商户', 2, '1,2', 3, 1, 3)
ON CONFLICT (id) DO NOTHING;

-- 插入运营商管理员
INSERT INTO sys_user (id, username, login_identifier, password, real_name, user_type, status, tenant_id)
VALUES (2, 'operator', 'operator@bj', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '运营商管理员', 1, 1, 2)
ON CONFLICT (id) DO NOTHING;

-- 插入商户管理员
INSERT INTO sys_user (id, username, login_identifier, password, real_name, user_type, status, tenant_id)
VALUES (3, 'merchant', 'merchant@mall', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '商户管理员', 1, 1, 3)
ON CONFLICT (id) DO NOTHING;

-- 关联角色
INSERT INTO sys_user_role (user_id, role_id, tenant_id) VALUES (2, 3, 2) ON CONFLICT DO NOTHING; -- OPERATOR_ADMIN
INSERT INTO sys_user_role (user_id, role_id, tenant_id) VALUES (3, 4, 3) ON CONFLICT DO NOTHING; -- STATION_MANAGER

-- =====================================================
-- 2.1 角色权限分配
-- =====================================================

-- 清理旧权限 (防止重复执行脚本时堆积)
DELETE FROM sys_role_permission WHERE role_id IN (3, 4);

-- 为运营商管理员 (Role 3) 分配所有权限
INSERT INTO sys_role_permission (role_id, permission_id, tenant_id)
SELECT 3, id, 2 FROM sys_permission WHERE tenant_id = 1;

-- 为站点管理员 (Role 4) 分配部分权限 (Station, Charger, Order, Billing)
INSERT INTO sys_role_permission (role_id, permission_id, tenant_id)
SELECT 4, id, 3 FROM sys_permission 
WHERE tenant_id = 1 
AND id IN (1, 4, 5, 6, 7, 401, 501, 601, 602, 701);

-- =====================================================
-- 3. 充电站与充电桩数据
-- =====================================================

-- 站点2：运营商站点
INSERT INTO charging_station (
    station_id, tenant_id, station_code, station_name, station_type,
    province, city, district, address, latitude, longitude,
    operator_name, operator_phone, service_hours, parking_fee, service_fee,
    payment_methods, facilities, status, create_by
) VALUES 
(2, 2, 'ST002', '北京三里屯充电站', 1, 
 '北京市', '朝阳区', '三里屯', '三里屯SOHO地下停车场B2', 39.9345, 116.4567,
 '北京城市运营商', '010-88888888', '24小时', 8.00, 1.20,
 ARRAY[1,2], '{"wifi": true, "coffee": true}', 1, 2)
ON CONFLICT (station_id) DO NOTHING;

-- 站点3：商户站点
INSERT INTO charging_station (
    station_id, tenant_id, station_code, station_name, station_type,
    province, city, district, address, latitude, longitude,
    operator_name, operator_phone, service_hours, parking_fee, service_fee,
    payment_methods, facilities, status, create_by
) VALUES 
(3, 3, 'ST003', '朝阳大悦城充电站', 2, 
 '北京市', '朝阳区', '青年路', '朝阳大悦城地下停车场B3', 39.9255, 116.5211,
 '大悦城物业', '010-66666666', '10:00-22:00', 6.00, 1.00,
 ARRAY[1,2,3], '{"shopping": true, "food": true}', 1, 3)
ON CONFLICT (station_id) DO NOTHING;

-- 充电桩 for ST002
INSERT INTO charger (
    tenant_id, station_id, charger_code, charger_name, charger_type,
    brand, model, rated_power, status, create_by
) VALUES 
(2, 2, 'CH002001', '三里屯01号桩', 1, '星星充电', 'XX-120', 120.00, 1, 2),
(2, 2, 'CH002002', '三里屯02号桩', 1, '星星充电', 'XX-120', 120.00, 1, 2),
(2, 2, 'CH002003', '三里屯03号桩', 2, '星星充电', 'XX-07', 7.00, 2, 2)
ON CONFLICT (charger_code, tenant_id) DO NOTHING;

-- 充电桩 for ST003
INSERT INTO charger (
    tenant_id, station_id, charger_code, charger_name, charger_type,
    brand, model, rated_power, status, create_by
) VALUES 
(3, 3, 'CH003001', '大悦城01号桩', 1, '特来电', 'TLD-60', 60.00, 1, 3),
(3, 3, 'CH003002', '大悦城02号桩', 2, '特来电', 'TLD-07', 7.00, 1, 3)
ON CONFLICT (charger_code, tenant_id) DO NOTHING;

-- =====================================================
-- 4. 计费方案数据
-- =====================================================

-- 默认方案 (Tenant 1)
INSERT INTO billing_plan (id, tenant_id, name, code, status, is_default, create_by)
VALUES (1, 1, '标准计费方案', 'PLAN_STD_001', 1, 1, 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO billing_plan_segment (plan_id, segment_index, start_time, end_time, energy_price, service_fee)
VALUES 
(1, 1, '00:00', '08:00', 0.4000, 0.6000),
(1, 2, '08:00', '18:00', 1.2000, 0.8000),
(1, 3, '18:00', '24:00', 0.8000, 0.6000)
ON CONFLICT (plan_id, segment_index) DO NOTHING;

-- 运营商方案 (Tenant 2)
INSERT INTO billing_plan (id, tenant_id, name, code, status, is_default, create_by)
VALUES (2, 2, 'VIP计费方案', 'PLAN_VIP_001', 1, 1, 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO billing_plan_segment (plan_id, segment_index, start_time, end_time, energy_price, service_fee)
VALUES 
(2, 1, '00:00', '24:00', 0.8000, 0.4000)
ON CONFLICT (plan_id, segment_index) DO NOTHING;

-- =====================================================
-- 5. 订单与支付数据 (生成过去7天的数据)
-- =====================================================

-- 插入一些已完成的订单
INSERT INTO charging_order (
    tenant_id, station_id, charger_id, session_id, user_id,
    start_time, end_time, energy, duration, amount,
    billing_plan_id, status, create_time
)
SELECT 
    1, 1, 1, 'SESSION_' || generate_series, 1,
    NOW() - (generate_series || ' days')::interval, 
    NOW() - (generate_series || ' days')::interval + '1 hour'::interval,
    30.5, 60, 45.50,
    1, 11, -- Paid
    NOW() - (generate_series || ' days')::interval
FROM generate_series(1, 7);

-- 插入一些进行中的订单
INSERT INTO charging_order (
    tenant_id, station_id, charger_id, session_id, user_id,
    start_time, status, create_time
) VALUES 
(2, 2, 3, 'SESSION_ACTIVE_001', 2, NOW() - '30 minutes'::interval, 1, NOW()), -- Charging
(3, 3, 5, 'SESSION_ACTIVE_002', 3, NOW() - '15 minutes'::interval, 1, NOW());

-- 插入支付记录 (对应已完成订单)
INSERT INTO payment_order (
    tenant_id, order_id, trade_no, payment_method, amount, status, paid_time, create_time
)
SELECT 
    tenant_id, id, 'TRADE_' || session_id, 'ALIPAY', amount, 2, end_time, end_time
FROM charging_order
WHERE status = 11;

