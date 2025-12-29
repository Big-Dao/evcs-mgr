package com.evcs.protocol.service.impl;

import com.evcs.common.result.Result;
import com.evcs.protocol.config.ProtocolProperties;
import com.evcs.protocol.dto.ChargerBasicInfo;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import com.evcs.protocol.enums.ProtocolType;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import com.evcs.protocol.websocket.OCPPSessionManager;
import com.evcs.protocol.websocket.OCPPWebSocketSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OCPP协议服务完整实现
 */
@Slf4j
@Service
public class OCPPProtocolServiceImpl extends BaseProtocolService {

    private final ProtocolEventPublisher eventPublisher;
    private final OCPPSessionManager sessionManager;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    public OCPPProtocolServiceImpl(ProtocolProperties protocolProperties,
                                  ProtocolEventPublisher eventPublisher,
                                  OCPPSessionManager sessionManager) {
        super(protocolProperties);
        this.eventPublisher = eventPublisher;
        this.sessionManager = sessionManager;
    }

    @Override
    public ProtocolType getSupportedProtocolType() {
        return ProtocolType.OCPP;
    }

    @Override
    public ProtocolResponse sendHeartbeat(ProtocolRequest request) {
        log.info("[OCPP] Sending heartbeat for device: {}", request.getDeviceCode());

        if (!ProtocolType.OCPP.equals(request.getProtocolType())) {
            return ProtocolResponse.failure("400", "Protocol type mismatch");
        }

        boolean success = doSendHeartbeat(request);
        if (success) {
            return ProtocolResponse.success("Heartbeat sent successfully");
        } else {
            return ProtocolResponse.failure("500", "Failed to send heartbeat");
        }
    }

    @Override
    public ProtocolResponse updateStatus(ProtocolRequest request) {
        log.info("[OCPP] Updating status for device: {}", request.getDeviceCode());

        if (!ProtocolType.OCPP.equals(request.getProtocolType())) {
            return ProtocolResponse.failure("400", "Protocol type mismatch");
        }

        boolean success = doUpdateStatus(request);
        if (success) {
            return ProtocolResponse.success("Status updated successfully");
        } else {
            return ProtocolResponse.failure("500", "Failed to update status");
        }
    }

    @Override
    public ProtocolResponse startCharging(ProtocolRequest request) {
        log.info("[OCPP] Starting charging for device: {}", request.getDeviceCode());

        if (!ProtocolType.OCPP.equals(request.getProtocolType())) {
            return ProtocolResponse.failure("400", "Protocol type mismatch");
        }

        boolean success = doStartCharging(request);
        if (success) {
            return ProtocolResponse.success("Charging started successfully");
        } else {
            return ProtocolResponse.failure("500", "Failed to start charging");
        }
    }

    @Override
    public ProtocolResponse stopCharging(ProtocolRequest request) {
        log.info("[OCPP] Stopping charging for device: {}", request.getDeviceCode());

        if (!ProtocolType.OCPP.equals(request.getProtocolType())) {
            return ProtocolResponse.failure("400", "Protocol type mismatch");
        }

        boolean success = doStopCharging(request);
        if (success) {
            return ProtocolResponse.success("Charging stopped successfully");
        } else {
            return ProtocolResponse.failure("500", "Failed to stop charging");
        }
    }

    @Override
    protected boolean doConnect(String deviceCode, ProtocolType protocolType) {
        if (!ProtocolType.OCPP.equals(protocolType)) {
            log.warn("Unsupported protocol type for OCPP service: {}", protocolType);
            return false;
        }

        try {
            // 检查充电站是否已经连接
            OCPPWebSocketSession existingSession = sessionManager.getSession(deviceCode);
            if (existingSession != null && existingSession.isActive()) {
                log.info("Charger {} already connected via OCPP", deviceCode);
                setDeviceConnectionStatus(deviceCode, true);
                return true;
            }

            // OCPP连接需要充电站主动连接WebSocket
            // 这里我们标记为等待连接
            log.info("OCPP connection waiting for charger {} to connect via WebSocket", deviceCode);
            setDeviceConnectionStatus(deviceCode, false);
            return true; // 返回true表示连接请求已受理

        } catch (Exception e) {
            log.error("Error initiating OCPP connection for device: {}", deviceCode, e);
            return false;
        }
    }

