-- 创建充电桩表（修复版）
CREATE TABLE IF NOT EXISTS charger (
    charger_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    charger_code VARCHAR(64) NOT NULL,
    charger_name VARCHAR(100) NOT NULL,
    
    -- 基本信息
    charger_type INTEGER NOT NULL, -- 1-直流快充 2-交流慢充
    brand VARCHAR(50),
    model VARCHAR(100),
    manufacturer VARCHAR(100),
    production_date DATE,
    operation_date DATE,
    
    -- 技术参数
    rated_power DECIMAL(8,2) NOT NULL,
    input_voltage INTEGER,
    output_voltage_range VARCHAR(50),
    output_current_range VARCHAR(50),
    
    -- 枪头信息
    gun_count INTEGER DEFAULT 1,
    gun_types VARCHAR(100),
    
    -- 协议支持
    supported_protocols JSONB DEFAULT '{"ocpp": "1.6"}'::jsonb,
    
    -- 实时状态
    status INTEGER DEFAULT 0,
    fault_code VARCHAR(20),
    fault_description TEXT,
    last_heartbeat TIMESTAMP,
    
    -- 运行统计
    total_charging_sessions BIGINT DEFAULT 0,
    total_charging_energy DECIMAL(12,2) DEFAULT 0,
    total_charging_time BIGINT DEFAULT 0,
    
    -- 当前充电会话
    current_session_id VARCHAR(64),
    current_user_id BIGINT,
    charging_start_time TIMESTAMP,
    charged_energy DECIMAL(8,2) DEFAULT 0,
    charged_duration INTEGER DEFAULT 0,
    
    -- 实时数据
    current_power DECIMAL(8,2),
    current_voltage DECIMAL(8,2),
    current_current DECIMAL(8,2),
    temperature DECIMAL(5,2),
    signal_strength INTEGER,
    
    -- 维护信息
    firmware_version VARCHAR(50),
    last_maintenance_time TIMESTAMP,
    next_maintenance_time TIMESTAMP,
    last_session_time TIMESTAMP,
    
    -- 管理字段
    enabled INTEGER DEFAULT 1,
    remark TEXT,
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    
    -- 计费方案
    billing_plan_id BIGINT,
    
    -- 约束
    CONSTRAINT uk_charger_code_tenant UNIQUE(charger_code, tenant_id),
    CONSTRAINT fk_charger_station FOREIGN KEY (station_id) REFERENCES charging_station(station_id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_charger_tenant_id ON charger(tenant_id);
CREATE INDEX IF NOT EXISTS idx_charger_station_id ON charger(station_id);
CREATE INDEX IF NOT EXISTS idx_charger_code ON charger(charger_code);
CREATE INDEX IF NOT EXISTS idx_charger_status ON charger(status);
CREATE INDEX IF NOT EXISTS idx_charger_type ON charger(charger_type);
CREATE INDEX IF NOT EXISTS idx_charger_heartbeat ON charger(last_heartbeat);
CREATE INDEX IF NOT EXISTS idx_charger_session ON charger(current_session_id);
CREATE INDEX IF NOT EXISTS idx_charger_billing_plan ON charger(billing_plan_id);
CREATE INDEX IF NOT EXISTS idx_charger_tenant_status ON charger(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_charger_station_status ON charger(station_id, status);

-- 创建触发器
DROP TRIGGER IF EXISTS update_charger_modtime ON charger;
CREATE TRIGGER update_charger_modtime 
    BEFORE UPDATE ON charger 
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
