-- Create per-connector (gun) table for charger status/alarms/sessions.
-- This table is tenant-scoped and uses soft delete via `deleted`.

CREATE TABLE IF NOT EXISTS charger_connector (
	charger_connector_id BIGSERIAL PRIMARY KEY,
	tenant_id            BIGINT       NOT NULL,
	charger_id           BIGINT       NOT NULL,
	connector_no         INTEGER      NOT NULL,
	connector_type       VARCHAR(64),
	status               INTEGER      DEFAULT 0,
	fault_code           VARCHAR(50),
	fault_description    VARCHAR(500),
	last_heartbeat       TIMESTAMP,
	current_session_id   VARCHAR(64),
	current_user_id      BIGINT,
	charging_start_time  TIMESTAMP,
	charged_energy       NUMERIC(10,2),
	charged_duration     INTEGER,
	create_time          TIMESTAMP,
	update_time          TIMESTAMP,
	create_by            BIGINT,
	update_by            BIGINT,
	deleted              INTEGER      DEFAULT 0,
	version              INTEGER      DEFAULT 0
);

-- Ensure a charger cannot have two active connector rows with the same connector_no.
CREATE UNIQUE INDEX IF NOT EXISTS uk_charger_connector_no_tenant
	ON charger_connector(tenant_id, charger_id, connector_no)
	WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_charger_connector_charger
	ON charger_connector(charger_id);

CREATE INDEX IF NOT EXISTS idx_charger_connector_status
	ON charger_connector(charger_id, status)
	WHERE deleted = 0;
