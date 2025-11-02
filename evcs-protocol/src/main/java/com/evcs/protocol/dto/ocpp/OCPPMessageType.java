package com.evcs.protocol.dto.ocpp;

/**
 * OCPP消息类型枚举
 * 定义OCPP协议中的消息类型
 */
public enum OCPPMessageType {

    /**
     * 调用消息
     */
    CALL(2),

    /**
     * 调用结果消息
     */
    CALL_RESULT(3),

    /**
     * 调用错误消息
     */
    CALL_ERROR(4);

    private final int typeId;

    OCPPMessageType(int typeId) {
        this.typeId = typeId;
    }

    /**
     * 获取消息类型ID
     */
    public int getTypeId() {
        return typeId;
    }

    /**
     * 根据类型ID获取消息类型
     */
    public static OCPPMessageType fromTypeId(int typeId) {
        for (OCPPMessageType type : values()) {
            if (type.typeId == typeId) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type ID: " + typeId);
    }
}