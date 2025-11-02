package com.evcs.protocol.dto.ocpp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OCPP请求载荷基类
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class OCPPRequestPayload {

    /**
     * 请求时间戳
     */
    private LocalDateTime timestamp;

    protected OCPPRequestPayload() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 获取动作名称
     */
    public abstract String getAction();

    /**
     * 验证载荷是否有效
     */
    public abstract boolean isValid();

    /**
     * 获取请求描述
     */
    public String getDescription() {
        return String.format("%s[action=%s]", getClass().getSimpleName(), getAction());
    }
}