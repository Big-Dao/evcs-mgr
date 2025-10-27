UPDATE sys_user SET password = '$2a$10$7JB720yubVSeLVa5fCJ8v.7lGjWNaDgGKDTpKUdZ5JN6XL4HY5sdi' WHERE username = 'admin';
SELECT username, password, LENGTH(password) as len FROM sys_user WHERE username='admin';
