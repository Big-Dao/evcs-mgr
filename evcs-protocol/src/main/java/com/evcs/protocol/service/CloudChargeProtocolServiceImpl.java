package com.evcs.protocol.service;

import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.ProtocolEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CloudChargeProtocolServiceImpl implements ICloudChargeProtocolService {
    private volatile ProtocolEventListener listener;

    @Override
    public boolean registerStation(String stationCode) {
        log.info("[CloudCharge] registerStation stationCode={}", stationCode);
        return true;
    }

    @Override
    public boolean startCharging(Long chargerId, String sessionId, Long userId) {
        log.info("[CloudCharge] startCharging chargerId={} sessionId={} userId={}", chargerId, sessionId, userId);
        if (listener != null) listener.onStartAck(chargerId, sessionId, true, "OK");
        return true;
    }

    @Override
    public boolean stopCharging(Long chargerId) {
        log.info("[CloudCharge] stopCharging chargerId={}", chargerId);
        if (listener != null) listener.onStopAck(chargerId, true, "OK");
        return true;
    }

    @Override
    public boolean reportHeartbeat(Long chargerId) {
        log.debug("[CloudCharge] heartbeat chargerId={}", chargerId);
        if (listener != null) listener.onHeartbeat(chargerId, LocalDateTime.now());
        return true;
    }

    @Override
    public boolean reportStatus(Long chargerId, Integer status) {
        log.info("[CloudCharge] status chargerId={} status={}", chargerId, status);
        if (listener != null) listener.onStatusChange(chargerId, status);
        return true;
    }

    @Override
    public void setEventListener(ProtocolEventListener listener) {
        this.listener = listener;
    }
}
