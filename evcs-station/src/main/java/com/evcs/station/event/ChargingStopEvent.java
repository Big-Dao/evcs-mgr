package com.evcs.station.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 充电停止事件
 */
@Getter
public class ChargingStopEvent extends ApplicationEvent {
    
    private final String sessionId;
    private final Double energy;
    private final Long duration;
    private final Long tenantId;
    
    public ChargingStopEvent(Object source, String sessionId, Double energy, Long duration, Long tenantId) {
        super(source);
        this.sessionId = sessionId;
        this.energy = energy;
        this.duration = duration;
        this.tenantId = tenantId;
    }
}
