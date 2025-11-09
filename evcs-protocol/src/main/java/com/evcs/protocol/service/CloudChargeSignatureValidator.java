package com.evcs.protocol.service;

import com.evcs.protocol.config.ProtocolProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * 云快充协议签名验证器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudChargeSignatureValidator {

    private final ProtocolProperties protocolProperties;

    /**
     * 验证云快充请求签名
     *
     * @param request 云快充请求对象
     * @return 验证结果
     */
    public boolean validateSignature(CloudChargeRequest request) {
        try {
            if (request == null || request.getSignature() == null) {
                log.warn("Request or signature is null");
                return false;
            }

            // 构建签名字符串
            String signString = buildSignString(request);
            log.debug("Sign string: {}", signString);

            // 计算期望的签名
            String expectedSignature = calculateSignature(signString);
            log.debug("Expected signature: {}, Actual signature: {}", expectedSignature, request.getSignature());

            // 验证签名
            boolean isValid = expectedSignature.equals(request.getSignature());

            if (!isValid) {
                log.warn("Signature validation failed for request: {}", request.getRequestId());
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating signature", e);
            return false;
        }
    }

    /**
     * 构建签名字符串
     */
    private String buildSignString(CloudChargeRequest request) {
        StringBuilder sb = new StringBuilder();

        // 按照云快充协议规范构建签名字符串
        sb.append("requestId=").append(request.getRequestId())
          .append("&apiVersion=").append(request.getApiVersion())
          .append("&timestamp=").append(request.getTimestamp())
          .append("&deviceCode=").append(request.getDeviceCode());

        if (request.getSessionId() != null) {
            sb.append("&sessionId=").append(request.getSessionId());
        }

        if (request.getAction() != null) {
            sb.append("&action=").append(request.getAction());
        }

        // 添加业务数据（按字母顺序排序）
        if (request.getData() != null && !request.getData().isEmpty()) {
            Map<String, Object> sortedData = new TreeMap<>(request.getData());
            sortedData.forEach((key, value) -> {
                if (value != null) {
                    sb.append("&").append(key).append("=").append(value.toString());
                }
            });
        }

        return sb.toString();
    }

    /**
     * 计算HMAC-SHA256签名
     */
    private String calculateSignature(String signString) {
        String appSecret = protocolProperties.getCloudCharge().getAppSecret();
        if (appSecret == null) {
            throw new IllegalStateException("CloudCharge app secret is not configured");
        }

        String algorithm = protocolProperties.getCloudCharge().getSignAlgorithm();
        // Only support HMAC-SHA256 for security reasons
        if ("HMAC-SHA256".equals(algorithm.toUpperCase())) {
            return calculateHmacSha256(appSecret, signString);
        } else {
            throw new IllegalArgumentException("Unsupported signature algorithm: " + algorithm +
                ". Only HMAC-SHA256 is supported for security reasons.");
        }
    }

    /**
     * 使用标准Java加密API计算HMAC-SHA256签名
     */
    private String calculateHmacSha256(String appSecret, String signString) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(signString.getBytes(StandardCharsets.UTF_8));

            // Convert to hexadecimal string
            StringBuilder sb = new StringBuilder(hmacBytes.length * 2);
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error calculating HMAC-SHA256 signature", e);
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    /**
     * 生成云快充请求签名
     */
    public String generateSignature(CloudChargeRequest request) {
        String signString = buildSignString(request);
        return calculateSignature(signString);
    }

    /**
     * 验证时间戳是否在有效范围内（防止重放攻击）
     */
    public boolean validateTimestamp(String timestamp) {
        try {
            LocalDateTime requestTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();

            // 允许5分钟的时间差
            LocalDateTime minTime = now.minusMinutes(5);
            LocalDateTime maxTime = now.plusMinutes(5);

            return requestTime.isAfter(minTime) && requestTime.isBefore(maxTime);

        } catch (Exception e) {
            log.error("Error validating timestamp: {}", timestamp, e);
            return false;
        }
    }

    /**
     * 简化的云快充请求对象（用于签名验证）
     */
    public static class CloudChargeRequest {
        private String requestId;
        private String apiVersion;
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

        @Override
        public String toString() {
            return "CloudChargeRequest{" +
                    "requestId='" + requestId + '\'' +
                    ", apiVersion='" + apiVersion + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    ", deviceCode='" + deviceCode + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    ", action='" + action + '\'' +
                    '}';
        }
    }
}