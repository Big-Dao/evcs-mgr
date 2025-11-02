package com.evcs.protocol.websocket;

import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.protocol.dto.ocpp.OCPPMessage;
import com.evcs.protocol.dto.ocpp.OCPPBootNotificationRequest;
import com.evcs.protocol.enums.OCPPMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OCPP消息处理器
 * 负责处理各种类型的OCPP消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OCPPMessageProcessor {

    private final ProtocolEventListener eventListener;
    private final ObjectMapper objectMapper;

    /**
     * 处理OCPP消息
     */
    public void processMessage(OCPPWebSocketSession session, OCPPMessage message) {
        if (message == null || !message.isValid()) {
            log.warn("Invalid message received from charger: {}", session.getChargerCode());
            return;
        }

        try {
            switch (message.getMessageType()) {
                case CALL:
                    processCallMessage(session, message);
                    break;
                case CALL_RESULT:
                    processCallResultMessage(session, message);
                    break;
                case CALL_ERROR:
                    processCallErrorMessage(session, message);
                    break;
                default:
                    log.warn("Unknown message type from charger {}: {}", session.getChargerCode(), message.getMessageType());
                    sendErrorResponse(session, message, "NotSupported", "Message type not supported");
            }
        } catch (Exception e) {
            log.error("Error processing message from charger: {}", session.getChargerCode(), e);
            sendErrorResponse(session, message, "InternalError", "Message processing failed");
        }
    }

    /**
     * 处理Call消息
     */
    private void processCallMessage(OCPPWebSocketSession session, OCPPMessage message) {
        String action = extractAction(message);
        log.debug("Processing OCPP Call message from charger {}: action={}", session.getChargerCode(), action);

        switch (action) {
            case "BootNotification":
                processBootNotification(session, message);
                break;
            case "Heartbeat":
                processHeartbeat(session, message);
                break;
            case "StatusNotification":
                processStatusNotification(session, message);
                break;
            case "Authorize":
                processAuthorize(session, message);
                break;
            case "StartTransaction":
                processStartTransaction(session, message);
                break;
            case "StopTransaction":
                processStopTransaction(session, message);
                break;
            case "MeterValues":
                processMeterValues(session, message);
                break;
            default:
                log.warn("Unsupported action from charger {}: {}", session.getChargerCode(), action);
                sendErrorResponse(session, message, "NotSupported", "Action not supported: " + action);
        }
    }

    /**
     * 处理CallResult消息
     */
    private void processCallResultMessage(OCPPWebSocketSession session, OCPPMessage message) {
        log.debug("Processing OCPP CallResult message from charger: {}", session.getChargerCode());
        // 这里处理响应消息
        // 可以根据messageId匹配之前发送的请求
    }

    /**
     * 处理CallError消息
     */
    private void processCallErrorMessage(OCPPWebSocketSession session, OCPPMessage message) {
        log.warn("Received OCPP CallError from charger {}: {}", session.getChargerCode(), message);
        // 这里处理错误消息
    }

    /**
     * 处理BootNotification消息
     */
    private void processBootNotification(OCPPWebSocketSession session, OCPPMessage message) {
        try {
            Map<String, Object> payload = extractPayload(message);

            // 创建BootNotification请求对象
            OCPPBootNotificationRequest bootRequest = new OCPPBootNotificationRequest();
            bootRequest.setChargePointVendor((String) payload.get("chargePointVendor"));
            bootRequest.setChargePointModel((String) payload.get("chargePointModel"));
            bootRequest.setChargePointSerialNumber((String) payload.get("chargePointSerialNumber"));
            bootRequest.setFirmwareVersion((String) payload.get("firmwareVersion"));

            log.info("Received BootNotification from charger {}: vendor={}, model={}",
                    session.getChargerCode(), bootRequest.getChargePointVendor(), bootRequest.getChargePointModel());

            // 验证并设置充电站信息
            if (bootRequest.isValid()) {
                session.setStatus(OCPPWebSocketSession.SessionStatus.AUTHENTICATED);
                session.setAttribute("vendor", bootRequest.getChargePointVendor());
                session.setAttribute("model", bootRequest.getChargePointModel());
                session.setAttribute("serialNumber", bootRequest.getChargePointSerialNumber());

                // 发送接受响应
                sendBootNotificationResponse(session, message, "Accepted", 300);

                // 触发事件
                if (eventListener != null) {
                    eventListener.onHeartbeat(
                            Long.valueOf(session.getChargerCode().replaceAll("[^0-9]", "")),
                            LocalDateTime.now()
                    );
                }

            } else {
                sendBootNotificationResponse(session, message, "Rejected", null);
                log.warn("Invalid BootNotification from charger: {}", session.getChargerCode());
            }

        } catch (Exception e) {
            log.error("Error processing BootNotification from charger: {}", session.getChargerCode(), e);
            sendErrorResponse(session, message, "FormationViolation", "Invalid BootNotification format");
        }
    }

    /**
     * 处理Heartbeat消息
     */
    private void processHeartbeat(OCPPWebSocketSession session, OCPPMessage message) {
        log.debug("Received heartbeat from charger: {}", session.getChargerCode());

        // 更新心跳信息
        session.updateLastActiveTime();

        // 发送当前时间响应
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("currentTime", LocalDateTime.now().toString());

        sendResponse(session, message, responsePayload);

        // 触发心跳事件
        if (eventListener != null) {
            try {
                Long chargerId = Long.valueOf(session.getChargerCode().replaceAll("[^0-9]", ""));
                eventListener.onHeartbeat(chargerId, LocalDateTime.now());
            } catch (Exception e) {
                log.debug("Error triggering heartbeat event", e);
            }
        }
    }

    /**
     * 处理StatusNotification消息
     */
    private void processStatusNotification(OCPPWebSocketSession session, OCPPMessage message) {
        try {
            Map<String, Object> payload = extractPayload(message);
            Integer connectorId = (Integer) payload.get("connectorId");
            String status = (String) payload.get("status");
            String errorCode = (String) payload.get("errorCode");

            log.info("Received StatusNotification from charger {}: connectorId={}, status={}, errorCode={}",
                    session.getChargerCode(), connectorId, status, errorCode);

            // 发送接受响应
            Map<String, Object> responsePayload = new HashMap<>();
            sendResponse(session, message, responsePayload);

            // 触发状态变更事件
            if (eventListener != null && connectorId != null && status != null) {
                try {
                    Long chargerId = Long.valueOf(session.getChargerCode().replaceAll("[^0-9]", ""));
                    Integer statusCode = parseStatus(status);
                    eventListener.onStatusChange(chargerId, statusCode);
                } catch (Exception e) {
                    log.debug("Error triggering status change event", e);
                }
            }

        } catch (Exception e) {
            log.error("Error processing StatusNotification from charger: {}", session.getChargerCode(), e);
            sendErrorResponse(session, message, "FormationViolation", "Invalid StatusNotification format");
        }
    }

    /**
     * 处理Authorize消息
     */
    private void processAuthorize(OCPPWebSocketSession session, OCPPMessage message) {
        try {
            Map<String, Object> payload = extractPayload(message);
            String idTag = (String) payload.get("idTag");

            log.info("Received Authorize from charger {}: idTag={}", session.getChargerCode(), idTag);

            // 简单的授权逻辑（实际应该调用授权服务）
            boolean authorized = idTag != null && !idTag.trim().isEmpty();

            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("idTagInfo", Map.of(
                "status", authorized ? "Accepted" : "Rejected",
                "expiryDate", LocalDateTime.now().plusDays(365).toString()
            ));

            sendResponse(session, message, responsePayload);

        } catch (Exception e) {
            log.error("Error processing Authorize from charger: {}", session.getChargerCode(), e);
            sendErrorResponse(session, message, "FormationViolation", "Invalid Authorize format");
        }
    }

    /**
     * 处理StartTransaction消息
     */
    private void processStartTransaction(OCPPWebSocketSession session, OCPPMessage message) {
        try {
            Map<String, Object> payload = extractPayload(message);
            Integer connectorId = (Integer) payload.get("connectorId");
            String idTag = (String) payload.get("idTag");
            Integer meterStart = (Integer) payload.get("meterStart");
            String timestamp = (String) payload.get("timestamp");

            log.info("Received StartTransaction from charger {}: connectorId={}, idTag={}, meterStart={}",
                    session.getChargerCode(), connectorId, idTag, meterStart);

            // 生成事务ID
            int transactionId = generateTransactionId();

            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("transactionId", transactionId);
            responsePayload.put("idTagInfo", Map.of(
                "status", "Accepted",
                "expiryDate", LocalDateTime.now().plusDays(365).toString()
            ));

            sendResponse(session, message, responsePayload);

            // 触发充电开始事件
            if (eventListener != null) {
                try {
                    Long chargerId = Long.valueOf(session.getChargerCode().replaceAll("[^0-9]", ""));
                    String sessionId = "TXN_" + transactionId;
                    eventListener.onStartAck(chargerId, sessionId, true, "Transaction started");
                } catch (Exception e) {
                    log.debug("Error triggering start transaction event", e);
                }
            }

        } catch (Exception e) {
            log.error("Error processing StartTransaction from charger: {}", session.getChargerCode(), e);
            sendErrorResponse(session, message, "FormationViolation", "Invalid StartTransaction format");
        }
    }

    /**
     * 处理StopTransaction消息
     */
    private void processStopTransaction(OCPPWebSocketSession session, OCPPMessage message) {
        try {
            Map<String, Object> payload = extractPayload(message);
            Integer transactionId = (Integer) payload.get("transactionId");
            String idTag = (String) payload.get("idTag");
            Integer meterStop = (Integer) payload.get("meterStop");
            String timestamp = (String) payload.get("timestamp");

            log.info("Received StopTransaction from charger {}: transactionId={}, idTag={}, meterStop={}",
                    session.getChargerCode(), transactionId, idTag, meterStop);

            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("idTagInfo", Map.of(
                "status", "Accepted"
            ));

            sendResponse(session, message, responsePayload);

            // 触发充电停止事件
            if (eventListener != null) {
                try {
                    Long chargerId = Long.valueOf(session.getChargerCode().replaceAll("[^0-9]", ""));
                    String sessionId = "TXN_" + transactionId;
                    eventListener.onStopAck(chargerId, true, "Transaction stopped");
                } catch (Exception e) {
                    log.debug("Error triggering stop transaction event", e);
                }
            }

        } catch (Exception e) {
            log.error("Error processing StopTransaction from charger: {}", session.getChargerCode(), e);
            sendErrorResponse(session, message, "FormationViolation", "Invalid StopTransaction format");
        }
    }

    /**
     * 处理MeterValues消息
     */
    private void processMeterValues(OCPPWebSocketSession session, OCPPMessage message) {
        try {
            Map<String, Object> payload = extractPayload(message);
            Integer connectorId = (Integer) payload.get("connectorId");
            Integer transactionId = (Integer) payload.get("transactionId");

            log.debug("Received MeterValues from charger {}: connectorId={}, transactionId={}",
                    session.getChargerCode(), connectorId, transactionId);

            // 发送接受响应
            Map<String, Object> responsePayload = new HashMap<>();
            sendResponse(session, message, responsePayload);

        } catch (Exception e) {
            log.error("Error processing MeterValues from charger: {}", session.getChargerCode(), e);
            sendErrorResponse(session, message, "FormationViolation", "Invalid MeterValues format");
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 提取动作名称
     */
    private String extractAction(OCPPMessage message) {
        // 这里应该从具体的消息类型中提取action
        // 暂时返回空字符串
        return "";
    }

    /**
     * 提取载荷
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(OCPPMessage message) {
        // 这里应该从具体的消息类型中提取payload
        // 暂时返回空Map
        return new HashMap<>();
    }

    /**
     * 解析状态
     */
    private Integer parseStatus(String status) {
        // 简单的状态解析
        switch (status.toLowerCase()) {
            case "available": return 1;
            case "preparing": return 2;
            case "charging": return 3;
            case "finishing": return 4;
            case "reserved": return 5;
            case "unavailable": return 6;
            case "faulted": return 7;
            default: return 0;
        }
    }

    /**
     * 生成事务ID
     */
    private int generateTransactionId() {
        return (int) (System.currentTimeMillis() % 1000000);
    }

    /**
     * 发送响应
     */
    private void sendResponse(OCPPWebSocketSession session, OCPPMessage requestMessage, Map<String, Object> payload) {
        try {
            String responseMessage = buildJsonMessage(OCPPMessageType.CALL_RESULT, requestMessage.getMessageId(), payload);
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(responseMessage));
        } catch (Exception e) {
            log.error("Error sending response to charger: {}", session.getChargerCode(), e);
        }
    }

    /**
     * 发送BootNotification响应
     */
    private void sendBootNotificationResponse(OCPPWebSocketSession session, OCPPMessage requestMessage, String status, Integer interval) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("status", status);
            payload.put("currentTime", LocalDateTime.now().toString());
            if (interval != null) {
                payload.put("interval", interval);
                session.setHeartbeatInterval(interval);
            }

            String responseMessage = buildJsonMessage(OCPPMessageType.CALL_RESULT, requestMessage.getMessageId(), payload);
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(responseMessage));

        } catch (Exception e) {
            log.error("Error sending BootNotification response to charger: {}", session.getChargerCode(), e);
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(OCPPWebSocketSession session, OCPPMessage requestMessage, String errorCode, String errorDescription) {
        try {
            String errorMessage = buildErrorMessage(requestMessage.getMessageId(), errorCode, errorDescription, null);
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(errorMessage));
        } catch (Exception e) {
            log.error("Error sending error response to charger: {}", session.getChargerCode(), e);
        }
    }

    /**
     * 构建JSON消息
     */
    private String buildJsonMessage(OCPPMessageType messageType, String messageId, Object payload) throws Exception {
        Object[] messageArray;
        switch (messageType) {
            case CALL_RESULT:
                messageArray = new Object[]{messageType.getTypeId(), messageId, payload};
                break;
            default:
                throw new IllegalArgumentException("Unsupported message type for response: " + messageType);
        }
        return objectMapper.writeValueAsString(messageArray);
    }

    /**
     * 构建错误消息
     */
    private String buildErrorMessage(String messageId, String errorCode, String errorDescription, Map<String, Object> errorDetails) throws Exception {
        Object[] messageArray = {OCPPMessageType.CALL_ERROR.getTypeId(), messageId, errorCode, errorDescription, errorDetails != null ? errorDetails : new HashMap<>()};
        return objectMapper.writeValueAsString(messageArray);
    }
}