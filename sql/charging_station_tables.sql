-- 充电站管理平台 - 充电站和充电桩表结构
-- PostgreSQL 数据库表结构，支持多租户数据隔离

-- =====================================================
-- 充电站表
-- =====================================================
CREATE TABLE IF NOT EXISTS charging_station (
    station_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_code VARCHAR(64) NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    station_type INTEGER DEFAULT 1, -- 1-公共充电站 2-专用充电站 3-个人充电站
    
    -- 地址和位置信息
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    address TEXT NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(11,7),
    
    -- 运营信息
    operator_name VARCHAR(100),
    operator_phone VARCHAR(20),
    operator_email VARCHAR(100),
    
    -- 服务信息
    service_hours VARCHAR(100) DEFAULT '24小时', -- 服务时间
    parking_fee DECIMAL(8,2) DEFAULT 0, -- 停车费(元/小时)
    service_fee DECIMAL(8,2) DEFAULT 0, -- 服务费(元/度)
    
    -- 充电桩统计字段已移除 (total_chargers, available_chargers, charging_chargers, fault_chargers)
    -- 这些数据通过视图 v_station_detail 实时计算
    
    -- 支付方式 (PostgreSQL数组)
    payment_methods INTEGER[] DEFAULT ARRAY[1,2,3], -- 1-支付宝 2-微信 3-银联 4-现金
    
    -- 设施配套 (JSONB格式)
    facilities JSONB DEFAULT '{}', -- {"wifi": true, "restaurant": false, "toilet": true}
    
    -- 状态信息
    status INTEGER DEFAULT 1, -- 0-停用 1-启用 2-维护中 3-建设中
    
    -- 统计信息
    total_sessions BIGINT DEFAULT 0, -- 总充电次数
    total_energy DECIMAL(12,2) DEFAULT 0, -- 总充电量(kWh)
    total_revenue DECIMAL(12,2) DEFAULT 0, -- 总收入(元)
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    
    -- 约束
    CONSTRAINT uk_station_code_tenant UNIQUE(station_code, tenant_id)
    -- 注意：不添加外键约束到 sys_tenant，因为可能在不同的初始化阶段
);

-- =====================================================
-- 充电桩表
-- =====================================================

-- 安全检查：确保 sys_tenant.tenant_id 有 UNIQUE 约束
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        JOIN pg_namespace n ON t.relnamespace = n.oid
        WHERE t.relname = 'sys_tenant'
          AND n.nspname = 'public'
          AND (c.contype = 'p' OR (c.contype = 'u' AND EXISTS (
               SELECT 1 
               FROM pg_attribute a 
               WHERE a.attrelid = t.oid 
                 AND a.attnum = ANY(c.conkey) 
                 AND a.attname = 'tenant_id'
          )))
    ) THEN
        ALTER TABLE sys_tenant ADD CONSTRAINT unique_sys_tenant_tenant_id UNIQUE (tenant_id);
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS charger (
    charger_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    charger_code VARCHAR(64) NOT NULL,
    charger_name VARCHAR(100) NOT NULL,
    
    -- 基本信息
    charger_type INTEGER NOT NULL, -- 1-直流快充 2-交流慢充
    brand VARCHAR(50), -- 品牌
    model VARCHAR(100), -- 型号
    manufacturer VARCHAR(100), -- 制造商
    production_date DATE, -- 生产日期
    operation_date DATE, -- 投运日期
    
    -- 技术参数
    rated_power DECIMAL(8,2) NOT NULL, -- 额定功率(kW)
    input_voltage INTEGER, -- 输入电压(V)
    output_voltage_range VARCHAR(50), -- 输出电压范围
    output_current_range VARCHAR(50), -- 输出电流范围
    
    -- 枪头信息
    gun_count INTEGER DEFAULT 1, -- 枪头数量
    gun_types VARCHAR(100), -- 枪头类型，逗号分隔
    
    -- 协议支持 (JSONB格式)
    supported_protocols JSONB DEFAULT '{"ocpp": "1.6"}', -- 支持的协议版本
    
    -- 实时状态
    status INTEGER DEFAULT 0, -- 0-离线 1-空闲 2-充电中 3-故障 4-维护 5-预约中
    fault_code VARCHAR(20), -- 故障代码
    fault_description TEXT, -- 故障描述
    last_heartbeat TIMESTAMP, -- 最后心跳时间
    
    -- 运行统计
    total_charging_sessions BIGINT DEFAULT 0, -- 累计充电次数
    total_charging_energy DECIMAL(12,2) DEFAULT 0, -- 累计充电量(kWh)
    total_charging_time BIGINT DEFAULT 0, -- 累计充电时长(分钟)
    
    -- 当前充电会话
    current_session_id VARCHAR(64), -- 当前会话ID
    current_user_id BIGINT, -- 当前用户ID
    charging_start_time TIMESTAMP, -- 充电开始时间
    charged_energy DECIMAL(8,2) DEFAULT 0, -- 已充电量(kWh)
    charged_duration INTEGER DEFAULT 0, -- 已充电时长(分钟)
    
    -- 实时数据
    current_power DECIMAL(8,2), -- 实时功率(kW)
    current_voltage DECIMAL(8,2), -- 实时电压(V)
    current_current DECIMAL(8,2), -- 实时电流(A)
    temperature DECIMAL(5,2), -- 温度(℃)
    signal_strength INTEGER, -- 信号强度(dbm)
    
    -- 维护信息
    firmware_version VARCHAR(50), -- 固件版本
    last_maintenance_time TIMESTAMP, -- 最后维护时间
    next_maintenance_time TIMESTAMP, -- 下次维护时间
    
    -- 管理字段
    enabled INTEGER DEFAULT 1, -- 0-禁用 1-启用
    remark TEXT, -- 备注
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    
    -- 约束
    CONSTRAINT uk_charger_code_tenant UNIQUE(charger_code, tenant_id),
    CONSTRAINT fk_charger_station FOREIGN KEY (station_id) REFERENCES charging_station(station_id),
    CONSTRAINT fk_charger_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id)
);

