package com.evcs.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 安全审计服务
 * 负责记录和存储安全相关事件
 */
@Slf4j
@Service
public class SecurityAuditService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String AUDIT_KEY_PREFIX = "security:audit:";
    private static final int AUDIT_RETENTION_DAYS = 30;

    public SecurityAuditService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 记录安全事件
     */
    public void logSecurityEvent(ServerHttpRequest request, String eventType) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .eventType(eventType)
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request.getHeaders().getFirst("User-Agent"))
                    .path(request.getPath().value())
                    .method(request.getMethod().name())
                    .queryString(request.getQueryParams().toSingleValueMap())
                    .build();

            // 异步存储到Redis
            CompletableFuture.runAsync(() -> storeAuditEvent(event));

            log.debug("Security audit event recorded: {} for path: {}", eventType, request.getPath().value());

        } catch (Exception e) {
            log.error("Failed to log security event", e);
        }
    }

    /**
     * 记录认证成功事件
     */
    public void logAuthenticationSuccess(ServerHttpRequest request, Long userId) {
        try {
            AuthenticationEvent event = AuthenticationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .eventType("AUTHENTICATION_SUCCESS")
                    .userId(userId)
                    .ipAddress(getClientIpAddress(request))
                    .path(request.getPath().value())
                    .method(request.getMethod().name())
                    .userAgent(request.getHeaders().getFirst("User-Agent"))
                    .build();

            storeAuditEvent(event);
            log.info("Authentication success for user {} from IP: {}", userId, getClientIpAddress(request));

        } catch (Exception e) {
            log.error("Failed to log authentication success event", e);
        }
    }

    /**
     * 记录认证失败事件
     */
    public void logAuthenticationFailure(ServerHttpRequest request, String reason) {
        try {
            AuthenticationEvent event = AuthenticationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .eventType("AUTHENTICATION_FAILURE")
                    .ipAddress(getClientIpAddress(request))
                    .path(request.getPath().value())
                    .method(request.getMethod().name())
                    .userAgent(request.getHeaders().getFirst("User-Agent"))
                    .failureReason(reason)
                    .build();

            storeAuditEvent(event);
            log.warn("Authentication failure from IP: {}, reason: {}", getClientIpAddress(request), reason);

        } catch (Exception e) {
            log.error("Failed to log authentication failure event", e);
        }
    }

    /**
     * 记录安全错误事件
     */
    public void logSecurityError(ServerHttpRequest request, Throwable error) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .eventType("SECURITY_ERROR")
                    .ipAddress(getClientIpAddress(request))
                    .path(request.getPath().value())
                    .method(request.getMethod().name())
                    .userAgent(request.getHeaders().getFirst("User-Agent"))
                    .errorType(error.getClass().getSimpleName())
                    .errorMessage(error.getMessage())
                    .stackTrace(getStackTrace(error))
                    .build();

            storeAuditEvent(event);
            log.error("Security error for IP: {}, error: {}", getClientIpAddress(request), error.getMessage(), error);

        } catch (Exception e) {
            log.error("Failed to log security error event", e);
        }
    }

    /**
     * 记录访问拒绝事件
     */
    public void logAccessDenied(ServerHttpRequest request, String reason, Long userId) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .eventType("ACCESS_DENIED")
                    .userId(userId)
                    .ipAddress(getClientIpAddress(request))
                    .path(request.getPath().value())
                    .method(request.getMethod().name())
                    .userAgent(request.getHeaders().getFirst("User-Agent"))
                    .details(reason)
                    .build();

            storeAuditEvent(event);
            log.warn("Access denied for user {} from IP: {}, reason: {}", userId, getClientIpAddress(request), reason);

        } catch (Exception e) {
            log.error("Failed to log access denied event", e);
        }
    }

    /**
     * 记录限流事件
     */
    public void logRateLimitExceeded(ServerHttpRequest request, String limitType, String limitValue) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .eventType("RATE_LIMIT_EXCEEDED")
                    .ipAddress(getClientIpAddress(request))
                    .path(request.getPath().value())
                    .method(request.getMethod().name())
                    .userAgent(request.getHeaders().getFirst("User-Agent"))
                    .details(String.format("Limit Type: %s, Limit: %s", limitType, limitValue))
                    .build();

            storeAuditEvent(event);
            log.warn("Rate limit exceeded for IP: {}, type: {}, limit: {}",
                    getClientIpAddress(request), limitType, limitValue);

        } catch (Exception e) {
            log.error("Failed to log rate limit exceeded event", e);
        }
    }

    /**
     * 记录可疑活动事件
     */
    public void logSuspiciousActivity(ServerHttpRequest request, String description) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .eventType("SUSPICIOUS_ACTIVITY")
                    .ipAddress(getClientIpAddress(request))
                    .path(request.getPath().value())
                    .method(request.getMethod().name())
                    .userAgent(request.getHeaders().getFirst("User-Agent"))
                    .details(description)
                    .build();

            storeAuditEvent(event);
            log.warn("Suspicious activity detected from IP: {}, description: {}",
                    getClientIpAddress(request), description);

        } catch (Exception e) {
            log.error("Failed to log suspicious activity event", e);
        }
    }

    /**
     * 存储审计事件到Redis
     */
    private void storeAuditEvent(Object event) {
        try {
            String key = AUDIT_KEY_PREFIX + Instant.now().toEpochMilli() + ":" + UUID.randomUUID();
            String jsonValue = objectMapper.writeValueAsString(event);

            redisTemplate.opsForValue().set(key, jsonValue, Duration.ofDays(AUDIT_RETENTION_DAYS));

            // 同时添加到列表中，用于快速查询最近的审计事件
            redisTemplate.opsForList().leftPush("security:audit:recent", jsonValue);

            // 保持列表大小，只保留最近1000条记录
            redisTemplate.opsForList().trim("security:audit:recent", 0, 999);

        } catch (Exception e) {
            log.error("Failed to store audit event", e);
        }
    }

    /**
     * 获取最近的审计事件
     */
    public Map<String, Object> getRecentAuditEvents(int limit) {
        try {
            java.util.List<String> events = redisTemplate.opsForList()
                    .range("security:audit:recent", 0, limit - 1);

            if (events == null) {
                events = new java.util.ArrayList<>();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("events", events);
            result.put("count", events.size());
            result.put("timestamp", Instant.now().toString());

            return result;
        } catch (Exception e) {
            log.error("Failed to retrieve recent audit events", e);
            return Map.of("error", "Failed to retrieve audit events", "timestamp", Instant.now().toString());
        }
    }

    /**
     * 获取审计统计信息
     */
    public Map<String, Object> getAuditStatistics() {
        try {
            // 获取总事件数量（估算）
            Long totalEvents = redisTemplate.opsForList().size("security:audit:recent");

            // 按事件类型统计
            Map<String, Long> eventCounts = new HashMap<>();
            // 这里可以添加更复杂的统计逻辑

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalEvents", totalEvents);
            statistics.put("eventCounts", eventCounts);
            statistics.put("retentionDays", AUDIT_RETENTION_DAYS);
            statistics.put("timestamp", Instant.now().toString());

            return statistics;
        } catch (Exception e) {
            log.error("Failed to get audit statistics", e);
            return Map.of("error", "Failed to get statistics", "timestamp", Instant.now().toString());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For可能包含多个IP，取第一个
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        try {
            var remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return remoteAddress.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            log.debug("Error getting remote address", e);
        }
        return "unknown";
    }

    /**
     * 获取异常堆栈跟踪
     */
    private String getStackTrace(Throwable throwable) {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return "Unable to get stack trace";
        }
    }

    /**
     * 清理过期的审计事件
     */
    public void cleanupExpiredEvents() {
        try {
            // Redis的TTL会自动清理过期键值
            log.debug("Expired audit events cleanup completed by Redis TTL");
        } catch (Exception e) {
            log.error("Error during expired audit events cleanup", e);
        }
    }

    /**
     * 安全事件基础类
     */
    public static class SecurityEvent {
        private String eventId;
        private Instant timestamp;
        private String eventType;
        private String ipAddress;
        private String userAgent;
        private String path;
        private String method;
        private Map<String, String> queryString;
        private Long userId;
        private String errorType;
        private String errorMessage;
        private String stackTrace;
        private String details;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private SecurityEvent event;

            public Builder() {
                this.event = new SecurityEvent();
            }

            public Builder eventId(String eventId) {
                event.eventId = eventId;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public Builder eventType(String eventType) {
                event.eventType = eventType;
                return this;
            }

            public Builder ipAddress(String ipAddress) {
                event.ipAddress = ipAddress;
                return this;
            }

            public Builder userAgent(String userAgent) {
                event.userAgent = userAgent;
                return this;
            }

            public Builder path(String path) {
                event.path = path;
                return this;
            }

            public Builder method(String method) {
                event.method = method;
                return this;
            }

            public Builder queryString(Map<String, String> queryString) {
                event.queryString = queryString;
                return this;
            }

            public Builder userId(Long userId) {
                event.userId = userId;
                return this;
            }

            public Builder errorType(String errorType) {
                event.errorType = errorType;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                event.errorMessage = errorMessage;
                return this;
            }

            public Builder stackTrace(String stackTrace) {
                event.stackTrace = stackTrace;
                return this;
            }

            public Builder details(String details) {
                event.details = details;
                return this;
            }

            public SecurityEvent build() {
                return event;
            }
        }

        // Getters
        public String getEventId() { return eventId; }
        public Instant getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getIpAddress() { return ipAddress; }
        public String getUserAgent() { return userAgent; }
        public String getPath() { return path; }
        public String getMethod() { return method; }
        public Map<String, String> getQueryString() { return queryString; }
        public Long getUserId() { return userId; }
        public String getErrorType() { return errorType; }
        public String getErrorMessage() { return errorMessage; }
        public String getStackTrace() { return stackTrace; }
        public String getDetails() { return details; }
    }

    /**
     * 认证事件类
     */
    public static class AuthenticationEvent {
        private String eventId;
        private Instant timestamp;
        private String eventType;
        private Long userId;
        private String ipAddress;
        private String path;
        private String method;
        private String userAgent;
        private String failureReason;

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private AuthenticationEvent event;

            public Builder() {
                this.event = new AuthenticationEvent();
            }

            public Builder eventId(String eventId) {
                event.eventId = eventId;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public Builder eventType(String eventType) {
                event.eventType = eventType;
                return this;
            }

            public Builder userId(Long userId) {
                event.userId = userId;
                return this;
            }

            public Builder ipAddress(String ipAddress) {
                event.ipAddress = ipAddress;
                return this;
            }

            public Builder path(String path) {
                event.path = path;
                return this;
            }

            public Builder method(String method) {
                event.method = method;
                return this;
            }

            public Builder userAgent(String userAgent) {
                event.userAgent = userAgent;
                return this;
            }

            public Builder failureReason(String failureReason) {
                event.failureReason = failureReason;
                return this;
            }

            public AuthenticationEvent build() {
                return event;
            }
        }

        // Getters
        public String getEventId() { return eventId; }
        public Instant getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public Long getUserId() { return userId; }
        public String getIpAddress() { return ipAddress; }
        public String getPath() { return path; }
        public String getMethod() { return method; }
        public String getUserAgent() { return userAgent; }
        public String getFailureReason() { return failureReason; }
    }
}