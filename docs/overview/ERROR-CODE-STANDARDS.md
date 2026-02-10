# EVCS 错误码规范

> **版本**: v1.1
> **创建日期**: 2026-01-13
> **更新日期**: 2026-02-10
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

| 类型 | HTTP状态码 | 含义 | 说明 |
|------|-----------|------|------|
| **2** | 200 | 成功 | 请求成功 |
| **4** | 400 | 参数错误 | 请求参数校验失败 |
| **4** | 401 | 认证错误 | 身份认证失败 |
| **4** | 403 | 权限错误 | 权限不足 |
| **4** | 404 | 资源错误 | 资源不存在 |
| **4** | 409 | 冲突错误 | 资源冲突 |
| **5** | 500 | 系统错误 | 服务器内部错误 |
| **5** | 503 | 服务不可用 | 服务暂时不可用 |

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

## 3. 统一异常体系

### 3.1 异常类层次结构

```
RuntimeException
    └── BaseException (基类)
        ├── BusinessException (业务异常)
        ├── ResourceNotFoundException (资源不存在 - 404)
        ├── ResourceConflictException (资源冲突 - 409)
        ├── ServiceUnavailableException (服务不可用 - 503)
        └── ThirdPartyServiceException (第三方服务异常 - 5001)
```

### 3.2 异常类说明

| 异常类 | HTTP状态码 | 使用场景 | 实现状态 |
|--------|-----------|----------|----------|
| `BaseException` | 动态 | 所有自定义异常的基类 | ✅ 已实现 |
| `BusinessException` | 400 | 通用业务异常 | ✅ 已实现 |
| `ResourceNotFoundException` | 404 | 资源不存在 | ✅ 已实现 |
| `ResourceConflictException` | 409 | 资源冲突（如重复创建） | ✅ 已实现 |
| `ServiceUnavailableException` | 503 | 服务暂时不可用 | ✅ 已实现 |
| `ThirdPartyServiceException` | 5001 | 第三方服务异常 | ✅ 已实现 |

### 3.3 异常类实现

```java
// evcs-common/src/main/java/com/evcs/common/exception/BaseException.java
@Getter
public class BaseException extends RuntimeException {

    private final Integer code;

    public BaseException(String message) {
        super(message);
        this.code = 500; // 默认服务器错误
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BaseException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BaseException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }
}

// 资源不存在异常
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(ResultCode code, String message) {
        super(code, message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(
            ResultCode.NOT_FOUND,
            String.format("%s不存在: %s", resource, id)
        );
    }
}

// 资源冲突异常
public class ResourceConflictException extends BaseException {

    public ResourceConflictException(ResultCode code, String message) {
        super(code, message);
    }

    public ResourceConflictException(ResultCode code, String message, Throwable cause) {
        super(code, message, cause);
    }
}

// 服务不可用异常
public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(String message) {
        super(ResultCode.SERVICE_UNAVAILABLE, message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(ResultCode.SERVICE_UNAVAILABLE, message, cause);
    }

    public static ServiceUnavailableException forService(String serviceName) {
        return new ServiceUnavailableException(
            String.format("%s服务暂时不可用", serviceName)
        );
    }
}

// 第三方服务异常
public class ThirdPartyServiceException extends BaseException {

    private final String thirdParty;

    public ThirdPartyServiceException(String thirdParty, String message) {
        super(ResultCode.THIRD_PARTY_ERROR,
              String.format("%s服务调用失败: %s", thirdParty, message));
        this.thirdParty = thirdParty;
    }

    public ThirdPartyServiceException(ResultCode resultCode, String thirdParty,
                                       String message, Throwable cause) {
        super(resultCode,
              String.format("%s服务调用失败: %s", thirdParty, message), cause);
        this.thirdParty = thirdParty;
    }

    public String getThirdParty() {
        return thirdParty;
    }
}
```

### 3.4 全局异常处理器

