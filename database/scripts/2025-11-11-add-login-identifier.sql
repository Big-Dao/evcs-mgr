-- 添加全局唯一的登录标识字段（手机号/邮箱）
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS login_identifier VARCHAR(100);

-- 初始化旧数据：优先使用邮箱，其次手机号，最后回退为 username@tenantId
UPDATE sys_user
SET login_identifier = COALESCE(NULLIF(email, ''), NULLIF(phone, ''), CONCAT(username, '@tenant', tenant_id))
WHERE login_identifier IS NULL OR login_identifier = '';

ALTER TABLE sys_user ALTER COLUMN login_identifier SET NOT NULL;
ALTER TABLE sys_user ADD CONSTRAINT uk_sys_user_login_identifier UNIQUE (login_identifier);
