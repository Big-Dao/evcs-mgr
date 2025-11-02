package com.evcs.gateway.controller;

import com.evcs.gateway.security.CircuitBreakerService;
import com.evcs.gateway.security.JwtBlacklistService;
import com.evcs.gateway.security.RateLimitingService;
import com.evcs.gateway.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全监控控制器
 * 提供安全组件的监控和管理API
 */
@Slf4j
@RestController
@RequestMapping("/actuator/security")
@RequiredArgsConstructor
public class SecurityMonitorController {

    private final SecurityAuditService auditService;
    private final RateLimitingService rateLimitingService;
    private final CircuitBreakerService circuitBreakerService;
    private final JwtBlacklistService blacklistService;

    /**
     * 获取安全概览信息
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSecurityOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 审计统计
        var auditStats = auditService.getAuditStatistics();
        overview.put("audit", auditStats);

        // 限流统计（采样）
        var ipStats = rateLimitingService.getStatistics("rate_limit:ip:sample", Duration.ofMinutes(5));
        var apiStats = rateLimitingService.getStatistics("rate_limit:api:sample", Duration.ofMinutes(5));
        overview.put("rateLimit", Map.of(
            "ipSample", ipStats,
            "apiSample", apiStats
        ));

        // 熔断器统计
        Map<String, Object> circuitBreakerStats = new HashMap<>();
        circuitBreakerService.getCircuitBreakers().forEach((serviceName, instance) -> {
            circuitBreakerStats.put(serviceName, instance.getStatistics());
        });
        overview.put("circuitBreakers", circuitBreakerStats);

        // 黑名单统计
        overview.put("blacklist", Map.of(
            "note", "Blacklist statistics available through specific endpoints"
        ));

        overview.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(overview);
    }

    /**
     * 获取审计事件
     */
    @GetMapping("/audit/events")
    public ResponseEntity<Map<String, Object>> getAuditEvents(
            @RequestParam(defaultValue = "50") int limit) {
        var events = auditService.getRecentAuditEvents(limit);
        return ResponseEntity.ok(events);
    }

    /**
     * 获取限流统计
     */
    @GetMapping("/rate-limit/stats")
    public ResponseEntity<Map<String, Object>> getRateLimitStats(
            @RequestParam String key,
            @RequestParam(defaultValue = "60") int windowSeconds) {

        Duration window = Duration.ofSeconds(windowSeconds);
        var stats = rateLimitingService.getStatistics(key, window);
        return ResponseEntity.ok(Map.of(
            "key", key,
            "window", windowSeconds,
            "statistics", stats
        ));
    }

    /**
     * 重置限流计数
     */
    @PostMapping("/rate-limit/reset")
    public ResponseEntity<Map<String, Object>> resetRateLimit(@RequestParam String key) {
        boolean success = rateLimitingService.resetRateLimit(key);
        return ResponseEntity.ok(Map.of(
            "key", key,
            "success", success,
            "message", success ? "Rate limit reset successfully" : "Failed to reset rate limit"
        ));
    }

    /**
     * 添加到黑名单
     */
    @PostMapping("/blacklist/add")
    public ResponseEntity<Map<String, Object>> addToBlacklist(
            @RequestParam String identifier,
            @RequestParam(defaultValue = "3600") int durationSeconds) {

        Duration duration = Duration.ofSeconds(durationSeconds);
        rateLimitingService.addToBlacklist(identifier, duration);

        return ResponseEntity.ok(Map.of(
            "identifier", identifier,
            "duration", durationSeconds,
            "message", "Added to blacklist successfully"
        ));
    }

    /**
     * 从黑名单移除
     */
    @PostMapping("/blacklist/remove")
    public ResponseEntity<Map<String, Object>> removeFromBlacklist(@RequestParam String identifier) {
        rateLimitingService.removeFromBlacklist(identifier);
        blacklistService.removeFromBlacklist(identifier);

        return ResponseEntity.ok(Map.of(
            "identifier", identifier,
            "message", "Removed from blacklist successfully"
        ));
    }

    /**
     * 检查黑名单状态
     */
    @GetMapping("/blacklist/check")
    public ResponseEntity<Map<String, Object>> checkBlacklist(@RequestParam String identifier) {
        boolean ipBlacklisted = rateLimitingService.isBlacklisted(identifier);
        boolean tokenBlacklisted = blacklistService.isBlacklisted(identifier);
        boolean userBlacklisted = blacklistService.isUserBlacklisted(Long.valueOf(identifier.hashCode() & Long.MAX_VALUE));

        return ResponseEntity.ok(Map.of(
            "identifier", identifier,
            "ipBlacklisted", ipBlacklisted,
            "tokenBlacklisted", tokenBlacklisted,
            "userBlacklisted", userBlacklisted,
            "blacklisted", ipBlacklisted || tokenBlacklisted || userBlacklisted
        ));
    }

