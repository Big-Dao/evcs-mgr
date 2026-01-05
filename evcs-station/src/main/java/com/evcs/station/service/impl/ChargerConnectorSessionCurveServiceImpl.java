package com.evcs.station.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.protocol.event.TelemetryEvent;
import com.evcs.station.entity.ChargerConnectorCurvePoint;
import com.evcs.station.entity.ChargerConnectorSession;
import com.evcs.station.mapper.ChargerConnectorCurvePointMapper;
import com.evcs.station.mapper.ChargerConnectorMapper;
import com.evcs.station.mapper.ChargerConnectorSessionMapper;
import com.evcs.station.service.IChargerConnectorService;
import com.evcs.station.service.IChargerConnectorSessionCurveService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargerConnectorSessionCurveServiceImpl implements IChargerConnectorSessionCurveService {

    private static final int SESSION_STATUS_ACTIVE = 1;
    private static final int SESSION_STATUS_STOPPED = 2;

    private final ChargerConnectorSessionMapper sessionMapper;
    private final ChargerConnectorCurvePointMapper curvePointMapper;
    private final ChargerConnectorMapper connectorMapper;
    private final IChargerConnectorService chargerConnectorService;

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public boolean recordSessionStart(StartEvent event) {
        if (event == null || event.getTenantId() == null || event.getChargerId() == null) {
            return false;
        }
        if (event.getConnectorId() == null || event.getConnectorId() <= 0) {
            return true;
        }
        if (event.getSessionId() == null || event.getSessionId().trim().isEmpty()) {
            return true;
        }

        Integer connectorNo = event.getConnectorId();
        ChargerConnectorSession session = findSession(event.getChargerId(), connectorNo, event.getSessionId());
        if (session == null) {
            session = new ChargerConnectorSession();
            session.setChargerId(event.getChargerId());
            session.setConnectorNo(connectorNo);
            session.setSessionId(event.getSessionId());
        }

        session.setProtocolType(event.getProtocolType());
        session.setStartTime(event.getEventTime() != null ? event.getEventTime() : LocalDateTime.now());
        session.setInitialEnergy(toBigDecimal(event.getInitialEnergy()));
        session.setStatus(SESSION_STATUS_ACTIVE);
        session.setStopTime(null);

        return saveOrUpdateSession(session);
    }

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public boolean recordSessionStop(StopEvent event) {
        if (event == null || event.getTenantId() == null || event.getChargerId() == null) {
            return false;
        }
        if (event.getConnectorId() == null || event.getConnectorId() <= 0) {
            return true;
        }
        if (event.getSessionId() == null || event.getSessionId().trim().isEmpty()) {
            return true;
        }

        Integer connectorNo = event.getConnectorId();
        ChargerConnectorSession session = findSession(event.getChargerId(), connectorNo, event.getSessionId());
        if (session == null) {
            session = new ChargerConnectorSession();
            session.setChargerId(event.getChargerId());
            session.setConnectorNo(connectorNo);
            session.setSessionId(event.getSessionId());
            session.setProtocolType(event.getProtocolType());
        }

        session.setStopTime(event.getEventTime() != null ? event.getEventTime() : LocalDateTime.now());
        if (event.getDuration() != null && event.getDuration() >= 0) {
            session.setDurationSeconds(event.getDuration() * 60);
        }
        session.setTotalEnergy(toBigDecimal(event.getEnergy()));
        session.setStatus(SESSION_STATUS_STOPPED);

        return saveOrUpdateSession(session);
    }

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public boolean recordTelemetry(TelemetryEvent event) {
        if (event == null || event.getTenantId() == null || event.getChargerId() == null) {
            return false;
        }
        if (event.getConnectorId() == null || event.getConnectorId() <= 0) {
            return true;
        }

        Integer connectorNo = event.getConnectorId();
        String sessionId = event.getSessionId() != null ? event.getSessionId().trim() : null;
        if (sessionId != null && sessionId.isEmpty()) {
            sessionId = null;
        }

        // Best-effort ensure connector row exists before snapshot update.
        try {
            chargerConnectorService.ensureConnectors(event.getChargerId());
        } catch (Exception e) {
            log.debug("ensureConnectors failed in telemetry ingest: chargerId={}", event.getChargerId(), e);
        }

        // Update connector snapshot (only when current_session_id matches, to avoid stale overwrites).
        connectorMapper.updateTelemetrySnapshot(
            event.getChargerId(),
            connectorNo,
            sessionId,
            event.getSampleTime(),
            toBigDecimal(event.getVoltage()),
            toBigDecimal(event.getCurrent()),
            toBigDecimal(event.getPower()),
            toBigDecimal(event.getSoc()),
            toBigDecimal(event.getEnergy())
        );

        if (sessionId == null || event.getSampleTime() == null) {
            return true;
        }

        // Curve point insert (ignore duplicates on unique index).
        ChargerConnectorCurvePoint point = new ChargerConnectorCurvePoint();
        point.setChargerId(event.getChargerId());
        point.setConnectorNo(connectorNo);
        point.setSessionId(sessionId);
        point.setSampleTime(event.getSampleTime());
        point.setVoltage(toBigDecimal(event.getVoltage()));
        point.setCurrentA(toBigDecimal(event.getCurrent()));
        point.setPower(toBigDecimal(event.getPower()));
        point.setSoc(toBigDecimal(event.getSoc()));
        point.setEnergy(toBigDecimal(event.getEnergy()));
        point.setDurationSeconds(event.getDurationSeconds());

        try {
            curvePointMapper.insert(point);
        } catch (DuplicateKeyException ex) {
            // idempotency: same (tenant, charger, connector, session, sample_time)
        }

        // Touch session aggregates for browsing.
        ChargerConnectorSession session = findSession(event.getChargerId(), connectorNo, sessionId);
        if (session == null) {
            session = new ChargerConnectorSession();
            session.setChargerId(event.getChargerId());
            session.setConnectorNo(connectorNo);
            session.setSessionId(sessionId);
            session.setProtocolType(event.getProtocolType());
            session.setStartTime(null);
            session.setStatus(SESSION_STATUS_ACTIVE);
        }

        session.setLastSampleTime(event.getSampleTime());
        session.setLastVoltage(toBigDecimal(event.getVoltage()));
        session.setLastCurrent(toBigDecimal(event.getCurrent()));
        session.setLastPower(toBigDecimal(event.getPower()));
        session.setLastSoc(toBigDecimal(event.getSoc()));
        session.setLastEnergy(toBigDecimal(event.getEnergy()));
        if (event.getDurationSeconds() != null && event.getDurationSeconds() >= 0) {
            session.setDurationSeconds(event.getDurationSeconds());
        }
        if (event.getEnergy() != null) {
            session.setTotalEnergy(toBigDecimal(event.getEnergy()));
        }

        return saveOrUpdateSession(session);
    }

    @Override
    @DataScope
    public IPage<ChargerConnectorSession> pageSessions(Long chargerId, Integer connectorNo, Page<ChargerConnectorSession> page) {
        if (chargerId == null || connectorNo == null) {
            return Page.of(page.getCurrent(), page.getSize());
        }

        LambdaQueryWrapper<ChargerConnectorSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChargerConnectorSession::getChargerId, chargerId);
        wrapper.eq(ChargerConnectorSession::getConnectorNo, connectorNo);
        wrapper.orderByDesc(ChargerConnectorSession::getStartTime);
        return sessionMapper.selectPage(page, wrapper);
    }

    @Override
    @DataScope
    public IPage<ChargerConnectorCurvePoint> pageCurvePoints(
        Long chargerId,
        Integer connectorNo,
        String sessionId,
        LocalDateTime from,
        LocalDateTime to,
        Page<ChargerConnectorCurvePoint> page
    ) {
        if (chargerId == null || connectorNo == null || sessionId == null || sessionId.trim().isEmpty()) {
            return Page.of(page.getCurrent(), page.getSize());
        }

        String normalizedSessionId = sessionId.trim();

        LambdaQueryWrapper<ChargerConnectorCurvePoint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChargerConnectorCurvePoint::getChargerId, chargerId);
        wrapper.eq(ChargerConnectorCurvePoint::getConnectorNo, connectorNo);
        wrapper.eq(ChargerConnectorCurvePoint::getSessionId, normalizedSessionId);
        if (from != null) {
            wrapper.ge(ChargerConnectorCurvePoint::getSampleTime, from);
        }
        if (to != null) {
            wrapper.le(ChargerConnectorCurvePoint::getSampleTime, to);
        }
        wrapper.orderByAsc(ChargerConnectorCurvePoint::getSampleTime);
        return curvePointMapper.selectPage(page, wrapper);
    }

    private ChargerConnectorSession findSession(Long chargerId, Integer connectorNo, String sessionId) {
        if (chargerId == null || connectorNo == null || sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<ChargerConnectorSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChargerConnectorSession::getChargerId, chargerId);
        wrapper.eq(ChargerConnectorSession::getConnectorNo, connectorNo);
        wrapper.eq(ChargerConnectorSession::getSessionId, sessionId.trim());
        wrapper.last("LIMIT 1");
        return sessionMapper.selectOne(wrapper);
    }

    private boolean saveOrUpdateSession(ChargerConnectorSession session) {
        if (session == null) {
            return false;
        }
        if (session.getId() == null) {
            try {
                return sessionMapper.insert(session) > 0;
            } catch (DuplicateKeyException ex) {
                ChargerConnectorSession existing = findSession(session.getChargerId(), session.getConnectorNo(), session.getSessionId());
                if (existing == null || existing.getId() == null) {
                    return false;
                }
                session.setId(existing.getId());
                return sessionMapper.updateById(session) > 0;
            }
        }
        return sessionMapper.updateById(session) > 0;
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
