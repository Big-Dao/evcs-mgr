package com.evcs.protocol.service;

import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.common.result.Result;
import com.evcs.protocol.dto.ChargerBasicInfo;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudChargeProtocolServiceImpl implements ICloudChargeProtocolService {
    private final ProtocolEventPublisher eventPublisher;
    private volatile ProtocolEventListener listener;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Override
    public boolean registerStation(String stationCode) {
        log.info("[CloudCharge] registerStation stationCode={}", stationCode);
        return true;
    }

    @Override
    public boolean startCharging(Long chargerId, String sessionId, Long userId) {
        log.info("[CloudCharge] startCharging chargerId={} sessionId={} userId={}", chargerId, sessionId, userId);
        
        Long stationId = null;
        Long tenantId = 1L;
        try {
            ChargerBasicInfo info = fetchChargerInfoById(chargerId);
            if (info != null) {
                stationId = info.getStationId();
                if (info.getTenantId() != null) {
                    tenantId = info.getTenantId();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve charger info before publishing start event, chargerId={}", chargerId, e);
        }

        if (stationId == null) {
            // 强制发布端必须带 stationId：无法补齐则视为失败，不发布
            if (listener != null) {
                listener.onStartAck(chargerId, sessionId, false, "Missing stationId");
            }
            return false;
        }

        if (listener != null) {
            listener.onStartAck(chargerId, sessionId, true, "OK");
        }

        try {
            eventPublisher.publishChargingStart(
                stationId,
                chargerId,
                tenantId,
                "CloudCharge",
                sessionId,
                userId,
                null,
                null,
                0.0,
                true,
                "Charging started successfully"
            );
        } catch (Exception e) {
            log.warn("Failed to publish charging start event to MQ", e);
            return false;
        }

        return true;
    }

    private ChargerBasicInfo fetchChargerInfoById(Long chargerId) {
        if (chargerId == null) {
            return null;
        }
        if (restTemplate == null) {
            return null;
        }
        try {
            String url = "http://evcs-station/charger/" + chargerId;
            ParameterizedTypeReference<Result<ChargerBasicInfo>> typeRef = new ParameterizedTypeReference<>() {};
            RequestEntity<Void> requestEntity = RequestEntity.get(requiredUri(url)).build();
            ResponseEntity<Result<ChargerBasicInfo>> response = restTemplate.exchange(requestEntity, typeRef);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }

            Result<ChargerBasicInfo> result = response.getBody();
            if (result == null) {
                return null;
            }

            Integer code = result.getCode();
            if (code != null && code == 200) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch charger info by id from station service, chargerId={}", chargerId, e);
        }
        return null;
    }

    @NonNull
    private static URI requiredUri(String url) {
        URI uri = URI.create(url);
        if (uri == null) {
            throw new IllegalStateException("URI.create returned null");
        }
        return uri;
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
