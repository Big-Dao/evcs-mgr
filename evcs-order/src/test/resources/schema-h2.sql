-- Charging Order schema for H2 test database

CREATE TABLE IF NOT EXISTS charging_order (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT,
    charger_id BIGINT,
    session_id VARCHAR(64),
    user_id BIGINT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    energy DOUBLE,
    duration BIGINT,
    amount DECIMAL(10,2),
    billing_plan_id BIGINT,
    payment_trade_id VARCHAR(100),
    paid_time TIMESTAMP,
    status INTEGER DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
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