    /**
     * 获取熔断器状态
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakers() {
        Map<String, Object> result = new HashMap<>();

        circuitBreakerService.getCircuitBreakers().forEach((serviceName, instance) -> {
            result.put(serviceName, instance.getStatistics());
        });

        return ResponseEntity.ok(Map.of(
            "circuitBreakers", result,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 重置熔断器
     */
    @PostMapping("/circuit-breakers/reset")
    public ResponseEntity<Map<String, Object>> resetCircuitBreaker(@RequestParam String serviceName) {
        circuitBreakerService.reset(serviceName);

        return ResponseEntity.ok(Map.of(
            "serviceName", serviceName,
            "message", "Circuit breaker reset successfully"
        ));
    }

    /**
     * 强制打开熔断器
     */
    @PostMapping("/circuit-breakers/force-open")
    public ResponseEntity<Map<String, Object>> forceOpenCircuitBreaker(@RequestParam String serviceName) {
        circuitBreakerService.forceOpen(serviceName);

        return ResponseEntity.ok(Map.of(
            "serviceName", serviceName,
            "message", "Circuit breaker forced open"
        ));
    }

    /**
     * 强制关闭熔断器
     */
    @PostMapping("/circuit-breakers/force-close")
    public ResponseEntity<Map<String, Object>> forceCloseCircuitBreaker(@RequestParam String serviceName) {
        circuitBreakerService.forceClose(serviceName);

        return ResponseEntity.ok(Map.of(
            "serviceName", serviceName,
            "message", "Circuit breaker forced closed"
        ));
    }

    /**
     * 获取JWT黑名单信息
     */
    @GetMapping("/jwt/blacklist")
    public ResponseEntity<Map<String, Object>> getJwtBlacklistInfo() {
        // 这里可以添加获取JWT黑名单统计的逻辑
        // 由于隐私考虑，不返回具体的token信息

        return ResponseEntity.ok(Map.of(
            "note", "JWT blacklist information is sensitive",
            "operation", "Use specific token check endpoints for verification",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 检查各个组件的健康状态
            auditService.getAuditStatistics();
            health.put("auditService", "UP");
        } catch (Exception e) {
            health.put("auditService", "DOWN: " + e.getMessage());
        }

        try {
            // 检查限流服务
            rateLimitingService.getStatistics("health-check", Duration.ofSeconds(1));
            health.put("rateLimitingService", "UP");
        } catch (Exception e) {
            health.put("rateLimitingService", "DOWN: " + e.getMessage());
        }

        try {
            // 检查熔断器服务
            circuitBreakerService.getCircuitBreakers();
            health.put("circuitBreakerService", "UP");
        } catch (Exception e) {
            health.put("circuitBreakerService", "DOWN: " + e.getMessage());
        }

        try {
            // 检查黑名单服务
            blacklistService.isBlacklisted("health-check");
            health.put("blacklistService", "UP");
        } catch (Exception e) {
            health.put("blacklistService", "DOWN: " + e.getMessage());
        }

        boolean allUp = health.values().stream().allMatch(status -> status.toString().equals("UP"));

        return ResponseEntity.ok(Map.of(
            "status", allUp ? "UP" : "DOWN",
            "components", health,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 安全指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getSecurityMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 基础指标
        metrics.put("timestamp", System.currentTimeMillis());
        metrics.put("uptime", System.currentTimeMillis() - startTime);

        // 审计指标
        var auditStats = auditService.getAuditStatistics();
        metrics.put("auditEvents", auditStats.getOrDefault("totalEvents", 0));

        // 熔断器指标
        long openCircuitBreakers = circuitBreakerService.getCircuitBreakers().values().stream()
                .filter(cb -> cb.getState() == CircuitBreakerService.CircuitBreakerState.OPEN)
                .count();
        metrics.put("openCircuitBreakers", openCircuitBreakers);
        metrics.put("totalCircuitBreakers", circuitBreakerService.getCircuitBreakers().size());

        // 限流指标（采样）
        var sampleStats = rateLimitingService.getStatistics("metrics-sample", Duration.ofMinutes(1));
        metrics.put("sampleRequestsPerMinute", sampleStats.getRequestCount());

        return ResponseEntity.ok(metrics);
    }

    // 记录服务启动时间
    private final long startTime = System.currentTimeMillis();
}