    @Override
    protected void doDisconnect(String deviceCode, ProtocolType protocolType) {
        if (!ProtocolType.OCPP.equals(protocolType)) {
            return;
        }

        try {
            boolean removed = sessionManager.removeSession(deviceCode);
            if (removed) {
                log.info("OCPP device disconnected: {}", deviceCode);
                setDeviceConnectionStatus(deviceCode, false);
            } else {
                log.warn("OCPP device not found for disconnection: {}", deviceCode);
            }
        } catch (Exception e) {
            log.error("Error disconnecting OCPP device: {}", deviceCode, e);
        }
    }

    @Override
    protected boolean doRegisterStation(ProtocolRequest request) {
        String deviceCode = request.getDeviceCode();
        if (deviceCode == null) {
            log.warn("Device code is required for OCPP station registration");
            return false;
        }

        try {
            OCPPWebSocketSession session = sessionManager.getSession(deviceCode);
            if (session != null && session.isActive()) {
                // 发送BootNotification请求到充电站
                sendBootNotificationRequest(session);
                log.info("BootNotification request sent to charger: {}", deviceCode);
                return true;
            } else {
                log.warn("OCPP session not found for charger: {}", deviceCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error registering OCPP station: {}", deviceCode, e);
            return false;
        }
    }

    @Override
    protected boolean doSendHeartbeat(ProtocolRequest request) {
        String deviceCode = request.getDeviceCode();
        if (deviceCode == null) {
            return false;
        }

        try {
            OCPPWebSocketSession session = sessionManager.getSession(deviceCode);
            if (session != null && session.isActive()) {
                // OCPP心跳由充电站主动发送，这里只是检查连接状态
                boolean isActive = session.isActive();
                if (isActive) {
                    session.updateLastActiveTime();
                }
                return isActive;
            } else {
                log.warn("OCPP session not found for heartbeat: {}", deviceCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending OCPP heartbeat: {}", deviceCode, e);
            return false;
        }
    }

    @Override
    protected boolean doUpdateStatus(ProtocolRequest request) {
        String deviceCode = request.getDeviceCode();
        Integer status = request.getData("status", Integer.class);

        if (deviceCode == null || status == null) {
            log.warn("Device code and status are required for OCPP status update");
            return false;
        }

        try {
            OCPPWebSocketSession session = sessionManager.getSession(deviceCode);
            if (session != null && session.isActive()) {
                // 发布状态变更事件
                eventPublisher.publishStatusChange(
                    request.getChargerId(),
                    request.getTenantId(),
                    "OCPP",
                    null, // oldStatus
                    status,
                    "Status updated via OCPP"
                );

                log.info("OCPP status updated for charger {}: status={}", deviceCode, status);
                return true;
            } else {
                log.warn("OCPP session not found for status update: {}", deviceCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error updating OCPP status: {}", deviceCode, e);
            return false;
        }
    }

    @Override
    protected boolean doStartCharging(ProtocolRequest request) {
        String deviceCode = request.getDeviceCode();
        String sessionId = request.getSessionId();

        if (deviceCode == null || sessionId == null) {
            log.warn("Device code and session ID are required for OCPP start charging");
            return false;
        }

        try {
            // 强制发布端必须携带 stationId：如果请求没带，尝试通过 chargerCode 从站点服务补齐
            if (request.getStationId() == null) {
                ChargerBasicInfo info = fetchChargerInfoByCode(deviceCode);
                if (info != null) {
                    if (request.getChargerId() == null) {
                        request.setChargerId(info.getId());
                    }
                    if (request.getTenantId() == null) {
                        request.setTenantId(info.getTenantId());
                    }
                    request.setStationId(info.getStationId());
                }
            }

            if (request.getStationId() == null) {
                log.warn(
                    "Missing stationId for OCPP start charging, refuse to publish start event. deviceCode={} sessionId={}",
                    deviceCode,
                    sessionId
                );
                return false;
            }

            OCPPWebSocketSession session = sessionManager.getSession(deviceCode);
            if (session != null && session.isActive()) {
                // 发送RemoteStartTransaction请求
                boolean sent = sendRemoteStartTransaction(session, sessionId, request.getUserId());

                if (sent) {
                    // 发布充电开始事件
                    eventPublisher.publishChargingStart(
                        request.getStationId(),
                        request.getChargerId(),
                        request.getTenantId(),
                        "OCPP",
                        sessionId,
                        request.getUserId(),
                        null,
                        null,
                        0.0,
                        true,
                        "Charging start command sent via OCPP"
                    );

                    log.info("OCPP start charging command sent to charger {}: sessionId={}", deviceCode, sessionId);
                    return true;
                } else {
                    log.warn("Failed to send OCPP start charging command to charger: {}", deviceCode);
                    return false;
                }
            } else {
                log.warn("OCPP session not found for start charging: {}", deviceCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error starting OCPP charging: {}", deviceCode, e);
            return false;
        }
    }

    private ChargerBasicInfo fetchChargerInfoByCode(String chargerCode) {
        if (chargerCode == null) {
            return null;
        }
        if (restTemplate == null) {
            log.warn("RestTemplate not available, cannot fetch charger info by code: {}", chargerCode);
            return null;
        }
        try {
            String url = "http://evcs-station/charger/code/" + chargerCode;
            ParameterizedTypeReference<Result<ChargerBasicInfo>> typeRef = new ParameterizedTypeReference<>() {};
            RequestEntity<Void> requestEntity = RequestEntity.get(requiredUri(url)).build();
            ResponseEntity<Result<ChargerBasicInfo>> response = restTemplate.exchange(requestEntity, typeRef);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }

            Result<ChargerBasicInfo> result = response.getBody();
            if (result == null) {
                return null;
            }

            Integer code = result.getCode();
            if (code != null && code == 200) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch charger info from station service, chargerCode={}", chargerCode, e);
        }
        return null;
    }

    @NonNull
    private static URI requiredUri(String url) {
        URI uri = URI.create(url);
        if (uri == null) {
            throw new IllegalStateException("URI.create returned null");
        }
        return uri;
    }

    @Override
    protected boolean doStopCharging(ProtocolRequest request) {
        String deviceCode = request.getDeviceCode();
        String sessionId = request.getSessionId();

        if (deviceCode == null || sessionId == null) {
            log.warn("Device code and session ID are required for OCPP stop charging");
            return false;
        }

        try {
            OCPPWebSocketSession session = sessionManager.getSession(deviceCode);
            if (session != null && session.isActive()) {
                // 发送RemoteStopTransaction请求
                boolean sent = sendRemoteStopTransaction(session, sessionId);

                if (sent) {
                    // 发布充电停止事件
                    eventPublisher.publishChargingStop(
                        request.getChargerId(),
                        request.getTenantId(),
                        "OCPP",
                        sessionId,
                        null,
                        0.0,
                        0L,
                        "Remote stop command via OCPP",
                        true,
                        "Charging stop command sent via OCPP"
                    );

                    log.info("OCPP stop charging command sent to charger {}: sessionId={}", deviceCode, sessionId);
                    return true;
                } else {
                    log.warn("Failed to send OCPP stop charging command to charger: {}", deviceCode);
                    return false;
                }
            } else {
                log.warn("OCPP session not found for stop charging: {}", deviceCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error stopping OCPP charging: {}", deviceCode, e);
            return false;
        }
    }

    @Override
    protected boolean doReserveNow(ProtocolRequest request) {
        String deviceCode = request.getDeviceCode();
        if (deviceCode == null) return false;

        try {
            OCPPWebSocketSession session = sessionManager.getSession(deviceCode);
            if (session != null && session.isActive()) {
                Map<String, Object> data = request.getData();
                Integer connectorId = data.get("connectorId") != null ? Integer.parseInt(data.get("connectorId").toString()) : 1;
                String expiryDate = (String) data.get("expiryDate");
                String idTag = (String) data.get("idTag");
                Integer reservationId = data.get("reservationId") != null ? Integer.parseInt(data.get("reservationId").toString()) : 0;

                return sendReserveNow(session, connectorId, expiryDate, idTag, reservationId);
            }
            return false;
        } catch (Exception e) {
            log.error("Error reserving now: {}", deviceCode, e);
            return false;
        }
    }

    @Override
    protected boolean doCancelReservation(ProtocolRequest request) {
        String deviceCode = request.getDeviceCode();
        if (deviceCode == null) return false;

        try {
            OCPPWebSocketSession session = sessionManager.getSession(deviceCode);
            if (session != null && session.isActive()) {
                Map<String, Object> data = request.getData();
                Integer reservationId = data.get("reservationId") != null ? Integer.parseInt(data.get("reservationId").toString()) : 0;
                return sendCancelReservation(session, reservationId);
            }
            return false;
        } catch (Exception e) {
            log.error("Error cancelling reservation: {}", deviceCode, e);
            return false;
        }
    }

    @Override
    protected boolean doUpdateFirmware(ProtocolRequest request) {
        String deviceCode = request.getDeviceCode();
        if (deviceCode == null) return false;

        try {
            OCPPWebSocketSession session = sessionManager.getSession(deviceCode);
            if (session != null && session.isActive()) {
                Map<String, Object> data = request.getData();
                String location = (String) data.get("location");
                String retrieveDate = (String) data.get("retrieveDate");
                Integer retries = data.get("retries") != null ? Integer.parseInt(data.get("retries").toString()) : null;
                Integer retryInterval = data.get("retryInterval") != null ? Integer.parseInt(data.get("retryInterval").toString()) : null;

                return sendUpdateFirmware(session, location, retrieveDate, retries, retryInterval);
            }
            return false;
        } catch (Exception e) {
            log.error("Error updating firmware: {}", deviceCode, e);
            return false;
        }
    }

    @Override
    protected boolean doSetChargingProfile(ProtocolRequest request) {
        String deviceCode = request.getDeviceCode();
        if (deviceCode == null) return false;

        try {
            OCPPWebSocketSession session = sessionManager.getSession(deviceCode);
            if (session != null && session.isActive()) {
                Map<String, Object> data = request.getData();
                Integer connectorId = data.get("connectorId") != null ? Integer.parseInt(data.get("connectorId").toString()) : 0;
                @SuppressWarnings("unchecked")
                Map<String, Object> csChargingProfiles = (Map<String, Object>) data.get("csChargingProfiles");

                return sendSetChargingProfile(session, connectorId, csChargingProfiles);
            }
            return false;
        } catch (Exception e) {
            log.error("Error setting charging profile: {}", deviceCode, e);
            return false;
        }
    }

    // ========== OCPP特定方法 ==========

    /**
     * 发送ReserveNow请求
     */
    private boolean sendReserveNow(OCPPWebSocketSession session, Integer connectorId, String expiryDate, String idTag, Integer reservationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("connectorId", connectorId);
            payload.put("expiryDate", expiryDate);
            payload.put("idTag", idTag);
            payload.put("reservationId", reservationId);

            String message = buildOCPPMessage("Call", UUID.randomUUID().toString(), "ReserveNow", payload);
            String messageToSend = message != null ? message : "";
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(messageToSend));
            return true;
        } catch (Exception e) {
            log.error("Error sending ReserveNow to charger: {}", session.getChargerCode(), e);
            return false;
        }
    }

    /**
     * 发送CancelReservation请求
     */
    private boolean sendCancelReservation(OCPPWebSocketSession session, Integer reservationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("reservationId", reservationId);

            String message = buildOCPPMessage("Call", UUID.randomUUID().toString(), "CancelReservation", payload);
            String messageToSend = message != null ? message : "";
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(messageToSend));
            return true;
        } catch (Exception e) {
            log.error("Error sending CancelReservation to charger: {}", session.getChargerCode(), e);
            return false;
        }
    }

    /**
     * 发送UpdateFirmware请求
     */
    private boolean sendUpdateFirmware(OCPPWebSocketSession session, String location, String retrieveDate, Integer retries, Integer retryInterval) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("location", location);
            payload.put("retrieveDate", retrieveDate);
            if (retries != null) payload.put("retries", retries);
            if (retryInterval != null) payload.put("retryInterval", retryInterval);

            String message = buildOCPPMessage("Call", UUID.randomUUID().toString(), "UpdateFirmware", payload);
            String messageToSend = message != null ? message : "";
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(messageToSend));
            return true;
        } catch (Exception e) {
            log.error("Error sending UpdateFirmware to charger: {}", session.getChargerCode(), e);
            return false;
        }
    }

    /**
     * 发送SetChargingProfile请求
     */
    private boolean sendSetChargingProfile(OCPPWebSocketSession session, Integer connectorId, Map<String, Object> csChargingProfiles) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("connectorId", connectorId);
            payload.put("csChargingProfiles", csChargingProfiles);

            String message = buildOCPPMessage("Call", UUID.randomUUID().toString(), "SetChargingProfile", payload);
            String messageToSend = message != null ? message : "";
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(messageToSend));
            return true;
        } catch (Exception e) {
            log.error("Error sending SetChargingProfile to charger: {}", session.getChargerCode(), e);
            return false;
        }
    }

    /**
     * 获取连接统计信息
     */
    public OCPPConnectionStatistics getConnectionStatistics() {
        var summary = sessionManager.getStatisticsSummary();
        var sessions = sessionManager.getAllSessions();

        int authenticatedCount = (int) sessions.stream()
                .filter(session -> session.getStatus() == OCPPWebSocketSession.SessionStatus.AUTHENTICATED)
                .count();

        return new OCPPConnectionStatistics(
            summary.getTotalSessions(),
            authenticatedCount,
            summary.getTotalMessages(),
            summary.getStatusCounts()
        );
    }

    /**
     * 获取在线充电站列表
     */
    public java.util.List<String> getOnlineChargers() {
        return sessionManager.getActiveChargerCodes().stream()
                .filter(sessionManager::isChargerOnline)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 检查充电站是否在线
     */
    public boolean isChargerOnline(String chargerCode) {
        return sessionManager.isChargerOnline(chargerCode);
    }

    /**
     * 发送BootNotification请求
     */
    private void sendBootNotificationRequest(OCPPWebSocketSession session) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("chargePointVendor", "EVCS Manager");
            payload.put("chargePointModel", "OCPP-1.6");
            payload.put("firmwareVersion", "1.0.0");

            String message = buildOCPPMessage("Call", UUID.randomUUID().toString(), "BootNotification", payload);
            String messageToSend = message;
            if (messageToSend == null) {
                messageToSend = "";
            }
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(messageToSend));

        } catch (Exception e) {
            log.error("Error sending BootNotification request to charger: {}", session.getChargerCode(), e);
        }
    }

    /**
     * 发送RemoteStartTransaction请求
     */
    private boolean sendRemoteStartTransaction(OCPPWebSocketSession session, String sessionId, Long userId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("connectorId", 1);
            payload.put("idTag", userId != null ? "USER_" + userId : "DEFAULT_TAG");

            String message = buildOCPPMessage("Call", UUID.randomUUID().toString(), "RemoteStartTransaction", payload);
            String messageToSend = message;
            if (messageToSend == null) {
                messageToSend = "";
            }
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(messageToSend));

            return true;
        } catch (Exception e) {
            log.error("Error sending RemoteStartTransaction to charger: {}", session.getChargerCode(), e);
            return false;
        }
    }

    /**
     * 发送RemoteStopTransaction请求
     */
    private boolean sendRemoteStopTransaction(OCPPWebSocketSession session, String sessionId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            // 这里应该从sessionId中提取transactionId
            payload.put("transactionId", 1); // 临时使用固定值

            String message = buildOCPPMessage("Call", UUID.randomUUID().toString(), "RemoteStopTransaction", payload);
            String messageToSend = message;
            if (messageToSend == null) {
                messageToSend = "";
            }
            session.getWebSocketSession().sendMessage(new org.springframework.web.socket.TextMessage(messageToSend));

            return true;
        } catch (Exception e) {
            log.error("Error sending RemoteStopTransaction to charger: {}", session.getChargerCode(), e);
            return false;
        }
    }

    /**
     * 构建OCPP消息
     */
    private String buildOCPPMessage(String messageType, String messageId, String action, Map<String, Object> payload) {
        try {
            int typeId;
            switch (messageType) {
                case "Call":
                    typeId = 2;
                    break;
                case "CallResult":
                    typeId = 3;
                    break;
                case "CallError":
                    typeId = 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown message type: " + messageType);
            }

            Object[] messageArray;
            if ("Call".equals(messageType)) {
                messageArray = new Object[]{typeId, messageId, action, payload};
            } else {
                messageArray = new Object[]{typeId, messageId, payload};
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(messageArray);

        } catch (Exception e) {
            log.error("Error building OCPP message", e);
            return null;
        }
    }

    /**
     * OCPP连接统计信息
     */
    public static class OCPPConnectionStatistics {
        private final int totalConnections;
        private final int authenticatedConnections;
        private final long totalMessages;
        private final Map<OCPPWebSocketSession.SessionStatus, Long> statusCounts;

        public OCPPConnectionStatistics(int totalConnections, int authenticatedConnections,
                                       long totalMessages, Map<OCPPWebSocketSession.SessionStatus, Long> statusCounts) {
            this.totalConnections = totalConnections;
            this.authenticatedConnections = authenticatedConnections;
            this.totalMessages = totalMessages;
            this.statusCounts = statusCounts;
        }

        public int getTotalConnections() { return totalConnections; }
        public int getAuthenticatedConnections() { return authenticatedConnections; }
        public long getTotalMessages() { return totalMessages; }
        public Map<OCPPWebSocketSession.SessionStatus, Long> getStatusCounts() { return statusCounts; }

        @Override
        public String toString() {
            return String.format("OCPPConnectionStatistics{total=%d, authenticated=%d, messages=%d, status=%s}",
                    totalConnections, authenticatedConnections, totalMessages, statusCounts);
        }
    }
}