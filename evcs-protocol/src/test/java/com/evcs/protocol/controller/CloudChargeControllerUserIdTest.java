package com.evcs.protocol.controller;

import com.evcs.common.test.base.BaseControllerTest;
import com.evcs.protocol.ProtocolServiceApplication;
import com.evcs.protocol.dto.ChargerBasicInfo;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StatusEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import com.evcs.protocol.service.CloudChargeSignatureValidator;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = ProtocolServiceApplication.class
)
@DisplayName("云快充 userId 上报测试")
@WithMockUser(username = "test-admin", roles = {"ADMIN", "TENANT_ADMIN", "OPERATOR"})
@SuppressWarnings("null")
class CloudChargeControllerUserIdTest extends BaseControllerTest {

    @MockBean
    private CloudChargeSignatureValidator signatureValidator;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

        @MockBean
        private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProtocolEventPublisher eventPublisher;

    @Test
    @DisplayName("开始充电 - data.userId 存在时应发布 StartEvent 并携带 userId")
    void testStartCharging_shouldPublishStartEventWithUserId_whenUserIdProvidedInData() throws Exception {
        // Arrange
        Mockito.when(signatureValidator.validateSignature(Mockito.any())).thenReturn(true);

        ChargerBasicInfo chargerInfo = new ChargerBasicInfo();
        chargerInfo.setId(1L);
        chargerInfo.setTenantId(1L);
        chargerInfo.setStationId(11L);
        chargerInfo.setChargerCode("DEVICE_1");
        chargerInfo.setChargerName("TEST");

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.<ParameterizedTypeReference<Object>>any()
        )).thenReturn(ResponseEntity.ok(com.evcs.common.result.Result.success(chargerInfo)));

        Long userId = 100L;
        Map<String, Object> body = new HashMap<>();
        body.put("requestId", "req-001");
        body.put("apiVersion", "3.0");
        body.put("timestamp", "2025-12-16T00:00:00");
        body.put("signature", "sig");
        body.put("deviceCode", "DEVICE_1");
        body.put("sessionId", "SESSION_001");
        body.put("action", "start");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("connectorId", 2);
        body.put("data", data);

        int beforeSize = eventPublisher.getEventHistory().size();

        // Act
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/cloudcharge/start")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(body))
                )
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        var events = eventPublisher.getEventHistory();
        assertTrue(events.size() >= beforeSize + 1, "Event history should grow after publishing start event");
        assertTrue(events.get(0) instanceof StartEvent, "Latest event should be StartEvent");
        assertEquals(userId, ((StartEvent) events.get(0)).getUserId(), "StartEvent.userId should match reported userId");
                assertEquals(11L, ((StartEvent) events.get(0)).getStationId(), "StartEvent.stationId should be provided by publishing side");
        assertEquals(2, ((StartEvent) events.get(0)).getConnectorId(), "StartEvent.connectorId should match reported connectorId");
    }

    @Test
    @DisplayName("状态上报 - data.connectorId 存在时应发布 StatusEvent 并携带 connectorId 与 faultCode")
    void testStatus_shouldPublishStatusEventWithConnectorId_whenConnectorIdProvidedInData() throws Exception {
        // Arrange
        Mockito.when(signatureValidator.validateSignature(Mockito.any())).thenReturn(true);

        ChargerBasicInfo chargerInfo = new ChargerBasicInfo();
        chargerInfo.setId(1L);
        chargerInfo.setTenantId(1L);
        chargerInfo.setStationId(11L);
        chargerInfo.setChargerCode("DEVICE_1");
        chargerInfo.setChargerName("TEST");

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.<ParameterizedTypeReference<Object>>any()
        )).thenReturn(ResponseEntity.ok(com.evcs.common.result.Result.success(chargerInfo)));

        Map<String, Object> body = new HashMap<>();
        body.put("requestId", "req-003");
        body.put("apiVersion", "3.0");
        body.put("timestamp", "2025-12-16T00:00:00");
        body.put("signature", "sig");
        body.put("deviceCode", "DEVICE_1");
        body.put("action", "status");

        Map<String, Object> data = new HashMap<>();
        data.put("status", 3);
        data.put("connectorId", 2);
        data.put("faultCode", "E01");
        body.put("data", data);

        int beforeSize = eventPublisher.getEventHistory().size();

        // Act
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/cloudcharge/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(body))
                )
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        var events = eventPublisher.getEventHistory();
        assertTrue(events.size() >= beforeSize + 1, "Event history should grow after publishing status event");
        assertTrue(events.get(0) instanceof StatusEvent, "Latest event should be StatusEvent");
        assertEquals(2, ((StatusEvent) events.get(0)).getConnectorId(), "StatusEvent.connectorId should match reported connectorId");
        assertEquals("E01", ((StatusEvent) events.get(0)).getFaultCode(), "StatusEvent.faultCode should match reported faultCode");
    }

    @Test
    @DisplayName("心跳 - 未知设备应拒绝且不发布事件（禁止兜底租户0）")
    void testHeartbeat_shouldRejectWithoutPublishing_whenDeviceUnknown() throws Exception {
        // Arrange
        Mockito.when(signatureValidator.validateSignature(Mockito.any())).thenReturn(true);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.<ParameterizedTypeReference<Object>>any()
        )).thenReturn(ResponseEntity.ok(com.evcs.common.result.Result.success(null)));

        Map<String, Object> body = new HashMap<>();
        body.put("requestId", "req-hb-unknown");
        body.put("apiVersion", "3.0");
        body.put("timestamp", "2025-12-16T00:00:00");
        body.put("signature", "sig");
        body.put("deviceCode", "UNKNOWN_DEVICE");
        body.put("action", "heartbeat");

        int beforeSize = eventPublisher.getEventHistory().size();

        // Act & Assert
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/cloudcharge/heartbeat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(body))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertEquals(beforeSize, eventPublisher.getEventHistory().size(),
                "未知设备不得发布任何事件");
    }

    @Test
    @DisplayName("停止充电 - 未知设备应拒绝且不发布事件（禁止兜底租户0）")
    void testStopCharging_shouldRejectWithoutPublishing_whenDeviceUnknown() throws Exception {
        // Arrange
        Mockito.when(signatureValidator.validateSignature(Mockito.any())).thenReturn(true);
        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.<ParameterizedTypeReference<Object>>any()
        )).thenReturn(ResponseEntity.ok(com.evcs.common.result.Result.success(null)));

        Map<String, Object> body = new HashMap<>();
        body.put("requestId", "req-stop-unknown");
        body.put("apiVersion", "3.0");
        body.put("timestamp", "2025-12-16T00:00:00");
        body.put("signature", "sig");
        body.put("deviceCode", "UNKNOWN_DEVICE");
        body.put("sessionId", "SESSION_005");
        body.put("action", "stop");
        body.put("data", Map.of("connectorId", 1));

        int beforeSize = eventPublisher.getEventHistory().size();

        // Act & Assert
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/cloudcharge/stop")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(body))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertEquals(beforeSize, eventPublisher.getEventHistory().size(),
                "未知设备不得发布任何事件");
    }

    @Test
    @DisplayName("开始充电 - 未携带 data.userId 时应返回 400")
    void testStartCharging_shouldReturn400_whenUserIdMissing() throws Exception {
        // Arrange
        Mockito.when(signatureValidator.validateSignature(Mockito.any())).thenReturn(true);

        ChargerBasicInfo chargerInfo = new ChargerBasicInfo();
        chargerInfo.setId(1L);
        chargerInfo.setTenantId(1L);
        chargerInfo.setStationId(11L);
        chargerInfo.setChargerCode("DEVICE_1");
        chargerInfo.setChargerName("TEST");

        Mockito.when(restTemplate.exchange(
                Mockito.any(RequestEntity.class),
                Mockito.<ParameterizedTypeReference<Object>>any()
        )).thenReturn(ResponseEntity.ok(com.evcs.common.result.Result.success(chargerInfo)));

        Map<String, Object> body = new HashMap<>();
        body.put("requestId", "req-002");
        body.put("apiVersion", "3.0");
        body.put("timestamp", "2025-12-16T00:00:00");
        body.put("signature", "sig");
        body.put("deviceCode", "DEVICE_1");
        body.put("sessionId", "SESSION_002");
        body.put("action", "start");
        body.put("data", Map.of());

        // Act & Assert
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/cloudcharge/start")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(body))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));
    }

        @Test
        @DisplayName("开始充电 - stationId 缺失时应返回 400")
        void testStartCharging_shouldReturn400_whenStationIdMissing() throws Exception {
                // Arrange
                Mockito.when(signatureValidator.validateSignature(Mockito.any())).thenReturn(true);

                ChargerBasicInfo chargerInfo = new ChargerBasicInfo();
                chargerInfo.setId(1L);
                chargerInfo.setTenantId(1L);
                chargerInfo.setStationId(null);
                chargerInfo.setChargerCode("DEVICE_1");
                chargerInfo.setChargerName("TEST");

                Mockito.when(restTemplate.exchange(
                                Mockito.any(RequestEntity.class),
                                Mockito.<ParameterizedTypeReference<Object>>any()
                )).thenReturn(ResponseEntity.ok(com.evcs.common.result.Result.success(chargerInfo)));

                Map<String, Object> body = new HashMap<>();
                body.put("requestId", "req-005");
                body.put("apiVersion", "3.0");
                body.put("timestamp", "2025-12-16T00:00:00");
                body.put("signature", "sig");
                body.put("deviceCode", "DEVICE_1");
                body.put("sessionId", "SESSION_005");
                body.put("action", "start");

                Map<String, Object> data = new HashMap<>();
                data.put("userId", 100L);
                data.put("connectorId", 1);
                body.put("data", data);

                // Act & Assert
                mockMvc.perform(
                                                MockMvcRequestBuilders.post("/api/cloudcharge/start")
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .content(toJson(body))
                                )
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("400"))
                                .andExpect(jsonPath("$.message").value("Missing stationId"));
        }

        @Test
        @DisplayName("停止充电 - data.connectorId 存在时应发布 StopEvent 并携带 connectorId")
        void testStopCharging_shouldPublishStopEventWithConnectorId_whenConnectorIdProvidedInData() throws Exception {
                // Arrange
                Mockito.when(signatureValidator.validateSignature(Mockito.any())).thenReturn(true);

                ChargerBasicInfo chargerInfo = new ChargerBasicInfo();
                chargerInfo.setId(1L);
                chargerInfo.setTenantId(1L);
                chargerInfo.setStationId(11L);
                chargerInfo.setChargerCode("DEVICE_1");
                chargerInfo.setChargerName("TEST");

                Mockito.when(restTemplate.exchange(
                                Mockito.any(RequestEntity.class),
                                Mockito.<ParameterizedTypeReference<Object>>any()
                )).thenReturn(ResponseEntity.ok(com.evcs.common.result.Result.success(chargerInfo)));

                Map<String, Object> body = new HashMap<>();
                body.put("requestId", "req-004");
                body.put("apiVersion", "3.0");
                body.put("timestamp", "2025-12-16T00:00:00");
                body.put("signature", "sig");
                body.put("deviceCode", "DEVICE_1");
                body.put("sessionId", "SESSION_004");
                body.put("action", "stop");

                Map<String, Object> data = new HashMap<>();
                data.put("connectorId", 1);
                data.put("energy", 12.5);
                data.put("duration", 60L);
                body.put("data", data);

                int beforeSize = eventPublisher.getEventHistory().size();

                // Act
                mockMvc.perform(
                                                MockMvcRequestBuilders.post("/api/cloudcharge/stop")
                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                .content(toJson(body))
                                )
                                // Assert
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                var events = eventPublisher.getEventHistory();
                assertTrue(events.size() >= beforeSize + 1, "Event history should grow after publishing stop event");
                assertTrue(events.get(0) instanceof StopEvent, "Latest event should be StopEvent");
                assertEquals(1, ((StopEvent) events.get(0)).getConnectorId(), "StopEvent.connectorId should match reported connectorId");
                assertEquals("SESSION_004", ((StopEvent) events.get(0)).getSessionId(), "StopEvent.sessionId should match request sessionId");
        }
}
