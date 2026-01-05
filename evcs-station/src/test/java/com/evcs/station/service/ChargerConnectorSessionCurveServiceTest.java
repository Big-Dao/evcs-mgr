package com.evcs.station.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.protocol.event.ProtocolEvent;
import com.evcs.protocol.event.TelemetryEvent;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.ChargerConnectorCurvePoint;
import com.evcs.station.entity.ChargerConnectorSession;
import com.evcs.station.entity.Station;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class, com.evcs.station.config.TestConfig.class})
@DisplayName("枪口会话历史/曲线 Service 测试")
@SuppressWarnings("null")
class ChargerConnectorSessionCurveServiceTest extends BaseServiceTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    @Resource
    private IChargerConnectorSessionCurveService sessionCurveService;

    @Test
    @DisplayName("会话+曲线 - 应可按会话分页查询")
    void testSessionAndCurve_shouldBeQueryableBySession() {
        // Arrange
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("测试站");
        station.setAddress("地址");
        station.setStatus(1);
        stationService.saveStation(station);

        Charger charger = new Charger();
        charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger.setChargerName("测试桩");
        charger.setStationId(station.getStationId());
        charger.setChargerType(1);
        charger.setStatus(1);
        charger.setEnabled(1);
        charger.setGunCount(1);
        chargerService.saveCharger(charger);

        Long chargerId = charger.getId();
        Integer connectorNo = 1;
        String sessionId = "OCPP_TXN_123";

        StartEvent start = StartEvent.builder()
            .eventId("e1")
            .tenantId(getTestTenantId())
            .chargerId(chargerId)
            .connectorId(connectorNo)
            .eventType(ProtocolEvent.EventType.CHARGING_START)
            .eventTime(LocalDateTime.now())
            .protocolType("OCPP")
            .stationId(station.getStationId())
            .sessionId(sessionId)
            .initialEnergy(0.0)
            .success(false)
            .message("test")
            .build();

        TelemetryEvent t1 = TelemetryEvent.builder()
            .eventId("t1")
            .tenantId(getTestTenantId())
            .chargerId(chargerId)
            .connectorId(connectorNo)
            .eventType(ProtocolEvent.EventType.TELEMETRY)
            .eventTime(LocalDateTime.now())
            .protocolType("OCPP")
            .sessionId(sessionId)
            .transactionId(123)
            .sampleTime(LocalDateTime.now().plusSeconds(5))
            .voltage(230.0)
            .current(10.0)
            .power(2.3)
            .soc(50.0)
            .energy(1.234)
            .durationSeconds(5L)
            .build();

        StopEvent stop = StopEvent.builder()
            .eventId("e2")
            .tenantId(getTestTenantId())
            .chargerId(chargerId)
            .connectorId(connectorNo)
            .eventType(ProtocolEvent.EventType.CHARGING_STOP)
            .eventTime(LocalDateTime.now().plusMinutes(1))
            .protocolType("OCPP")
            .sessionId(sessionId)
            .energy(2.345)
            .duration(1L)
            .reason("StopTransaction")
            .success(false)
            .message("test")
            .build();

        // Act
        assertTrue(sessionCurveService.recordSessionStart(start));
        assertTrue(sessionCurveService.recordTelemetry(t1));
        assertTrue(sessionCurveService.recordSessionStop(stop));

        // Assert - sessions
        IPage<ChargerConnectorSession> sessions = sessionCurveService.pageSessions(
            chargerId,
            connectorNo,
            new Page<>(1, 10)
        );
        assertEquals(1, sessions.getTotal());
        ChargerConnectorSession s = sessions.getRecords().get(0);
        assertEquals(sessionId, s.getSessionId());
        assertEquals("OCPP", s.getProtocolType());
        assertNotNull(s.getLastSampleTime());
        assertNotNull(s.getLastVoltage());

        // Assert - curve points
        IPage<ChargerConnectorCurvePoint> curve = sessionCurveService.pageCurvePoints(
            chargerId,
            connectorNo,
            sessionId,
            null,
            null,
            new Page<>(1, 200)
        );
        assertEquals(1, curve.getTotal());
        assertEquals(sessionId, curve.getRecords().get(0).getSessionId());
        assertNotNull(curve.getRecords().get(0).getSampleTime());
    }

    @Test
    @DisplayName("多租户隔离 - 不应查询到其他租户会话")
    void testTenantIsolation_shouldNotLeakSessions() {
        // Arrange
        Long chargerId = 999L;
        Integer connectorNo = 1;

        StartEvent t1Start = StartEvent.builder()
            .eventId("t1s")
            .tenantId(1L)
            .chargerId(chargerId)
            .connectorId(connectorNo)
            .eventType(ProtocolEvent.EventType.CHARGING_START)
            .eventTime(LocalDateTime.now())
            .protocolType("OCPP")
            .stationId(1L)
            .sessionId("S1")
            .initialEnergy(0.0)
            .success(false)
            .message("test")
            .build();

        StartEvent t2Start = StartEvent.builder()
            .eventId("t2s")
            .tenantId(2L)
            .chargerId(chargerId)
            .connectorId(connectorNo)
            .eventType(ProtocolEvent.EventType.CHARGING_START)
            .eventTime(LocalDateTime.now())
            .protocolType("OCPP")
            .stationId(1L)
            .sessionId("S2")
            .initialEnergy(0.0)
            .success(false)
            .message("test")
            .build();

        // Act - tenant 1
        switchTenant(1L);
        assertTrue(sessionCurveService.recordSessionStart(t1Start));

        // Act - tenant 2
        switchTenant(2L);
        assertTrue(sessionCurveService.recordSessionStart(t2Start));

        // Assert - tenant 1 only sees its data
        switchTenant(1L);
        IPage<ChargerConnectorSession> sessionsTenant1 = sessionCurveService.pageSessions(
            chargerId,
            connectorNo,
            new Page<>(1, 10)
        );
        assertEquals(1, sessionsTenant1.getTotal());
        assertEquals("S1", sessionsTenant1.getRecords().get(0).getSessionId());
    }
}
