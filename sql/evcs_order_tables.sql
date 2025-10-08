-- EVCS Manager - Order & Billing DDL (PostgreSQL)
-- Billing plan, plan segments, charging order and minimal indexes

-- =============================
-- Billing Plan
-- =============================
CREATE TABLE IF NOT EXISTS billing_plan (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64),
    status INTEGER DEFAULT 1, -- 0-禁用 1-启用
    is_default INTEGER DEFAULT 0, -- 0-否 1-是（同站点仅允许一个启用默认）

    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_plan_code_tenant ON billing_plan(tenant_id, code) WHERE code IS NOT NULL AND deleted = 0;
CREATE INDEX IF NOT EXISTS idx_billing_plan_tenant_station ON billing_plan(tenant_id, station_id, status, is_default);

-- =============================
-- Billing Plan Segment (up to 96 per plan)
-- =============================
CREATE TABLE IF NOT EXISTS billing_plan_segment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT, -- 可选：便于多租户查询（若不存，则通过 plan_id 关联查）
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

CREATE INDEX IF NOT EXISTS idx_bps_plan ON billing_plan_segment(plan_id);
CREATE INDEX IF NOT EXISTS idx_bps_plan_idx ON billing_plan_segment(plan_id, segment_index);

-- =============================
-- Charging Order
-- =============================
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

CREATE INDEX IF NOT EXISTS idx_order_tenant ON charging_order(tenant_id, status, deleted);
CREATE INDEX IF NOT EXISTS idx_order_session_tenant ON charging_order(session_id, tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_order_station ON charging_order(station_id);
CREATE INDEX IF NOT EXISTS idx_order_charger ON charging_order(charger_id);
CREATE INDEX IF NOT EXISTS idx_order_plan ON charging_order(billing_plan_id);

-- =============================
-- Alter charger: add billing_plan_id for assignment
-- =============================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name='charger' AND column_name='billing_plan_id'
    ) THEN
        ALTER TABLE charger ADD COLUMN billing_plan_id BIGINT;
        CREATE INDEX IF NOT EXISTS idx_charger_billing_plan ON charger(billing_plan_id);
    END IF;
END $$;

-- =============================
-- Trigger to auto update update_time (shared)
-- =============================
CREATE OR REPLACE FUNCTION update_modified_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

DROP TRIGGER IF EXISTS trg_update_billing_plan ON billing_plan;
CREATE TRIGGER trg_update_billing_plan BEFORE UPDATE ON billing_plan
FOR EACH ROW EXECUTE FUNCTION update_modified_time();

DROP TRIGGER IF EXISTS trg_update_billing_plan_segment ON billing_plan_segment;
CREATE TRIGGER trg_update_billing_plan_segment BEFORE UPDATE ON billing_plan_segment
FOR EACH ROW EXECUTE FUNCTION update_modified_time();

DROP TRIGGER IF EXISTS trg_update_charging_order ON charging_order;
CREATE TRIGGER trg_update_charging_order BEFORE UPDATE ON charging_order
FOR EACH ROW EXECUTE FUNCTION update_modified_time();
