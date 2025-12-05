# EVCS Manager API 文档指南

> **版本**: v2.1 | **最后更�?*: 2025-11-10 | **维护�?*: 技术负责人 | **状�?*: 活跃
>
> 📝 **用�?*: 指导如何编写和维护平�?API 文档

## 📚 文档概述

EVCS Manager 提供完整�?RESTful API 接口，支持充电站管理、订单处理、支付集成、多租户管理等核心功能。本文档介绍API的使用方法、接口规范和最佳实践�?
## 🚀 快速开�?
### 访问API文档

#### Knife4j 界面（推荐）
- **租户服务**: http://localhost:8086/doc.html
- **认证服务**: http://localhost:8081/doc.html
- **充电站服�?*: http://localhost:8082/doc.html
- **订单服务**: http://localhost:8083/doc.html
- **支付服务**: http://localhost:8084/doc.html

#### Swagger UI 界面
- **租户服务**: http://localhost:8086/swagger-ui.html
- **认证服务**: http://localhost:8081/swagger-ui.html
- **充电站服�?*: http://localhost:8082/swagger-ui.html
- **订单服务**: http://localhost:8083/swagger-ui.html
- **支付服务**: http://localhost:8084/swagger-ui.html

#### OpenAPI 规范文件
- **租户服务**: http://localhost:8086/v3/api-docs
- **认证服务**: http://localhost:8081/v3/api-docs
- **充电站服�?*: http://localhost:8082/v3/api-docs
- **订单服务**: http://localhost:8083/v3/api-docs
- **支付服务**: http://localhost:8084/v3/api-docs

### API网关统一入口
所有API请求都应该通过API网关访问�?- **网关地址**: http://localhost:8080
- **API前缀**: `/api/{service-name}/**`

**示例**:
```bash
# 通过网关访问租户服务API
curl http://localhost:8080/api/tenant/tenants

# 通过网关访问认证服务API
curl http://localhost:8080/api/auth/auth/login
```

## 🔐 认证与授�?
### JWT Token 认证

#### 1. 获取Token
```bash
curl -X POST http://localhost:8080/api/auth/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "admin@tenant1",
    "password": "admin123"
  }'
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "refresh_token_here",
    "expiresIn": 7200,
    "user": {
      "id": 1,
      "username": "admin",
      "identifier": "admin@tenant1",
      "tenantId": 1
    }
  }
}
```

#### 2. 使用Token访问API
```bash
curl -X GET http://localhost:8080/api/tenant/tenants \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "X-Tenant-Id: 1"   # 手工调用时从登录响应获取；前端会自动注入
```

#### 3. Token刷新
```bash
curl -X POST http://localhost:8080/api/auth/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "refresh_token_here"
  }'
```

### 多租户访问控�?
所有API请求都需要包含租户ID头部�?```bash
curl -X GET http://localhost:8080/api/tenant/tenants \
  -H "Authorization: Bearer {token}" \
  -H "X-Tenant-Id: {tenant_id}"
```

**权限级别**:
- `ALL`: 查看所有数据（超级管理员）
- `SELF`: 只能查看当前租户数据
- `CHILDREN`: 可以查看当前租户及其子租户数�?
## 📊 API接口分类

### 1. 认证授权 API (`/api/auth`)

#### 用户认证
```yaml
POST /api/auth/auth/login
POST /api/auth/auth/logout
POST /api/auth/auth/refresh
GET  /api/auth/auth/info
```

#### 用户管理
```yaml
GET    /api/auth/users
POST   /api/auth/users
GET    /api/auth/users/{id}
PUT    /api/auth/users/{id}
DELETE /api/auth/users/{id}
POST   /api/auth/users/{id}/reset-password
```

#### 权限管理
```yaml
GET    /api/auth/permissions
GET    /api/auth/roles
POST   /api/auth/roles
PUT    /api/auth/roles/{id}
DELETE /api/auth/roles/{id}
```

### 2. 租户管理 API (`/api/tenant`)

#### 租户CRUD
```yaml
GET    /api/tenant/tenants
POST   /api/tenant/tenants
GET    /api/tenant/tenants/{id}
PUT    /api/tenant/tenants/{id}
DELETE /api/tenant/tenants/{id}
POST   /api/tenant/tenants/{id}/enable
POST   /api/tenant/tenants/{id}/disable
```

