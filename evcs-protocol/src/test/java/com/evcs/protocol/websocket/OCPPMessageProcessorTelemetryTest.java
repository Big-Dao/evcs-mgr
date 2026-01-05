package com.evcs.protocol.websocket;

import com.evcs.protocol.dto.ChargerBasicInfo;
import com.evcs.protocol.dto.ocpp.OCPPCallMessage;
import com.evcs.protocol.event.ProtocolEvent;
import com.evcs.protocol.event.TelemetryEvent;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import com.evcs.protocol.service.ChargerInfoResolver;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("OCPP MeterValues 遥测事件发布测试")
class OCPPMessageProcessorTelemetryTest {

    @Test
    @DisplayName("MeterValues - 应发布 TELEMETRY 事件并归一化单位")
    @SuppressWarnings("null")
    void testMeterValues_shouldPublishTelemetryEvent_whenValidPayload() throws Exception {
        // Arrange
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.isOpen()).thenReturn(true);
        when(ws.getId()).thenReturn("ws-1");
        doNothing().when(ws).sendMessage(any(WebSocketMessage.class));

        OCPPWebSocketSession session = new OCPPWebSocketSession(ws, "CHARGER_100");
        session.setAttribute("ocpp.txn.startTime.123", LocalDateTime.parse("2025-01-01T00:00:00"));

        ProtocolEventPublisher publisher = new ProtocolEventPublisher();
        ChargerInfoResolver resolver = new ChargerInfoResolverStub();

        OCPPMessageProcessor processor = new OCPPMessageProcessor(
            null,
            null,
            publisher,
            resolver
        );

        Map<String, Object> mv = Map.of(
            "timestamp", "2025-01-01T00:00:10Z",
            "sampledValue", List.of(
                Map.of("measurand", "Voltage", "unit", "V", "value", "230"),
                Map.of("measurand", "Current.Import", "unit", "A", "value", "16"),
                Map.of("measurand", "Power.Active.Import", "unit", "W", "value", "3680"),
                Map.of("measurand", "SoC", "unit", "%", "value", "55"),
                Map.of("measurand", "Energy.Active.Import.Register", "unit", "Wh", "value", "12345")
            )
        );

        Map<String, Object> payload = Map.of(
            "connectorId", 1,
            "transactionId", 123,
            "meterValue", List.of(mv)
        );

        OCPPCallMessage msg = new OCPPCallMessage("m1", "MeterValues", payload);

        // Act
        processor.processMessage(session, msg);

        // Assert
        TelemetryEvent telemetry = publisher.getEventHistory().stream()
            .filter(e -> e instanceof TelemetryEvent)
            .map(e -> (TelemetryEvent) e)
            .findFirst()
            .orElse(null);

        assertNotNull(telemetry, "应发布 TelemetryEvent");
        assertEquals(ProtocolEvent.EventType.TELEMETRY, telemetry.getEventType());
        assertEquals(1, telemetry.getConnectorId());
        assertEquals("OCPP_TXN_123", telemetry.getSessionId());
        assertEquals(LocalDateTime.parse("2025-01-01T00:00:10"), telemetry.getSampleTime());

        assertEquals(230.0, telemetry.getVoltage(), 0.0001);
        assertEquals(16.0, telemetry.getCurrent(), 0.0001);
        assertEquals(3.680, telemetry.getPower(), 0.0001);
        assertEquals(55.0, telemetry.getSoc(), 0.0001);
        assertEquals(12.345, telemetry.getEnergy(), 0.0001);
        assertEquals(10L, telemetry.getDurationSeconds());
    }

    private static final class ChargerInfoResolverStub extends ChargerInfoResolver {

        private ChargerInfoResolverStub() {
            super(null);
        }

        @Override
        public ChargerBasicInfo resolveByChargerCode(String chargerCode) {
            ChargerBasicInfo info = new ChargerBasicInfo();
            info.setTenantId(1L);
            info.setStationId(10L);
            info.setId(100L);
            info.setChargerCode(chargerCode);
            return info;
        }
    }
}
