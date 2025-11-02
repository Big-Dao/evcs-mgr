package com.evcs.protocol.dto;

import com.evcs.protocol.enums.ProtocolType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一协议请求对象
 */
@Data
public class ProtocolRequest {
    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 协议类型
     */
    private ProtocolType protocolType;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 充电桩ID
     */
    private Long chargerId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 请求时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 请求数据
     */
    private Map<String, Object> data;

    /**
     * 签名（用于云快充协议）
     */
    private String signature;

    /**
     * API版本（用于云快充协议）
     */
    private String apiVersion;

    /**
     * 消息ID（用于OCPP协议）
     */
    private String messageId;

    public ProtocolRequest() {
        this.timestamp = LocalDateTime.now();
        this.requestId = java.util.UUID.randomUUID().toString();
    }

    public ProtocolRequest(ProtocolType protocolType, String deviceCode, String action) {
        this();
        this.protocolType = protocolType;
        this.deviceCode = deviceCode;
        this.action = action;
    }

    /**
     * 是否为OCPP协议请求
     */
    public boolean isOCPP() {
        return ProtocolType.OCPP.equals(protocolType);
    }

    /**
     * 是否为云快充协议请求
     */
    public boolean isCloudCharge() {
        return ProtocolType.CLOUD_CHARGE.equals(protocolType);
    }

    /**
     * 获取请求数据中的指定字段
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        if (data == null || !data.containsKey(key)) {
            return null;
        }
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 设置请求数据
     */
    public void setData(String key, Object value) {
        if (data == null) {
            data = new java.util.HashMap<>();
        }
        data.put(key, value);
    }
}