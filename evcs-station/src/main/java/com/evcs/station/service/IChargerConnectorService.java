package com.evcs.station.service;

import com.evcs.station.entity.Charger;
import com.evcs.station.entity.ChargerConnector;
import java.time.LocalDateTime;
import java.util.List;

public interface IChargerConnectorService {

    List<ChargerConnector> listByChargerId(Long chargerId);

    /**
     * Ensure connector rows exist for charger based on gunCount/gunTypes.
     */
    List<ChargerConnector> ensureConnectors(Long chargerId);

    List<ChargerConnector> ensureConnectors(Charger charger);

    boolean updateStatus(
        Long chargerId,
        Integer connectorNo,
        Integer status,
        String faultCode,
        String faultDescription,
        LocalDateTime heartbeat
    );

    boolean touchAllHeartbeat(Long chargerId, LocalDateTime heartbeat);

    boolean updateSessionStart(
        Long chargerId,
        Integer connectorNo,
        String sessionId,
        Long userId,
        LocalDateTime startTime,
        Double initialEnergy
    );

    boolean updateSessionStop(
        Long chargerId,
        Integer connectorNo,
        String sessionId,
        Double energy,
        Long duration
    );
}
