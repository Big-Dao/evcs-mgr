# EVCS Manager API接口设计

> **版本**: v2.0 | **更新日期**: 2025-11-02 | **状态**: 活跃

## 📋 概述

本文档定义了 EVCS Manager 充电站管理平台的 API 接口设计规范，包括 RESTful API 设计原则、接口规范、数据格式等。

### 🎯 设计目标
- **RESTful 规范**: 遵循 REST 架构风格
- **统一格式**: 统一的请求和响应格式
- **版本管理**: 支持API版本控制
- **安全认证**: JWT令牌认证
- **文档完善**: Swagger/OpenAPI文档

## 🔐 认证授权

### JWT认证
所有API都需要在请求头中包含JWT令牌：

```http
Authorization: Bearer <token>
```

### 获取令牌
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password",
  "tenantId": 1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "refresh_token_here",
    "expiresIn": 7200,
    "user": {
      "userId": 1,
      "username": "admin",
      "realName": "管理员",
      "tenantId": 1
    }
  }
}
```

## 📊 响应格式

### 统一响应结构
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2025-11-02T10:30:00",
  "traceId": "abc123"
}
```

### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "充电站名称"
  }
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "参数错误",
  "data": null,
  "errors": [
    {
      "field": "stationCode",
      "message": "充电站编码不能为空"
    }
  ]
}
```

### 分页响应
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [...],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

## 🏗️ API接口规范

### 1. 认证服务 API

#### 用户登录
```http
POST /api/auth/login
```

**请求参数**:
```json
{
  "username": "string",
  "password": "string",
  "tenantId": "number"
}
```

#### 刷新令牌
```http
POST /api/auth/refresh
```

#### 用户信息
```http
GET /api/auth/userinfo
Authorization: Bearer <token>
```

#### 退出登录
```http
POST /api/auth/logout
Authorization: Bearer <token>
```

### 2. 充电站管理 API

#### 获取充电站列表
```http
GET /api/stations?current=1&size=10&stationName=市中心&status=1
Authorization: Bearer <token>
```

**查询参数**:
- `current`: 当前页码 (默认: 1)
- `size`: 每页大小 (默认: 10)
- `stationName`: 充电站名称 (模糊查询)
- `status`: 状态 (1:启用, 0:停用)

**响应**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "stationId": 1,
        "stationCode": "ST001",
        "stationName": "市中心充电站",
        "address": "北京市朝阳区建国路88号",
        "latitude": 39.9042,
        "longitude": 116.4074,
        "status": 1,
        "createTime": "2025-11-01T10:00:00",
        "updateTime": "2025-11-01T10:00:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 10,
    "pages": 5
  }
}
```

#### 获取充电站详情
```http
GET /api/stations/{stationId}
Authorization: Bearer <token>
```

#### 创建充电站
```http
POST /api/stations
Authorization: Bearer <token>
Content-Type: application/json

{
  "stationCode": "ST002",
  "stationName": "新建充电站",
  "address": "北京市海淀区中关村大街1号",
  "latitude": 39.9042,
  "longitude": 116.4074,
  "operatorName": "运营商名称",
  "servicePhone": "400-123-4567"
}
```

#### 更新充电站
```http
PUT /api/stations/{stationId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "stationName": "更新后的充电站名称",
  "address": "更新后的地址"
}
```

#### 删除充电站
```http
DELETE /api/stations/{stationId}
Authorization: Bearer <token>
```

#### 附近充电站查询
```http
GET /api/stations/nearby?latitude=39.9042&longitude=116.4074&radius=5&limit=20
Authorization: Bearer <token>
```

**查询参数**:
- `latitude`: 纬度
- `longitude`: 经度
- `radius`: 搜索半径(公里) (默认: 5)
- `limit`: 返回数量限制 (默认: 20)

### 3. 充电桩管理 API

#### 获取充电桩列表
```http
GET /api/chargers?current=1&size=10&stationId=1&status=1
Authorization: Bearer <token>
```

