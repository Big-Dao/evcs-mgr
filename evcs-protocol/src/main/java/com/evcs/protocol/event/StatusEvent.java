package com.evcs.protocol.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 状态变更事件
 * 充电桩状态发生变化时触发
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StatusEvent extends ProtocolEvent {
    private static final long serialVersionUID = 1L;

    /**
     * 旧状态
     */
    private Integer oldStatus;

    /**
     * 新状态
     * 0-离线，1-空闲，2-充电中，3-故障
     */
    private Integer newStatus;

    /**
     * 状态描述
     */
    private String statusDesc;

    @Override
    public String getRoutingKey() {
        return "protocol.status." + getProtocolType();
    }
}
