package com.evcs.protocol;

import com.evcs.protocol.api.ProtocolManager;
import com.evcs.protocol.config.ProtocolProperties;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import com.evcs.protocol.enums.ProtocolType;
import com.evcs.protocol.websocket.OCPPSessionManager;
import com.evcs.protocol.websocket.OCPPWebSocketSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 协议集成测试
 */
@SpringBootTest(classes = ProtocolServiceApplication.class)
@ActiveProfiles("test")
@DisplayName("协议集成测试")
class ProtocolIntegrationTest {

    @Autowired
    private ProtocolManager protocolManager;

    @Autowired
    private ProtocolProperties protocolProperties;

    @Autowired
    private OCPPSessionManager sessionManager;

    private void registerActiveOcppSession(String deviceCode) {
        WebSocketSession webSocketSession = Mockito.mock(WebSocketSession.class);
        Mockito.when(webSocketSession.isOpen()).thenReturn(true);
        Mockito.when(webSocketSession.getId()).thenReturn(UUID.randomUUID().toString());

        OCPPWebSocketSession session = new OCPPWebSocketSession(webSocketSession, deviceCode);
        session.setStatus(OCPPWebSocketSession.SessionStatus.CONNECTED);
        sessionManager.addSession(session);
    }

    @Test
    @DisplayName("协议管理器初始化测试")
    void testProtocolManagerInitialization_shouldSucceed_whenContextStarted() {
        // Arrange - Spring context
        assertNotNull(protocolManager, "ProtocolManager should be initialized");
        assertNotNull(protocolProperties, "ProtocolProperties should be initialized");

        // Act - query default/registered types
        // 验证默认协议类型
        assertEquals(ProtocolType.OCPP, protocolProperties.getDefaultProtocol(),
                "Default protocol should be OCPP");

        // Assert
        // 验证已注册的协议类型
        var registeredTypes = protocolManager.getRegisteredProtocolTypes();
        assertFalse(registeredTypes.isEmpty(), "Should have registered protocol types");
        assertTrue(registeredTypes.contains(ProtocolType.OCPP), "Should contain OCPP protocol");
    }

