-- ============================================
-- 充电站管理平台 - PostgreSQL 多租户数据库设计
-- ============================================

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "cube";
CREATE EXTENSION IF NOT EXISTS "earthdistance";

-- ============================================
-- 1. 系统管理模块
-- ============================================

-- 租户表
CREATE TABLE sys_tenant (
    id BIGINT PRIMARY KEY DEFAULT (extract(epoch from now()) * 1000000 + floor(random() * 1000000)::bigint),
    tenant_code VARCHAR(50) UNIQUE NOT NULL,
    tenant_name VARCHAR(100) NOT NULL,
    parent_id BIGINT REFERENCES sys_tenant(id),
    ancestors TEXT, -- 祖级列表，逗号分隔
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    address VARCHAR(200),
    social_code VARCHAR(30), -- 统一社会信用代码
    license_url VARCHAR(500),
    tenant_type INTEGER DEFAULT 1, -- 1-平台方，2-运营商，3-第三方合作伙伴
    status INTEGER DEFAULT 1, -- 0-禁用，1-启用
    expire_time TIMESTAMP,
    max_users INTEGER DEFAULT 100,
    max_stations INTEGER DEFAULT 50,
    max_chargers INTEGER DEFAULT 1000,
    tenant_id BIGINT, -- 自引用租户ID
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1,
    remark TEXT
);

-- 为租户表创建索引
CREATE INDEX idx_tenant_code ON sys_tenant(tenant_code);
CREATE INDEX idx_tenant_parent ON sys_tenant(parent_id);
CREATE INDEX idx_tenant_status ON sys_tenant(status, deleted);

-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY DEFAULT (extract(epoch from now()) * 1000000 + floor(random() * 1000000)::bigint),
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    avatar VARCHAR(500),
    gender INTEGER DEFAULT 2, -- 0-女，1-男，2-未知
    status INTEGER DEFAULT 1, -- 0-禁用，1-启用
    user_type INTEGER DEFAULT 2, -- 0-系统用户，1-租户管理员，2-普通用户
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1,
    remark TEXT
);

-- 为用户表创建唯一索引（租户内用户名唯一）
CREATE UNIQUE INDEX idx_user_tenant_username ON sys_user(tenant_id, username) WHERE deleted = 0;
CREATE INDEX idx_user_tenant ON sys_user(tenant_id, status, deleted);
CREATE INDEX idx_user_phone ON sys_user(phone) WHERE phone IS NOT NULL;
CREATE INDEX idx_user_email ON sys_user(email) WHERE email IS NOT NULL;

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY DEFAULT (extract(epoch from now()) * 1000000 + floor(random() * 1000000)::bigint),
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    sort INTEGER DEFAULT 0,
    data_scope INTEGER DEFAULT 1, -- 1-全部数据权限，2-自定义数据权限，3-本部门数据权限，4-本部门及以下数据权限，5-仅本人数据权限
    status INTEGER DEFAULT 1, -- 0-禁用，1-启用
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1,
    remark TEXT
);

-- 为角色表创建唯一索引（租户内角色编码唯一）
CREATE UNIQUE INDEX idx_role_tenant_code ON sys_role(tenant_id, role_code) WHERE deleted = 0;

-- 权限表
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY DEFAULT (extract(epoch from now()) * 1000000 + floor(random() * 1000000)::bigint),
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    type INTEGER DEFAULT 1, -- 1-菜单，2-按钮，3-接口
    path VARCHAR(200),
    component VARCHAR(200),
    perms VARCHAR(100),
    icon VARCHAR(100),
    sort INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1, -- 0-禁用，1-启用
    is_frame INTEGER DEFAULT 0, -- 0-否，1-是
    is_cache INTEGER DEFAULT 0, -- 0-否，1-是
    visible INTEGER DEFAULT 1, -- 0-隐藏，1-显示
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1,
    remark TEXT
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY DEFAULT (extract(epoch from now()) * 1000000 + floor(random() * 1000000)::bigint),
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT
);

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY DEFAULT (extract(epoch from now()) * 1000000 + floor(random() * 1000000)::bigint),
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    permission_id BIGINT NOT NULL REFERENCES sys_permission(id),
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT
);

-- ============================================
-- 2. 充电站管理模块
-- ============================================

-- 充电站表
CREATE TABLE charging_station (
    id BIGINT PRIMARY KEY DEFAULT (extract(epoch from now()) * 1000000 + floor(random() * 1000000)::bigint),
    station_code VARCHAR(50) UNIQUE NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    operator_id BIGINT REFERENCES sys_tenant(id),
    operator_name VARCHAR(100),
    station_type INTEGER DEFAULT 1, -- 1-直流快充，2-交流慢充，3-混合站
    province VARCHAR(20),
    city VARCHAR(20),
    district VARCHAR(20),
    address VARCHAR(200),
    longitude DECIMAL(10,7),
    latitude DECIMAL(10,7),
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    service_time VARCHAR(50),
    parking_fee VARCHAR(200),
    payment_methods INTEGER[] DEFAULT ARRAY[1,2,3], -- PostgreSQL数组类型
    total_chargers INTEGER DEFAULT 0,
    available_chargers INTEGER DEFAULT 0,
    faulty_chargers INTEGER DEFAULT 0,
    charging_chargers INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1, -- 0-建设中，1-运营中，2-维护中，3-停用
    support_reservation INTEGER DEFAULT 0, -- 0-否，1-是
    network_type INTEGER DEFAULT 2, -- 1-有线，2-4G，3-5G，4-WiFi
    last_heartbeat TIMESTAMP,
    image_urls JSONB, -- JSON数组
    facilities JSONB, -- JSON对象
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1,
    remark TEXT
);

