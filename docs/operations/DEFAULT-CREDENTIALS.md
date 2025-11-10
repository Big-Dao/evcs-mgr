# EVCS 系统默认登录凭证

> **版本**: v1.1 | **最后更新**: 2025-11-10 | **维护者**: DevOps 团队 | **状态**: 活跃
>
> 🔐 **用途**: 演示 / 测试环境账号密码清单（禁止用于生产）

## 默认管理员账号

### 管理员账号 ✅
- **用户名**: `admin`
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
  - 用户名: `admin`
  - 密码: `password`
  - 租户ID: `1` (已预填)

### API 测试
```powershell
# 测试登录接口
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"password","tenantId":1}'
```

## 前端更新说明

已完成以下更新：

1. ✅ 创建 `src/api/auth.ts` - 登录API接口
2. ✅ 更新 `src/views/Login.vue` - 集成真实登录API
3. ✅ 更新 `src/utils/request.ts` - 修正响应格式处理（success字段）
4. ✅ 添加租户ID字段（默认为1）

## 登录流程

```
用户输入凭证 
  → 前端调用 /api/auth/login 
  → Vite代理转发到 Gateway (localhost:8080)
  → Gateway路由到 evcs-auth 服务
  → 返回JWT token
  → 前端保存token到localStorage
  → 跳转到Dashboard
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