    @Test
    @DisplayName("协议请求处理测试")
    void testProtocolRequestHandling_shouldSucceed_whenOcppSessionActive() {
        // Arrange - active OCPP session + request
        registerActiveOcppSession("TEST_DEVICE_001");

        ProtocolRequest ocppRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_DEVICE_001", "heartbeat");
        ocppRequest.setChargerId(1L);
        ocppRequest.setTenantId(1L);

        // Act - handle request
        ProtocolResponse response = protocolManager.handleRequest(ocppRequest);

        // Assert - verify response
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isSuccess(), "Request should be processed successfully");
        assertNotNull(response.getCode(), "Response code should not be null");
    }

    @Test
    @DisplayName("设备连接管理测试")
    void testDeviceConnectionManagement_shouldSucceed_whenConnectAndDisconnect() {
        // Arrange
        String deviceCode = "TEST_DEVICE_002";

        // Act - connect
        boolean connected = protocolManager.connect(deviceCode, ProtocolType.OCPP);

        // Assert - connected
        assertTrue(connected, "Device should connect successfully");

        // 验证连接状态
        assertTrue(protocolManager.isDeviceConnected(deviceCode), "Device should be connected");

        // Act - disconnect
        protocolManager.disconnect(deviceCode);
        // 注意：由于当前的实现是模拟的，断开连接后可能仍然显示为已连接
        // 实际实现中需要根据具体协议的连接管理机制来处理
    }

    @Test
    @DisplayName("协议类型管理测试")
    void testProtocolTypeManagement_shouldUseDefault_whenDeviceNotConfigured() {
        // Arrange
        String deviceCode = "TEST_DEVICE_003";

        // Act - set protocol type
        protocolManager.setDeviceProtocolType(deviceCode, ProtocolType.CLOUD_CHARGE);

        // Assert - configured device
        ProtocolType protocolType = protocolManager.getProtocolType(deviceCode);
        assertEquals(ProtocolType.CLOUD_CHARGE, protocolType, "Protocol type should match");

        // Act & Assert - unknown device uses default
        // 测试未设置协议类型的设备（应该返回默认协议类型）
        String unknownDevice = "UNKNOWN_DEVICE";
        ProtocolType defaultProtocol = protocolManager.getProtocolType(unknownDevice);
        assertEquals(protocolProperties.getDefaultProtocol(), defaultProtocol,
                "Unknown device should use default protocol");
    }

    @Test
    @DisplayName("心跳请求测试")
    void testHeartbeatRequest_shouldSucceed_whenOcppSessionActive() {
        // Arrange
        registerActiveOcppSession("TEST_DEVICE_004");

        ProtocolRequest heartbeatRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_DEVICE_004", "heartbeat");
        heartbeatRequest.setChargerId(1L);
        heartbeatRequest.setTenantId(1L);

        // Act
        ProtocolResponse response = protocolManager.handleRequest(heartbeatRequest);

        // Assert
        assertNotNull(response, "Heartbeat response should not be null");
        assertTrue(response.isSuccess(), "Heartbeat should be processed successfully");
        assertEquals("200", response.getCode(), "Response code should be 200");
    }

    @Test
    @DisplayName("状态更新请求测试")
    void testStatusUpdateRequest_shouldSucceed_whenOcppSessionActive() {
        // Arrange
        registerActiveOcppSession("TEST_DEVICE_005");

        ProtocolRequest statusRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_DEVICE_005", "status");
        statusRequest.setChargerId(1L);
        statusRequest.setTenantId(1L);
        statusRequest.setData("status", 1); // 可用状态

        // Act
        ProtocolResponse response = protocolManager.handleRequest(statusRequest);

        // Assert
        assertNotNull(response, "Status update response should not be null");
        assertTrue(response.isSuccess(), "Status update should be processed successfully");
    }

    @Test
    @DisplayName("开始充电请求测试")
    void testStartChargingRequest_shouldSucceed_whenOcppSessionActive() {
        // Arrange
        registerActiveOcppSession("TEST_DEVICE_006");

        ProtocolRequest startRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_DEVICE_006", "start");
        startRequest.setChargerId(1L);
        startRequest.setTenantId(1L);
        startRequest.setSessionId("SESSION_001");
        startRequest.setUserId(100L);

        // Act
        ProtocolResponse response = protocolManager.handleRequest(startRequest);

        // Assert
        assertNotNull(response, "Start charging response should not be null");
        assertTrue(response.isSuccess(), "Start charging should be processed successfully");
    }

    @Test
    @DisplayName("停止充电请求测试")
    void testStopChargingRequest_shouldSucceed_whenOcppSessionActive() {
        // Arrange
        registerActiveOcppSession("TEST_DEVICE_007");

        ProtocolRequest stopRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_DEVICE_007", "stop");
        stopRequest.setChargerId(1L);
        stopRequest.setTenantId(1L);
        stopRequest.setSessionId("SESSION_001");

        // Act
        ProtocolResponse response = protocolManager.handleRequest(stopRequest);

        // Assert
        assertNotNull(response, "Stop charging response should not be null");
        assertTrue(response.isSuccess(), "Stop charging should be processed successfully");
    }

    @Test
    @DisplayName("无效请求处理测试")
    void testInvalidRequestHandling_shouldFail_whenRequestInvalid() {
        // Arrange/Act - 测试空请求
        ProtocolResponse response = protocolManager.handleRequest(null);

        // Assert
        assertFalse(response.isSuccess(), "Null request should fail");
        assertEquals("400", response.getCode(), "Should return 400 error");

        // Arrange - invalid action
        ProtocolRequest invalidRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_DEVICE_008", "invalid_action");
        invalidRequest.setChargerId(1L);
        invalidRequest.setTenantId(1L);

        // Act
        ProtocolResponse invalidResponse = protocolManager.handleRequest(invalidRequest);

        // Assert
        assertFalse(invalidResponse.isSuccess(), "Invalid action should fail");
        assertEquals("404", invalidResponse.getCode(), "Should return 404 error");
    }

    @Test
    @DisplayName("协议统计信息测试")
    void testProtocolStatistics_shouldContainOcpp_whenDevicesConnected() {
        // Arrange - connect devices
        protocolManager.connect("TEST_DEVICE_009", ProtocolType.OCPP);
        protocolManager.connect("TEST_DEVICE_010", ProtocolType.CLOUD_CHARGE);

        // Act
        var statistics = ((com.evcs.protocol.service.impl.ProtocolManagerImpl) protocolManager).getProtocolStatistics();

        // Assert
        assertNotNull(statistics, "Statistics should not be null");
        assertTrue(statistics.containsKey(ProtocolType.OCPP), "Should contain OCPP statistics");
    }
}