-- 为充电站表创建索引
CREATE INDEX idx_station_code ON charging_station(station_code);
CREATE INDEX idx_station_tenant ON charging_station(tenant_id, status, deleted);
CREATE INDEX idx_station_operator ON charging_station(operator_id);
CREATE INDEX idx_station_location ON charging_station(province, city, district);
CREATE INDEX idx_station_geo ON charging_station USING GIST(ll_to_earth(latitude, longitude)) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- 充电桩表
CREATE TABLE charger (
    id BIGINT PRIMARY KEY DEFAULT (extract(epoch from now()) * 1000000 + floor(random() * 1000000)::bigint),
    charger_code VARCHAR(50) UNIQUE NOT NULL,
    charger_name VARCHAR(100) NOT NULL,
    station_id BIGINT NOT NULL REFERENCES charging_station(id),
    station_code VARCHAR(50) NOT NULL,
    charger_type INTEGER DEFAULT 1, -- 1-直流快充，2-交流慢充
    brand VARCHAR(50),
    model VARCHAR(50),
    manufacturer VARCHAR(100),
    production_date DATE,
    operation_date DATE,
    rated_power DECIMAL(8,2), -- kW
    input_voltage INTEGER, -- V
    output_voltage_range VARCHAR(50), -- V
    output_current_range VARCHAR(50), -- A
    gun_count INTEGER DEFAULT 1,
    gun_types INTEGER[] DEFAULT ARRAY[1], -- PostgreSQL数组：1-国标，2-欧标，3-美标
    supported_protocols JSONB, -- JSON对象
    status INTEGER DEFAULT 1, -- 0-离线，1-空闲，2-充电中，3-故障，4-维护，5-预约中
    fault_code VARCHAR(20),
    fault_description VARCHAR(200),
    last_heartbeat TIMESTAMP,
    total_charging_sessions BIGINT DEFAULT 0,
    total_charging_energy DECIMAL(12,2) DEFAULT 0, -- kWh
    total_charging_time BIGINT DEFAULT 0, -- 分钟
    current_session_id VARCHAR(50),
    current_user_id BIGINT,
    charging_start_time TIMESTAMP,
    charged_energy DECIMAL(10,2) DEFAULT 0, -- kWh
    charged_duration INTEGER DEFAULT 0, -- 分钟
    current_power DECIMAL(8,2), -- kW
    current_voltage DECIMAL(8,2), -- V
    current_current DECIMAL(8,2), -- A
    temperature DECIMAL(5,2), -- ℃
    signal_strength INTEGER, -- dbm
    firmware_version VARCHAR(50),
    last_maintenance_time TIMESTAMP,
    next_maintenance_time TIMESTAMP,
    enabled INTEGER DEFAULT 1, -- 0-禁用，1-启用
    tenant_id BIGINT NOT NULL REFERENCES sys_tenant(id),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1,
    remark TEXT
);

-- 为充电桩表创建索引
CREATE INDEX idx_charger_code ON charger(charger_code);
CREATE INDEX idx_charger_station ON charger(station_id, status, enabled, deleted);
CREATE INDEX idx_charger_tenant ON charger(tenant_id, status, deleted);
CREATE INDEX idx_charger_status ON charger(status, enabled) WHERE deleted = 0;

-- ============================================
-- 触发器：自动更新时间戳
-- ============================================

-- 创建通用的更新时间戳触发器函数
CREATE OR REPLACE FUNCTION update_updated_time_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为各表添加更新时间戳触发器
CREATE TRIGGER update_tenant_updated_time BEFORE UPDATE ON sys_tenant FOR EACH ROW EXECUTE PROCEDURE update_updated_time_column();
CREATE TRIGGER update_user_updated_time BEFORE UPDATE ON sys_user FOR EACH ROW EXECUTE PROCEDURE update_updated_time_column();
CREATE TRIGGER update_role_updated_time BEFORE UPDATE ON sys_role FOR EACH ROW EXECUTE PROCEDURE update_updated_time_column();
CREATE TRIGGER update_permission_updated_time BEFORE UPDATE ON sys_permission FOR EACH ROW EXECUTE PROCEDURE update_updated_time_column();
CREATE TRIGGER update_station_updated_time BEFORE UPDATE ON charging_station FOR EACH ROW EXECUTE PROCEDURE update_updated_time_column();
CREATE TRIGGER update_charger_updated_time BEFORE UPDATE ON charger FOR EACH ROW EXECUTE PROCEDURE update_updated_time_column();

-- ============================================
-- 初始化数据
-- ============================================

-- 插入系统默认租户
INSERT INTO sys_tenant (id, tenant_code, tenant_name, tenant_type, status, max_users, max_stations, max_chargers, tenant_id) 
VALUES (1, 'SYSTEM', '系统租户', 1, 1, 999999, 999999, 999999, 1);

-- 插入默认系统管理员用户
INSERT INTO sys_user (id, username, password, real_name, user_type, status, tenant_id) 
VALUES (1, 'admin', '$2a$10$7JB720yubVSeLVa5fCJ8v.7lGjWNaDgGKDTpKUdZ5JN6XL4HY5sdi', '系统管理员', 0, 1, 1);

-- 插入默认角色
INSERT INTO sys_role (id, role_code, role_name, status, tenant_id) 
VALUES (1, 'SUPER_ADMIN', '超级管理员', 1, 1);

-- 关联用户和角色
INSERT INTO sys_user_role (user_id, role_id, tenant_id) 
VALUES (1, 1, 1);

COMMENT ON DATABASE postgres IS '充电站管理平台数据库';
COMMENT ON TABLE sys_tenant IS '租户表';
COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON TABLE sys_permission IS '权限表';
COMMENT ON TABLE charging_station IS '充电站表';
COMMENT ON TABLE charger IS '充电桩表';