-- Add per-connector telemetry snapshot columns + session history + curve points.
-- These tables are tenant-scoped and use soft delete via `deleted`.

-- 1) Extend charger_connector with latest telemetry snapshot fields (nullable)
ALTER TABLE charger_connector
	ADD COLUMN IF NOT EXISTS last_meter_time TIMESTAMP;

ALTER TABLE charger_connector
	ADD COLUMN IF NOT EXISTS last_voltage NUMERIC(10,2);

ALTER TABLE charger_connector
	ADD COLUMN IF NOT EXISTS last_current NUMERIC(10,2);

ALTER TABLE charger_connector
	ADD COLUMN IF NOT EXISTS last_power NUMERIC(12,4);

ALTER TABLE charger_connector
	ADD COLUMN IF NOT EXISTS last_soc NUMERIC(5,2);

ALTER TABLE charger_connector
	ADD COLUMN IF NOT EXISTS last_energy NUMERIC(12,4);


-- 2) Session history table (for browsing historical sessions and attaching curves)
CREATE TABLE IF NOT EXISTS charger_connector_session (
	charger_connector_session_id BIGSERIAL PRIMARY KEY,
	tenant_id                   BIGINT       NOT NULL,
	charger_id                  BIGINT       NOT NULL,
	connector_no                INTEGER      NOT NULL,
	session_id                  VARCHAR(64)  NOT NULL,
	protocol_type               VARCHAR(32),
	start_time                  TIMESTAMP,
	stop_time                   TIMESTAMP,
	initial_energy              NUMERIC(12,4),
	total_energy                NUMERIC(12,4),
	duration_seconds            BIGINT,
	last_sample_time            TIMESTAMP,
	last_voltage                NUMERIC(10,2),
	last_current                NUMERIC(10,2),
	last_power                  NUMERIC(12,4),
	last_soc                    NUMERIC(5,2),
	last_energy                 NUMERIC(12,4),
	status                      INTEGER      DEFAULT 1, -- 1=ACTIVE, 2=STOPPED
	create_time                 TIMESTAMP,
	update_time                 TIMESTAMP,
	create_by                   BIGINT,
	update_by                   BIGINT,
	deleted                     INTEGER      DEFAULT 0,
	version                     INTEGER      DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_connector_session_tenant
	ON charger_connector_session(tenant_id, charger_id, connector_no, session_id)
	WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_connector_session_list
	ON charger_connector_session(tenant_id, charger_id, connector_no, start_time DESC)
	WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_connector_session_by_session
	ON charger_connector_session(tenant_id, session_id)
	WHERE deleted = 0;


-- 3) Curve points table (time-series)
CREATE TABLE IF NOT EXISTS charger_connector_curve_point (
	charger_connector_curve_point_id BIGSERIAL PRIMARY KEY,
	tenant_id                        BIGINT      NOT NULL,
	charger_id                       BIGINT      NOT NULL,
	connector_no                     INTEGER     NOT NULL,
	session_id                       VARCHAR(64) NOT NULL,
	sample_time                      TIMESTAMP   NOT NULL,
	voltage                          NUMERIC(10,2),
	current                          NUMERIC(10,2),
	power                            NUMERIC(12,4),
	soc                              NUMERIC(5,2),
	energy                           NUMERIC(12,4),
	duration_seconds                 BIGINT,
	create_time                      TIMESTAMP,
	update_time                      TIMESTAMP,
	create_by                        BIGINT,
	update_by                        BIGINT,
	deleted                          INTEGER     DEFAULT 0,
	version                          INTEGER     DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_connector_curve_point
	ON charger_connector_curve_point(tenant_id, charger_id, connector_no, session_id, sample_time)
	WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_connector_curve_query
	ON charger_connector_curve_point(tenant_id, charger_id, connector_no, session_id, sample_time)
	WHERE deleted = 0;
