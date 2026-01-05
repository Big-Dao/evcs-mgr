package com.evcs.station.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.tenant.TenantContext;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.ChargerConnector;
import com.evcs.station.mapper.ChargerConnectorMapper;
import com.evcs.station.service.IChargerConnectorService;
import com.evcs.station.service.IChargerService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargerConnectorServiceImpl implements IChargerConnectorService {

    private final ChargerConnectorMapper connectorMapper;
    private final IChargerService chargerService;

    @Override
    @DataScope
    public IPage<ChargerConnector> queryPage(Page<ChargerConnector> page, ChargerConnector queryParam) {
        return connectorMapper.selectPageList(page, queryParam);
    }

    @Override
    @DataScope
    public List<ChargerConnector> listByChargerId(Long chargerId) {
        if (chargerId == null) {
            return List.of();
        }
        return connectorMapper.selectByChargerId(chargerId);
    }

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public List<ChargerConnector> ensureConnectors(Long chargerId) {
        if (chargerId == null) {
            return List.of();
        }
        Charger charger = chargerService.getById(chargerId);
        if (charger == null) {
            return List.of();
        }
        return ensureConnectors(charger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChargerConnector> ensureConnectors(Charger charger) {
        if (charger == null || charger.getId() == null) {
            return List.of();
        }

        int gunCount = charger.getGunCount() != null && charger.getGunCount() > 0 ? charger.getGunCount() : 1;
        List<String> gunTypes = parseGunTypes(charger.getGunTypes());

        for (int i = 1; i <= gunCount; i++) {
            Integer connectorNo = i;

            ChargerConnector existing = connectorMapper.selectByChargerIdAndConnectorNo(charger.getId(), connectorNo);
            if (existing != null) {
                // Optionally backfill connector_type if missing
                String expectedType = connectorTypeForIndex(gunTypes, i);
                if (existing.getConnectorType() == null && expectedType != null) {
                    ChargerConnector update = new ChargerConnector();
                    update.setId(existing.getId());
                    update.setConnectorType(expectedType);
                    update.setUpdateTime(LocalDateTime.now());
                    update.setUpdateBy(TenantContext.getCurrentUserId());
                    connectorMapper.updateById(update);
                }
                continue;
            }

            ChargerConnector connector = new ChargerConnector();
            connector.setChargerId(charger.getId());
            connector.setConnectorNo(connectorNo);
            connector.setConnectorType(connectorTypeForIndex(gunTypes, i));
            connector.setStatus(charger.getStatus() != null ? charger.getStatus() : 0);
            connector.setLastHeartbeat(charger.getLastHeartbeat());

            // BaseEntity meta fields (tenantId/createTime/...) are filled by MyBatis handler.
            connectorMapper.insert(connector);
        }

        return connectorMapper.selectByChargerId(charger.getId());
    }

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(
        Long chargerId,
        Integer connectorNo,
        Integer status,
        String faultCode,
        String faultDescription,
        LocalDateTime heartbeat
    ) {
        if (chargerId == null || connectorNo == null || status == null) {
            return false;
        }

        // Ensure the row exists (idempotent upsert-by-select-then-insert)
        ChargerConnector existing = connectorMapper.selectByChargerIdAndConnectorNo(chargerId, connectorNo);
        if (existing == null) {
            Charger charger = chargerService.getById(chargerId);
            if (charger != null) {
                ensureConnectors(charger);
            } else {
                // Fallback: insert a minimal connector row
                ChargerConnector connector = new ChargerConnector();
                connector.setChargerId(chargerId);
                connector.setConnectorNo(connectorNo);
                connector.setStatus(status);
                connector.setFaultCode(faultCode);
                connector.setFaultDescription(faultDescription);
                connector.setLastHeartbeat(heartbeat);
                connectorMapper.insert(connector);
                return true;
            }
        }

        int updated = connectorMapper.updateStatus(
            chargerId,
            connectorNo,
            status,
            faultCode,
            faultDescription,
            heartbeat
        );

        boolean ok = updated > 0;
        if (ok) {
            // 同步刷新充电桩聚合状态：charger.status 是枪口状态的聚合视图
            try {
                syncChargerAggregateStatus(chargerId);
            } catch (Exception e) {
                // 不影响主流程（MQ 消费幂等），聚合失败可在下次事件/查询时修复
                log.debug("Failed to sync charger aggregate status: chargerId={}", chargerId, e);
            }
        }
        return ok;
    }

    @Override
    @DataScope
    public boolean touchAllHeartbeat(Long chargerId, LocalDateTime heartbeat) {
        if (chargerId == null || heartbeat == null) {
            return false;
        }
        return connectorMapper.touchAllHeartbeat(chargerId, heartbeat) > 0;
    }

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSessionStart(
        Long chargerId,
        Integer connectorNo,
        String sessionId,
        Long userId,
        LocalDateTime startTime,
        Double initialEnergy
    ) {
        if (chargerId == null || connectorNo == null) {
            return false;
        }

        String normalizedSessionId = sessionId == null ? null : sessionId.trim();
        if (normalizedSessionId != null && normalizedSessionId.isEmpty()) {
            normalizedSessionId = null;
        }

        // Ensure the row exists (idempotent)
        ChargerConnector existing = connectorMapper.selectByChargerIdAndConnectorNo(chargerId, connectorNo);
        if (existing == null) {
            Charger charger = chargerService.getById(chargerId);
            if (charger != null) {
                ensureConnectors(charger);
            }
        }

        BigDecimal energy = initialEnergy == null ? BigDecimal.ZERO : BigDecimal.valueOf(initialEnergy);
        LocalDateTime effectiveStartTime = startTime != null ? startTime : LocalDateTime.now();

        int updated = connectorMapper.updateSessionStart(
            chargerId,
            connectorNo,
            normalizedSessionId,
            userId,
            effectiveStartTime,
            energy,
            0
        );
        boolean ok = updated > 0;
        if (ok) {
            try {
                syncChargerAggregateStatus(chargerId);
            } catch (Exception e) {
                log.debug("Failed to sync charger aggregate status after session start: chargerId={}", chargerId, e);
            }
        }
        return ok;
    }

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSessionStop(
        Long chargerId,
        Integer connectorNo,
        String sessionId,
        Double energy,
        Long duration
    ) {
        if (chargerId == null || connectorNo == null) {
            return false;
        }

        BigDecimal energyValue = energy == null ? null : BigDecimal.valueOf(energy);
        Integer durationValue = duration == null ? null : Math.toIntExact(duration);

        int updated = 0;
        String normalizedSessionId = sessionId == null ? null : sessionId.trim();
        if (normalizedSessionId != null && normalizedSessionId.isEmpty()) {
            normalizedSessionId = null;
        }

        if (normalizedSessionId != null) {
            updated = connectorMapper.updateSessionStopBySessionId(
                chargerId,
                connectorNo,
                normalizedSessionId,
                energyValue,
                durationValue
            );
        }

        if (updated <= 0) {
            updated = connectorMapper.updateSessionStopFallback(
                chargerId,
                connectorNo,
                energyValue,
                durationValue
            );
        }

        boolean ok = updated > 0;
        if (ok) {
            try {
                syncChargerAggregateStatus(chargerId);
            } catch (Exception e) {
                log.debug("Failed to sync charger aggregate status after session stop: chargerId={}", chargerId, e);
            }
        }
        return ok;
    }

    private void syncChargerAggregateStatus(Long chargerId) {
        if (chargerId == null) {
            return;
        }
        List<ChargerConnector> connectors = connectorMapper.selectByChargerId(chargerId);
        if (connectors == null || connectors.isEmpty()) {
            return;
        }

        int aggregateStatus = aggregateStatusFromConnectors(connectors);
        // 统一通过 chargerService.updateStatus 走指标/日志逻辑
        chargerService.updateStatus(chargerId, aggregateStatus);
    }

    private static int aggregateStatusFromConnectors(List<ChargerConnector> connectors) {
        boolean hasFault = false;
        boolean hasCharging = false;
        boolean hasMaintenance = false;
        boolean hasReserved = false;
        boolean hasIdle = false;
        boolean allOffline = true;

        for (ChargerConnector c : connectors) {
            if (c == null) {
                continue;
            }
            Integer s = c.getStatus();
            int status = s == null ? 0 : s;

            if (status != 0) {
                allOffline = false;
            }

            if (status == 3) {
                hasFault = true;
            } else if (status == 2) {
                hasCharging = true;
            } else if (status == 4) {
                hasMaintenance = true;
            } else if (status == 5) {
                hasReserved = true;
            } else if (status == 1) {
                hasIdle = true;
            }
        }

        // 优先级：故障 > 充电中 > 维护 > 预约 > 空闲 > 离线
        if (hasFault) {
            return 3;
        }
        if (hasCharging) {
            return 2;
        }
        if (hasMaintenance) {
            return 4;
        }
        if (hasReserved) {
            return 5;
        }
        if (hasIdle) {
            return 1;
        }
        if (allOffline) {
            return 0;
        }
        return 0;
    }

    private static List<String> parseGunTypes(String gunTypes) {
        if (gunTypes == null || gunTypes.trim().isEmpty()) {
            return List.of();
        }
        // Supports formats like: "CCS,CHAdeMO" or "[\"CCS\",\"CHAdeMO\"]"
        String normalized = gunTypes.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replace("\"", "");

        String[] parts = normalized.split("[,;]");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String v = part == null ? null : part.trim();
            if (v == null || v.isEmpty()) {
                continue;
            }
            out.add(v);
        }
        return out;
    }

    private static String connectorTypeForIndex(List<String> gunTypes, int index1Based) {
        if (gunTypes == null || gunTypes.isEmpty()) {
            return null;
        }
        int idx = index1Based - 1;
        if (idx < 0) {
            return null;
        }
        if (idx < gunTypes.size()) {
            return normalizeType(gunTypes.get(idx));
        }
        // If fewer types provided than gunCount, repeat the last known type.
        return normalizeType(gunTypes.get(gunTypes.size() - 1));
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        String t = type.trim();
        return t.isEmpty() ? null : t;
    }
}
