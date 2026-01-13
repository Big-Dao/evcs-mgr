# EVCS 错误码规范

> **版本**: v1.0  
> **创建日期**: 2026-01-13  
> **维护者**: 架构团队  
> **状态**: 已发布

---

## 1. 概述

本文档定义 EVCS 充电站管理系统的错误码规范，确保 API 响应的一致性和可维护性。

### 1.1 设计原则

- **唯一性**: 每个错误码在系统内唯一
- **可读性**: 错误码能够表达错误类型和模块
- **可扩展性**: 预留足够的编码空间
- **国际化**: 支持多语言错误消息

---

## 2. 错误码格式

### 2.1 编码规则

```
XYYZZZ

X   - 错误类型 (1位)
YY  - 模块编码 (2位)
ZZZ - 具体错误 (3位)
```

### 2.2 错误类型 (X)

| 类型 | 含义 | 说明 |
|------|------|------|
| **1** | 系统错误 | 服务器内部错误 |
| **2** | 参数错误 | 请求参数校验失败 |
| **3** | 业务错误 | 业务规则校验失败 |
| **4** | 认证错误 | 身份认证失败 |
| **5** | 权限错误 | 权限不足 |
| **6** | 资源错误 | 资源不存在或冲突 |
| **7** | 外部错误 | 第三方服务异常 |

### 2.3 模块编码 (YY)

| 模块 | 编码 | 说明 |
|------|------|------|
| **通用** | 00 | 通用错误 |
| **认证** | 01 | Auth 模块 |
| **租户** | 02 | Tenant 模块 |
| **用户** | 03 | User 模块 |
| **站点** | 10 | Station 模块 |
| **充电桩** | 11 | Charger 模块 |
| **订单** | 20 | Order 模块 |
| **支付** | 21 | Payment 模块 |
| **协议** | 30 | Protocol 模块 |
| **通知** | 40 | Notification 模块 |
| **监控** | 50 | Monitoring 模块 |

---

## 3. 错误码定义

### 3.1 通用错误 (X00XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 100000 | SYSTEM_ERROR | 系统错误 | 未知系统异常 |
| 100001 | SERVICE_UNAVAILABLE | 服务暂不可用 | 服务不可用 |
| 100002 | DATABASE_ERROR | 数据库错误 | 数据库异常 |
| 100003 | CACHE_ERROR | 缓存错误 | Redis 异常 |
| 100004 | MQ_ERROR | 消息队列错误 | RabbitMQ 异常 |
| 100005 | TIMEOUT | 请求超时 | 操作超时 |
| 200001 | PARAM_ERROR | 参数错误 | 通用参数错误 |
| 200002 | PARAM_MISSING | 缺少必填参数 | 必填参数缺失 |
| 200003 | PARAM_INVALID | 参数格式错误 | 参数格式不正确 |
| 200004 | PARAM_OUT_OF_RANGE | 参数超出范围 | 参数值超出允许范围 |

### 3.2 认证错误 (X01XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 401001 | TOKEN_MISSING | Token 缺失 | 未提供 Token |
| 401002 | TOKEN_INVALID | Token 无效 | Token 格式错误 |
| 401003 | TOKEN_EXPIRED | Token 已过期 | Token 已过期 |
| 401004 | LOGIN_FAILED | 登录失败 | 用户名或密码错误 |
| 401005 | ACCOUNT_LOCKED | 账户已锁定 | 账户被锁定 |
| 401006 | ACCOUNT_DISABLED | 账户已禁用 | 账户被禁用 |
| 401007 | PASSWORD_EXPIRED | 密码已过期 | 密码需要更新 |
| 401008 | CAPTCHA_ERROR | 验证码错误 | 验证码不正确 |
| 401009 | SMS_CODE_ERROR | 短信验证码错误 | 短信验证码不正确 |
| 401010 | SMS_CODE_EXPIRED | 短信验证码过期 | 短信验证码已过期 |

### 3.3 权限错误 (X01XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 501001 | ACCESS_DENIED | 访问被拒绝 | 无权限访问 |
| 501002 | TENANT_ACCESS_DENIED | 租户访问被拒绝 | 跨租户访问 |
| 501003 | RESOURCE_ACCESS_DENIED | 资源访问被拒绝 | 无资源权限 |
| 501004 | OPERATION_NOT_ALLOWED | 操作不允许 | 当前状态不允许操作 |

