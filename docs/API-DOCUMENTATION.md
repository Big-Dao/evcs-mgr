# EVCS Manager API 文档

## 📋 概述

本文档描述了 EVCS Manager 系统的 RESTful API 接口规范。

**基础信息**
- **版本**: v1.0.0
- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Bearer Token
- **内容类型**: `application/json`

## 🔐 认证

所有API请求都需要在请求头中包含JWT令牌：

```http
Authorization: Bearer <your-jwt-token>
```

### 获取访问令牌

**端点**: `POST /auth/login`

**请求体**:
```json
{
  "username": "admin@example.com",
  "password": "password123"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh_token_here",
    "expiresIn": 7200,
    "userId": 1,
    "tenantId": 1,
    "username": "admin@example.com"
  }
}
```

## 🏢 租户管理 API

### 查询租户列表

**端点**: `GET /tenant/list`

**权限**: `tenant:query`

**请求参数**:
- `pageNum` (integer): 页码，默认1
- `pageSize` (integer): 每页大小，默认10
- `tenantName` (string, optional): 租户名称（模糊查询）
- `status` (integer, optional): 状态（1=启用，0=停用）

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 25,
    "list": [
      {
        "tenantId": 1,
        "tenantCode": "T001",
        "tenantName": "示例租户",
        "parentId": null,
        "contactPerson": "张三",
        "contactPhone": "13800138000",
        "contactEmail": "zhangsan@example.com",
        "status": 1,
        "createTime": "2025-01-01T00:00:00Z",
        "updateTime": "2025-01-08T10:00:00Z"
      }
    ],
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 创建租户

**端点**: `POST /tenant/create`

**权限**: `tenant:create`

**请求体**:
```json
{
  "tenantCode": "T002",
  "tenantName": "新租户",
  "parentId": 1,
  "contactPerson": "李四",
  "contactPhone": "13900139000",
  "contactEmail": "lisi@example.com",
  "status": 1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "tenantId": 2,
    "tenantCode": "T002",
    "tenantName": "新租户"
  }
}
```

## ⚡ 充电站管理 API

### 查询充电站列表

**端点**: `GET /station/list`

**权限**: `station:query`

**请求参数**:
- `pageNum` (integer): 页码
- `pageSize` (integer): 每页大小
- `stationName` (string, optional): 充电站名称
- `status` (integer, optional): 状态
- `latitude` (double, optional): 纬度（用于附近搜索）
- `longitude` (double, optional): 经度（用于附近搜索）
- `radius` (double, optional): 搜索半径（公里）

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 50,
    "list": [
      {
        "stationId": 1,
        "stationCode": "ST001",
        "stationName": "市中心充电站",
        "address": "北京市朝阳区XX路XX号",
        "latitude": 39.9042,
        "longitude": 116.4074,
        "totalChargers": 20,
        "availableChargers": 15,
        "facilities": ["停车", "休息区", "便利店"],
        "paymentMethods": [1, 2, 3],
        "status": 1,
        "operatingHours": "00:00-24:00"
      }
    ]
  }
}
```

### 创建充电站

**端点**: `POST /station/create`

**权限**: `station:create`

**请求体**:
```json
{
  "stationCode": "ST002",
  "stationName": "新充电站",
  "address": "北京市海淀区XX路XX号",
  "latitude": 39.9889,
  "longitude": 116.3060,
  "contactPhone": "010-12345678",
  "facilities": ["停车", "休息区"],
  "paymentMethods": [1, 2],
  "status": 1,
  "operatingHours": "00:00-24:00"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "stationId": 2,
    "stationCode": "ST002",
    "stationName": "新充电站"
  }
}
```

### 查询充电桩列表

**端点**: `GET /charger/list`

**权限**: `charger:query`

**请求参数**:
- `stationId` (long, optional): 充电站ID
- `status` (integer, optional): 状态（0=离线，1=空闲，2=充电中，3=故障）

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "list": [
      {
        "chargerId": 1,
        "chargerCode": "CH001",
        "chargerName": "1号充电桩",
        "stationId": 1,
        "chargerType": 1,
        "powerType": 1,
        "maxPower": 120.0,
        "status": 1,
        "currentPower": 0.0,
        "voltage": 380.0,
        "current": 0.0,
        "lastHeartbeat": "2025-01-08T10:30:00Z"
      }
    ]
  }
}
```

## 📦 订单管理 API

### 创建充电订单

**端点**: `POST /order/create`

**权限**: `order:create`

**请求体**:
```json
{
  "stationId": 1,
  "chargerId": 1,
  "userId": 100,
  "estimatedDuration": 60,
  "idempotencyKey": "ORDER_20250108_123456"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "订单创建成功",
  "data": {
    "orderId": 1001,
    "orderNo": "ORD20250108123456",
    "status": 0,
    "createTime": "2025-01-08T10:30:00Z"
  }
}
```

### 开始充电

**端点**: `POST /order/{orderId}/start`

**权限**: `order:start`

**响应**:
```json
{
  "code": 200,
  "message": "充电已开始",
  "data": {
    "orderId": 1001,
    "status": 1,
    "startTime": "2025-01-08T10:35:00Z"
  }
}
```

