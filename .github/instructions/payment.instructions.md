---
applyTo: "evcs-payment/**/*.java"
---

# evcs-payment 模块开发规范

> **最后更新**: 2025-12-18 | **维护者**: 支付团队 | **状态**: 已发布

本模块负责支付网关集成、退款处理和对账。涉及资金安全，必须严格遵守以下规范。
请遵循 [PROJECT-CODING-STANDARDS.md](../../docs/overview/PROJECT-CODING-STANDARDS.md) 中的核心规范。

## 关键要求

### 1. 资金安全
**金额计算必须精确**
- 禁止使用 `double` 或 `float` 进行金额计算
- 必须使用 `BigDecimal`，并指定舍入模式（通常为 `RoundingMode.HALF_UP`）
- 数据库金额字段统一使用 `DECIMAL(20, 2)`

### 2. 幂等性设计
**支付接口必须支持幂等**
- 所有的支付创建、退款接口必须包含 `requestId` 或 `orderId`
- 使用 Redis 或数据库唯一索引防止重复提交
- 第三方回调处理必须检查订单状态，防止重复入账

### 3. 数据脱敏
**严禁明文记录敏感信息**
- 日志中必须对以下字段脱敏：
    - 银行卡号
    - 身份证号
    - 手机号
    - 支付密码/CVV
- 使用 `com.evcs.common.util.SensitiveDataMasker` 工具类进行脱敏

### 4. 异常处理
**支付异常需明确分类**
- 区分“支付失败”（明确失败，可关闭订单）与“支付未知”（网络超时，需轮询查询）
- 遇到“未知”状态时，禁止直接回滚或重试，必须发起主动查询

---

## ✅ 测试准则

- ✅ 金额计算精度测试（边界值、多位小数）
- ✅ 并发重复支付测试（幂等性）
- ✅ 支付回调状态机测试（防止状态回退）
- ✅ 敏感数据日志脱敏测试

---

## 常见模式

### 金额计算

```java
// ✅ 正确
BigDecimal amount = new BigDecimal("10.05");
BigDecimal result = amount.multiply(new BigDecimal("3")).setScale(2, RoundingMode.HALF_UP);

// ❌ 错误
double amount = 10.05;
double result = amount * 3;
```

### 幂等性检查

```java
// 使用 Redis 锁或数据库唯一约束
if (paymentRepository.existsByOrderId(orderId)) {
    return paymentRepository.findByOrderId(orderId);
}
// ... 创建支付单
```