#### 创建充电桩
```http
POST /api/chargers
Authorization: Bearer <token>
Content-Type: application/json

{
  "stationId": 1,
  "chargerCode": "CH001",
  "chargerName": "1号充电桩",
  "manufacturer": "厂商名称",
  "model": "设备型号",
  "chargerType": 1,
  "powerRate": 120.5,
  "voltageLevel": 380,
  "protocolType": "OCPP"
}
```

#### 更新充电桩状态
```http
PUT /api/chargers/{chargerId}/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": 1
}
```

#### 批量更新充电桩状态
```http
PUT /api/chargers/batch/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "chargerIds": [1, 2, 3],
  "status": 1
}
```

### 4. 订单管理 API

#### 获取订单列表
```http
GET /api/orders?current=1&size=10&status=1&startTime=2025-11-01&endTime=2025-11-02
Authorization: Bearer <token>
```

#### 创建订单(开始充电)
```http
POST /api/orders/start
Authorization: Bearer <token>
Content-Type: application/json

{
  "stationId": 1,
  "chargerId": 1,
  "userId": 100,
  "billingPlanId": 1
}
```

#### 完成订单(结束充电)
```http
POST /api/orders/{orderId}/stop
Authorization: Bearer <token>
```

#### 订单支付
```http
POST /api/orders/{orderId}/pay
Authorization: Bearer <token>
Content-Type: application/json

{
  "paymentMethod": "alipay",
  "amount": 50.00
}
```

### 5. 支付管理 API

#### 创建支付订单
```http
POST /api/payments
Authorization: Bearer <token>
Content-Type: application/json

{
  "orderId": 1,
  "paymentMethod": "alipay",
  "amount": 50.00,
  "subject": "充电服务费",
  "returnUrl": "https://example.com/return",
  "notifyUrl": "https://example.com/notify"
}
```

#### 查询支付状态
```http
GET /api/payments/{paymentId}
Authorization: Bearer <token>
```

#### 支付回调处理
```http
POST /api/payments/callback/{paymentMethod}
Content-Type: application/x-www-form-urlencoded

trade_no=202511021234567890&out_trade_no=PAY001&trade_status=TRADE_SUCCESS...
```

### 6. 租户管理 API

#### 获取租户列表
```http
GET /api/tenants?current=1&size=10&tenantName=测试
Authorization: Bearer <token>
```

#### 创建租户
```http
POST /api/tenants
Authorization: Bearer <token>
Content-Type: application/json

{
  "tenantCode": "T001",
  "tenantName": "测试租户",
  "parentId": null,
  "tenantType": 2,
  "contactName": "联系人",
  "contactPhone": "13800138000",
  "contactEmail": "contact@example.com"
}
```

#### 获取租户树
```http
GET /api/tenants/tree
Authorization: Bearer <token>
```

#### 启用/禁用租户
```http
PUT /api/tenants/{tenantId}/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": 1
}
```

## 🔒 权限控制

### 权限注解
```java
@PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:list')")
```

### 权限类型
- `station:add`: 添加充电站
- `station:edit`: 编辑充电站
- `station:delete`: 删除充电站
- `station:view`: 查看充电站
- `charger:add`: 添加充电桩
- `charger:edit`: 编辑充电桩
- `charger:delete`: 删除充电桩
- `order:view`: 查看订单
- `payment:view`: 查看支付
- `tenant:add`: 添加租户
- `tenant:edit`: 编辑租户
- `tenant:delete`: 删除租户

### 数据权限
```java
@DataScope(value = DataScope.DataScopeType.TENANT)
```

## 📝 数据模型

### 充电站 (Station)
```json
{
  "stationId": "number",
  "tenantId": "number",
  "stationCode": "string",
  "stationName": "string",
  "address": "string",
  "province": "string",
  "city": "string",
  "district": "string",
  "latitude": "number",
  "longitude": "number",
  "operatorName": "string",
  "servicePhone": "string",
  "constructionType": "number",
  "stationType": "number",
  "status": "number",
  "createTime": "datetime",
  "updateTime": "datetime"
}
```

