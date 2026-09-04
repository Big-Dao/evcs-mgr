package com.evcs.protocol.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 停止充电事件
 * 充电结束时触发
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StopEvent extends ProtocolEvent {
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 充电量（kWh）
     */
    private Double energy;

    /**
     * 充电时长（分钟）
     */
    private Long duration;

    /**
     * 结束原因
     */
    private String reason;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    @Override
    public String getRoutingKey() {
        return "protocol.charging.stop";
    }
}
