package com.evcs.protocol.dto.ocpp;

import com.evcs.protocol.enums.OCPPMessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * OCPP消息解析器
 * 负责解析JSON格式的OCPP消息为相应的消息对象
 */
@Slf4j
@Component
public class OCPPMessageParser {

    private final ObjectMapper objectMapper;

    public OCPPMessageParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 解析JSON格式的OCPP消息
     */
    public OCPPMessage parse(String jsonMessage) {
        if (jsonMessage == null || jsonMessage.trim().isEmpty()) {
            log.warn("Empty OCPP message received");
            return null;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonMessage);

            if (!rootNode.isArray() || rootNode.size() < 3) {
                log.warn("Invalid OCPP message format: expected array with at least 3 elements, got: {}", jsonMessage);
                return null;
            }

            ArrayNode arrayNode = (ArrayNode) rootNode;

            // 获取消息类型ID
            JsonNode typeIdNode = arrayNode.get(0);
            if (!typeIdNode.isInt()) {
                log.warn("Invalid message type ID: {}", typeIdNode);
                return null;
            }

            int typeId = typeIdNode.asInt();
            OCPPMessageType messageType = OCPPMessageType.fromTypeId(typeId);

            // 获取消息ID
            JsonNode messageIdNode = arrayNode.get(1);
            if (!messageIdNode.isTextual()) {
                log.warn("Invalid message ID: {}", messageIdNode);
                return null;
            }

            String messageId = messageIdNode.asText();

            // 根据消息类型创建相应的消息对象
            switch (messageType) {
                case CALL:
                    return parseCallMessage(arrayNode, messageId);
                case CALL_RESULT:
                    return parseCallResultMessage(arrayNode, messageId);
                case CALL_ERROR:
                    return parseCallErrorMessage(arrayNode, messageId);
                default:
                    log.warn("Unsupported message type: {}", typeId);
                    return null;
            }

        } catch (Exception e) {
            log.error("Error parsing OCPP message: {}", jsonMessage, e);
            return null;
        }
    }

    /**
     * 解析Call消息 [2, messageId, action, payload]
     */
    private OCPPMessage parseCallMessage(ArrayNode arrayNode, String messageId) {
        try {
            if (arrayNode.size() != 4) {
                log.warn("Invalid Call message format: expected 4 elements, got {}", arrayNode.size());
                return null;
            }

            // 获取action
            JsonNode actionNode = arrayNode.get(2);
            if (!actionNode.isTextual()) {
                log.warn("Invalid action: {}", actionNode);
                return null;
            }
            String action = actionNode.asText();

            // 获取payload
            JsonNode payloadNode = arrayNode.get(3);
            Map<String, Object> payload = parsePayload(payloadNode);

            OCPPCallMessage callMessage = new OCPPCallMessage(messageId, action, payload);
            log.debug("Parsed OCPP Call message: {}", callMessage.getDescription());
            return callMessage;

        } catch (Exception e) {
            log.error("Error parsing Call message", e);
            return null;
        }
    }

    /**
     * 解析CallResult消息 [3, messageId, payload]
     */
    private OCPPMessage parseCallResultMessage(ArrayNode arrayNode, String messageId) {
        try {
            if (arrayNode.size() != 3) {
                log.warn("Invalid CallResult message format: expected 3 elements, got {}", arrayNode.size());
                return null;
            }

            // 获取payload
            JsonNode payloadNode = arrayNode.get(2);
            Map<String, Object> payload = parsePayload(payloadNode);

            OCPPCallResultMessage callResultMessage = new OCPPCallResultMessage(messageId, payload);
            log.debug("Parsed OCPP CallResult message: {}", callResultMessage.getDescription());
            return callResultMessage;

        } catch (Exception e) {
            log.error("Error parsing CallResult message", e);
            return null;
        }
    }

    /**
     * 解析CallError消息 [4, messageId, errorCode, errorDescription, errorDetails]
     */
    private OCPPMessage parseCallErrorMessage(ArrayNode arrayNode, String messageId) {
        try {
            if (arrayNode.size() < 4) {
                log.warn("Invalid CallError message format: expected at least 4 elements, got {}", arrayNode.size());
                return null;
            }

            // 获取errorCode
            JsonNode errorCodeNode = arrayNode.get(2);
            if (!errorCodeNode.isTextual()) {
                log.warn("Invalid error code: {}", errorCodeNode);
                return null;
            }
            String errorCodeStr = errorCodeNode.asText();
            OCPPErrorCode errorCode;
            try {
                errorCode = OCPPErrorCode.fromCode(errorCodeStr);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown error code: {}, using GenericError", errorCodeStr);
                errorCode = OCPPErrorCode.GENERIC_ERROR;
            }

            // 获取errorDescription
            JsonNode errorDescriptionNode = arrayNode.get(3);
            String errorDescription = errorDescriptionNode.isTextual() ? errorDescriptionNode.asText() : "";

            // 获取errorDetails（可选）
            Map<String, Object> errorDetails = null;
            if (arrayNode.size() > 4) {
                JsonNode errorDetailsNode = arrayNode.get(4);
                errorDetails = parsePayload(errorDetailsNode);
            }

            OCPPCallErrorMessage callErrorMessage = new OCPPCallErrorMessage(messageId, errorCode, errorDescription, errorDetails);
            log.debug("Parsed OCPP CallError message: {}", callErrorMessage.getDescription());
            return callErrorMessage;

        } catch (Exception e) {
            log.error("Error parsing CallError message", e);
            return null;
        }
    }

    /**
     * 解析载荷数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(JsonNode payloadNode) {
        if (payloadNode == null || payloadNode.isNull()) {
            return new HashMap<>();
        }

        if (payloadNode.isObject()) {
            try {
                return objectMapper.convertValue(payloadNode, Map.class);
            } catch (Exception e) {
                log.warn("Error converting payload node to Map: {}", payloadNode, e);
                return new HashMap<>();
            }
        } else if (payloadNode.isArray()) {
            // 如果是数组，创建一个包含数组内容的Map
            Map<String, Object> result = new HashMap<>();
            result.put("arrayData", objectMapper.convertValue(payloadNode, Object.class));
            return result;
        } else {
            // 如果是简单值，创建一个包含该值的Map
            Map<String, Object> result = new HashMap<>();
            result.put("value", objectMapper.convertValue(payloadNode, Object.class));
            return result;
        }
    }

    /**
     * 创建Call消息的JSON字符串
     */
    public String createCallMessage(String messageId, String action, Map<String, Object> payload) {
        try {
            Object[] messageArray = {2, messageId, action, payload};
            return objectMapper.writeValueAsString(messageArray);
        } catch (Exception e) {
            log.error("Error creating Call message", e);
            return null;
        }
    }

    /**
     * 创建CallResult消息的JSON字符串
     */
    public String createCallResultMessage(String messageId, Map<String, Object> payload) {
        try {
            Object[] messageArray = {3, messageId, payload};
            return objectMapper.writeValueAsString(messageArray);
        } catch (Exception e) {
            log.error("Error creating CallResult message", e);
            return null;
        }
    }

    /**
     * 创建CallError消息的JSON字符串
     */
    public String createCallErrorMessage(String messageId, OCPPErrorCode errorCode, String errorDescription, Map<String, Object> errorDetails) {
        try {
            Object[] messageArray = {4, messageId, errorCode.getCode(), errorDescription, errorDetails != null ? errorDetails : new HashMap<>()};
            return objectMapper.writeValueAsString(messageArray);
        } catch (Exception e) {
            log.error("Error creating CallError message", e);
            return null;
        }
    }

    /**
     * 检查消息格式是否有效
     */
    public boolean isValidMessageFormat(String jsonMessage) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonMessage);
            return rootNode.isArray() && rootNode.size() >= 3;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取消息类型（不完整解析）
     */
    public OCPPMessageType getMessageType(String jsonMessage) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonMessage);
            if (rootNode.isArray() && rootNode.size() > 0) {
                JsonNode typeIdNode = rootNode.get(0);
                if (typeIdNode.isInt()) {
                    return OCPPMessageType.fromTypeId(typeIdNode.asInt());
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting message type", e);
        }
        return null;
    }
}