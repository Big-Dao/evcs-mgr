package com.evcs.protocol.controller;

import com.evcs.protocol.client.StationServiceClient;
import com.evcs.protocol.dto.ChargerBasicInfo;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.service.CloudChargeSignatureValidator;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 云快充协议HTTP API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cloudcharge")
@RequiredArgsConstructor
public class CloudChargeController {

    private final CloudChargeSignatureValidator signatureValidator;
    private final ProtocolEventPublisher eventPublisher;
    private final StationServiceClient stationServiceClient;

    /**
     * 处理心跳请求
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<CloudChargeApiResponse> handleHeartbeat(
            @RequestBody CloudChargeRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received heartbeat request from device: {}", request.getDeviceCode());

        try {
            // 验证签名
            if (!validateRequest(request)) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("401", "Invalid signature"));
            }

            // 构建协议请求
            ProtocolRequest protocolRequest = buildProtocolRequest(request, "heartbeat");

            if (protocolRequest.getTenantId() == null) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("404", "Unknown device: " + request.getDeviceCode()));
            }

            Integer connectorId = extractConnectorIdFromData(request.getData());

            // 发布心跳事件
            eventPublisher.publishHeartbeat(
                protocolRequest.getChargerId(),
                connectorId,
                protocolRequest.getTenantId(),
                "CLOUD_CHARGE",
                LocalDateTime.now()
            );

            // 返回成功响应
            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", LocalDateTime.now());
            data.put("nextHeartbeat", 60); // 60秒后下次心跳

            return ResponseEntity.ok(CloudChargeApiResponse.success(data));

        } catch (Exception e) {
            log.error("Error processing heartbeat request", e);
            return ResponseEntity.internalServerError()
                    .body(CloudChargeApiResponse.failure("500", "Internal server error"));
        }
    }

    /**
     * 处理状态上报请求
     */
    @PostMapping("/status")
    public ResponseEntity<CloudChargeApiResponse> handleStatus(
            @RequestBody CloudChargeRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received status report from device: {}, status: {}",
                request.getDeviceCode(), request.getData().get("status"));

