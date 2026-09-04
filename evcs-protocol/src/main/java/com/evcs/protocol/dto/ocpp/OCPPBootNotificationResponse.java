package com.evcs.protocol.dto.ocpp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * OCPP BootNotification响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
class OCPPBootNotificationResponse extends OCPPResponsePayload {

    /**
     * 当前时间
     */
    @JsonProperty("currentTime")
    private LocalDateTime currentTime;

    /**
     * 间隔时间（秒）
     */
    @JsonProperty("interval")
    private Integer interval;

    /**
     * 状态
     */
    @JsonProperty("status")
    private OCPPRegistrationStatus status;

    public OCPPBootNotificationResponse() {
        super();
        this.currentTime = LocalDateTime.now();
    }

    public OCPPBootNotificationResponse(OCPPRegistrationStatus status, Integer interval) {
        this();
        this.status = status;
        this.interval = interval;
    }

    /**
     * 创建成功响应
     */
    public static OCPPBootNotificationResponse success(Integer interval) {
        return new OCPPBootNotificationResponse(OCPPRegistrationStatus.ACCEPTED, interval);
    }

    /**
     * 创建失败响应
     */
    public static OCPPBootNotificationResponse failure(OCPPRegistrationStatus status) {
        return new OCPPBootNotificationResponse(status, null);
    }

    @Override
    public boolean isValid() {
        return status != null && interval != null && interval > 0;
    }
}
