package com.evcs.protocol.enums;

/**
 * 协议类型枚举
 */
public enum ProtocolType {
    OCPP("OCPP", "OCPP 1.6 Protocol", "WebSocket"),
    CLOUD_CHARGE("CLOUD_CHARGE", "Cloud Charge HTTP API", "HTTP REST");

    private final String code;
    private final String description;
    private final String transport;

    ProtocolType(String code, String description, String transport) {
        this.code = code;
        this.description = description;
        this.transport = transport;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getTransport() {
        return transport;
    }

    public static ProtocolType fromCode(String code) {
        for (ProtocolType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown protocol type: " + code);
    }
}