```java
// evcs-common/src/main/java/com/evcs/common/exception/GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 优先级由 @Order 控制，数字越小优先级越高

    @ExceptionHandler(TenantContextMissingException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @Order(1)
    public Result<Void> handleTenantContextMissingException(TenantContextMissingException e) {
        log.error("租户上下文缺失: {}", e.getMessage());
        return Result.failure(ResultCode.UNAUTHORIZED.getCode(), e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @Order(2)
    public Result<Void> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @Order(3)
    public Result<Void> handleResourceConflictException(ResourceConflictException e) {
        log.warn("资源冲突: {}", e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @Order(4)
    public Result<Void> handleServiceUnavailableException(ServiceUnavailableException e) {
        log.error("服务不可用: {}", e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ThirdPartyServiceException.class)
    @Order(5)
    public Result<Void> handleThirdPartyServiceException(ThirdPartyServiceException e) {
        log.error("第三方服务异常: thirdParty={}, message={}",
                e.getThirdParty(), e.getMessage(), e);
        return Result.failure(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BaseException.class)
    @Order(6)
    public Result<Void> handleBaseException(BaseException e, HttpServletResponse response) {
        log.warn("基础异常: code={}, message={}", e.getCode(), e.getMessage());
        response.setStatus(resolveHttpStatus(e.getCode()));
        return Result.failure(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @Order(7)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常: {}", e.getMessage());
        response.setStatus(resolveHttpStatus(e.getCode()));
        return Result.failure(e.getCode(), e.getMessage());
    }

    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        return Result.failure(ResultCode.PARAM_ERROR.getCode(), message);
    }

    // 通用异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @Order(Integer.MAX_VALUE)
    public Result<Void> handleException(Exception e) {
        log.error("未处理的异常", e);
        return Result.failure(ResultCode.INTERNAL_SERVER_ERROR);
    }

    private int resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST.value();
        }
        // 标准 HTTP 4xx/5xx 直接映射
        if (code >= 400 && code < 600) {
            return code;
        }
        // 非标准业务码（如 4xxx/5xxx）：统一返回 400，由 body.code 传递业务码
        return HttpStatus.BAD_REQUEST.value();
    }
}
```

---

## 4. 错误码定义

### 4.1 ResultCode 枚举（已实现）

```java
// evcs-common/src/main/java/com/evcs/common/result/ResultCode.java
@Getter
@AllArgsConstructor
public enum ResultCode {

    /* 成功状态码 */
    SUCCESS(200, "操作成功"),

    /* 客户端错误 */
    FAILURE(400, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    PARAM_NULL(400, "参数为空"),
    PARAM_FORMAT_ERROR(400, "参数格式错误"),

    /* 认证授权相关 */
    UNAUTHORIZED(401, "未认证"),
    TOKEN_INVALID(401, "Token无效"),
    TOKEN_EXPIRED(401, "Token已过期"),
    FORBIDDEN(403, "无权限访问"),

    /* 业务错误 */
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不被允许"),
    CONFLICT(409, "资源冲突"),

    /* 租户相关 */
    TENANT_NOT_FOUND(4001, "租户不存在"),
    TENANT_DISABLED(4002, "租户已禁用"),
    TENANT_PERMISSION_DENIED(4003, "租户权限不足"),

    /* 用户相关 */
    USER_NOT_FOUND(4101, "用户不存在"),
    USER_DISABLED(4102, "用户已禁用"),
    USER_PASSWORD_ERROR(4103, "密码错误"),
    USER_ACCOUNT_LOCKED(4104, "账户已锁定"),

    /* 充电站相关 */
    STATION_NOT_FOUND(4201, "充电站不存在"),
    STATION_OFFLINE(4202, "充电站离线"),
    CHARGER_NOT_FOUND(4203, "充电桩不存在"),
    CHARGER_UNAVAILABLE(4204, "充电桩不可用"),
    CHARGER_OCCUPIED(4205, "充电桩被占用"),

    /* 订单相关 */
    ORDER_NOT_FOUND(4301, "订单不存在"),
    ORDER_STATUS_ERROR(4302, "订单状态错误"),
    ORDER_EXPIRED(4303, "订单已过期"),
    ORDER_PAYMENT_FAILED(4304, "订单支付失败"),

    /* 支付相关 */
    PAYMENT_NOT_FOUND(4401, "支付记录不存在"),
    PAYMENT_FAILED(4402, "支付失败"),
    PAYMENT_TIMEOUT(4403, "支付超时"),
    PAYMENT_CANCELLED(4404, "支付已取消"),

    /* 协议相关 */
    PROTOCOL_ERROR(4501, "协议错误"),
    OCPP_CONNECTION_FAILED(4502, "OCPP连接失败"),
    OCPP_MESSAGE_ERROR(4503, "OCPP消息错误"),

    /* 服务器错误 */
    INTERNAL_SERVER_ERROR(500, "内部服务器错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    /* 第三方服务错误 */
    THIRD_PARTY_ERROR(5001, "第三方服务错误"),
    ALIPAY_ERROR(5002, "支付宝服务错误"),
    WECHAT_ERROR(5003, "微信支付服务错误"),
    UNIONPAY_ERROR(5004, "网联支付服务错误");

    private final Integer code;
    private final String message;
}
```

### 4.2 业务错误码扩展

