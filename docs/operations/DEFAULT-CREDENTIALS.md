# EVCS 系统默认登录凭证

> **版本**: v1.1 | **最后更新**: 2025-12-18 | **维护者**: DevOps 团队 | **状态**: 活跃
>
> **用途**: 演示 / 测试环境账号密码清单（禁止用于生产）

## 默认管理员账号

### 管理员账号
- **用户名**: `admin`
- **登录账号**: `admin@tenant1`  _(租户ID自动写入 Token，无需手工输入)_
- **密码**: `password`
- **租户ID**: `1`
- **状态**: 激活

## 登录测试

### 前端登录页面
1. 启动前端服务：
   ```powershell
   cd evcs-admin
   npm run dev
   ```

2. 访问 http://localhost:3000

3. 使用以下凭证登录：
  - 登录账号: `admin@tenant1` （或管理员设置的手机号/邮箱）
  - 密码: `password`

### API 测试
```powershell
# 测试登录接口（identifier = 手机 / 邮箱 / 迁移脚本生成的占位）
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"identifier":"admin@tenant1","password":"password"}'
```

## 前端更新说明

已完成以下更新：

1. 创建 `src/api/auth.ts` - 登录 API 接口
2. 更新 `src/views/Login.vue` - 集成真实登录 API
3. 更新 `src/utils/request.ts` - 修正响应格式处理（success 字段）
4. 登录账号自动解析租户，无需手工输入租户 ID

## 登录流程

```
用户输入账号/密码
  → 前端调用 /api/auth/login（identifier + password）
  → Vite 代理转发到 Gateway (localhost:8080)
  → Gateway 路由到 evcs-auth 服务
  → 返回 JWT（payload 含 userId / tenantId / username）
  → 前端保存 token 与用户信息到 localStorage
  → 跳转到 Dashboard
```

## 密码信息
- 存储哈希: `$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG`
- 算法: BCrypt
- 强度: 10 rounds
- 明文密码: `password`

## 重置密码（如需要）
```sql
-- 将密码重置为 password
UPDATE sys_user
SET password = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG'
WHERE username = 'admin';
```