-- =====================================================
-- 索引优化
-- =====================================================

-- 充电站表索引
CREATE INDEX IF NOT EXISTS idx_station_tenant_id ON charging_station(tenant_id);
CREATE INDEX IF NOT EXISTS idx_station_code ON charging_station(station_code);
CREATE INDEX IF NOT EXISTS idx_station_status ON charging_station(status);
CREATE INDEX IF NOT EXISTS idx_station_location ON charging_station(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_station_create_time ON charging_station(create_time);

-- 充电桩表索引
CREATE INDEX IF NOT EXISTS idx_charger_tenant_id ON charger(tenant_id);
CREATE INDEX IF NOT EXISTS idx_charger_station_id ON charger(station_id);
CREATE INDEX IF NOT EXISTS idx_charger_code ON charger(charger_code);
CREATE INDEX IF NOT EXISTS idx_charger_status ON charger(status);
CREATE INDEX IF NOT EXISTS idx_charger_type ON charger(charger_type);
CREATE INDEX IF NOT EXISTS idx_charger_heartbeat ON charger(last_heartbeat);
CREATE INDEX IF NOT EXISTS idx_charger_session ON charger(current_session_id);

-- 复合索引
CREATE INDEX IF NOT EXISTS idx_station_tenant_status ON charging_station(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_charger_tenant_status ON charger(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_charger_station_status ON charger(station_id, status);

-- =====================================================
-- 触发器 - 自动更新时间戳
-- =====================================================
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 充电站更新时间触发器
DROP TRIGGER IF EXISTS update_station_modtime ON charging_station;
CREATE TRIGGER update_station_modtime 
    BEFORE UPDATE ON charging_station 
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

-- 充电桩更新时间触发器
DROP TRIGGER IF EXISTS update_charger_modtime ON charger;
CREATE TRIGGER update_charger_modtime 
    BEFORE UPDATE ON charger 
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

-- =====================================================
-- 触发器 - 更新充电站统计信息
-- =====================================================
-- 注意：充电桩统计字段已从 charging_station 表中移除
-- 统计信息通过视图 v_station_detail 实时计算，不再需要触发器维护
-- 旧的 update_station_charger_count() 函数和 update_station_stats 触发器已删除

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入测试充电站数据(仅在开发环境)
INSERT INTO charging_station (
    tenant_id, station_code, station_name, station_type,
    province, city, district, address, latitude, longitude,
    operator_name, operator_phone,
    service_hours, parking_fee, service_fee,
    payment_methods, facilities, status, create_by
) VALUES 
(1, 'ST001', '北京国贸充电站', 1, 
 '北京市', '朝阳区', '建国门外', '国贸大厦地下停车场B1层', 39.9087, 116.4089,
 '北京充电运营公司', '010-12345678',
 '24小时', 5.00, 0.80,
 ARRAY[1,2,3], '{"wifi": true, "restaurant": true, "toilet": true, "convenience_store": true}',
 1, 1)
ON CONFLICT (station_code, tenant_id) DO NOTHING;

-- 插入测试充电桩数据
INSERT INTO charger (
    tenant_id, station_id, charger_code, charger_name, charger_type,
    brand, model, manufacturer, rated_power,
    input_voltage, output_voltage_range, output_current_range,
    gun_count, gun_types, supported_protocols,
    status, create_by
) VALUES 
(1, 1, 'CH001001', '国贸站01号桩', 1,
 '特来电', 'TLD-120DC', '青岛特锐德电气股份有限公司', 120.00,
 380, '200-750V', '0-200A',
 2, 'GB/T,CCS', '{"ocpp": "1.6", "cloudCharge": "2.0"}',
 1, 1),
(1, 1, 'CH001002', '国贸站02号桩', 1,
 '星星充电', 'XX-60DC', '万帮充电设备有限公司', 60.00,
 380, '200-500V', '0-150A',
 1, 'GB/T', '{"ocpp": "1.6"}',
 1, 1)
ON CONFLICT (charger_code, tenant_id) DO NOTHING;

-- =====================================================
-- 查询视图 - 充电站详情
-- =====================================================
CREATE OR REPLACE VIEW v_station_detail AS
SELECT 
    s.*,
    COUNT(c.charger_id) as actual_total_chargers,
    COUNT(CASE WHEN c.status = 1 THEN 1 END) as actual_available_chargers,
    COUNT(CASE WHEN c.status = 2 THEN 1 END) as charging_chargers,
    COUNT(CASE WHEN c.status = 3 THEN 1 END) as fault_chargers
FROM charging_station s
LEFT JOIN charger c ON s.station_id = c.station_id AND c.deleted = 0
WHERE s.deleted = 0
GROUP BY s.station_id;

COMMENT ON TABLE charging_station IS '充电站信息表';
COMMENT ON TABLE charger IS '充电桩信息表';
COMMENT ON VIEW v_station_detail IS '充电站详情视图，包含实时统计信息';