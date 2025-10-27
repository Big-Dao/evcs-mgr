-- 恢复init.sql中的原始BCrypt哈希
-- 明文密码应该是: admin123
UPDATE sys_user 
SET password = '$2a$10$7JB720yubVSeLVa5fCJ8v.7lGjWNaDgGKDTpKUdZ5JN6XL4HY5sdi'
WHERE username = 'admin';

-- 验证更新
SELECT id, username, LENGTH(password) as pwd_length, 
       SUBSTRING(password, 1, 20) as pwd_prefix 
FROM sys_user 
WHERE username = 'admin';
