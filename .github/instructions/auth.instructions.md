---
applyTo: "evcs-auth/**/*.java"
priority: high
---

# 认证与安全规范 (Security & Auth)

> **最后更新**: 2025-12-18 | **维护者**: 安全团队 | **状态**: 已发布

本规范适用于 `evcs-auth` 模块及所有涉及用户认证、密码处理的代码。

## 关键要求

### 1. 密码安全
**严禁明文存储**
- 用户密码必须使用强哈希算法存储（推荐 BCrypt 或 Argon2）。
- 禁止使用 MD5 或 SHA1。
- 密码比对必须使用哈希验证，禁止明文比对。

### 2. Token 管理
**JWT 规范**
- 签发的 JWT 必须包含：
  - `exp`: 过期时间（建议 Access Token < 2小时）
  - `jti`: 唯一标识（用于黑名单/撤销）
  - `sub`: 用户ID
- 敏感信息（如手机号、角色详情）不应放入 JWT Payload，应通过 ID 查库或缓存获取。

### 3. 日志安全
**零信任日志**
- **严禁**在日志中打印：
  - 用户密码（明文或哈希）
  - JWT Token / Session ID
  - 验证码 / OTP
- 在进入 Controller 之前，通过 Filter 或 AOP 对请求体中的敏感字段进行脱敏。

---

## ✅ 代码示例

### 密码编码配置

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// 注册用户
public void register(UserRegisterDTO dto) {
    User user = new User();
    // ✅ 正确：加密存储
    user.setPassword(passwordEncoder.encode(dto.getPassword()));
    userRepository.save(user);
}
```
