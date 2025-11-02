package com.evcs.protocol.dto.ocpp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * OCPP CallResult消息
 * [3, messageId, payload]
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "messageTypeId", "messageId", "payload" })
public class OCPPCallResultMessage extends OCPPMessage {

    /**
     * 响应载荷
     */
    @JsonProperty(index = 2)
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