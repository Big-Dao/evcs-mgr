-- 租户审计日志表
-- 记录跨层级管理行为，包括创建子租户、禁用/恢复、配额变更等
CREATE TABLE IF NOT EXISTS tenant_audit_log (
    id BIGSERIAL PRIMARY KEY,
    operator_tenant_id BIGINT NOT NULL,
    operator_user_id BIGINT,
    operator_user_name VARCHAR(100),
    target_tenant_id BIGINT NOT NULL,
    target_tenant_name VARCHAR(200),
    action VARCHAR(50) NOT NULL,
    result VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    details TEXT,
    trace_id VARCHAR(100),
    client_ip VARCHAR(50),
    user_agent VARCHAR(500),
    operate_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_tal_target_tenant ON tenant_audit_log(target_tenant_id);
CREATE INDEX IF NOT EXISTS idx_tal_operator_tenant ON tenant_audit_log(operator_tenant_id);
CREATE INDEX IF NOT EXISTS idx_tal_action ON tenant_audit_log(action);
CREATE INDEX IF NOT EXISTS idx_tal_operate_time ON tenant_audit_log(operate_time DESC);
CREATE INDEX IF NOT EXISTS idx_tal_cross_layer ON tenant_audit_log(operator_tenant_id, target_tenant_id)
    WHERE operator_tenant_id != target_tenant_id;

-- 添加注释
COMMENT ON TABLE tenant_audit_log IS '租户审计日志表，记录跨层级管理行为';
COMMENT ON COLUMN tenant_audit_log.operator_tenant_id IS '操作者租户ID';
COMMENT ON COLUMN tenant_audit_log.operator_user_id IS '操作者用户ID';
COMMENT ON COLUMN tenant_audit_log.target_tenant_id IS '目标租户ID';
COMMENT ON COLUMN tenant_audit_log.action IS '操作类型：CREATE_TENANT, DELETE_TENANT, UPDATE_QUOTA, UPDATE_STATUS, etc.';
COMMENT ON COLUMN tenant_audit_log.result IS '操作结果：SUCCESS, FAILURE';
COMMENT ON COLUMN tenant_audit_log.details IS '详细信息（JSON格式）';
COMMENT ON COLUMN tenant_audit_log.trace_id IS '请求追踪ID';
COMMENT ON COLUMN tenant_audit_log.client_ip IS '客户端IP';
COMMENT ON COLUMN tenant_audit_log.operate_time IS '操作时间';

-- 为 sys_tenant 表添加配额字段
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS max_children INTEGER DEFAULT 0;
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS max_sessions INTEGER DEFAULT 0;

COMMENT ON COLUMN sys_tenant.max_children IS '最大子租户数量（0表示不限制）';
COMMENT ON COLUMN sys_tenant.max_sessions IS '最大并发会话数（0表示不限制）';

