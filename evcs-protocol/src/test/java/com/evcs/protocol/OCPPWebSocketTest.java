package com.evcs.protocol;

import com.evcs.protocol.config.ProtocolProperties;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import com.evcs.protocol.enums.ProtocolType;
import com.evcs.protocol.service.impl.OCPPProtocolServiceImpl;
import com.evcs.protocol.websocket.OCPPSessionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OCPP WebSocket协议测试
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "evcs.protocol.ocpp.enabled=true",
    "evcs.protocol.ocpp.port=8089"
})
@DisplayName("OCPP WebSocket协议测试")
class OCPPWebSocketTest {

    /**
     * 测试OCPP协议服务初始化
     */
    @Test
    @DisplayName("OCPP协议服务初始化测试")
    void testOCPPProtocolServiceInitialization() {
        ProtocolProperties properties = new ProtocolProperties();
        OCPPSessionManager sessionManager = new OCPPSessionManager();

        OCPPProtocolServiceImpl ocppService = new OCPPProtocolServiceImpl(properties) {
            @Override
            protected boolean doConnect(String deviceCode, ProtocolType protocolType) {
                return true;
            }

            @Override
            protected void doDisconnect(String deviceCode, ProtocolType protocolType) {
            }

            @Override
            protected boolean doRegisterStation(ProtocolRequest request) {
                return true;
            }

            @Override
            protected boolean doSendHeartbeat(ProtocolRequest request) {
                return true;
            }

            @Override
            protected boolean doUpdateStatus(ProtocolRequest request) {
                return true;
            }

            @Override
            protected boolean doStartCharging(ProtocolRequest request) {
                return true;
            }

            @Override
            protected boolean doStopCharging(ProtocolRequest request) {
                return true;
            }
        };

        assertEquals(ProtocolType.OCPP, ocppService.getSupportedProtocolType());
        assertNotNull(ocppService);
    }

    /**
     * 测试OCPP连接管理
     */
    @Test
    @DisplayName("OCPP连接管理测试")
    void testOCPPConnectionManagement() {
        OCPPSessionManager sessionManager = new OCPPSessionManager();

        String chargerCode = "TEST_CHARGER_001";

        // 测试初始状态
        assertFalse(sessionManager.isChargerOnline(chargerCode));
        assertEquals(0, sessionManager.getActiveSessionCount());

        // 模拟连接（在实际测试中需要真实的WebSocket会话）
        // 这里只测试会话管理器的基本功能

        // 测试获取统计信息
        var statistics = sessionManager.getStatisticsSummary();
        assertNotNull(statistics);
        assertEquals(0, statistics.getTotalSessions());
        assertEquals(0, statistics.getActiveSessions());

        sessionManager.shutdown();
    }

    /**
     * 测试OCPP消息类型
     */
    @Test
    @DisplayName("OCPP消息类型测试")
    void testOCPPMessageTypes() {
        // 测试核心消息类型
        assertEquals(2, com.evcs.protocol.enums.OCPPMessageType.CALL.getTypeId());
        assertEquals(3, com.evcs.protocol.enums.OCPPMessageType.CALL_RESULT.getTypeId());
        assertEquals(4, com.evcs.protocol.enums.OCPPMessageType.CALL_ERROR.getTypeId());

        // 测试动作消息
        assertEquals("BootNotification", com.evcs.protocol.enums.OCPPMessageType.BOOT_NOTIFICATION.getAction());
        assertEquals("Heartbeat", com.evcs.protocol.enums.OCPPMessageType.HEARTBEAT.getAction());
        assertEquals("StatusNotification", com.evcs.protocol.enums.OCPPMessageType.STATUS_NOTIFICATION.getAction());

        // 测试类型转换
        assertEquals(com.evcs.protocol.enums.OCPPMessageType.CALL,
                    com.evcs.protocol.enums.OCPPMessageType.fromTypeId(2));
        assertEquals(com.evcs.protocol.enums.OCPPMessageType.HEARTBEAT,
                    com.evcs.protocol.enums.OCPPMessageType.fromAction("Heartbeat"));
    }