| 错误码 | 常量名 | HTTP状态 | 消息 | 说明 |
|--------|--------|----------|------|------|
| 4001 | TENANT_NOT_FOUND | 400 | 租户不存在 | 租户未找到 |
| 4002 | TENANT_DISABLED | 400 | 租户已禁用 | 租户被禁用 |
| 4003 | TENANT_PERMISSION_DENIED | 400 | 租户权限不足 | 跨租户访问 |
| 4201 | STATION_NOT_FOUND | 404 | 充电站不存在 | 站点未找到 |
| 4203 | CHARGER_NOT_FOUND | 404 | 充电桩不存在 | 充电桩未找到 |
| 4204 | CHARGER_UNAVAILABLE | 503 | 充电桩不可用 | 充电桩离线/故障 |
| 4205 | CHARGER_OCCUPIED | 409 | 充电桩被占用 | 充电桩正在使用 |
| 4301 | ORDER_NOT_FOUND | 404 | 订单不存在 | 订单未找到 |
| 5001 | THIRD_PARTY_ERROR | 500 | 第三方服务错误 | 第三方服务调用失败 |
| 5002 | ALIPAY_ERROR | 500 | 支付宝服务错误 | 支付宝异常 |
| 5003 | WECHAT_ERROR | 500 | 微信支付服务错误 | 微信支付异常 |

---

## 5. 统一响应格式

### 5.1 Result 类（已实现）

```java
// evcs-common/src/main/java/com/evcs/common/result/Result.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        result.setTraceId(MDC.get("traceId"));
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 失败响应（带结果码）
     */
    public static <T> Result<T> failure(ResultCode resultCode) {
        return failure(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> failure(ResultCode resultCode, String message) {
        return failure(resultCode.getCode(), message);
    }

    /**
     * 失败响应（完整自定义）
     */
    public static <T> Result<T> failure(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTraceId(MDC.get("traceId"));
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}
```

### 5.2 响应示例

```json
// 成功响应
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "id": 12345,
        "orderNo": "ORD20260210001"
    },
    "traceId": "abc123def456",
    "timestamp": 1736766468114
}

// 失败响应 - 资源不存在
{
    "code": 4203,
    "message": "充电桩不存在",
    "data": null,
    "traceId": "abc123def456",
    "timestamp": 1736766468114
}

// 失败响应 - 资源冲突
{
    "code": 4205,
    "message": "充电桩被占用",
    "data": null,
    "traceId": "abc123def456",
    "timestamp": 1736766468114
}

// 失败响应 - 第三方服务异常
{
    "code": 5002,
    "message": "支付宝服务调用失败: 网络超时",
    "data": null,
    "traceId": "abc123def456",
    "timestamp": 1736766468114
}
```

---

## 6. 最佳实践

### 6.1 异常使用指南

```java
// ✅ 推荐：使用具体异常类型
public ChargingSession startSession(Long chargerId, Long userId) {
    Charger charger = chargerMapper.selectById(chargerId);
    if (charger == null) {
        throw ResourceNotFoundException.of("充电桩", chargerId);
    }

    if (charger.getStatus() == ChargerStatus.BUSY) {
        throw new ResourceConflictException(
            ResultCode.CHARGER_OCCUPIED,
            "充电桩正在使用中"
        );
    }

    if (!ocppService.isAvailable()) {
        throw ServiceUnavailableException.forService("OCPP协议");
    }

    return createSession(charger, userId);
}

// ✅ 推荐：第三方服务异常处理
public AlipayResponse callAlipay(AlipayRequest request) {
    try {
        return alipayClient.execute(request);
    } catch (AlipayApiException e) {
        throw new ThirdPartyServiceException(
            ResultCode.ALIPAY_ERROR,
            "支付宝",
            e.getMessage(),
            e
        );
    }
}

// ❌ 不推荐：使用通用异常
public void doSomething(Long id) {
    if (id == null) {
        throw new BusinessException("ID不能为空"); // 不够明确
    }
}
```

### 6.2 错误日志规范

```java
// 使用异常处理器时，日志已在处理器中记录
throw new ResourceConflictException("充电桩被占用");
// GlobalExceptionHandler 会记录: log.warn("资源冲突: {}", e.getMessage());

// 对于需要额外上下文的错误，可以在抛出前记录
if (charger.getStatus() == ChargerStatus.BUSY) {
    log.warn("尝试启动繁忙充电桩: chargerId={}, currentStatus={}, existingSession={}",
             chargerId, charger.getStatus(), charger.getActiveSessionId());
    throw new ResourceConflictException(
        ResultCode.CHARGER_OCCUPIED,
        "充电桩正在使用中"
    );
}
```

---

## 7. 相关文档

- [API 设计规范](../architecture/api-design.md)
- [日志规范](LOGGING-STANDARDS.md)
- [项目编码规范](PROJECT-CODING-STANDARDS.md)
- [系统架构风险审计报告](../architecture/RISK-AUDIT-REPORT.md)

---

## 8. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-02-10 | v1.1 | 新增统一异常体系说明、异常类层次结构、ResultCode 枚举定义 |
| 2026-01-13 | v1.0 | 初始版本 |
