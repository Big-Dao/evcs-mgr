package com.evcs.gateway.monitoring;

import com.evcs.gateway.security.CircuitBreakerService;
import com.evcs.gateway.security.JwtBlacklistService;
import com.evcs.gateway.security.RateLimitingService;
import com.evcs.gateway.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 健康检查服务
 * 提供系统各组件的健康状态检查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final SecurityAuditService auditService;
    private final RateLimitingService rateLimitingService;
    private final CircuitBreakerService circuitBreakerService;
    private final JwtBlacklistService blacklistService;
    private final RedisTemplate<String, String> redisTemplate;

    // 记录服务启动时间
    private final LocalDateTime startTime = LocalDateTime.now();

    // 健康检查结果缓存
    private final Map<String, HealthCheckResult> healthCheckCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastCheckTime = new ConcurrentHashMap<>();

    // 健康检查缓存TTL（秒）
    private static final int HEALTH_CHECK_CACHE_TTL = 30;

    /**
     * 系统健康检查
     */
    public HealthCheckResult performSystemHealthCheck() {
        String cacheKey = "system";

        // 检查缓存
        HealthCheckResult cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        Map<String, ComponentHealth> components = new HashMap<>();
        boolean allHealthy = true;

        // 检查Redis连接
        ComponentHealth redisHealth = checkRedisHealth();
        components.put("redis", redisHealth);
        if (!redisHealth.isHealthy()) {
            allHealthy = false;
        }

        // 检查审计服务
        ComponentHealth auditHealth = checkAuditServiceHealth();
        components.put("auditService", auditHealth);
        if (!auditHealth.isHealthy()) {
            allHealthy = false;
        }

        // 检查限流服务
        ComponentHealth rateLimitHealth = checkRateLimitServiceHealth();
        components.put("rateLimitService", rateLimitHealth);
        if (!rateLimitHealth.isHealthy()) {
            allHealthy = false;
        }

        // 检查熔断器服务
        ComponentHealth circuitBreakerHealth = checkCircuitBreakerServiceHealth();
        components.put("circuitBreakerService", circuitBreakerHealth);
        if (!circuitBreakerHealth.isHealthy()) {
            allHealthy = false;
        }

        // 检查黑名单服务
        ComponentHealth blacklistHealth = checkBlacklistServiceHealth();
        components.put("blacklistService", blacklistHealth);
        if (!blacklistHealth.isHealthy()) {
            allHealthy = false;
        }

        // 检查磁盘空间
        ComponentHealth diskHealth = checkDiskHealth();
        components.put("disk", diskHealth);
        if (!diskHealth.isHealthy()) {
            allHealthy = false;
        }

        // 检查内存使用
        ComponentHealth memoryHealth = checkMemoryHealth();
        components.put("memory", memoryHealth);
        if (!memoryHealth.isHealthy()) {
            allHealthy = false;
        }

        HealthCheckResult result = HealthCheckResult.builder()
                .status(allHealthy ? "UP" : "DOWN")
                .components(components)
                .uptime(getUptime())
                .timestamp(LocalDateTime.now())
                .build();

        // 缓存结果
        cacheResult(cacheKey, result);

        return result;
    }

    /**
     * Redis健康检查
     */
    private ComponentHealth checkRedisHealth() {
        try {
            String testKey = "health_check_" + System.currentTimeMillis();
            String testValue = "ping";

            // 写入测试
            redisTemplate.opsForValue().set(testKey, testValue, Duration.ofSeconds(10));

            // 读取测试
            String readValue = redisTemplate.opsForValue().get(testKey);

            // 清理测试数据
            redisTemplate.delete(testKey);

            if (testValue.equals(readValue)) {
                return ComponentHealth.builder()
                        .status("UP")
                        .details(Map.of("message", "Redis connection is healthy"))
                        .build();
            } else {
                return ComponentHealth.builder()
                        .status("DOWN")
                        .details(Map.of("message", "Redis read/write test failed"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return ComponentHealth.builder()
                    .status("DOWN")
                    .details(Map.of("message", "Redis connection failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 审计服务健康检查
     */
    private ComponentHealth checkAuditServiceHealth() {
        try {
            var statistics = auditService.getAuditStatistics();
            if (statistics.containsKey("error")) {
                return ComponentHealth.builder()
                        .status("DOWN")
                        .details(Map.of("message", "Audit service returned error", "error", statistics.get("error")))
                        .build();
            }

            return ComponentHealth.builder()
                    .status("UP")
                    .details(Map.of("message", "Audit service is healthy", "totalEvents", statistics.get("totalEvents")))
                    .build();
        } catch (Exception e) {
            log.error("Audit service health check failed", e);
            return ComponentHealth.builder()
                    .status("DOWN")
                    .details(Map.of("message", "Audit service failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 限流服务健康检查
     */
    private ComponentHealth checkRateLimitServiceHealth() {
        try {
            String testKey = "health_check_rate_limit";
            var stats = rateLimitingService.getStatistics(testKey, Duration.ofSeconds(1));

            return ComponentHealth.builder()
                    .status("UP")
                    .details(Map.of("message", "Rate limiting service is healthy", "requestCount", stats.getRequestCount()))
                    .build();
        } catch (Exception e) {
            log.error("Rate limiting service health check failed", e);
            return ComponentHealth.builder()
                    .status("DOWN")
                    .details(Map.of("message", "Rate limiting service failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 熔断器服务健康检查
     */
    private ComponentHealth checkCircuitBreakerServiceHealth() {
        try {
            int totalCircuitBreakers = circuitBreakerService.getCircuitBreakers().size();
            long openCircuitBreakers = circuitBreakerService.getCircuitBreakers().values().stream()
                    .filter(cb -> cb.getState() == CircuitBreakerService.CircuitBreakerState.OPEN)
                    .count();

            String status = (openCircuitBreakers > totalCircuitBreakers * 0.5) ? "DEGRADED" : "UP";

            return ComponentHealth.builder()
                    .status(status)
                    .details(Map.of(
                        "message", "Circuit breaker service is " + status,
                        "totalCircuitBreakers", totalCircuitBreakers,
                        "openCircuitBreakers", openCircuitBreakers
                    ))
                    .build();
        } catch (Exception e) {
            log.error("Circuit breaker service health check failed", e);
            return ComponentHealth.builder()
                    .status("DOWN")
                    .details(Map.of("message", "Circuit breaker service failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 黑名单服务健康检查
     */
    private ComponentHealth checkBlacklistServiceHealth() {
        try {
            String testToken = "test_token_" + System.currentTimeMillis();
            boolean isBlacklisted = blacklistService.isBlacklisted(testToken);

            // 测试token应该不在黑名单中
            if (!isBlacklisted) {
                return ComponentHealth.builder()
                        .status("UP")
                        .details(Map.of("message", "Blacklist service is healthy"))
                        .build();
            } else {
                return ComponentHealth.builder()
                        .status("DEGRADED")
                        .details(Map.of("message", "Blacklist service returned unexpected result"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Blacklist service health check failed", e);
            return ComponentHealth.builder()
                    .status("DOWN")
                    .details(Map.of("message", "Blacklist service failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 磁盘健康检查
     */
    private ComponentHealth checkDiskHealth() {
        try {
            java.io.File disk = new java.io.File("/");
            long freeSpace = disk.getFreeSpace();
            long totalSpace = disk.getTotalSpace();
            double freePercentage = (double) freeSpace / totalSpace * 100;

            String status = freePercentage < 10 ? "DOWN" : (freePercentage < 20 ? "DEGRADED" : "UP");

            return ComponentHealth.builder()
                    .status(status)
                    .details(Map.of(
                        "message", String.format("Disk %s (%.2f%% free)", status.toLowerCase(), freePercentage),
                        "freeSpace", freeSpace,
                        "totalSpace", totalSpace,
                        "freePercentage", String.format("%.2f%%", freePercentage)
                    ))
                    .build();
        } catch (Exception e) {
            log.error("Disk health check failed", e);
            return ComponentHealth.builder()
                    .status("DOWN")
                    .details(Map.of("message", "Disk health check failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 内存健康检查
     */
    private ComponentHealth checkMemoryHealth() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            double usedPercentage = (double) usedMemory / maxMemory * 100;

            String status = usedPercentage > 90 ? "DOWN" : (usedPercentage > 75 ? "DEGRADED" : "UP");

            return ComponentHealth.builder()
                    .status(status)
                    .details(Map.of(
                        "message", String.format("Memory %s (%.2f%% used)", status.toLowerCase(), usedPercentage),
                        "usedMemory", usedMemory,
                        "totalMemory", totalMemory,
                        "maxMemory", maxMemory,
                        "usedPercentage", String.format("%.2f%%", usedPercentage)
                    ))
                    .build();
        } catch (Exception e) {
            log.error("Memory health check failed", e);
            return ComponentHealth.builder()
                    .status("DOWN")
                    .details(Map.of("message", "Memory health check failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 异步健康检查
     */
    public CompletableFuture<HealthCheckResult> performAsyncHealthCheck() {
        return CompletableFuture.supplyAsync(this::performSystemHealthCheck);
    }

    /**
     * 获取运行时间
     */
    private String getUptime() {
        Duration uptime = Duration.between(startTime, LocalDateTime.now());
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        return String.format("%d days, %02d:%02d:%02d", days, hours, minutes, seconds);
    }

    /**
     * 获取缓存的检查结果
     */
    private HealthCheckResult getCachedResult(String key) {
        LocalDateTime lastTime = lastCheckTime.get(key);
        if (lastTime != null && ChronoUnit.SECONDS.between(lastTime, LocalDateTime.now()) < HEALTH_CHECK_CACHE_TTL) {
            return healthCheckCache.get(key);
        }
        return null;
    }

    /**
     * 缓存检查结果
     */
    private void cacheResult(String key, HealthCheckResult result) {
        healthCheckCache.put(key, result);
        lastCheckTime.put(key, LocalDateTime.now());
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        healthCheckCache.clear();
        lastCheckTime.clear();
    }

    /**
     * 组件健康状态
     */
    @lombok.Builder
    @lombok.Data
    public static class ComponentHealth {
        private String status; // UP, DOWN, DEGRADED
        private Map<String, Object> details;
        private boolean isHealthy() {
            return "UP".equals(status);
        }
    }

    /**
     * 健康检查结果
     */
    @lombok.Builder
    @lombok.Data
    public static class HealthCheckResult {
        private String status; // UP, DOWN, DEGRADED
        private Map<String, ComponentHealth> components;
        private String uptime;
        private LocalDateTime timestamp;

        public boolean isHealthy() {
            return "UP".equals(status);
        }

        public boolean isDegraded() {
            return "DEGRADED".equals(status);
        }
    }
}