#### 租户层级管理
```yaml
GET    /api/tenant/tenants/{id}/children
GET    /api/tenant/tenants/{id}/parents
GET    /api/tenant/tenants/tree
POST   /api/tenant/tenants/{id}/move
```

### 3. 充电站管�?API (`/api/station`)

#### 充电站管�?```yaml
GET    /api/station/stations
POST   /api/station/stations
GET    /api/station/stations/{id}
PUT    /api/station/stations/{id}
DELETE /api/station/stations/{id}
GET    /api/station/stations/{id}/status
```

#### 充电桩管�?```yaml
GET    /api/station/chargers
POST   /api/station/chargers
GET    /api/station/chargers/{id}
PUT    /api/station/chargers/{id}
DELETE /api/station/chargers/{id}
POST   /api/station/chargers/{id}/enable
POST   /api/station/chargers/{id}/disable
```

#### 实时监控
```yaml
GET    /api/station/chargers/{id}/status
GET    /api/station/chargers/{id}/sessions
POST   /api/station/chargers/{id}/start-session
POST   /api/station/chargers/{id}/stop-session
```

### 4. 订单管理 API (`/api/order`)

#### 订单管理
```yaml
GET    /api/order/orders
POST   /api/order/orders
GET    /api/order/orders/{id}
PUT    /api/order/orders/{id}
DELETE /api/order/orders/{id}
```

#### 计费管理
```yaml
GET    /api/order/pricing-plans
POST   /api/order/pricing-plans
PUT    /api/order/pricing-plans/{id}
GET    /api/order/orders/{id}/billing
POST   /api/order/orders/{id}/calculate
```

#### 统计分析
```yaml
GET    /api/order/statistics/daily
GET    /api/order/statistics/monthly
GET    /api/order/statistics/revenue
```

### 5. 支付集成 API (`/api/payment`)

#### 支付处理
```yaml
POST   /api/payment/payments/create
POST   /api/payment/payments/{id}/confirm
GET    /api/payment/payments/{id}/status
POST   /api/payment/payments/{id}/refund
```

#### 支付渠道
```yaml
GET    /api/payment/channels
GET    /api/payment/channels/alipay/config
GET    /api/payment/channels/wechat/config
```

#### 对账管理
```yaml
POST   /api/payment/reconciliation/trigger
GET    /api/payment/reconciliation/status
GET    /api/payment/reconciliation/report
```

## 📝 请求响应格式

### 统一响应格式

#### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // 响应数据
  },
  "timestamp": "2025-11-02T06:30:00Z",
  "traceId": "abc123-def456"
}
```

#### 错误响应
```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null,
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为�?
    }
  ],
  "timestamp": "2025-11-02T06:30:00Z",
  "traceId": "abc123-def456"
}
```

### 状态码说明

| 状态码 | 说明 | 示例场景 |
|--------|------|----------|
| 200 | 成功 | 操作成功完成 |
| 201 | 创建成功 | 资源创建成功 |
| 400 | 请求错误 | 参数验证失败 |
| 401 | 未授�?| Token无效或过�?|
| 403 | 禁止访问 | 权限不足 |
| 404 | 资源不存�?| 资源ID不存�?|
| 409 | 冲突 | 资源已存�?|
| 500 | 服务器错�?| 系统内部错误 |

### 分页响应格式
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "content": [
      // 数据列表
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5,
      "first": true,
      "last": false
    }
  }
}
```

## 🔧 开发工具集�?
### Postman 集合

导入以下环境变量和集合到Postman�?
#### 环境变量
```json
{
  "base_url": "http://localhost:8080",
  "tenant_id": "1",
  "auth_token": "",
  "refresh_token": ""
}
```

#### 认证脚本（Pre-request Script�?```javascript
// 自动刷新Token
if (!pm.environment.get("auth_token") ||
    pm.environment.get("auth_token") === "") {
    // 首次登录
    pm.sendRequest({
        url: pm.environment.get("base_url") + "/api/auth/auth/login",
        method: "POST",
        header: {
            "Content-Type": "application/json"
        },
        body: {
            mode: "raw",
            raw: JSON.stringify({
                username: "admin",
                password: "admin123"
            })
        }
    }, function (err, res) {
        if (res.json().code === 200) {
            pm.environment.set("auth_token", res.json().data.token);
            pm.environment.set("refresh_token", res.json().data.refreshToken);
        }
    });
}
```

### cURL 示例

#### 基础认证
```bash
# 登录获取Token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  jq -r '.data.token')

