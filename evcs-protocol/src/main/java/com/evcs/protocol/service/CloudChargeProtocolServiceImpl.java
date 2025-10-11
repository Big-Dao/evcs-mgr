package com.evcs.protocol.service;

import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudChargeProtocolServiceImpl implements ICloudChargeProtocolService {
    private final ProtocolEventPublisher eventPublisher;
    private volatile ProtocolEventListener listener;

    @Override
    public boolean registerStation(String stationCode) {
        log.info("[CloudCharge] registerStation stationCode={}", stationCode);
        return true;
    }

    @Override
    public boolean startCharging(Long chargerId, String sessionId, Long userId) {
        log.info("[CloudCharge] startCharging chargerId={} sessionId={} userId={}", chargerId, sessionId, userId);
        
        // 触发本地监听器
        if (listener != null) listener.onStartAck(chargerId, sessionId, true, "OK");
        
        // 发布到RabbitMQ
        try {
            eventPublisher.publishChargingStart(chargerId, 1L, "CloudCharge", sessionId, userId,
                    null, 0.0, true, "OK");
        } catch (Exception e) {
            log.warn("Failed to publish charging start event to MQ", e);
        }
        
        return true;
    }

    @Override
    public boolean stopCharging(Long chargerId) {
        log.info("[CloudCharge] stopCharging chargerId={}", chargerId);
        
        // 触发本地监听器
        if (listener != null) listener.onStopAck(chargerId, true, "OK");
        
        // 发布到RabbitMQ
        try {
            eventPublisher.publishChargingStop(chargerId, 1L, "CloudCharge", null, null,
                    0.0, 0L, "Manual stop", true, "OK");
        } catch (Exception e) {
            log.warn("Failed to publish charging stop event to MQ", e);
        }
        
        return true;
    }

    @Override
    public boolean reportHeartbeat(Long chargerId) {
        log.debug("[CloudCharge] heartbeat chargerId={}", chargerId);
        LocalDateTime now = LocalDateTime.now();
        
        // 触发本地监听器
        if (listener != null) listener.onHeartbeat(chargerId, now);
        
        // 发布到RabbitMQ
        try {
            eventPublisher.publishHeartbeat(chargerId, 1L, "CloudCharge", now);
        } catch (Exception e) {
            log.warn("Failed to publish heartbeat event to MQ", e);
        }
        
        return true;
    }

    @Override
    public boolean reportStatus(Long chargerId, Integer status) {
        log.info("[CloudCharge] status chargerId={} status={}", chargerId, status);
        
        // 触发本地监听器
        if (listener != null) listener.onStatusChange(chargerId, status);
        
        // 发布到RabbitMQ
        try {
            eventPublisher.publishStatusChange(chargerId, 1L, "CloudCharge", null, status, "Status updated");
        } catch (Exception e) {
            log.warn("Failed to publish status event to MQ", e);
        }
        
        return true;
    }

    @Override
    public void setEventListener(ProtocolEventListener listener) {
        this.listener = listener;
    }
}
