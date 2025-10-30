# 默认登录凭据

## 系统管理员账号

### EVCS Manager 管理后台
- **访问地址**: http://localhost:3000
- **用户名**: `admin`
- **密码**: `password`
- **租户ID**: `1`
- **说明**: 系统超级管理员，属于租户 ID=1

### Grafana 监控后台
- **访问地址**: http://localhost:3000 (独立部署时)
- **用户名**: `admin`
- **密码**: `admin`
- **说明**: Grafana 默认管理员账号（首次登录后会要求修改密码）

## 测试账号

### 数据库初始化账号
所有测试账号的密码均为 BCrypt 加密后的 `password`：
```
$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iDTcDXFe
```

### 租户信息
- **租户 ID**: 1
- **租户编码**: `DEFAULT`
- **租户名称**: 默认租户
- **状态**: 启用

## 安全提示

⚠️ **生产环境必须修改默认密码！**

修改步骤：
1. 首次登录后立即修改密码
2. 使用强密码（至少 12 位，包含大小写字母、数字、特殊字符）
3. 定期轮换密码（建议每 90 天）
4. 启用双因素认证（待实现）

## API 直接调用示例

```bash
# 通过 Gateway 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password",
    "tenantId": 1
  }'

# 直接调用 Auth 服务
curl -X POST http://localhost:8081/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password",
    "tenantId": 1
  }'
```

## 密码重置

如需重置管理员密码，执行：
```bash
# 使用 SQL 脚本重置
psql -h localhost -U evcs_user -d evcs_db < reset-admin-password.sql

# 或手动更新
# 密码: password
UPDATE users 
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iDTcDXFe'
WHERE username = 'admin' AND tenant_id = 1;
```

## 常见问题

### Q: 为什么需要提供租户ID？
A: 系统支持多租户架构，每个用户必须属于一个租户。租户 ID=1 是默认的系统租户。

### Q: 前端显示的默认密码是 `admin123`，为什么实际是 `password`？
A: 前端代码中的默认值在开发过程中有过调整，当前数据库中的实际密码是 `password`。建议同步更新前端默认值。

### Q: 登录失败提示"租户ID不能为空"？
A: 请确保请求中包含 `tenantId` 字段。如果使用租户编码，可以提供 `tenantCode: "DEFAULT"` 代替 `tenantId: 1`。

### Q: 登录失败提示"JSON parse error"？
A: 检查 JSON 格式是否正确。在 PowerShell 中使用 curl 时，字符串引号可能需要转义。建议使用变量方式：
```powershell
$body = '{"username":"admin","password":"password","tenantId":1}'
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d $body
```