### 3.4 租户错误 (X02XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 302001 | TENANT_NOT_FOUND | 租户不存在 | 租户未找到 |
| 302002 | TENANT_DISABLED | 租户已禁用 | 租户被禁用 |
| 302003 | TENANT_EXPIRED | 租户已过期 | 租户服务已到期 |
| 302004 | TENANT_QUOTA_EXCEEDED | 租户配额超限 | 超出租户配额 |
| 602001 | TENANT_CODE_EXISTS | 租户编码已存在 | 编码重复 |

### 3.5 用户错误 (X03XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 303001 | USER_NOT_FOUND | 用户不存在 | 用户未找到 |
| 303002 | USER_DISABLED | 用户已禁用 | 用户被禁用 |
| 303003 | PASSWORD_WRONG | 密码错误 | 密码不正确 |
| 303004 | OLD_PASSWORD_WRONG | 原密码错误 | 修改密码时原密码错误 |
| 303005 | PASSWORD_TOO_SIMPLE | 密码过于简单 | 密码不满足复杂度要求 |
| 603001 | USERNAME_EXISTS | 用户名已存在 | 用户名重复 |
| 603002 | PHONE_EXISTS | 手机号已存在 | 手机号重复 |
| 603003 | EMAIL_EXISTS | 邮箱已存在 | 邮箱重复 |

### 3.6 站点错误 (X10XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 310001 | STATION_NOT_FOUND | 站点不存在 | 站点未找到 |
| 310002 | STATION_OFFLINE | 站点已下线 | 站点不在线 |
| 310003 | STATION_MAINTENANCE | 站点维护中 | 站点正在维护 |
| 610001 | STATION_CODE_EXISTS | 站点编码已存在 | 编码重复 |
| 610002 | STATION_HAS_CHARGERS | 站点存在充电桩 | 删除前需移除充电桩 |

### 3.7 充电桩错误 (X11XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 311001 | CHARGER_NOT_FOUND | 充电桩不存在 | 充电桩未找到 |
| 311002 | CHARGER_OFFLINE | 充电桩离线 | 充电桩不在线 |
| 311003 | CHARGER_BUSY | 充电桩使用中 | 充电桩正在使用 |
| 311004 | CHARGER_FAULT | 充电桩故障 | 充电桩存在故障 |
| 311005 | CHARGER_MAINTENANCE | 充电桩维护中 | 充电桩正在维护 |
| 311006 | CONNECTOR_NOT_FOUND | 枪口不存在 | 枪口未找到 |
| 311007 | CONNECTOR_BUSY | 枪口使用中 | 枪口正在使用 |
| 311008 | CONNECTOR_FAULT | 枪口故障 | 枪口存在故障 |
| 611001 | CHARGER_CODE_EXISTS | 充电桩编码已存在 | 编码重复 |

### 3.8 订单错误 (X20XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 320001 | ORDER_NOT_FOUND | 订单不存在 | 订单未找到 |
| 320002 | ORDER_STATUS_ERROR | 订单状态异常 | 订单状态不允许操作 |
| 320003 | ORDER_EXPIRED | 订单已过期 | 订单已超时 |
| 320004 | ORDER_CANCELLED | 订单已取消 | 订单已被取消 |
| 320005 | ORDER_COMPLETED | 订单已完成 | 订单已结束 |
| 320010 | CHARGING_IN_PROGRESS | 充电中 | 用户有未完成的充电 |
| 320011 | CHARGING_START_FAILED | 启动充电失败 | 充电启动失败 |
| 320012 | CHARGING_STOP_FAILED | 停止充电失败 | 充电停止失败 |
| 320020 | BALANCE_INSUFFICIENT | 余额不足 | 用户余额不足 |
| 320021 | PRICING_NOT_FOUND | 计费方案不存在 | 未配置计费方案 |

### 3.9 支付错误 (X21XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 321001 | PAYMENT_NOT_FOUND | 支付单不存在 | 支付单未找到 |
| 321002 | PAYMENT_FAILED | 支付失败 | 支付处理失败 |
| 321003 | PAYMENT_TIMEOUT | 支付超时 | 支付请求超时 |
| 321004 | PAYMENT_CANCELLED | 支付已取消 | 用户取消支付 |
| 321005 | PAYMENT_COMPLETED | 支付已完成 | 重复支付 |
| 321010 | REFUND_FAILED | 退款失败 | 退款处理失败 |
| 321011 | REFUND_AMOUNT_EXCEEDED | 退款金额超限 | 退款金额超过可退金额 |
| 721001 | PAYMENT_CHANNEL_ERROR | 支付渠道异常 | 第三方支付异常 |
| 721002 | PAYMENT_SIGN_ERROR | 支付签名错误 | 签名验证失败 |

### 3.10 协议错误 (X30XXX)

