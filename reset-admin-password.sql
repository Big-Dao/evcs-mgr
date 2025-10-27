-- 真实的 admin123 BCrypt哈希 (Cost 10)
-- 从在线工具生成: https://bcrypt.online/
UPDATE sys_user 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIiamKOPXeLolcVVpxZ3i4MNH67iYH3u'
WHERE username = 'admin';

SELECT id, username, LENGTH(password) as len, SUBSTRING(password, 1, 30) as prefix 
FROM sys_user 
WHERE username = 'admin';
