package com.evcs.protocol.dto.ocpp;

import com.evcs.protocol.enums.OCPPMessageType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OCPP消息基类
 * 支持OCPP 1.6规范的消息格式
 */
@Data
public abstract class OCPPMessage {

    /**
     * 消息类型ID (2=Call, 3=CallResult, 4=CallError)
     */
    private Integer messageTypeId;

    /**
     * 消息唯一ID
     */
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