# 使用Token访问API
curl -X GET http://localhost:8080/api/tenant/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: 1"
```

#### 创建充电�?```bash
curl -X POST http://localhost:8080/api/station/stations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试充电�?,
    "code": "STATION001",
    "address": "北京市朝阳区",
    "latitude": 39.9042,
    "longitude": 116.4074,
    "capacity": 10
  }'
```

## 🔒 安全最佳实�?
### 1. Token管理
- **Token有效�?*: 默认2小时，建议客户端在过期前自动刷新
- **Token存储**: 客户端应安全存储Token，避免XSS攻击
- **Token撤销**: 登出时将Token加入黑名�?
### 2. HTTPS使用
- **生产环境**: 必须使用HTTPS协议
- **本地开�?*: 可使用HTTP，但需注意安全�?
### 3. 请求限制
- **频率限制**: 每个IP每分钟最�?00次请�?- **数据大小**: 请求体大小限制为10MB
- **超时设置**: 连接超时30秒，读取超时60�?
### 4. 数据验证
- **输入验证**: 所有输入参数都会进行严格验�?- **SQL注入防护**: 使用MyBatis Plus防止SQL注入
- **XSS防护**: 输出数据会进行XSS过滤

## 📊 监控与日�?
### API监控指标

#### Prometheus指标
- `http_requests_total`: HTTP请求总数
- `http_request_duration_seconds`: HTTP请求响应时间
- `http_active_connections`: 活跃连接�?- `security_authentication_success_total`: 认证成功次数
- `security_authentication_failure_total`: 认证失败次数

#### 访问日志格式
```json
{
  "timestamp": "2025-11-02T06:30:00Z",
  "level": "INFO",
  "logger": "com.evcs.gateway.filter",
  "message": "Request processed",
  "traceId": "abc123-def456",
  "spanId": "ghi789",
  "userId": "user123",
  "tenantId": 1,
  "request": {
    "method": "GET",
    "path": "/api/tenant/tenants",
    "remoteAddr": "192.168.1.100",
    "userAgent": "Mozilla/5.0..."
  },
  "response": {
    "status": 200,
    "duration": 150
  }
}
```

### 错误处理

#### 常见错误�?| 错误�?| 说明 | 解决方案 |
|--------|------|----------|
| 1001 | Token无效 | 重新登录获取新Token |
| 1002 | Token过期 | 使用refreshToken刷新 |
| 1003 | 权限不足 | 联系管理员分配权�?|
| 2001 | 租户不存�?| 检查租户ID是否正确 |
| 2002 | 租户被禁�?| 联系管理员启用租�?|
| 3001 | 充电站不存在 | 检查充电站ID |
| 3002 | 充电桩离�?| 检查充电桩连接状�?|

## 🚀 部署配置

### 开发环�?```yaml
# application-dev.yml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true

knife4j:
  enable: true
  production: false
  cors: true
```

### 生产环境
```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

knife4j:
  enable: false
  production: true
  cors: false
```

### Docker部署
```bash
# 构建镜像
./gradlew :evcs-tenant:bootJar

# 运行容器
docker run -d \
  --name evcs-tenant \
  -p 8086:8086 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=postgresql \
  -e REDIS_HOST=redis \
  evcs-manager/evcs-tenant:2.0.0
```

## 📚 更多资源

### 官方文档
- [SpringDoc OpenAPI 官方文档](https://springdoc.org/)
- [Knife4j 官方文档](https://doc.xiaominfo.com/)
- [OpenAPI 3.0 规范](https://swagger.io/specification/)

### 示例代码
- [Postman 集合下载](examples/postman-collection.json)
- [cURL 示例脚本](examples/curl-examples.sh)
- [Java SDK 示例](examples/java-sdk/)

### 社区支持
- **GitHub Issues**: [提交问题](https://github.com/Big-Dao/evcs-mgr/issues)
- **技术讨�?*: [GitHub Discussions](https://github.com/Big-Dao/evcs-mgr/discussions)
- **API反馈**: api-feedback@evcs-manager.com

---

**最后更�?*: 2025-11-02
**文档维护**: EVCS Manager 开发团�?**版本**: v2.0
