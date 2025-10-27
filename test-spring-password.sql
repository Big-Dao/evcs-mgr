-- Spring Security官方示例
-- 密码: password
-- 哈希: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
UPDATE sys_user 
SET password = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG'
WHERE username = 'admin';

SELECT id, username, LENGTH(password) as len FROM sys_user WHERE username='admin';
