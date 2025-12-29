-- 租户审计日志表
CREATE TABLE IF NOT EXISTS tenant_audit_log (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL COMMENT '操作动作',
    target_tenant_id BIGINT NOT NULL COMMENT '目标租户ID',
    operator_tenant_id BIGINT COMMENT '操作者租户ID',
    operator_user_id BIGINT COMMENT '操作者用户ID',
    details TEXT COMMENT '详细信息或快照',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_target ON tenant_audit_log(target_tenant_id);
CREATE INDEX idx_audit_operator ON tenant_audit_log(operator_tenant_id);
CREATE INDEX idx_audit_time ON tenant_audit_log(create_time);
