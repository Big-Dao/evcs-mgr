# EVCS认证授权服务

> **端口**: 8081 | **状态**: 活跃
>
> **功能**: 提供用户认证、授权和JWT令牌管理

## 📋 服务概述

EVCS认证授权服务是系统的安全核心，负责：
- 用户身份认证和授权
- JWT令牌的生成和验证
- 基于RBAC的权限控制
- 多租户用户管理

## 🔧 技术栈
- Spring Boot 3.2.10
- Spring Security 6.x
- JWT (JSON Web Token)
- Spring Data JPA
- PostgreSQL

## 🚀 快速启动

### 本地开发
```bash
# 构建服务
./gradlew :evcs-auth:build

# 运行服务
./gradlew :evcs-auth:bootRun

# 访问健康检查
curl http://localhost:8081/actuator/health
```

### Docker部署
```bash
# 构建镜像
docker build -t evcs-auth:latest ./evcs-auth

# 运行容器
docker run -p 8081:8081 evcs-auth:latest
```

## 📡 API端点

### 认证相关
- `POST /api/v1/auth/login` - 用户登录
- `POST /api/v1/auth/logout` - 用户登出
- `POST /api/v1/auth/refresh` - 刷新令牌
- `GET /api/v1/auth/profile` - 获取用户信息

### 用户管理
- `POST /api/v1/users` - 创建用户
- `GET /api/v1/users` - 查询用户列表
- `PUT /api/v1/users/{id}` - 更新用户信息
- `DELETE /api/v1/users/{id}` - 删除用户

## 🔒 安全特性

- **JWT认证**: 无状态令牌认证
- **密码加密**: BCrypt哈希加密
- **权限控制**: 基于角色的访问控制(RBAC)
- **多租户**: 租户级别的数据隔离

## 📊 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/evcs_auth
    username: ${DB_USERNAME:evcs}
    password: ${DB_PASSWORD:password}
```

### JWT配置
```yaml
evcs:
  auth:
    jwt:
      secret: ${JWT_SECRET:your-secret-key}
      expiration: 86400000 # 24小时
```

## 🔗 相关链接

- [API文档](../../docs/references/API-DOCUMENTATION.md#evcs-auth)
- [项目文档](../../docs/README.md)
- [开发指南](../../docs/development/DEVELOPER-GUIDE.md)
- [架构设计](../../docs/architecture/architecture.md)

## 🧪 测试

```bash
# 运行单元测试
./gradlew :evcs-auth:test

# 运行集成测试
./gradlew :evcs-auth:integrationTest
```

---

**服务负责人**: 认证团队
**代码审查**: 安全团队
**部署负责人**: DevOps团队