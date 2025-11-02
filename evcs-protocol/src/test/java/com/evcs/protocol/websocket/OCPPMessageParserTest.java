package com.evcs.protocol.websocket;

import com.evcs.protocol.dto.ocpp.OCPPMessage;
import com.evcs.protocol.dto.ocpp.OCPPMessageParser;
import com.evcs.protocol.enums.OCPPMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OCPP消息解析器测试
 */
@DisplayName("OCPP消息解析器测试")
class OCPPMessageParserTest {

    private OCPPMessageParser messageParser;

    @BeforeEach
    void setUp() {
        messageParser = new OCPPMessageParser();
    }

    @Test
    @DisplayName("解析BootNotification消息")
    void testParseBootNotificationMessage() {
        // Given
        String bootNotificationJson = "[2,\"12345\",\"BootNotification\",{\"chargePointVendor\":\"TestVendor\",\"chargePointModel\":\"TestModel\",\"chargePointSerialNumber\":\"SN001\",\"firmwareVersion\":\"1.0.0\"}]";

        // When
        OCPPMessage message = messageParser.parse(bootNotificationJson);

        // Then
        assertNotNull(message);
        assertTrue(message.isValid());
        assertEquals(OCPPMessageType.CALL, message.getMessageType());
        assertEquals("12345", message.getMessageId());

        assertTrue(message instanceof com.evcs.protocol.dto.ocpp.OCPPCallMessage);
        com.evcs.protocol.dto.ocpp.OCPPCallMessage callMessage = (com.evcs.protocol.dto.ocpp.OCPPCallMessage) message;
        assertEquals("BootNotification", callMessage.getAction());
        assertNotNull(callMessage.getPayload());
        assertEquals("TestVendor", callMessage.getPayload().get("chargePointVendor"));
    }

    @Test
    @DisplayName("解析CallResult消息")
    void testParseCallResultMessage() {
        // Given
        String callResultJson = "[3,\"12345\",{\"status\":\"Accepted\",\"currentTime\":\"2023-01-01T00:00:00Z\",\"interval\":300}]";

        // When
        OCPPMessage message = messageParser.parse(callResultJson);

        // Then
        assertNotNull(message);
        assertTrue(message.isValid());
        assertEquals(OCPPMessageType.CALL_RESULT, message.getMessageType());
        assertEquals("12345", message.getMessageId());

        assertTrue(message instanceof com.evcs.protocol.dto.ocpp.OCPPCallResultMessage);
        com.evcs.protocol.dto.ocpp.OCPPCallResultMessage callResultMessage = (com.evcs.protocol.dto.ocpp.OCPPCallResultMessage) message;
        assertNotNull(callResultMessage.getPayload());
        assertEquals("Accepted", callResultMessage.getPayload().get("status"));
        assertEquals(300, callResultMessage.getPayload().get("interval"));
    }

    @Test
    @DisplayName("解析CallError消息")
    void testParseCallErrorMessage() {
        // Given
        String callErrorJson = "[4,\"12345\",\"NotSupported\",\"Action not supported\",{\"details\":\"Additional info\"}]";

        // When
        OCPPMessage message = messageParser.parse(callErrorJson);

        // Then
        assertNotNull(message);
        assertTrue(message.isValid());
        assertEquals(OCPPMessageType.CALL_ERROR, message.getMessageType());
        assertEquals("12345", message.getMessageId());

        assertTrue(message instanceof com.evcs.protocol.dto.ocpp.OCPPCallErrorMessage);
        com.evcs.protocol.dto.ocpp.OCPPCallErrorMessage callErrorMessage = (com.evcs.protocol.dto.ocpp.OCPPCallErrorMessage) message;
        assertEquals(com.evcs.protocol.dto.ocpp.OCPPErrorCode.NOT_SUPPORTED, callErrorMessage.getErrorCode());
        assertEquals("Action not supported", callErrorMessage.getErrorDescription());
        assertNotNull(callErrorMessage.getErrorDetails());
    }

    @Test
    @DisplayName("处理无效JSON")
    void testParseInvalidJson() {
        // Given
        String invalidJson = "invalid json";

        // When
        OCPPMessage message = messageParser.parse(invalidJson);

        // Then
        assertNull(message);
    }

    @Test
    @DisplayName("处理空消息")
    void testParseEmptyMessage() {
        // Given
        String emptyMessage = "";

        // When
        OCPPMessage message = messageParser.parse(emptyMessage);

        // Then
        assertNull(message);
    }

    @Test
    @DisplayName("处理null消息")
    void testParseNullMessage() {
        // Given
        String nullMessage = null;

        // When
        OCPPMessage message = messageParser.parse(nullMessage);

        // Then
        assertNull(message);
    }

    @Test
    @DisplayName("创建Call消息")
    void testCreateCallMessage() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("key", "value");

        // When
        String message = messageParser.createCallMessage("12345", "TestAction", payload);

