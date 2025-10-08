package com.evcs.protocol.service;

import com.evcs.protocol.api.IOCPPProtocolService;
import com.evcs.protocol.api.ProtocolEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class OCPPProtocolServiceImpl implements IOCPPProtocolService {
    private volatile ProtocolEventListener listener;

    @Override
    public boolean connect(String chargerCode) {
        log.info("[OCPP] connect chargerCode={}", chargerCode);
        return true;
    }

    @Override
    public void disconnect(String chargerCode) {
        log.info("[OCPP] disconnect chargerCode={}", chargerCode);
    }

    @Override
    public boolean sendHeartbeat(Long chargerId) {
        log.debug("[OCPP] heartbeat chargerId={}", chargerId);
        if (listener != null) listener.onHeartbeat(chargerId, LocalDateTime.now());
        return true;
    }

    @Override
    public boolean updateStatus(Long chargerId, Integer status) {
        log.info("[OCPP] status chargerId={} status={}", chargerId, status);
        if (listener != null) listener.onStatusChange(chargerId, status);
        return true;
    }

    @Override
    public boolean startCharging(Long chargerId, String sessionId, Long userId) {
        log.info("[OCPP] startCharging chargerId={} sessionId={} userId={}", chargerId, sessionId, userId);
        if (listener != null) listener.onStartAck(chargerId, sessionId, true, "OK");
        return true;
    }

    @Override
    public boolean stopCharging(Long chargerId) {
        log.info("[OCPP] stopCharging chargerId={}", chargerId);
        if (listener != null) listener.onStopAck(chargerId, true, "OK");
        return true;
    }

    @Override
    public void setEventListener(ProtocolEventListener listener) {
        this.listener = listener;
    }
}
