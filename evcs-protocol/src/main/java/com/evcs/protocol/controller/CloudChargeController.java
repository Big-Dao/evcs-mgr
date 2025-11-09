package com.evcs.protocol.controller;

import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.service.CloudChargeSignatureValidator;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            protocolRequest.setChargerId(extractChargerId(request.getDeviceCode()));

            // 发布心跳事件
            eventPublisher.publishHeartbeat(
                protocolRequest.getChargerId(),
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
            protocolRequest.setChargerId(extractChargerId(request.getDeviceCode()));

            Integer status = (Integer) request.getData().get("status");

            // 发布状态变更事件
            eventPublisher.publishStatusChange(
                protocolRequest.getChargerId(),
                protocolRequest.getTenantId(),
                "CLOUD_CHARGE",
                null, // oldStatus
                status,
                "Status reported by device"
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
            protocolRequest.setChargerId(extractChargerId(request.getDeviceCode()));

            // 发布充电开始事件
            eventPublisher.publishChargingStart(
                protocolRequest.getChargerId(),
                protocolRequest.getTenantId(),
                "CLOUD_CHARGE",
                request.getSessionId(),
                protocolRequest.getUserId(),
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
            protocolRequest.setChargerId(extractChargerId(request.getDeviceCode()));

            // 从请求数据中获取充电信息
            Map<String, Object> data = request.getData();
            Double energy = data != null ? (Double) data.getOrDefault("energy", 0.0) : 0.0;
            Long duration = data != null ? (Long) data.getOrDefault("duration", 0L) : 0L;
            String reason = data != null ? (String) data.get("reason") : "Manual stop";

            // 发布充电停止事件
            eventPublisher.publishChargingStop(
                protocolRequest.getChargerId(),
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

        // TODO: 从请求中获取或从数据库查询租户ID和用户ID
        protocolRequest.setTenantId(1L); // 临时硬编码
        protocolRequest.setUserId(null); // 根据实际情况设置

        return protocolRequest;
    }

    /**
     * 从设备编码提取充电桩ID
     * 这里假设设备编码包含充电桩ID信息，实际实现需要根据具体编码规则
     */
    private Long extractChargerId(String deviceCode) {
        // 简单实现：假设设备编码后缀是数字ID
        try {
            String[] parts = deviceCode.split("_");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("Failed to extract charger ID from device code: {}", deviceCode);
            return 0L; // 临时返回0，实际应该从数据库查询
        }
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