### 充电桩 (Charger)
```json
{
  "chargerId": "number",
  "tenantId": "number",
  "stationId": "number",
  "chargerCode": "string",
  "chargerName": "string",
  "manufacturer": "string",
  "model": "string",
  "chargerType": "number",
  "powerRate": "number",
  "voltageLevel": "number",
  "protocolType": "string",
  "status": "number",
  "createTime": "datetime",
  "updateTime": "datetime"
}
```

### 订单 (Order)
```json
{
  "orderId": "number",
  "tenantId": "number",
  "orderNo": "string",
  "userId": "number",
  "stationId": "number",
  "chargerId": "number",
  "sessionId": "string",
  "billingPlanId": "number",
  "startTime": "datetime",
  "endTime": "datetime",
  "startEnergy": "number",
  "endEnergy": "number",
  "totalEnergy": "number",
  "energyAmount": "number",
  "serviceAmount": "number",
  "totalAmount": "number",
  "status": "number",
  "createTime": "datetime",
  "updateTime": "datetime"
}
```

## 🔧 错误码定义

### 系统错误码
| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 业务错误码
| 错误码 | 说明 |
|--------|------|
| 1001 | 充电站编码已存在 |
| 1002 | 充电站不存在 |
| 1003 | 充电站状态不正确 |
| 2001 | 充电桩编码已存在 |
| 2002 | 充电桩不存在 |
| 2003 | 充电桩状态不正确 |
| 3001 | 订单不存在 |
| 3002 | 订单状态不正确 |
| 3003 | 订单已支付 |
| 4001 | 支付失败 |
| 4002 | 支付超时 |
| 4003 | 支付金额不正确 |
| 5001 | 租户编码已存在 |
| 5002 | 租户不存在 |
| 5003 | 租户状态不正确 |

## 🌐 API版本管理

### 版本策略
- URL路径版本: `/api/v1/stations`
- 请求头版本: `Accept: application/vnd.evcs.v1+json`

### 版本兼容性
- 向后兼容: 新版本兼容旧版本客户端
- 废弃通知: 提前3个月通知API废弃
- 版本支持: 同时支持最近3个主版本

## 📊 API限流

### 限流策略
- 用户级别: 每用户1000次/小时
- IP级别: 每IP 5000次/小时
- 接口级别: 根据接口重要性设置不同限流

### 限流响应
```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "data": {
    "retryAfter": 60
  }
}
```

## 📋 API文档

### Swagger文档
访问地址: `http://localhost:8080/doc.html`

### 文档内容
- API接口列表
- 请求参数说明
- 响应格式说明
- 错误码说明
- 在线测试功能

## 🔍 API测试

### Postman集合
提供完整的Postman测试集合，包含：
- 认证接口测试
- 业务接口测试
- 异常场景测试
- 性能测试脚本

### 自动化测试
```bash
# 运行API测试
./scripts/api-test.sh

# 生成测试报告
./scripts/api-test-report.sh
```

## 🚀 最佳实践

### 请求最佳实践
1. 使用HTTPS协议
2. 设置合理的超时时间
3. 使用适当的HTTP方法
4. 包含必要的请求头
5. 处理异常响应

### 响应最佳实践
1. 统一响应格式
2. 提供详细的错误信息
3. 使用合适的状态码
4. 包含时间戳和追踪ID
5. 分页响应包含分页信息

### 安全最佳实践
1. 永远不要在URL中传递敏感信息
2. 使用HTTPS传输敏感数据
3. 定期轮换API密钥
4. 实施访问控制和权限验证
5. 记录和监控API访问日志

---

**相关文档**:
- [产品需求文档](./requirements.md)
- [技术架构设计](./architecture.md)
- [数据模型设计](./data-model.md)
- [开发规范](../02-development/coding-standards.md)