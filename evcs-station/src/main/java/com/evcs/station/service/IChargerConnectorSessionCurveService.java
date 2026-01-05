package com.evcs.station.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.protocol.event.TelemetryEvent;
import com.evcs.station.entity.ChargerConnectorCurvePoint;
import com.evcs.station.entity.ChargerConnectorSession;
import java.time.LocalDateTime;

/**
 * 枪口会话历史 + 曲线
 */
public interface IChargerConnectorSessionCurveService {

    boolean recordSessionStart(StartEvent event);

    boolean recordSessionStop(StopEvent event);

    boolean recordTelemetry(TelemetryEvent event);

    IPage<ChargerConnectorSession> pageSessions(Long chargerId, Integer connectorNo, Page<ChargerConnectorSession> page);

    IPage<ChargerConnectorCurvePoint> pageCurvePoints(
        Long chargerId,
        Integer connectorNo,
        String sessionId,
        LocalDateTime from,
        LocalDateTime to,
        Page<ChargerConnectorCurvePoint> page
    );
}