    /**
     * 测试OCPP协议请求处理
     */
    @Test
    @DisplayName("OCPP协议请求处理测试")
    void testOCPPProtocolRequestHandling() {
        ProtocolProperties properties = new ProtocolProperties();
        OCPPSessionManager sessionManager = new OCPPSessionManager();

        OCPPProtocolServiceImpl ocppService = new OCPPProtocolServiceImpl(properties) {
            @Override
            protected boolean doConnect(String deviceCode, ProtocolType protocolType) {
                return ProtocolType.OCPP.equals(protocolType);
            }

            @Override
            protected void doDisconnect(String deviceCode, ProtocolType protocolType) {
            }

            @Override
            protected boolean doRegisterStation(ProtocolRequest request) {
                return request.getDeviceCode() != null;
            }

            @Override
            protected boolean doSendHeartbeat(ProtocolRequest request) {
                return request.getDeviceCode() != null;
            }

            @Override
            protected boolean doUpdateStatus(ProtocolRequest request) {
                return request.getDeviceCode() != null && request.getData("status", Integer.class) != null;
            }

            @Override
            protected boolean doStartCharging(ProtocolRequest request) {
                return request.getDeviceCode() != null && request.getSessionId() != null;
            }

            @Override
            protected boolean doStopCharging(ProtocolRequest request) {
                return request.getDeviceCode() != null && request.getSessionId() != null;
            }
        };

        // 测试连接请求
        ProtocolRequest connectRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_CHARGER_001", "connect");
        connectRequest.setChargerId(1L);
        connectRequest.setTenantId(1L);

        boolean connected = ocppService.connect("TEST_CHARGER_001", ProtocolType.OCPP);
        assertTrue(connected);

        // 测试心跳请求
        ProtocolRequest heartbeatRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_CHARGER_001", "heartbeat");
        heartbeatRequest.setChargerId(1L);
        heartbeatRequest.setTenantId(1L);

        ProtocolResponse heartbeatResponse = ocppService.sendHeartbeat(heartbeatRequest);
        assertNotNull(heartbeatResponse);
        assertTrue(heartbeatResponse.isSuccess());

        // 测试状态更新请求
        ProtocolRequest statusRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_CHARGER_001", "status");
        statusRequest.setChargerId(1L);
        statusRequest.setTenantId(1L);
        statusRequest.setData("status", 1);

        ProtocolResponse statusResponse = ocppService.updateStatus(statusRequest);
        assertNotNull(statusResponse);
        assertTrue(statusResponse.isSuccess());

        // 测试开始充电请求
        ProtocolRequest startRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_CHARGER_001", "start");
        startRequest.setChargerId(1L);
        startRequest.setTenantId(1L);
        startRequest.setSessionId("SESSION_001");
        startRequest.setUserId(100L);

        ProtocolResponse startResponse = ocppService.startCharging(startRequest);
        assertNotNull(startResponse);
        assertTrue(startResponse.isSuccess());

        // 测试停止充电请求
        ProtocolRequest stopRequest = new ProtocolRequest(ProtocolType.OCPP, "TEST_CHARGER_001", "stop");
        stopRequest.setChargerId(1L);
        stopRequest.setTenantId(1L);
        stopRequest.setSessionId("SESSION_001");

        ProtocolResponse stopResponse = ocppService.stopCharging(stopRequest);
        assertNotNull(stopResponse);
        assertTrue(stopResponse.isSuccess());

        sessionManager.shutdown();
    }

    /**
     * 测试无效协议类型
     */
    @Test
    @DisplayName("无效协议类型处理测试")
    void testInvalidProtocolType() {
        ProtocolProperties properties = new ProtocolProperties();
        OCPPSessionManager sessionManager = new OCPPSessionManager();

        OCPPProtocolServiceImpl ocppService = new OCPPProtocolServiceImpl(properties) {
            @Override
            protected boolean doConnect(String deviceCode, ProtocolType protocolType) {
                return false;
            }

            @Override
            protected void doDisconnect(String deviceCode, ProtocolType protocolType) {
            }

            @Override
            protected boolean doRegisterStation(ProtocolRequest request) {
                return false;
            }

            @Override
            protected boolean doSendHeartbeat(ProtocolRequest request) {
                return false;
            }

            @Override
            protected boolean doUpdateStatus(ProtocolRequest request) {
                return false;
            }

            @Override
            protected boolean doStartCharging(ProtocolRequest request) {
                return false;
            }

            @Override
            protected boolean doStopCharging(ProtocolRequest request) {
                return false;
            }
        };

        // 测试使用云快充协议类型
        boolean connected = ocppService.connect("TEST_CHARGER_001", ProtocolType.CLOUD_CHARGE);
        assertFalse(connected);

        sessionManager.shutdown();
    }

    /**
     * 测试协议配置
     */
    @Test
    @DisplayName("OCPP协议配置测试")
    void testOCPPProtocolConfiguration() {
        ProtocolProperties properties = new ProtocolProperties();

        assertNotNull(properties.getOcpp());
        assertTrue(properties.getOcpp().isEnabled());
        assertEquals(8088, properties.getOcpp().getPort());
        assertEquals("1.6", properties.getOcpp().getVersion());
        assertEquals(60, properties.getOcpp().getHeartbeatInterval());
        assertEquals(30, properties.getOcpp().getConnectionTimeout());
        assertEquals(10, properties.getOcpp().getMessageTimeout());
        assertEquals(1000, properties.getOcpp().getMaxConnections());
    }

    /**
     * 测试会话状态管理
     */
    @Test
    @DisplayName("OCPP会话状态管理测试")
    void testOCPPSessionStateManagement() {
        OCPPSessionManager sessionManager = new OCPPSessionManager();

        // 测试初始统计
        var stats = sessionManager.getStatisticsSummary();
        assertEquals(0, stats.getTotalSessions());
        assertEquals(0, stats.getActiveSessions());
        assertEquals(0, stats.getTotalMessages());

        // 测试会话列表
        var sessions = sessionManager.getAllSessions();
        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());

        var activeChargers = sessionManager.getActiveChargerCodes();
        assertNotNull(activeChargers);
        assertTrue(activeChargers.isEmpty());

        sessionManager.shutdown();
    }
}