        // Then
        assertNotNull(message);
        assertTrue(message.contains("\"2\""));
        assertTrue(message.contains("\"12345\""));
        assertTrue(message.contains("\"TestAction\""));
        assertTrue(message.contains("\"value\""));
    }

    @Test
    @DisplayName("创建CallResult消息")
    void testCreateCallResultMessage() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "Accepted");

        // When
        String message = messageParser.createCallResultMessage("12345", payload);

        // Then
        assertNotNull(message);
        assertTrue(message.contains("\"3\""));
        assertTrue(message.contains("\"12345\""));
        assertTrue(message.contains("\"Accepted\""));
    }

    @Test
    @DisplayName("创建CallError消息")
    void testCreateCallErrorMessage() {
        // Given
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("detail", "error detail");

        // When
        String message = messageParser.createCallErrorMessage("12345",
            com.evcs.protocol.dto.ocpp.OCPPErrorCode.NOT_SUPPORTED, "Not supported", errorDetails);

        // Then
        assertNotNull(message);
        assertTrue(message.contains("\"4\""));
        assertTrue(message.contains("\"12345\""));
        assertTrue(message.contains("\"NotSupported\""));
        assertTrue(message.contains("\"Not supported\""));
        assertTrue(message.contains("\"error detail\""));
    }

    @Test
    @DisplayName("验证消息格式")
    void testIsValidMessageFormat() {
        // Given
        String validMessage = "[2,\"12345\",\"Action\",{}]";
        String invalidMessage = "invalid format";

        // When & Then
        assertTrue(messageParser.isValidMessageFormat(validMessage));
        assertFalse(messageParser.isValidMessageFormat(invalidMessage));
    }

    @Test
    @DisplayName("获取消息类型")
    void testGetMessageType() {
        // Given
        String callMessage = "[2,\"12345\",\"Action\",{}]";
        String callResultMessage = "[3,\"12345\",{}]";
        String callErrorMessage = "[4,\"12345\",\"ErrorCode\",\"Description\"]";

        // When & Then
        assertEquals(OCPPMessageType.CALL, messageParser.getMessageType(callMessage));
        assertEquals(OCPPMessageType.CALL_RESULT, messageParser.getMessageType(callResultMessage));
        assertEquals(OCPPMessageType.CALL_ERROR, messageParser.getMessageType(callErrorMessage));
    }

    @Test
    @DisplayName("解析Heartbeat消息")
    void testParseHeartbeatMessage() {
        // Given
        String heartbeatJson = "[2,\"12346\",\"Heartbeat\",{}]";

        // When
        OCPPMessage message = messageParser.parse(heartbeatJson);

        // Then
        assertNotNull(message);
        assertTrue(message.isValid());
        assertEquals(OCPPMessageType.CALL, message.getMessageType());

        com.evcs.protocol.dto.ocpp.OCPPCallMessage callMessage = (com.evcs.protocol.dto.ocpp.OCPPCallMessage) message;
        assertEquals("Heartbeat", callMessage.getAction());
        assertNotNull(callMessage.getPayload());
    }

    @Test
    @DisplayName("解析StatusNotification消息")
    void testParseStatusNotificationMessage() {
        // Given
        String statusNotificationJson = "[2,\"12347\",\"StatusNotification\",{\"connectorId\":1,\"status\":\"Available\",\"errorCode\":\"NoError\"}]";

        // When
        OCPPMessage message = messageParser.parse(statusNotificationJson);

        // Then
        assertNotNull(message);
        assertTrue(message.isValid());
        assertEquals(OCPPMessageType.CALL, message.getMessageType());

        com.evcs.protocol.dto.ocpp.OCPPCallMessage callMessage = (com.evcs.protocol.dto.ocpp.OCPPCallMessage) message;
        assertEquals("StatusNotification", callMessage.getAction());
        assertNotNull(callMessage.getPayload());
        assertEquals(1, callMessage.getPayload().get("connectorId"));
        assertEquals("Available", callMessage.getPayload().get("status"));
        assertEquals("NoError", callMessage.getPayload().get("errorCode"));
    }

    @Test
    @DisplayName("解析StartTransaction消息")
    void testParseStartTransactionMessage() {
        // Given
        String startTransactionJson = "[2,\"12348\",\"StartTransaction\",{\"connectorId\":1,\"idTag\":\"USER123\",\"meterStart\":1000,\"timestamp\":\"2023-01-01T00:00:00Z\"}]";

        // When
        OCPPMessage message = messageParser.parse(startTransactionJson);

        // Then
        assertNotNull(message);
        assertTrue(message.isValid());
        assertEquals(OCPPMessageType.CALL, message.getMessageType());

        com.evcs.protocol.dto.ocpp.OCPPCallMessage callMessage = (com.evcs.protocol.dto.ocpp.OCPPCallMessage) message;
        assertEquals("StartTransaction", callMessage.getAction());
        assertNotNull(callMessage.getPayload());
        assertEquals(1, callMessage.getPayload().get("connectorId"));
        assertEquals("USER123", callMessage.getPayload().get("idTag"));
        assertEquals(1000, callMessage.getPayload().get("meterStart"));
    }

    @Test
    @DisplayName("处理不完整的消息")
    void testParseIncompleteMessage() {
        // Given
        String incompleteJson = "[2,\"12345\"]"; // 缺少action和payload

        // When
        OCPPMessage message = messageParser.parse(incompleteJson);

        // Then
        assertNull(message);
    }

    @Test
    @DisplayName("处理无效的消息类型")
    void testParseInvalidMessageType() {
        // Given
        String invalidTypeJson = "[99,\"12345\",\"Action\",{}]"; // 无效的消息类型ID

        // When
        OCPPMessage message = messageParser.parse(invalidTypeJson);

        // Then
        assertNull(message);
    }
}