| 错误码 | 常量名 | 消息 | 说明 |
|--------|--------|------|------|
| 330001 | PROTOCOL_ERROR | 协议错误 | 协议解析失败 |
| 330002 | HEARTBEAT_TIMEOUT | 心跳超时 | 设备心跳超时 |
| 330003 | COMMAND_FAILED | 指令执行失败 | 下发指令失败 |
| 330004 | COMMAND_TIMEOUT | 指令超时 | 指令响应超时 |
| 330005 | DEVICE_NOT_REGISTERED | 设备未注册 | 设备未在系统注册 |

---

## 4. 错误码实现

### 4.1 错误码枚举

```java
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // 成功
    SUCCESS(0, "成功"),
    
    // 系统错误 100XXX
    SYSTEM_ERROR(100000, "系统错误"),
    SERVICE_UNAVAILABLE(100001, "服务暂不可用"),
    DATABASE_ERROR(100002, "数据库错误"),
    CACHE_ERROR(100003, "缓存错误"),
    MQ_ERROR(100004, "消息队列错误"),
    TIMEOUT(100005, "请求超时"),
    
    // 参数错误 200XXX
    PARAM_ERROR(200001, "参数错误"),
    PARAM_MISSING(200002, "缺少必填参数"),
    PARAM_INVALID(200003, "参数格式错误"),
    PARAM_OUT_OF_RANGE(200004, "参数超出范围"),
    
    // 业务错误 - 订单 320XXX
    ORDER_NOT_FOUND(320001, "订单不存在"),
    ORDER_STATUS_ERROR(320002, "订单状态异常"),
    ORDER_EXPIRED(320003, "订单已过期"),
    CHARGING_IN_PROGRESS(320010, "充电中"),
    BALANCE_INSUFFICIENT(320020, "余额不足"),
    
    // 认证错误 401XXX
    TOKEN_MISSING(401001, "Token缺失"),
    TOKEN_INVALID(401002, "Token无效"),
    TOKEN_EXPIRED(401003, "Token已过期"),
    LOGIN_FAILED(401004, "登录失败"),
    
    // 权限错误 501XXX
    ACCESS_DENIED(501001, "访问被拒绝"),
    TENANT_ACCESS_DENIED(501002, "租户访问被拒绝"),
    
    // 资源错误 6XXXXX
    CHARGER_CODE_EXISTS(611001, "充电桩编码已存在"),
    
    // 外部错误 7XXXXX
    PAYMENT_CHANNEL_ERROR(721001, "支付渠道异常");
    
    private final int code;
    private final String message;
    
    /**
     * 根据错误码获取枚举
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }
}
```

### 4.2 业务异常

```java
@Getter
public class BusinessException extends RuntimeException {
    
    private final int code;
    private final String message;
    private final Object data;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.data = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
        this.data = null;
    }

    public BusinessException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.data = data;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = null;
    }
}
```

### 4.3 全局异常处理

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
```

---

## 5. 统一响应格式

### 5.1 响应结构

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    
    private int code;
    private String message;
    private T data;
    private String traceId;
    private long timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMessage("成功");
        result.setData(data);
        result.setTraceId(MDC.get("traceId"));
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTraceId(MDC.get("traceId"));
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public boolean isSuccess() {
        return code == 0;
    }
}
```

### 5.2 响应示例

```json
// 成功响应
{
    "code": 0,
    "message": "成功",
    "data": {
        "id": 12345,
        "orderNo": "ORD20260113001"
    },
    "traceId": "abc123def456",
    "timestamp": 1736766468114
}

// 失败响应
{
    "code": 320020,
    "message": "余额不足",
    "data": null,
    "traceId": "abc123def456",
    "timestamp": 1736766468114
}
```

---

## 6. 国际化

### 6.1 消息配置

```properties
# messages.properties (默认中文)
error.100000=系统错误
error.320020=余额不足，当前余额: {0}，需要: {1}

# messages_en.properties (英文)
error.100000=System error
error.320020=Insufficient balance, current: {0}, required: {1}
```

### 6.2 国际化工具

```java
@Component
@RequiredArgsConstructor
public class ErrorMessageSource {

    private final MessageSource messageSource;

    public String getMessage(int code, Object... args) {
        try {
            return messageSource.getMessage(
                "error." + code, 
                args, 
                LocaleContextHolder.getLocale()
            );
        } catch (NoSuchMessageException e) {
            return ErrorCode.fromCode(code).getMessage();
        }
    }
}
```

---

## 7. 相关文档

- [API 设计规范](../../.github/instructions/api.instructions.md)
- [日志规范](LOGGING-STANDARDS.md)
- [项目编码规范](PROJECT-CODING-STANDARDS.md)

---

## 8. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本 |