        try {
            // 验证签名
            if (!validateRequest(request)) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("401", "Invalid signature"));
            }

            // 构建协议请求
            ProtocolRequest protocolRequest = buildProtocolRequest(request, "status");

            if (protocolRequest.getTenantId() == null) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("404", "Unknown device: " + request.getDeviceCode()));
            }

            Integer status = (Integer) request.getData().get("status");

            Integer connectorId = extractConnectorIdFromData(request.getData());
            String faultCode = extractStringFromData(request.getData(), "faultCode", "errorCode", "errCode", "fault");
            String faultDescription = extractStringFromData(request.getData(), "faultDescription", "errorMsg", "errorMessage");

            // 发布状态变更事件
            eventPublisher.publishStatusChange(
                protocolRequest.getChargerId(),
                connectorId,
                protocolRequest.getTenantId(),
                "CLOUD_CHARGE",
                null, // oldStatus
                status,
                "Status reported by device",
                faultCode,
                faultDescription
            );

            // 返回成功响应
            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", LocalDateTime.now());
            data.put("status", "received");

            return ResponseEntity.ok(CloudChargeApiResponse.success(data));

        } catch (Exception e) {
            log.error("Error processing status request", e);
            return ResponseEntity.internalServerError()
                    .body(CloudChargeApiResponse.failure("500", "Internal server error"));
        }
    }

    /**
     * 处理开始充电请求
     */
    @PostMapping("/start")
    public ResponseEntity<CloudChargeApiResponse> handleStartCharging(
            @RequestBody CloudChargeRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received start charging request from device: {}, sessionId: {}",
                request.getDeviceCode(), request.getSessionId());

        try {
            // 验证签名
            if (!validateRequest(request)) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("401", "Invalid signature"));
            }

            // 构建协议请求
            ProtocolRequest protocolRequest = buildProtocolRequest(request, "start");

            if (protocolRequest.getTenantId() == null) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("404", "Unknown device: " + request.getDeviceCode()));
            }

            Integer connectorId = extractConnectorIdFromData(request.getData());

            // 云快充：userId 由协议上报携带，且必须在 data 内（确保签名覆盖），否则拒绝
            if (protocolRequest.getUserId() == null) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("400", "Missing userId (must be provided in data.userId)"));
            }

            // 强制发布端必须携带 stationId，否则拒绝（避免订单服务无法建单导致 DLQ 堆积）
            if (protocolRequest.getStationId() == null) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("400", "Missing stationId"));
            }

            // 发布充电开始事件
            eventPublisher.publishChargingStart(
                protocolRequest.getStationId(),
                protocolRequest.getChargerId(),
                connectorId,
                protocolRequest.getTenantId(),
                "CLOUD_CHARGE",
                request.getSessionId(),
                protocolRequest.getUserId(),
                null,
                null,
                0.0,
                true,
                "Charging started successfully"
            );

            // 返回成功响应
            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", request.getSessionId());
            data.put("timestamp", LocalDateTime.now());
            data.put("status", "charging");

            return ResponseEntity.ok(CloudChargeApiResponse.success(data));

        } catch (Exception e) {
            log.error("Error processing start charging request", e);
            return ResponseEntity.internalServerError()
                    .body(CloudChargeApiResponse.failure("500", "Internal server error"));
        }
    }

    /**
     * 处理停止充电请求
     */
    @PostMapping("/stop")
    public ResponseEntity<CloudChargeApiResponse> handleStopCharging(
            @RequestBody CloudChargeRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received stop charging request from device: {}, sessionId: {}",
                request.getDeviceCode(), request.getSessionId());

        try {
            // 验证签名
            if (!validateRequest(request)) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("401", "Invalid signature"));
            }

            // 构建协议请求
            ProtocolRequest protocolRequest = buildProtocolRequest(request, "stop");

            if (protocolRequest.getTenantId() == null) {
                return ResponseEntity.badRequest()
                        .body(CloudChargeApiResponse.failure("404", "Unknown device: " + request.getDeviceCode()));
            }

            // 从请求数据中获取充电信息
            Map<String, Object> data = request.getData();
            Double energy = data != null ? toDoubleValue(data.get("energy")) : null;
            if (energy == null) {
                energy = 0.0;
            }
            Long duration = data != null ? toLongValue(data.get("duration")) : null;
            if (duration == null) {
                duration = 0L;
            }
            String reason = data != null ? (String) data.get("reason") : null;
            if (reason == null || reason.trim().isEmpty()) {
                reason = "Manual stop";
            }

            Integer connectorId = extractConnectorIdFromData(request.getData());

            // 发布充电停止事件
            eventPublisher.publishChargingStop(
                protocolRequest.getChargerId(),
                connectorId,
                protocolRequest.getTenantId(),
                "CLOUD_CHARGE",
                request.getSessionId(),
                null,
                energy,
                duration,
                reason,
                true,
                "Charging stopped successfully"
            );

            // 返回成功响应
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("sessionId", request.getSessionId());
            responseData.put("timestamp", LocalDateTime.now());
            responseData.put("status", "stopped");
            responseData.put("energy", energy);
            responseData.put("duration", duration);

            return ResponseEntity.ok(CloudChargeApiResponse.success(responseData));

        } catch (Exception e) {
            log.error("Error processing stop charging request", e);
            return ResponseEntity.internalServerError()
                    .body(CloudChargeApiResponse.failure("500", "Internal server error"));
        }
    }

    /**
     * 验证请求签名
     */
    private boolean validateRequest(CloudChargeRequest request) {
        try {
            // 转换为签名验证器需要的格式
            CloudChargeSignatureValidator.CloudChargeRequest validatorRequest =
                new CloudChargeSignatureValidator.CloudChargeRequest();
            validatorRequest.setRequestId(request.getRequestId());
            validatorRequest.setApiVersion(request.getApiVersion());
            validatorRequest.setTimestamp(request.getTimestamp());
            validatorRequest.setSignature(request.getSignature());
            validatorRequest.setDeviceCode(request.getDeviceCode());
            validatorRequest.setSessionId(request.getSessionId());
            validatorRequest.setAction(request.getAction());
            validatorRequest.setData(request.getData());

            return signatureValidator.validateSignature(validatorRequest);
        } catch (Exception e) {
            log.error("Error validating signature for request: {}", request, e);
            return false;
        }
    }

    /**
     * 构建协议请求对象
     */
    private ProtocolRequest buildProtocolRequest(CloudChargeRequest request, String action) {
        ProtocolRequest protocolRequest = new ProtocolRequest();
        protocolRequest.setRequestId(request.getRequestId());
        protocolRequest.setDeviceCode(request.getDeviceCode());
        protocolRequest.setSessionId(request.getSessionId());
        protocolRequest.setAction(action);
        protocolRequest.setData(request.getData());
        protocolRequest.setSignature(request.getSignature());
        protocolRequest.setApiVersion(request.getApiVersion());
        protocolRequest.setTimestamp(LocalDateTime.parse(request.getTimestamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // 从设备服务查询设备信息
        ChargerBasicInfo info = fetchChargerInfo(request.getDeviceCode());
        if (info != null) {
            protocolRequest.setTenantId(info.getTenantId());
            protocolRequest.setChargerId(info.getId());
            protocolRequest.setStationId(info.getStationId());
        } else {
            // 未知设备：不猜测租户/桩ID；各 handler 必须拒绝，禁止兜底造成错误租户归属
            log.warn("Unknown device code: {}", request.getDeviceCode());
        }

        Long userIdFromData = extractUserIdFromData(request.getData());
        Long userIdFromTopLevel = request.getUserId();

        // 允许顶层同时携带，但要求与 data.userId 一致；避免未签名字段被篡改
        if (userIdFromTopLevel != null && userIdFromData == null) {
            log.warn("CloudCharge request includes top-level userId but data.userId is missing; "
                    + "rejecting unsigned userId. requestId={}", request.getRequestId());
        }
        if (userIdFromTopLevel != null && userIdFromData != null && !userIdFromTopLevel.equals(userIdFromData)) {
            log.warn(
                "CloudCharge request userId mismatch: topLevel={} data.userId={} requestId={}",
                userIdFromTopLevel,
                userIdFromData,
                request.getRequestId()
            );
            // 不使用有歧义的 userId（start 会因此返回 400）
            userIdFromData = null;
        }

        // 仅信任 data.userId（受签名保护）
        protocolRequest.setUserId(userIdFromData);

        return protocolRequest;
    }

    private Long extractUserIdFromData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        return toLongValue(data.get("userId"));
    }

    private Integer extractConnectorIdFromData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        Object raw = null;
        // 常见字段：connectorId / connectorNo / gunNo / port
        for (String key : new String[]{"connectorId", "connectorNo", "gunNo", "gunId", "port", "connector"}) {
            if (data.containsKey(key)) {
                raw = data.get(key);
                break;
            }
        }

        Integer v = toIntegerValue(raw);
        if (v == null || v <= 0) {
            return null;
        }
        return v;
    }

    private String extractStringFromData(Map<String, Object> data, String... keys) {
        if (data == null || data.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        for (String key : keys) {
            if (!data.containsKey(key)) {
                continue;
            }
            Object v = data.get(key);
            if (v == null) {
                continue;
            }
            String s = v.toString().trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private Long toLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
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

    private Double toDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    /**
     * 从设备服务获取充电桩信息
     */
    private ChargerBasicInfo fetchChargerInfo(String deviceCode) {
        return stationServiceClient.getChargerByCode(deviceCode);
    }

    /**
     * 云快充请求对象
     */
    public static class CloudChargeRequest {
        private String requestId;
        private String apiVersion = "3.0";
        private String timestamp;
        private String signature;
        private String deviceCode;
        private String sessionId;
        private Long userId;
        private String action;
        private Map<String, Object> data;

        // Getters and Setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public String getDeviceCode() { return deviceCode; }
        public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    /**
     * 云快充响应对象
     */
    public static class CloudChargeApiResponse {
        private String code;
        private String message;
        private boolean success;
        private Object data;
        private String requestId;
        private String apiVersion = "3.0";
        private String timestamp;

        public CloudChargeApiResponse() {
            this.timestamp = LocalDateTime.now().toString();
            this.success = true;
            this.code = "200";
            this.message = "Success";
        }

        public static CloudChargeApiResponse success(Object data) {
            CloudChargeApiResponse response = new CloudChargeApiResponse();
            response.setData(data);
            return response;
        }

        public static CloudChargeApiResponse failure(String code, String message) {
            CloudChargeApiResponse response = new CloudChargeApiResponse();
            response.setCode(code);
            response.setMessage(message);
            response.setSuccess(false);
            return response;
        }

        // Getters and Setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}