package com.evcs.protocol.dto.ocpp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * OCPP CallError消息
 * [4, messageId, errorCode, errorDescription, errorDetails]
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "messageTypeId", "messageId", "errorCode", "errorDescription", "errorDetails" })
public class OCPPCallErrorMessage extends OCPPMessage {

    /**
     * 错误代码
     */
    @JsonProperty(index = 2)
    private OCPPErrorCode errorCode;

    /**
     * 错误描述
     */
    @JsonProperty(index = 3)
    private String errorDescription;

    /**
     * 错误详情
     */
    @JsonProperty(index = 4)
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