### 停止充电

**端点**: `POST /order/{orderId}/stop`

**权限**: `order:stop`

**响应**:
```json
{
  "code": 200,
  "message": "充电已停止",
  "data": {
    "orderId": 1001,
    "status": 2,
    "startTime": "2025-01-08T10:35:00Z",
    "endTime": "2025-01-08T11:30:00Z",
    "duration": 55,
    "totalPower": 45.5,
    "totalAmount": 68.25
  }
}
```

### 查询订单详情

**端点**: `GET /order/{orderId}`

**权限**: `order:query`

**响应**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "orderId": 1001,
    "orderNo": "ORD20250108123456",
    "tenantId": 1,
    "userId": 100,
    "stationId": 1,
    "stationName": "市中心充电站",
    "chargerId": 1,
    "chargerCode": "CH001",
    "status": 3,
    "startTime": "2025-01-08T10:35:00Z",
    "endTime": "2025-01-08T11:30:00Z",
    "duration": 55,
    "totalPower": 45.5,
    "serviceAmount": 45.50,
    "electricityAmount": 22.75,
    "totalAmount": 68.25,
    "paymentStatus": 1,
    "createTime": "2025-01-08T10:30:00Z"
  }
}
```

## 💳 支付管理 API

### 创建支付订单

**端点**: `POST /payment/create`

**权限**: `payment:create`

**请求体**:
```json
{
  "orderId": 1001,
  "paymentChannel": "ALIPAY",
  "paymentMethod": "APP",
  "amount": 68.25
}
```

**响应**:
```json
{
  "code": 200,
  "message": "支付订单创建成功",
  "data": {
    "paymentId": 5001,
    "paymentNo": "PAY20250108123456",
    "orderString": "alipay_sdk=alipay-sdk-java...",
    "expireTime": "2025-01-08T11:00:00Z"
  }
}
```

### 支付宝回调

**端点**: `POST /payment/alipay/notify`

**说明**: 该接口由支付宝服务器调用，不需要认证

**请求体**: form-urlencoded 格式的支付宝通知参数

**响应**: `success` 或 `failure`

### 查询支付状态

**端点**: `GET /payment/{paymentId}/status`

**权限**: `payment:query`

**响应**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "paymentId": 5001,
    "paymentNo": "PAY20250108123456",
    "orderId": 1001,
    "status": 1,
    "paymentChannel": "ALIPAY",
    "amount": 68.25,
    "paidTime": "2025-01-08T10:40:00Z"
  }
}
```

## 📊 计费管理 API

### 查询计费计划

**端点**: `GET /billing/plan/{planId}`

**权限**: `billing:query`

**响应**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "planId": 1,
    "planName": "标准计费计划",
    "tenantId": 1,
    "stationId": 1,
    "isDefault": 1,
    "segments": [
      {
        "segmentId": 1,
        "startTime": "00:00",
        "endTime": "08:00",
        "servicePrice": 0.80,
        "electricityPrice": 0.50
      },
      {
        "segmentId": 2,
        "startTime": "08:00",
        "endTime": "22:00",
        "servicePrice": 1.00,
        "electricityPrice": 0.60
      },
      {
        "segmentId": 3,
        "startTime": "22:00",
        "endTime": "24:00",
        "servicePrice": 0.80,
        "electricityPrice": 0.50
      }
    ]
  }
}
```

## 🔍 错误码说明

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 200 | 成功 | 200 |
| 400 | 请求参数错误 | 400 |
| 401 | 未授权（令牌无效或过期） | 401 |
| 403 | 权限不足 | 403 |
| 404 | 资源不存在 | 404 |
| 409 | 资源冲突（如重复创建） | 409 |
| 500 | 服务器内部错误 | 500 |
| 1001 | 租户不存在 | 400 |
| 1002 | 租户已存在 | 409 |
| 2001 | 充电站不存在 | 404 |
| 2002 | 充电桩不可用 | 400 |
| 2003 | 充电桩正在充电中 | 409 |
| 3001 | 订单不存在 | 404 |
| 3002 | 订单状态不正确 | 400 |
| 3003 | 订单已完成，不能修改 | 400 |
| 4001 | 支付失败 | 400 |
| 4002 | 支付已过期 | 400 |

## 📝 通用响应格式

所有API响应遵循统一的格式：

**成功响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { }
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

## 🔄 分页格式

列表查询接口的分页参数和响应格式：

**分页参数**:
- `pageNum`: 页码（从1开始）
- `pageSize`: 每页大小（默认10，最大100）

**分页响应**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 100,
    "list": [],
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  }
}
```

## 🌐 多租户隔离

所有API都会自动进行租户隔离：
- 从JWT令牌中提取租户ID
- 查询和修改操作自动过滤租户数据
- 跨租户访问会返回404或403错误

## 📞 技术支持

- **API文档更新**: 2025-01-08
- **Swagger UI**: http://localhost:8080/doc.html
- **问题反馈**: GitHub Issues

---

**注意**: 本文档描述的是核心API接口，完整的API列表请参考Swagger UI文档。
