package com.evcs.station.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 充电开始事件
 */
@Getter
public class ChargingStartEvent extends ApplicationEvent {
    
    private final Long stationId;
    private final Long chargerId;
    private final String sessionId;
    private final Long userId;
    private final Long billingPlanId;
    private final Long tenantId;
    
    public ChargingStartEvent(Object source, Long stationId, Long chargerId, 
                              String sessionId, Long userId, Long billingPlanId, Long tenantId) {
        super(source);
        this.stationId = stationId;
        this.chargerId = chargerId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.billingPlanId = billingPlanId;
        this.tenantId = tenantId;
    }
}
