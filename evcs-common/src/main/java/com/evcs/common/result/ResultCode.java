package com.evcs.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 */
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