package com.evcs.protocol.dto.ocpp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * OCPP错误代码枚举
 */
public enum OCPPErrorCode {
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