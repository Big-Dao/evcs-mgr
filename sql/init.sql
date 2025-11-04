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
    tenant_id BIGINT UNIQUE, -- 自引用租户ID，添加UNIQUE约束以支持外键引用
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

-- 充电站和充电桩表由 charging_station_tables.sql 创建
-- （为了保持代码一致性，这两个表的定义移到了专门的文件中）

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
-- charging_station 和 charger 表的触发器由 charging_station_tables.sql 创建

-- ============================================
-- 初始化数据
-- ============================================

-- 插入系统默认租户
INSERT INTO sys_tenant (id, tenant_code, tenant_name, tenant_type, status, max_users, max_stations, max_chargers, tenant_id) 
VALUES (1, 'SYSTEM', '系统租户', 1, 1, 999999, 999999, 999999, 1);

-- 插入默认系统管理员用户 (密码: password)
INSERT INTO sys_user (id, username, password, real_name, user_type, status, tenant_id)
VALUES (1, 'admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '系统管理员', 0, 1, 1);

-- 插入默认角色
INSERT INTO sys_role (id, role_code, role_name, sort, data_scope, status, tenant_id, remark) 
VALUES 
(1, 'SUPER_ADMIN', '超级管理员', 1, 1, 1, 1, '拥有所有权限'),
(2, 'PLATFORM_ADMIN', '平台管理员', 2, 2, 1, 1, '平台运营管理'),
(3, 'OPERATOR_ADMIN', '运营商管理员', 3, 3, 1, 1, '运营商管理'),
(4, 'STATION_MANAGER', '站点管理员', 4, 4, 1, 1, '充电站管理');

-- 关联用户和角色
INSERT INTO sys_user_role (user_id, role_id, tenant_id) 
VALUES (1, 1, 1);

-- 插入默认菜单权限
INSERT INTO sys_permission (id, permission_code, permission_name, parent_id, type, path, component, icon, sort, status, visible, tenant_id, remark)
VALUES
-- 一级菜单
(1, 'dashboard', '仪表盘', NULL, 1, '/dashboard', 'Dashboard', 'DataAnalysis', 1, 1, 1, 1, '数据统计仪表盘'),
(2, 'tenant', '租户管理', NULL, 1, '/tenant', NULL, 'OfficeBuilding', 2, 1, 1, 1, '租户管理模块'),
(3, 'user', '用户管理', NULL, 1, '/user', NULL, 'User', 3, 1, 1, 1, '用户管理模块'),
(4, 'station', '充电站管理', NULL, 1, '/station', NULL, 'Location', 4, 1, 1, 1, '充电站管理模块'),
(5, 'charger', '充电桩管理', NULL, 1, '/charger', NULL, 'Monitor', 5, 1, 1, 1, '充电桩管理模块'),
(6, 'order', '订单管理', NULL, 1, '/order', NULL, 'Document', 6, 1, 1, 1, '充电订单管理'),
(7, 'billing', '计费管理', NULL, 1, '/billing', NULL, 'Money', 7, 1, 1, 1, '计费方案管理'),
-- 二级菜单 - 租户管理
(201, 'tenant:list', '租户列表', 2, 1, '/tenant/list', 'TenantList', 'List', 1, 1, 1, 1, '租户列表页面'),
(202, 'tenant:tree', '租户树形', 2, 1, '/tenant/tree', 'TenantTree', 'Tree', 2, 1, 1, 1, '租户树形结构'),
-- 二级菜单 - 用户管理
(301, 'user:list', '用户列表', 3, 1, '/user/list', 'UserList', 'List', 1, 1, 1, 1, '用户列表页面'),
(302, 'user:role', '角色管理', 3, 1, '/user/role', 'RoleList', 'Avatar', 2, 1, 1, 1, '角色权限管理'),
-- 二级菜单 - 充电站管理
(401, 'station:list', '站点列表', 4, 1, '/station/list', 'StationList', 'List', 1, 1, 1, 1, '充电站列表'),
-- 二级菜单 - 充电桩管理
(501, 'charger:list', '充电桩列表', 5, 1, '/charger/list', 'ChargerList', 'List', 1, 1, 1, 1, '充电桩列表'),
-- 二级菜单 - 订单管理
(601, 'order:list', '订单列表', 6, 1, '/order/list', 'OrderList', 'List', 1, 1, 1, 1, '充电订单列表'),
(602, 'order:dashboard', '订单统计', 6, 1, '/order/dashboard', 'OrderDashboard', 'DataAnalysis', 2, 1, 1, 1, '订单统计分析'),
-- 二级菜单 - 计费管理
(701, 'billing:plan', '计费方案', 7, 1, '/billing/plan', 'BillingPlanList', 'List', 1, 1, 1, 1, '计费方案管理');

COMMENT ON DATABASE postgres IS '充电站管理平台数据库';
COMMENT ON TABLE sys_tenant IS '租户表';
COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON TABLE sys_permission IS '权限表';
-- charging_station 和 charger 表的注释由 charging_station_tables.sql 创建