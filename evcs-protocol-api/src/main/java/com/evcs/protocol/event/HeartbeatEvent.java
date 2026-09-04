package com.evcs.protocol.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 心跳事件
 * 充电桩定期发送心跳，更新最后在线时间
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HeartbeatEvent extends ProtocolEvent {
    private static final long serialVersionUID = 1L;

    /**
     * 最后心跳时间
     */
    private java.time.LocalDateTime lastHeartbeatTime;

    @Override
    public String getRoutingKey() {
        return "protocol.heartbeat." + getProtocolType();
    }
}
