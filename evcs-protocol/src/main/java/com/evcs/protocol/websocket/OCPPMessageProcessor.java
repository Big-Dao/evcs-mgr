package com.evcs.protocol.websocket;

import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.protocol.dto.ChargerBasicInfo;
import com.evcs.protocol.dto.ocpp.OCPPMessage;
import com.evcs.protocol.dto.ocpp.OCPPCallMessage;
import com.evcs.protocol.dto.ocpp.OCPPBootNotificationRequest;
import com.evcs.protocol.dto.ocpp.OCPPMessageParser;
import com.evcs.protocol.dto.ocpp.OCPPErrorCode;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import com.evcs.protocol.service.ChargerInfoResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * OCPP消息处理器
 * 负责处理各种类型的OCPP消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OCPPMessageProcessor {

    private final ProtocolEventListener eventListener;
    private final OCPPMessageParser messageParser;
    private final ProtocolEventPublisher eventPublisher;
    private final ChargerInfoResolver chargerInfoResolver;

    /**
     * 处理OCPP消息（从JSON字符串）
     */
    public void processMessage(OCPPWebSocketSession session, String jsonMessage) {
        log.debug("Processing OCPP message from charger {}: {}", session.getChargerCode(), jsonMessage);

        OCPPMessage message = messageParser.parse(jsonMessage);
        if (message == null) {
            log.warn("Failed to parse OCPP message from charger {}: {}", session.getChargerCode(), jsonMessage);
            return;
        }

        processMessage(session, message);
    }

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
        if (!(message instanceof OCPPCallMessage)) {
            log.warn("Expected CallMessage but got: {}", message.getClass().getSimpleName());
            return;
        }

        OCPPCallMessage callMessage = (OCPPCallMessage) message;
        String action = callMessage.getAction();
        Map<String, Object> payload = callMessage.getPayload();

        log.debug("Processing OCPP Call message from charger {}: action={}", session.getChargerCode(), action);

        switch (action) {
            case "BootNotification":
                processBootNotification(session, callMessage, payload);
                break;
            case "Heartbeat":
                processHeartbeat(session, callMessage, payload);
                break;
            case "StatusNotification":
                processStatusNotification(session, callMessage, payload);
                break;
            case "Authorize":
                processAuthorize(session, callMessage, payload);
                break;
            case "StartTransaction":
                processStartTransaction(session, callMessage, payload);
                break;
            case "StopTransaction":
                processStopTransaction(session, callMessage, payload);
                break;
            case "MeterValues":
                processMeterValues(session, callMessage, payload);
                break;
            default:
                log.warn("Unsupported action from charger {}: {}", session.getChargerCode(), action);
                sendErrorResponse(session, callMessage, "NotSupported", "Action not supported: " + action);
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
    private void processBootNotification(OCPPWebSocketSession session, OCPPCallMessage message, Map<String, Object> payload) {
        try {
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
    private void processHeartbeat(OCPPWebSocketSession session, OCPPCallMessage message, Map<String, Object> payload) {
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

        // 发布到RabbitMQ（用于站点服务落库/离线检测）
        try {
            ChargerBasicInfo info = chargerInfoResolver.resolveByChargerCode(session.getChargerCode());
            if (info != null && info.getId() != null && info.getTenantId() != null) {
                eventPublisher.publishHeartbeat(
                    info.getId(),
                    null,
                    info.getTenantId(),
                    "OCPP",
                    LocalDateTime.now()
                );
            }
        } catch (Exception e) {
            log.debug("Failed to publish OCPP heartbeat event to MQ, chargerCode={}", session.getChargerCode(), e);
        }
    }

    /**
     * 处理StatusNotification消息
     */
    private void processStatusNotification(OCPPWebSocketSession session, OCPPCallMessage message, Map<String, Object> payload) {
        try {
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

            // 发布到RabbitMQ（带 connectorId / faultCode）
            if (connectorId != null && status != null) {
                try {
                    ChargerBasicInfo info = chargerInfoResolver.resolveByChargerCode(session.getChargerCode());
                    if (info != null && info.getId() != null && info.getTenantId() != null) {
                        Integer statusCode = parseStatus(status);
                        eventPublisher.publishStatusChange(
                            info.getId(),
                            connectorId,
                            info.getTenantId(),
                            "OCPP",
                            null,
                            statusCode,
                            status,
                            errorCode,
                            errorCode
                        );
                    }
                } catch (Exception e) {
                    log.debug("Failed to publish OCPP status event to MQ, chargerCode={}", session.getChargerCode(), e);
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
    private void processAuthorize(OCPPWebSocketSession session, OCPPCallMessage message, Map<String, Object> payload) {
        try {
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
    private void processStartTransaction(OCPPWebSocketSession session, OCPPCallMessage message, Map<String, Object> payload) {
        try {
            Integer connectorId = toIntegerValue(payload.get("connectorId"));
            String idTag = (String) payload.get("idTag");
            Integer meterStart = toIntegerValue(payload.get("meterStart"));
            LocalDateTime startTime = parseOcppTimestamp(payload.get("timestamp"));

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

            // Persist minimal mapping for StopTransaction (OCPP 1.6 stop does not carry connectorId)
            session.setAttribute("ocpp.txn.connector." + transactionId, connectorId);
            session.setAttribute("ocpp.txn.meterStart." + transactionId, meterStart);
            session.setAttribute("ocpp.txn.startTime." + transactionId, startTime != null ? startTime : LocalDateTime.now());
            if (connectorId != null && connectorId > 0) {
                session.setAttribute("ocpp.connector.txn." + connectorId, transactionId);
            }

            // Publish charging start to MQ (connector session tracking). Mark success=false to prevent order service side-effects
            try {
                ChargerBasicInfo info = chargerInfoResolver.resolveByChargerCode(session.getChargerCode());
                if (info != null && info.getId() != null && info.getTenantId() != null) {
                    String sessionId = "OCPP_TXN_" + transactionId;
                    Double initialEnergy = meterStart != null ? (meterStart / 1000.0) : 0.0;
                    eventPublisher.publishChargingStart(
                        info.getStationId(),
                        info.getId(),
                        connectorId,
                        info.getTenantId(),
                        "OCPP",
                        sessionId,
                        null,
                        null,
                        null,
                        initialEnergy,
                        false,
                        "OCPP StartTransaction received (order creation skipped: no user/station mapping)"
                    );
                }
            } catch (Exception e) {
                log.debug("Failed to publish OCPP charging start event to MQ, chargerCode={}", session.getChargerCode(), e);
            }

            // 触发充电开始事件
            if (eventListener != null) {
                try {
                    ChargerBasicInfo info = chargerInfoResolver.resolveByChargerCode(session.getChargerCode());
                    Long chargerId = info != null && info.getId() != null
                            ? info.getId()
                            : Long.valueOf(session.getChargerCode().replaceAll("[^0-9]", ""));
                    eventListener.onStartAck(chargerId, "OCPP_TXN_" + transactionId, true, "Transaction started");
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
    private void processStopTransaction(OCPPWebSocketSession session, OCPPCallMessage message, Map<String, Object> payload) {
        try {
            Integer transactionId = toIntegerValue(payload.get("transactionId"));
            String idTag = (String) payload.get("idTag");
            Integer meterStop = toIntegerValue(payload.get("meterStop"));
            LocalDateTime stopTime = parseOcppTimestamp(payload.get("timestamp"));

            log.info("Received StopTransaction from charger {}: transactionId={}, idTag={}, meterStop={}",
                    session.getChargerCode(), transactionId, idTag, meterStop);

            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("idTagInfo", Map.of(
                "status", "Accepted"
            ));

            sendResponse(session, message, responsePayload);

            // Publish charging stop to MQ. Try to recover connectorId/meterStart from StartTransaction mapping.
            try {
                ChargerBasicInfo info = chargerInfoResolver.resolveByChargerCode(session.getChargerCode());
                if (info != null && info.getId() != null && info.getTenantId() != null && transactionId != null) {
                    Integer connectorId = session.getAttribute("ocpp.txn.connector." + transactionId, Integer.class);
                    Integer meterStart = session.getAttribute("ocpp.txn.meterStart." + transactionId, Integer.class);
                    LocalDateTime startTime = session.getAttribute("ocpp.txn.startTime." + transactionId, LocalDateTime.class);
                    Double energy = null;
                    if (meterStart != null && meterStop != null && meterStop >= meterStart) {
                        energy = (meterStop - meterStart) / 1000.0;
                    }
                    Long durationMinutes = null;
                    if (startTime != null) {
                        LocalDateTime effectiveStop = stopTime != null ? stopTime : LocalDateTime.now();
                        long minutes = Duration.between(startTime, effectiveStop).toMinutes();
                        durationMinutes = minutes >= 0 ? minutes : null;
                    }
                    String sessionId = "OCPP_TXN_" + transactionId;
                    eventPublisher.publishChargingStop(
                        info.getId(),
                        connectorId,
                        info.getTenantId(),
                        "OCPP",
                        sessionId,
                        null,
                        energy,
                        durationMinutes,
                        "StopTransaction",
                        false,
                        "OCPP StopTransaction received (order completion skipped: no user/station mapping)"
                    );

                    // cleanup mappings (best-effort)
                    if (connectorId != null && connectorId > 0) {
                        session.removeAttribute("ocpp.connector.txn." + connectorId);
                    }
                    session.removeAttribute("ocpp.txn.connector." + transactionId);
                    session.removeAttribute("ocpp.txn.meterStart." + transactionId);
                    session.removeAttribute("ocpp.txn.startTime." + transactionId);
                }
            } catch (Exception e) {
                log.debug("Failed to publish OCPP charging stop event to MQ, chargerCode={}", session.getChargerCode(), e);
            }

            // 触发充电停止事件
            if (eventListener != null) {
                try {
                    ChargerBasicInfo info = chargerInfoResolver.resolveByChargerCode(session.getChargerCode());
                    Long chargerId = info != null && info.getId() != null
                            ? info.getId()
                            : Long.valueOf(session.getChargerCode().replaceAll("[^0-9]", ""));
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
    private void processMeterValues(OCPPWebSocketSession session, OCPPCallMessage message, Map<String, Object> payload) {
        try {
            Integer connectorId = toIntegerValue(payload.get("connectorId"));
            Integer transactionId = toIntegerValue(payload.get("transactionId"));
            if (transactionId == null && connectorId != null && connectorId > 0) {
                transactionId = session.getAttribute("ocpp.connector.txn." + connectorId, Integer.class);
            }

            log.debug("Received MeterValues from charger {}: connectorId={}, transactionId={}",
                    session.getChargerCode(), connectorId, transactionId);

            // 发送接受响应
            Map<String, Object> responsePayload = new HashMap<>();
            sendResponse(session, message, responsePayload);

            // Publish telemetry to MQ for station persistence (curve/session diagnostics)
            if (connectorId == null || connectorId <= 0) {
                return;
            }

            ChargerBasicInfo info = null;
            try {
                info = chargerInfoResolver.resolveByChargerCode(session.getChargerCode());
            } catch (Exception e) {
                log.debug("Failed to resolve charger info for MeterValues, chargerCode={}", session.getChargerCode(), e);
            }
            if (info == null || info.getId() == null || info.getTenantId() == null) {
                return;
            }

            String sessionId = transactionId != null ? ("OCPP_TXN_" + transactionId) : null;
            LocalDateTime startTime = transactionId != null
                ? session.getAttribute("ocpp.txn.startTime." + transactionId, LocalDateTime.class)
                : null;

            List<?> meterValues = asList(payload.get("meterValue"));
            if (meterValues == null || meterValues.isEmpty()) {
                return;
            }

            for (Object mvObj : meterValues) {
                if (!(mvObj instanceof Map<?, ?>)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> mv = (Map<String, Object>) mvObj;

                LocalDateTime sampleTime = parseOcppTimestamp(mv.get("timestamp"));
                if (sampleTime == null) {
                    sampleTime = LocalDateTime.now();
                }

                TelemetrySample sample = parseTelemetrySample(mv.get("sampledValue"));
                if (sample == null || sample.isEmpty()) {
                    continue;
                }

                Long durationSeconds = null;
                if (startTime != null) {
                    long seconds = Duration.between(startTime, sampleTime).getSeconds();
                    durationSeconds = seconds >= 0 ? seconds : null;
                }

                try {
                    eventPublisher.publishTelemetry(
                        info.getId(),
                        connectorId,
                        info.getTenantId(),
                        "OCPP",
                        sessionId,
                        transactionId,
                        sampleTime,
                        sample.voltage,
                        sample.current,
                        sample.powerKw,
                        sample.soc,
                        sample.energyKwh,
                        durationSeconds
                    );
                } catch (Exception e) {
                    log.debug(
                        "Failed to publish telemetry event, chargerCode={}, chargerId={}, connectorId={}, sessionId={}, transactionId={}",
                        session.getChargerCode(),
                        info.getId(),
                        connectorId,
                        sessionId,
                        transactionId,
                        e
                    );
                }
            }

        } catch (Exception e) {
            log.error("Error processing MeterValues from charger: {}", session.getChargerCode(), e);
            sendErrorResponse(session, message, "FormationViolation", "Invalid MeterValues format");
        }
    }

    private static List<?> asList(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?>) {
            return (List<?>) value;
        }
        return null;
    }

    private static LocalDateTime parseOcppTimestamp(Object timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp instanceof LocalDateTime) {
            return (LocalDateTime) timestamp;
        }
        String s = String.valueOf(timestamp).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            // Typical OCPP: 2025-01-01T00:00:00Z or with offset
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (Exception ignored) {
            // fallthrough
        }
        try {
            return Instant.parse(s).atOffset(java.time.ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception ignored) {
            // fallthrough
        }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static TelemetrySample parseTelemetrySample(Object sampledValueObj) {
        List<?> sampledValues = asList(sampledValueObj);
        if (sampledValues == null || sampledValues.isEmpty()) {
            return null;
        }

        double voltageSum = 0.0;
        int voltageCount = 0;
        double currentSum = 0.0;
        int currentCount = 0;
        Double powerKw = null;
        Double soc = null;
        Double energyKwh = null;

        for (Object svObj : sampledValues) {
            if (!(svObj instanceof Map<?, ?>)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> sv = (Map<String, Object>) svObj;

            String measurand = sv.get("measurand") == null ? null : String.valueOf(sv.get("measurand")).trim();
            String unit = sv.get("unit") == null ? null : String.valueOf(sv.get("unit")).trim();
            Double value = toDoubleValue(sv.get("value"));
            if (value == null) {
                continue;
            }

            String m = measurand == null || measurand.isEmpty() ? "Energy.Active.Import.Register" : measurand;
            switch (m) {
                case "Voltage": {
                    Double v = normalizeVoltage(value, unit);
                    if (v != null) {
                        voltageSum += v;
                        voltageCount++;
                    }
                    break;
                }
                case "Current.Import":
                case "Current.Offered":
                case "Current.Export": {
                    Double a = normalizeCurrent(value, unit);
                    if (a != null) {
                        currentSum += a;
                        currentCount++;
                    }
                    break;
                }
                case "Power.Active.Import":
                case "Power.Active.Export":
                case "Power.Active.Net": {
                    Double kw = normalizePowerKw(value, unit);
                    if (kw != null) {
                        powerKw = kw;
                    }
                    break;
                }
                case "SoC": {
                    soc = value;
                    break;
                }
                case "Energy.Active.Import.Register":
                case "Energy.Active.Import.Interval": {
                    Double kwh = normalizeEnergyKwh(value, unit);
                    if (kwh != null) {
                        energyKwh = kwh;
                    }
                    break;
                }
                default:
                    // ignore other measurands for now
                    break;
            }
        }

        Double voltage = voltageCount > 0 ? (voltageSum / voltageCount) : null;
        Double current = currentCount > 0 ? (currentSum / currentCount) : null;

        return new TelemetrySample(voltage, current, powerKw, soc, energyKwh);
    }

    private static Double toDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double normalizeVoltage(Double value, String unit) {
        if (value == null) {
            return null;
        }
        if (unit == null || unit.isBlank() || "V".equalsIgnoreCase(unit)) {
            return value;
        }
        return value;
    }

    private static Double normalizeCurrent(Double value, String unit) {
        if (value == null) {
            return null;
        }
        if (unit == null || unit.isBlank() || "A".equalsIgnoreCase(unit)) {
            return value;
        }
        return value;
    }

    private static Double normalizePowerKw(Double value, String unit) {
        if (value == null) {
            return null;
        }
        if (unit == null || unit.isBlank()) {
            // OCPP defaults to W for power if unit omitted in some implementations
            return value / 1000.0;
        }
        if ("W".equalsIgnoreCase(unit)) {
            return value / 1000.0;
        }
        if ("kW".equalsIgnoreCase(unit)) {
            return value;
        }
        return value;
    }

    private static Double normalizeEnergyKwh(Double value, String unit) {
        if (value == null) {
            return null;
        }
        if (unit == null || unit.isBlank()) {
            // Most devices report Wh without unit
            return value / 1000.0;
        }
        if ("Wh".equalsIgnoreCase(unit)) {
            return value / 1000.0;
        }
        if ("kWh".equalsIgnoreCase(unit)) {
            return value;
        }
        return value;
    }

    private record TelemetrySample(
        Double voltage,
        Double current,
        Double powerKw,
        Double soc,
        Double energyKwh
    ) {
        boolean isEmpty() {
            return voltage == null && current == null && powerKw == null && soc == null && energyKwh == null;
        }
    }

    // ========== 辅助方法 ==========

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

    private Integer toIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
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
            String responseMessage = messageParser.createCallResultMessage(requestMessage.getMessageId(), payload);
            session.getWebSocketSession().sendMessage(
                new org.springframework.web.socket.TextMessage(
                    java.util.Objects.requireNonNull(responseMessage, "responseMessage must not be null")
                )
            );
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

            String responseMessage = messageParser.createCallResultMessage(requestMessage.getMessageId(), payload);
            session.getWebSocketSession().sendMessage(
                new org.springframework.web.socket.TextMessage(
                    java.util.Objects.requireNonNull(responseMessage, "responseMessage must not be null")
                )
            );

        } catch (Exception e) {
            log.error("Error sending BootNotification response to charger: {}", session.getChargerCode(), e);
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(OCPPWebSocketSession session, OCPPMessage requestMessage, String errorCode, String errorDescription) {
        try {
            OCPPErrorCode ocppErrorCode;
            try {
                ocppErrorCode = OCPPErrorCode.fromCode(errorCode);
            } catch (IllegalArgumentException e) {
                ocppErrorCode = OCPPErrorCode.GENERIC_ERROR;
            }

            String errorMessage = messageParser.createCallErrorMessage(
                requestMessage.getMessageId(), ocppErrorCode, errorDescription, null);
            session.getWebSocketSession().sendMessage(
                new org.springframework.web.socket.TextMessage(
                    java.util.Objects.requireNonNull(errorMessage, "errorMessage must not be null")
                )
            );
        } catch (Exception e) {
            log.error("Error sending error response to charger: {}", session.getChargerCode(), e);
        }
    }
}