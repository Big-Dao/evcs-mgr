# EVCS Payment Service

电动汽车充电站管理系统 - 支付服务模块

## 功能概述

支付服务提供完整的支付解决方案，支持多种支付方式，包括支付宝和微信支付。

### 核心功能

1. **支付订单管理**
   - 创建支付订单
   - 查询支付状态
   - 处理支付回调
   - 支持幂等性保证

2. **退款管理**
   - 全额退款
   - 部分退款
   - 退款状态跟踪

3. **对账功能**
   - 每日自动对账
   - 手动触发对账
   - 对账结果统计和报表

4. **多租户支持**
   - 数据完全隔离
   - 租户级别的支付统计
   - 多租户对账

## 支持的支付方式

### 支付宝
- APP支付 (`ALIPAY_APP`)
- 扫码支付 (`ALIPAY_QR`)

### 微信支付
- 小程序支付 (`WECHAT_JSAPI`)
- Native扫码支付 (`WECHAT_NATIVE`)

## API接口

### 支付管理

#### 创建支付订单
```http
POST /api/payment/create
Content-Type: application/json

{
  "orderId": 1001,
  "amount": 99.99,
  "paymentMethod": "ALIPAY_APP",
  "userId": 100,
  "description": "充电订单支付",
  "idempotentKey": "unique-key-123"
}
```

#### 查询支付状态
```http
GET /api/payment/query/{tradeNo}
```

#### 支付宝回调
```http
POST /api/payment/callback/alipay?tradeNo={tradeNo}&success=true
```

#### 微信支付回调
```http
POST /api/payment/callback/wechat?tradeNo={tradeNo}&success=true
```

#### 退款
```http
POST /api/payment/refund
Content-Type: application/json

{
  "paymentId": 12345,
  "refundAmount": 50.00,
  "refundReason": "用户取消订单"
}
```

### 对账管理

#### 执行对账
```http
POST /api/reconciliation/execute
Content-Type: application/json

{
  "reconciliationDate": "2025-10-10",
  "channel": "alipay"
}
```

#### 每日自动对账
```http
POST /api/reconciliation/daily/{channel}
```

## 数据库设计

### payment_order 表结构

```sql
CREATE TABLE payment_order (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    trade_no VARCHAR(64) NOT NULL UNIQUE,
    payment_method VARCHAR(32) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status INTEGER NOT NULL DEFAULT 0,
    out_trade_no VARCHAR(64),
    paid_time TIMESTAMP,
    idempotent_key VARCHAR(64),
    description VARCHAR(255),
    pay_params TEXT,
    pay_url VARCHAR(512),
    refund_amount DECIMAL(10, 2),
    refund_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);
```

### 支付状态枚举

- `0` - PENDING（待支付）
- `1` - PROCESSING（支付中）
- `2` - SUCCESS（支付成功）
- `3` - FAILED（支付失败）
- `4` - REFUNDING（退款中）
- `5` - REFUNDED（已退款）

## 配置说明

### application.yml

```yaml
spring:
  application:
    name: evcs-payment
  
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/evcs
    username: postgres
    password: postgres

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## 集成测试

项目包含完整的集成测试套件，覆盖以下场景：

### 测试用例

1. **完整支付流程测试**
   - 创建支付订单
   - 查询支付状态
   - 模拟支付回调
   - 验证订单状态更新

2. **退款流程测试**
   - 全额退款
   - 部分退款
   - 验证退款状态

3. **对账功能测试**
   - 创建多笔订单
   - 执行对账
   - 验证对账结果统计

4. **多租户隔离测试**
   - 租户A创建订单
   - 租户B无法查询租户A的订单
   - 验证数据隔离

5. **幂等性测试**
   - 重复创建订单请求
   - 重复支付回调
   - 验证幂等性保证

6. **异常场景测试**
   - 退款金额超过支付金额
   - 未支付订单退款
   - 验证异常处理

### 运行测试

```bash
# 运行所有测试
./gradlew :evcs-payment:test

# 运行集成测试
./gradlew :evcs-payment:test --tests PaymentIntegrationTest

# 运行单元测试
./gradlew :evcs-payment:test --tests PaymentServiceTestTemplate
```

## 开发指南

### 添加新的支付渠道

1. 实现 `IPaymentChannel` 接口
2. 添加新的支付方式到 `PaymentMethod` 枚举
3. 在 `PaymentServiceImpl.selectChannel()` 中添加渠道选择逻辑
4. 编写集成测试验证新渠道

### 扩展对账功能

1. 实现对账单文件下载
2. 解析对账单数据（CSV/Excel格式）
3. 比对系统订单与对账单
4. 生成差异报表和导出功能

## 技术栈

- Java 17
- Spring Boot 3.2.2
- MyBatis Plus 3.5.6
- PostgreSQL 42.7.1
- Lombok
- JUnit 5

## 注意事项

### 安全性

1. **签名验证**：所有支付回调必须验证签名
2. **HTTPS**：生产环境必须使用HTTPS
3. **敏感信息**：API密钥和证书不得提交到代码仓库

### 幂等性

1. 使用 `idempotentKey` 防止重复创建支付订单
2. 支付回调自动实现幂等性（检查订单状态）
3. 退款操作需要业务层面保证幂等性

### 多租户

1. 所有查询自动添加 `tenant_id` 过滤（MyBatis Plus自动处理）
2. 创建订单时必须设置租户ID（从 `TenantContext` 获取）
3. 跨租户查询需要管理员权限

## 后续优化

- [ ] 集成真实的支付宝SDK
- [ ] 集成真实的微信支付SDK
- [ ] 实现对账单文件下载和解析
- [ ] 增加更多支付方式（银联、数字货币等）
- [ ] 实现支付重试机制
- [ ] 添加支付监控和告警
- [ ] 优化大批量对账性能
- [ ] 支持异步通知消息队列

## 联系方式

如有问题，请联系开发团队或提交Issue。

