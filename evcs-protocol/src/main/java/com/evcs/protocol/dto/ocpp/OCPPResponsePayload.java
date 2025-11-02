package com.evcs.protocol.dto.ocpp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OCPP响应载荷基类
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class OCPPResponsePayload {

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    protected OCPPResponsePayload() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 验证响应是否有效
     */
    public abstract boolean isValid();

    /**
     * 获取响应描述
     */
    public String getDescription() {
        return String.format("%s[valid=%s]", getClass().getSimpleName(), isValid());
    }
}