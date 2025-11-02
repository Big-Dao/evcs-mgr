package com.evcs.protocol.websocket;

import com.evcs.protocol.dto.ocpp.OCPPMessage;
import com.evcs.protocol.enums.OCPPMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * OCPP WebSocket处理器
 * 负责处理WebSocket连接、消息接收和发送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OCPPWebSocketHandler implements WebSocketHandler {

    private final OCPPSessionManager sessionManager;
    private final OCPPMessageProcessor messageProcessor;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String chargerCode = extractChargerCode(session);
        if (chargerCode == null) {
            log.warn("Unable to extract charger code from session: {}, closing connection", session.getId());
            session.close();
            return;
        }

        OCPPWebSocketSession ocppSession = new OCPPWebSocketSession(session, chargerCode);
        boolean added = sessionManager.addSession(ocppSession);

        if (added) {
            log.info("OCPP WebSocket connection established: charger={}, sessionId={}, remoteAddress={}",
                    chargerCode, session.getId(), ocppSession.getRemoteAddress());

            // 发送欢迎消息
            sendWelcomeMessage(ocppSession);
        } else {
            log.warn("Failed to add session for charger: {}, closing connection", chargerCode);
            session.close();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String chargerCode = extractChargerCode(session);
        if (chargerCode == null) {
            log.warn("Unable to extract charger code from session: {}", session.getId());
            return;
        }

        OCPPWebSocketSession ocppSession = sessionManager.getSession(chargerCode);
        if (ocppSession == null) {
            log.warn("Session not found for charger: {}", chargerCode);
            return;
        }

        // 更新会话活跃时间
        sessionManager.updateSessionActivity(chargerCode);

        try {
            // 处理消息
            String messageText = message.getPayload().toString();
            log.debug("Received OCPP message from charger {}: {}", chargerCode, messageText);

            OCPPMessage ocppMessage = parseMessage(messageText);
            if (ocppMessage != null) {
                messageProcessor.processMessage(ocppSession, ocppMessage);
            } else {
                log.error("Failed to parse OCPP message from charger {}: {}", chargerCode, messageText);
                sendErrorMessage(ocppSession, "FormationViolation", "Invalid message format");
            }

        } catch (Exception e) {
            log.error("Error handling message from charger {}: {}", chargerCode, message.getPayload(), e);
            sendErrorMessage(ocppSession, "InternalError", "Message processing failed");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String chargerCode = extractChargerCode(session);
        log.error("WebSocket transport error for charger {}: {}", chargerCode, session.getId(), exception);

        if (chargerCode != null) {
            OCPPWebSocketSession ocppSession = sessionManager.getSession(chargerCode);
            if (ocppSession != null) {
                ocppSession.setStatus(OCPPWebSocketSession.SessionStatus.ERROR);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String chargerCode = extractChargerCode(session);
        log.info("OCPP WebSocket connection closed: charger={}, sessionId={}, status={}, reason={}",
                chargerCode, session.getId(), closeStatus.getCode(), closeStatus.getReason());

        if (chargerCode != null) {
            sessionManager.removeSession(chargerCode);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false; // 不支持部分消息
    }

    /**
     * 从WebSocket会话中提取充电站编码
     */
    private String extractChargerCode(WebSocketSession session) {
        // 从URI路径中提取充电站编码
        // 例如: /ocpp/CHARGER_001/websocket
        String path = session.getUri() != null ? session.getUri().getPath() : null;
        if (path != null && path.startsWith("/ocpp/")) {
            String[] parts = path.split("/");
            if (parts.length >= 3) {
                return parts[2];
            }
        }

        // 从查询参数中提取
        if (session.getUri() != null) {
            String query = session.getUri().getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("charger=")) {
                        return param.substring(8);
                    }
                }
            }
        }

        // 从会话属性中提取
        Object chargerCode = session.getAttributes().get("chargerCode");
        if (chargerCode instanceof String) {
            return (String) chargerCode;
        }

        return null;
    }

    /**
     * 解析OCPP消息
     */
    private OCPPMessage parseMessage(String messageText) {
        try {
            // OCPP消息是JSON数组格式: [messageTypeId, messageId, action, payload] 或 [messageTypeId, messageId, payload]
            Object jsonValue = objectMapper.readValue(messageText, Object.class);

            if (jsonValue instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> messageArray = (java.util.List<Object>) jsonValue;

                if (messageArray.size() >= 2) {
                    Integer messageTypeId = (Integer) messageArray.get(0);
                    String messageId = messageArray.get(1).toString();

                    OCPPMessageType messageType = OCPPMessageType.fromTypeId(messageTypeId);

                    switch (messageType) {
                        case CALL:
                            if (messageArray.size() >= 4) {
                                String action = messageArray.get(2).toString();
                                Map<String, Object> payload = (Map<String, Object>) messageArray.get(3);
                                return createCallMessage(messageId, action, payload);
                            }
                            break;

                        case CALL_RESULT:
                            if (messageArray.size() >= 3) {
                                Map<String, Object> payload = (Map<String, Object>) messageArray.get(2);
                                return createCallResultMessage(messageId, payload);
                            }
                            break;

                        case CALL_ERROR:
                            if (messageArray.size() >= 5) {
                                String errorCode = messageArray.get(2).toString();
                                String errorDescription = messageArray.get(3).toString();
                                Map<String, Object> errorDetails = (Map<String, Object>) messageArray.get(4);
                                return createCallErrorMessage(messageId, errorCode, errorDescription, errorDetails);
                            }
                            break;
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error parsing OCPP message: {}", messageText, e);
        }

        return null;
    }

    /**
     * 创建Call消息
     */
    private OCPPMessage createCallMessage(String messageId, String action, Map<String, Object> payload) {
        // 这里应该创建具体的OCPP消息子类
        // 暂时返回基础消息类
        OCPPMessage message = new OCPPMessage() {
            @Override
            public boolean isValid() {
                return true;
            }
        };
        message.setMessageType(OCPPMessageType.CALL);
        message.setMessageId(messageId);
        return message;
    }

    /**
     * 创建CallResult消息
     */
    private OCPPMessage createCallResultMessage(String messageId, Map<String, Object> payload) {
        OCPPMessage message = new OCPPMessage() {
            @Override
            public boolean isValid() {
                return true;
            }
        };
        message.setMessageType(OCPPMessageType.CALL_RESULT);
        message.setMessageId(messageId);
        return message;
    }

    /**
     * 创建CallError消息
     */
    private OCPPMessage createCallErrorMessage(String messageId, String errorCode, String errorDescription, Map<String, Object> errorDetails) {
        OCPPMessage message = new OCPPMessage() {
            @Override
            public boolean isValid() {
                return true;
            }
        };
        message.setMessageType(OCPPMessageType.CALL_ERROR);
        message.setMessageId(messageId);
        return message;
    }

    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(OCPPWebSocketSession session) {
        try {
            Map<String, Object> welcomePayload = Map.of(
                "message", "Welcome to OCPP Server",
                "timestamp", LocalDateTime.now().toString(),
                "serverVersion", "1.0"
            );

            String welcomeMessage = buildJsonMessage(OCPPMessageType.CALL_RESULT,
                    UUID.randomUUID().toString(), welcomePayload);

            session.getWebSocketSession().sendMessage(new TextMessage(welcomeMessage));
            log.debug("Welcome message sent to charger: {}", session.getChargerCode());

        } catch (Exception e) {
            log.error("Error sending welcome message to charger: {}", session.getChargerCode(), e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(OCPPWebSocketSession session, String errorCode, String errorDescription) {
        try {
            String messageId = UUID.randomUUID().toString();
            String errorMessage = buildErrorMessage(messageId, errorCode, errorDescription, null);

            session.getWebSocketSession().sendMessage(new TextMessage(errorMessage));
            log.debug("Error message sent to charger {}: {}", session.getChargerCode(), errorCode);

        } catch (Exception e) {
            log.error("Error sending error message to charger: {}", session.getChargerCode(), e);
        }
    }

    /**
     * 构建JSON消息
     */
    private String buildJsonMessage(OCPPMessageType messageType, String messageId, Object payload) throws Exception {
        Object[] messageArray;
        switch (messageType) {
            case CALL:
                // [2, messageId, action, payload]
                messageArray = new Object[]{messageType.getTypeId(), messageId, "", payload};
                break;
            case CALL_RESULT:
                // [3, messageId, payload]
                messageArray = new Object[]{messageType.getTypeId(), messageId, payload};
                break;
            default:
                throw new IllegalArgumentException("Unsupported message type: " + messageType);
        }
        return objectMapper.writeValueAsString(messageArray);
    }

    /**
     * 构建错误消息
     */
    private String buildErrorMessage(String messageId, String errorCode, String errorDescription, Map<String, Object> errorDetails) throws Exception {
        Object[] messageArray = {OCPPMessageType.CALL_ERROR.getTypeId(), messageId, errorCode, errorDescription, errorDetails != null ? errorDetails : Map.of()};
        return objectMapper.writeValueAsString(messageArray);
    }
}