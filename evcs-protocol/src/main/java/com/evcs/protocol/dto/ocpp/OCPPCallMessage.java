package com.evcs.protocol.dto.ocpp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * OCPP Call消息
 * [2, messageId, action, payload]
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "messageTypeId", "messageId", "action", "payload" })
public class OCPPCallMessage extends OCPPMessage {

    /**
     * 动作名称
     */
    @JsonProperty(index = 2)
    private String action;

    /**
     * 消息载荷
     */
    @JsonProperty(index = 3)
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