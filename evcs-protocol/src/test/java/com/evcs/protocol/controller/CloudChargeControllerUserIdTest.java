package com.evcs.protocol.controller;

import com.evcs.common.test.base.BaseControllerTest;
import com.evcs.protocol.ProtocolServiceApplication;
import com.evcs.protocol.event.StartEvent;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ProtocolServiceApplication.class)
@DisplayName("云快充 userId 上报测试")
class CloudChargeControllerUserIdTest extends BaseControllerTest {

    @MockBean
    private CloudChargeSignatureValidator signatureValidator;

    @MockBean
    private org.springframework.web.client.RestTemplate restTemplate;

    @Autowired
    private ProtocolEventPublisher eventPublisher;

    @Test
    @DisplayName("开始充电 - data.userId 存在时应发布 StartEvent 并携带 userId")
    void testStartCharging_shouldPublishStartEventWithUserId_whenUserIdProvidedInData() throws Exception {
        // Arrange
        Mockito.when(signatureValidator.validateSignature(Mockito.any())).thenReturn(true);
        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.eq(HttpMethod.GET),
                Mockito.isNull(),
                Mockito.<ParameterizedTypeReference<Object>>any()
        )).thenThrow(new RuntimeException("mock station service unavailable"));

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
    }

    @Test
    @DisplayName("开始充电 - 未携带 data.userId 时应返回 400")
    void testStartCharging_shouldReturn400_whenUserIdMissing() throws Exception {
        // Arrange
        Mockito.when(signatureValidator.validateSignature(Mockito.any())).thenReturn(true);
        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.eq(HttpMethod.GET),
                Mockito.isNull(),
                Mockito.<ParameterizedTypeReference<Object>>any()
        )).thenThrow(new RuntimeException("mock station service unavailable"));

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
}
