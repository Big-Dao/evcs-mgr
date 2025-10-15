-- Week 4 Day 4: Database Query Optimization
-- Add performance indexes for charging_order table

-- Guard to ensure core billing/ordering tables exist even if Flyway V1 was skipped
CREATE TABLE IF NOT EXISTS billing_plan (
	id BIGSERIAL PRIMARY KEY,
	tenant_id BIGINT NOT NULL,
	station_id BIGINT,
	name VARCHAR(100) NOT NULL,
	code VARCHAR(64),
	status INTEGER DEFAULT 1,
	is_default INTEGER DEFAULT 0,

	create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	create_by BIGINT,
	update_by BIGINT,
	deleted INTEGER DEFAULT 0,
	version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_billing_plan_code_tenant ON billing_plan(tenant_id, code) WHERE code IS NOT NULL AND deleted = 0;
CREATE INDEX IF NOT EXISTS idx_billing_plan_tenant_station ON billing_plan(tenant_id, station_id, status, is_default);

CREATE TABLE IF NOT EXISTS billing_plan_segment (
	id BIGSERIAL PRIMARY KEY,
	tenant_id BIGINT,
	plan_id BIGINT NOT NULL REFERENCES billing_plan(id) ON DELETE CASCADE,
	segment_index INTEGER,
	start_time VARCHAR(8) NOT NULL,
	end_time VARCHAR(8) NOT NULL,
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

CREATE TABLE IF NOT EXISTS charging_order (
	id BIGSERIAL PRIMARY KEY,
	tenant_id BIGINT NOT NULL,
	station_id BIGINT,
	charger_id BIGINT,
	session_id VARCHAR(64) NOT NULL,
	user_id BIGINT,

	start_time TIMESTAMP,
	end_time TIMESTAMP,

	energy DECIMAL(12,4),
	duration BIGINT,
	amount DECIMAL(12,4),

	billing_plan_id BIGINT,
	payment_trade_id VARCHAR(100),
	paid_time TIMESTAMP,

	status INTEGER DEFAULT 0,

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

-- Index for tenant-based time range queries (used in reconciliation and reports)
-- Covers: tenant_id + start_time for efficient time-based filtering
CREATE INDEX IF NOT EXISTS idx_charging_order_tenant_start_time 
ON charging_order(tenant_id, start_time);

-- Index for charger status queries (used in charger management)
-- Covers: charger_id + status for efficient charger order lookup
CREATE INDEX IF NOT EXISTS idx_charging_order_charger_status 
ON charging_order(charger_id, status);

-- Index for station-based queries through charger relationship
-- Note: Assumes there's a charger table with station_id and status columns
-- This index is added to the charger table for efficient station filtering
-- CREATE INDEX IF NOT EXISTS idx_charger_station_status 
-- ON charger(station_id, status);
-- Commented out as charger table is in evcs-station module

COMMENT ON INDEX idx_charging_order_tenant_start_time IS 'Performance index for tenant-based time range queries';
COMMENT ON INDEX idx_charging_order_charger_status IS 'Performance index for charger status queries';
