package com.evcs.protocol.service;

import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.protocol.client.StationServiceClient;
import com.evcs.protocol.dto.ChargerBasicInfo;
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
    private final StationServiceClient stationServiceClient;
    private volatile ProtocolEventListener listener;

    @Override
    public boolean registerStation(String stationCode) {
        log.info("[CloudCharge] registerStation stationCode={}", stationCode);
        return true;
    }

    @Override
    public boolean startCharging(Long chargerId, String sessionId, Long userId) {
        log.info("[CloudCharge] startCharging chargerId={} sessionId={} userId={}", chargerId, sessionId, userId);

        ChargerBasicInfo info = fetchChargerInfoById(chargerId);
        if (isNotResolvable(info)) {
            // 无法归属租户的事件禁止发布：兜底固定租户会把充电事件错误记到别的租户名下
            log.warn("[CloudCharge] startCharging rejected: charger info unresolvable, chargerId={}", chargerId);
            if (listener != null) {
                listener.onStartAck(chargerId, sessionId, false, "Charger info unresolvable");
            }
            return false;
        }

        if (listener != null) {
            listener.onStartAck(chargerId, sessionId, true, "OK");
        }

        try {
            eventPublisher.publishChargingStart(
                info.getStationId(),
                chargerId,
                info.getTenantId(),
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
        return stationServiceClient.getChargerById(chargerId);
    }

    private boolean isNotResolvable(ChargerBasicInfo info) {
        return info == null || info.getTenantId() == null || info.getStationId() == null;
    }

    @Override
    public boolean stopCharging(Long chargerId) {
        log.info("[CloudCharge] stopCharging chargerId={}", chargerId);

        ChargerBasicInfo info = fetchChargerInfoById(chargerId);
        if (info == null || info.getTenantId() == null) {
            log.warn("[CloudCharge] stopCharging rejected: charger info unresolvable, chargerId={}", chargerId);
                        if (listener != null) {
                listener.onStopAck(chargerId, false, "Charger info unresolvable");
            }
            return false;
        }

        // 触发本地监听器
                if (listener != null) {
            listener.onStopAck(chargerId, true, "OK");
        }

        // 发布到RabbitMQ
        try {
            eventPublisher.publishChargingStop(chargerId, info.getTenantId(), "CloudCharge", null, null,
                    0.0, 0L, "Manual stop", true, "OK");
        } catch (Exception e) {
            log.warn("Failed to publish charging stop event to MQ", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean reportHeartbeat(Long chargerId) {
        log.debug("[CloudCharge] heartbeat chargerId={}", chargerId);

        ChargerBasicInfo info = fetchChargerInfoById(chargerId);
        if (info == null || info.getTenantId() == null) {
            log.warn("[CloudCharge] heartbeat dropped: charger info unresolvable, chargerId={}", chargerId);
            return false;
        }
        LocalDateTime now = LocalDateTime.now();

        // 触发本地监听器
                if (listener != null) {
            listener.onHeartbeat(chargerId, now);
        }

        // 发布到RabbitMQ
        try {
            eventPublisher.publishHeartbeat(chargerId, info.getTenantId(), "CloudCharge", now);
        } catch (Exception e) {
            log.warn("Failed to publish heartbeat event to MQ", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean reportStatus(Long chargerId, Integer status) {
        log.info("[CloudCharge] status chargerId={} status={}", chargerId, status);

        ChargerBasicInfo info = fetchChargerInfoById(chargerId);
        if (info == null || info.getTenantId() == null) {
            log.warn("[CloudCharge] status dropped: charger info unresolvable, chargerId={}", chargerId);
            return false;
        }

        // 触发本地监听器
                if (listener != null) {
            listener.onStatusChange(chargerId, status);
        }

        // 发布到RabbitMQ
        try {
            eventPublisher.publishStatusChange(chargerId, info.getTenantId(), "CloudCharge", null, status, "Status updated");
        } catch (Exception e) {
            log.warn("Failed to publish status event to MQ", e);
            return false;
        }

        return true;
    }

    @Override
    public void setEventListener(ProtocolEventListener listener) {
        this.listener = listener;
    }
}
