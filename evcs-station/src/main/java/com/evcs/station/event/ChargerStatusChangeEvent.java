package com.evcs.station.event;

import com.evcs.station.enums.ChargerStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 充电桩状态变更事件
 */
@Getter
public class ChargerStatusChangeEvent extends ApplicationEvent {

    private final Long chargerId;
    private final ChargerStatus oldStatus;
    private final ChargerStatus newStatus;
    private final Long tenantId;

    public ChargerStatusChangeEvent(Object source, Long chargerId, ChargerStatus oldStatus, ChargerStatus newStatus,
            Long tenantId) {
        super(source);
        this.chargerId = chargerId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.tenantId = tenantId;
    }
}
