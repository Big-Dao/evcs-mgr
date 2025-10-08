package com.evcs.protocol;

import com.evcs.protocol.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 协议事件集成测试
 * 测试事件模型和发布功能
 */
@DisplayName("协议事件集成测试")
class ProtocolEventIntegrationTest {

    @Test
    @DisplayName("测试心跳事件模型")
    void testHeartbeatEventModel() {
        HeartbeatEvent event = HeartbeatEvent.builder()
                .eventId("test-001")
                .chargerId(1L)
                .tenantId(1L)
                .eventType(ProtocolEvent.EventType.HEARTBEAT)
                .eventTime(LocalDateTime.now())
                .protocolType("OCPP")
                .lastHeartbeatTime(LocalDateTime.now())
                .build();

        assertNotNull(event);
        assertEquals("test-001", event.getEventId());
        assertEquals(1L, event.getChargerId());
        assertEquals(ProtocolEvent.EventType.HEARTBEAT, event.getEventType());
        assertEquals("protocol.heartbeat.OCPP", event.getRoutingKey());
    }

    @Test
    @DisplayName("测试状态变更事件模型")
    void testStatusEventModel() {
        StatusEvent event = StatusEvent.builder()
                .eventId("test-002")
                .chargerId(1L)
                .tenantId(1L)
                .eventType(ProtocolEvent.EventType.STATUS_CHANGE)
                .eventTime(LocalDateTime.now())
                .protocolType("CloudCharge")
                .oldStatus(1)
                .newStatus(2)
                .statusDesc("From idle to charging")
                .build();

        assertNotNull(event);
        assertEquals(Integer.valueOf(1), event.getOldStatus());
        assertEquals(Integer.valueOf(2), event.getNewStatus());
        assertEquals("protocol.status.CloudCharge", event.getRoutingKey());
    }

    @Test
    @DisplayName("测试开始充电事件模型")
    void testStartEventModel() {
        StartEvent event = StartEvent.builder()
                .eventId("test-003")
                .chargerId(1L)
                .tenantId(1L)
                .eventType(ProtocolEvent.EventType.CHARGING_START)
                .eventTime(LocalDateTime.now())
                .protocolType("OCPP")
                .sessionId("session-001")
                .userId(100L)
                .orderNo("ORDER-001")
                .initialEnergy(0.0)
                .success(true)
                .message("Charging started")
                .build();

        assertNotNull(event);
        assertEquals("session-001", event.getSessionId());
        assertEquals(100L, event.getUserId());
        assertTrue(event.getSuccess());
        assertEquals("protocol.charging.start", event.getRoutingKey());
    }

    @Test
    @DisplayName("测试停止充电事件模型")
    void testStopEventModel() {
        StopEvent event = StopEvent.builder()
                .eventId("test-004")
                .chargerId(1L)
                .tenantId(1L)
                .eventType(ProtocolEvent.EventType.CHARGING_STOP)
                .eventTime(LocalDateTime.now())
                .protocolType("OCPP")
                .sessionId("session-001")
                .orderNo("ORDER-001")
                .energy(25.5)
                .duration(60L)
                .reason("User stopped")
                .success(true)
                .message("Charging completed")
                .build();

        assertNotNull(event);
        assertEquals(25.5, event.getEnergy());
        assertEquals(60L, event.getDuration());
        assertEquals("User stopped", event.getReason());
        assertEquals("protocol.charging.stop", event.getRoutingKey());
    }

    @Test
    @DisplayName("测试事件序列化")
    void testEventSerialization() {
        // 测试事件对象可以被正确创建和序列化
        HeartbeatEvent event = HeartbeatEvent.builder()
                .eventId("test-005")
                .chargerId(1L)
                .tenantId(1L)
                .eventType(ProtocolEvent.EventType.HEARTBEAT)
                .eventTime(LocalDateTime.now())
                .protocolType("OCPP")
                .lastHeartbeatTime(LocalDateTime.now())
                .build();

        // 验证对象可以转换为字符串（模拟序列化）
        String eventString = event.toString();
        assertNotNull(eventString);
        assertTrue(eventString.contains("OCPP"), "Event string should contain protocol type");
        assertTrue(eventString.contains("chargerId"), "Event string should contain chargerId field");
    }
}
