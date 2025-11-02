package com.evcs.protocol.dto.ocpp;

import com.evcs.protocol.enums.OCPPMessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OCPP消息基类
 * 支持OCPP 1.6规范的消息格式
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class OCPPMessage {

    /**
     * 消息类型ID (2=Call, 3=CallResult, 4=CallError)
     */
    @JsonProperty([0])
    private Integer messageTypeId;

    /**
     * 消息唯一ID
     */
    @JsonProperty([1])
    private String messageId;

    /**
     * 消息创建时间
     */
    private LocalDateTime timestamp;

    protected OCPPMessage() {
        this.timestamp = LocalDateTime.now();
    }

    protected OCPPMessage(Integer messageTypeId, String messageId) {
        this();
        this.messageTypeId = messageTypeId;
        this.messageId = messageId;
    }

    /**
     * 获取消息类型
     */
    public OCPPMessageType getMessageType() {
        if (messageTypeId == null) {
            return null;
        }
        return OCPPMessageType.fromTypeId(messageTypeId);
    }

    /**
     * 设置消息类型
     */
    public void setMessageType(OCPPMessageType messageType) {
        if (messageType != null && messageType.getTypeId() != null) {
            this.messageTypeId = messageType.getTypeId();
        }
    }

    /**
     * 验证消息格式
     */
    public boolean isValid() {
        return messageTypeId != null && messageId != null && !messageId.trim().isEmpty();
    }

    /**
     * 获取消息描述
     */
    public String getDescription() {
        return String.format("%s[id=%s, type=%s]",
                getClass().getSimpleName(),
                messageId,
                getMessageType() != null ? getMessageType().getAction() : "Unknown");
    }
}

/**
 * OCPP Call消息
 * [2, messageId, action, payload]
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class OCPPCallMessage extends OCPPMessage {

    /**
     * 动作名称
     */
    @JsonProperty([2])
    private String action;

    /**
     * 消息载荷
     */
    @JsonProperty([3])
    private Map<String, Object> payload;

    public OCPPCallMessage() {
        super(OCPPMessageType.CALL.getTypeId(), null);
    }

    public OCPPCallMessage(String messageId, String action, Map<String, Object> payload) {
        super(OCPPMessageType.CALL.getTypeId(), messageId);
        this.action = action;
        this.payload = payload;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && action != null && !action.trim().isEmpty();
    }

    /**
     * 获取载荷中的指定字段
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayloadField(String key, Class<T> type) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        Object value = payload.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 设置载荷字段
     */
    public void setPayloadField(String key, Object value) {
        if (payload == null) {
            payload = new java.util.HashMap<>();
        }
        payload.put(key, value);
    }
}

/**
 * OCPP CallResult消息
 * [3, messageId, payload]
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class OCPPCallResultMessage extends OCPPMessage {

    /**
     * 响应载荷
     */
    @JsonProperty([2])
    private Map<String, Object> payload;

    public OCPPCallResultMessage() {
        super(OCPPMessageType.CALL_RESULT.getTypeId(), null);
    }

    public OCPPCallResultMessage(String messageId, Map<String, Object> payload) {
        super(OCPPMessageType.CALL_RESULT.getTypeId(), messageId);
        this.payload = payload;
    }

    /**
     * 获取载荷中的指定字段
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayloadField(String key, Class<T> type) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        Object value = payload.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 设置载荷字段
     */
    public void setPayloadField(String key, Object value) {
        if (payload == null) {
            payload = new java.util.HashMap<>();
        }
        payload.put(key, value);
    }
}

/**
 * OCPP CallError消息
 * [4, messageId, errorCode, errorDescription, errorDetails]
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class OCPPCallErrorMessage extends OCPPMessage {

    /**
     * 错误代码
     */
    @JsonProperty([2])
    private OCPPErrorCode errorCode;

    /**
     * 错误描述
     */
    @JsonProperty([3])
    private String errorDescription;

    /**
     * 错误详情
     */
    @JsonProperty([4])
    private Map<String, Object> errorDetails;

    public OCPPCallErrorMessage() {
        super(OCPPMessageType.CALL_ERROR.getTypeId(), null);
    }

    public OCPPCallErrorMessage(String messageId, OCPPErrorCode errorCode,
                               String errorDescription, Map<String, Object> errorDetails) {
        super(OCPPMessageType.CALL_ERROR.getTypeId(), messageId);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorDetails = errorDetails;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && errorCode != null;
    }
}

/**
 * OCPP错误代码枚举
 */
enum OCPPErrorCode {
    NOT_IMPLEMENTED("NotImplemented"),
    NOT_SUPPORTED("NotSupported"),
    INTERNAL_ERROR("InternalError"),
    PROTOCOL_ERROR("ProtocolError"),
    SECURITY_ERROR("SecurityError"),
    FORMATION_VIOLATION("FormationViolation"),
    PROPERTY_CONSTRAINT_VIOLATION("PropertyConstraintViolation"),
    OCCUPANCY_CONSTRAINT_VIOLATION("OccupancyConstraintViolation"),
    TYPE_CONSTRAINT_VIOLATION("TypeConstraintViolation"),
    GENERIC_ERROR("GenericError");

    private final String code;

    OCPPErrorCode(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static OCPPErrorCode fromCode(String code) {
        for (OCPPErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("Unknown OCPP error code: " + code);
    }
}