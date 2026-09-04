package com.evcs.protocol.dto;

import com.evcs.protocol.enums.ProtocolType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一协议响应对象
 */
@Data
public class ProtocolResponse {
    /**
     * 响应码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 协议类型
     */
    private ProtocolType protocolType;

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 响应数据
     */
    private Object data;

    /**
     * 扩展信息
     */
    private Map<String, Object> extra;

    /**
     * OCPP消息ID
     */
    private String messageId;

    /**
     * 云快充API版本
     */
    private String apiVersion;

    public ProtocolResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ProtocolResponse(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
        this.code = success ? "200" : "500";
    }

    /**
     * 创建成功响应
     */
    public static ProtocolResponse success() {
        return new ProtocolResponse(true, "Success");
    }

    /**
     * 创建成功响应（带数据）
     */
    public static ProtocolResponse success(Object data) {
        ProtocolResponse response = new ProtocolResponse(true, "Success");
        response.setData(data);
        return response;
    }

    /**
     * 创建失败响应
     */
    public static ProtocolResponse failure(String message) {
        return new ProtocolResponse(false, message);
    }

    /**
     * 创建失败响应（带错误码）
     */
    public static ProtocolResponse failure(String code, String message) {
        ProtocolResponse response = new ProtocolResponse(false, message);
        response.setCode(code);
        return response;
    }

    /**
     * 设置响应数据
     */
    public void setData(Object data) {
        this.data = data;
    }

    /**
     * 设置扩展信息
     */
    public void setExtra(String key, Object value) {
        if (extra == null) {
            extra = new java.util.HashMap<>();
        }
        extra.put(key, value);
    }

    /**
     * 获取扩展信息
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key, Class<T> type) {
        if (extra == null || !extra.containsKey(key)) {
            return null;
        }
        Object value = extra.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
}