-- Integration Test Database Schema for H2
-- Includes schemas from all modules

-- ==================== Station Module ====================

CREATE TABLE IF NOT EXISTS charging_station (
    station_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_code VARCHAR(64) NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    latitude DOUBLE,
    longitude DOUBLE,
    status INTEGER DEFAULT 1,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    -- 统计字段已移除 (total_chargers, available_chargers, charging_chargers, fault_chargers)
    -- 这些字段通过 StationMapper 的 JOIN 查询实时计算
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS charger (
    charger_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    charger_code VARCHAR(64) NOT NULL,
    charger_name VARCHAR(100),
    station_code VARCHAR(64),
    charger_type INTEGER,
    brand VARCHAR(100),
    model VARCHAR(100),
    manufacturer VARCHAR(100),
    production_date DATE,
    operation_date DATE,
    rated_power DECIMAL(10,2),
    input_voltage INTEGER,
    output_voltage_range VARCHAR(50),
    output_current_range VARCHAR(50),
    gun_count INTEGER DEFAULT 1,
    gun_types VARCHAR(255),
    supported_protocols TEXT,
    status INTEGER DEFAULT 0,
    fault_code VARCHAR(50),
    fault_description VARCHAR(500),
    enabled INTEGER DEFAULT 1,
    last_heartbeat TIMESTAMP,
    current_session_id VARCHAR(64),
    current_user_id BIGINT,
    charging_start_time TIMESTAMP,
    charged_energy DECIMAL(10,2),
    charged_duration INTEGER,
    current_power DECIMAL(10,2),
    current_voltage DECIMAL(10,2),
    current_current DECIMAL(10,2),
    temperature DECIMAL(10,2),
    signal_strength INTEGER,
    firmware_version VARCHAR(50),
    last_maintenance_time TIMESTAMP,
    next_maintenance_time TIMESTAMP,
    billing_plan_id BIGINT,
    remark VARCHAR(500),
    total_charging_sessions BIGINT,
    total_charging_energy DECIMAL(10,2),
    total_charging_time BIGINT,
    update_time TIMESTAMP,
    create_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_station_code_tenant ON charging_station(station_code, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_charger_code_tenant ON charger(charger_code, tenant_id);

-- ==================== Order Module ====================

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
    payment_trade_id VARCHAR(100),
    paid_time TIMESTAMP,

    status INTEGER DEFAULT 0,   -- 0-created, 1-completed, 2-cancelled, 10-to_pay, 11-paid, 12-refunding, 13-refunded

    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS billing_plan (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status INTEGER DEFAULT 1,
    is_default INTEGER DEFAULT 0,
    priority INTEGER DEFAULT 0,
    effective_start_date DATE,
    effective_end_date DATE,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS billing_plan_segment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    start_time TIME,
    end_time TIME,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS billing_rate (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    segment_id BIGINT NOT NULL,
    rate_type INTEGER,
    rate DECIMAL(10,4),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_plan_code_tenant ON billing_plan(code, tenant_id);

-- ==================== Payment Module ====================

CREATE TABLE IF NOT EXISTS payment_order (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    trade_no VARCHAR(100) NOT NULL,
    payment_method VARCHAR(50),
    amount DECIMAL(10,2) NOT NULL,
    status INTEGER DEFAULT 0,
    out_trade_no VARCHAR(100),
    paid_time TIMESTAMP,
    idempotent_key VARCHAR(100),
    description VARCHAR(500),
    pay_params TEXT,
    pay_url VARCHAR(500),
    refund_amount DECIMAL(10,2),
    refund_time TIMESTAMP,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_trade_no ON payment_order(trade_no);
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payment_order(order_id);
