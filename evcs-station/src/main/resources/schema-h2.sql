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
    total_chargers INTEGER DEFAULT 0,
    available_chargers INTEGER DEFAULT 0,
    charging_chargers INTEGER DEFAULT 0,
    fault_chargers INTEGER DEFAULT 0,
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
    charger_type INTEGER,
    rated_power DOUBLE,
    status INTEGER DEFAULT 0,
    last_heartbeat TIMESTAMP,
    current_session_id VARCHAR(64),
    current_user_id BIGINT,
    charging_start_time TIMESTAMP,
    charged_energy DOUBLE,
    charged_duration INTEGER,
    current_power DOUBLE,
    current_voltage DOUBLE,
    current_current DOUBLE,
    temperature DOUBLE,
    total_charging_sessions BIGINT,
    total_charging_energy DOUBLE,
    total_charging_time BIGINT,
    update_time TIMESTAMP,
    create_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_station_code_tenant ON charging_station(station_code, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_charger_code_tenant ON charger(charger_code, tenant_id);
