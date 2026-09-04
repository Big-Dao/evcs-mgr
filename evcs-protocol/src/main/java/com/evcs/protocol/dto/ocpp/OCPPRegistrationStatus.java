package com.evcs.protocol.dto.ocpp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OCPP注册状态枚举
 */
enum OCPPRegistrationStatus {
    @JsonProperty("Accepted")
    ACCEPTED,

    @JsonProperty("Pending")
    PENDING,

    @JsonProperty("Rejected")
    REJECTED
}
