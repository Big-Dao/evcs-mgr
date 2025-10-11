package com.evcs.protocol.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 协议事件基类
 * 所有协议事件都继承此类
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class ProtocolEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 事件ID（唯一标识）
     */
    private String eventId;

    /**
     * 充电桩ID
     */
    private Long chargerId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

    /**
     * 协议类型（OCPP/CloudCharge）
     */
    private String protocolType;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        HEARTBEAT,      // 心跳
        STATUS_CHANGE,  // 状态变更
        CHARGING_START, // 开始充电
        CHARGING_STOP,  // 停止充电
        ERROR           // 错误
    }

    /**
     * 获取路由键
     * 用于RabbitMQ消息路由
     */
    public abstract String getRoutingKey();
}
