package com.evcs.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 安全的监控控制器
 * 避免所有阻塞操作，使用响应式编程模型
 */
@Slf4j
@RestController
@RequestMapping("/actuator")
@RequiredArgsConstructor
public class SafeMonitoringController implements HealthIndicator {

    // 内存中的统计数据（避免阻塞的数据库或Redis操作）
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong authenticationSuccesses = new AtomicLong(0);
    private final AtomicLong authenticationFailures = new AtomicLong(0);
    private final AtomicLong rateLimitViolations = new AtomicLong(0);

    private final AtomicReference<Instant> startTime = new AtomicReference<>(Instant.now());

    /**
     * Spring Boot Actuator健康检查
     */
    @Override
    public Health health() {
        try {
            long uptime = Duration.between(startTime.get(), Instant.now()).toSeconds();

            return Health.up()
                    .withDetail("uptime", formatUptime(uptime))
                    .withDetail("totalRequests", totalRequests.get())
                    .withDetail("activeConnections", activeConnections.get())
                    .withDetail("authenticationSuccesses", authenticationSuccesses.get())
                    .withDetail("authenticationFailures", authenticationFailures.get())
                    .withDetail("rateLimitViolations", rateLimitViolations.get())
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
    }

    /**
     * 系统信息端点
     */
    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> getSystemInfo() {
        return Mono.fromCallable(() -> {
            Map<String, Object> info = new HashMap<>();

            // 应用信息
            info.put("application", Map.of(
                "name", "EVCS Gateway",
                "version", "1.0.0",
                "description", "Electric Vehicle Charging Station Management Gateway",
                "uptime", formatUptime(Duration.between(startTime.get(), Instant.now()).toSeconds())
            ));

            // 运行时信息（不进行阻塞的I/O操作）
            Runtime runtime = Runtime.getRuntime();
            info.put("runtime", Map.of(
                "javaVersion", System.getProperty("java.version"),
                "javaVendor", System.getProperty("java.vendor"),
                "osName", System.getProperty("os.name"),
                "osVersion", System.getProperty("os.version"),
                "availableProcessors", runtime.availableProcessors(),
                "maxMemory", runtime.maxMemory(),
                "totalMemory", runtime.totalMemory(),
                "freeMemory", runtime.freeMemory(),
                "usedMemoryPercentage", String.format("%.2f%%",
                    ((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100)
            ));

            // 统计信息
            info.put("statistics", Map.of(
                "totalRequests", totalRequests.get(),
                "activeConnections", activeConnections.get(),
                "authenticationSuccesses", authenticationSuccesses.get(),
                "authenticationFailures", authenticationFailures.get(),
                "rateLimitViolations", rateLimitViolations.get()
            ));

            info.put("timestamp", Instant.now().toString());

            return info;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.internalServerError().build());
    }

    /**
     * 获取请求统计
     */
    @GetMapping("/statistics")
    public Mono<ResponseEntity<Map<String, Object>>> getRequestStatistics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();

            // 请求统计
            stats.put("requests", Map.of(
                "total", totalRequests.get(),
                "active", activeConnections.get()
            ));

            // 认证统计
            long totalAuthAttempts = authenticationSuccesses.get() + authenticationFailures.get();
            double authSuccessRate = totalAuthAttempts > 0 ?
                (double) authenticationSuccesses.get() / totalAuthAttempts * 100 : 0;

            stats.put("authentication", Map.of(
                "successes", authenticationSuccesses.get(),
                "failures", authenticationFailures.get(),
                "totalAttempts", totalAuthAttempts,
                "successRate", String.format("%.2f%%", authSuccessRate)
            ));

            // 安全统计
            stats.put("security", Map.of(
                "rateLimitViolations", rateLimitViolations.get()
            ));

            // 运行时统计
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsagePercentage = (double) usedMemory / runtime.maxMemory() * 100;

            stats.put("runtime", Map.of(
                "memoryUsagePercentage", String.format("%.2f%%", memoryUsagePercentage),
                "availableProcessors", runtime.availableProcessors(),
                "uptime", formatUptime(Duration.between(startTime.get(), Instant.now()).toSeconds())
            ));

            stats.put("timestamp", Instant.now().toString());

            return stats;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.internalServerError().build());
    }

    /**
     * 增量统计数据（供过滤器调用）
     */
    public void incrementTotalRequests() {
        totalRequests.incrementAndGet();
    }

    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    public void incrementAuthenticationSuccesses() {
        authenticationSuccesses.incrementAndGet();
    }

    public void incrementAuthenticationFailures() {
        authenticationFailures.incrementAndGet();
    }

    public void incrementRateLimitViolations() {
        rateLimitViolations.incrementAndGet();
    }

    /**
     * 重置统计数据
     */
    @PostMapping("/statistics/reset")
    public Mono<ResponseEntity<Map<String, Object>>> resetStatistics() {
        return Mono.fromCallable(() -> {
            totalRequests.set(0);
            activeConnections.set(0);
            authenticationSuccesses.set(0);
            authenticationFailures.set(0);
            rateLimitViolations.set(0);

            Map<String, Object> result = Map.of(
                "message", "统计数据已重置",
                "timestamp", Instant.now().toString()
            );

            return result;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.internalServerError().build());
    }

    /**
     * 性能指标（简化版本，不进行阻塞操作）
     */
    @GetMapping("/metrics/simple")
    public Mono<ResponseEntity<Map<String, Object>>> getSimpleMetrics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> metrics = new HashMap<>();

            // JVM内存指标
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();

            metrics.put("memory", Map.of(
                "used", usedMemory,
                "free", runtime.freeMemory(),
                "total", runtime.totalMemory(),
                "max", runtime.maxMemory(),
                "usagePercentage", String.format("%.2f%%", (double) usedMemory / runtime.maxMemory() * 100)
            ));

            // 线程信息
            metrics.put("threads", Map.of(
                "activeThreadCount", Thread.activeCount(),
                "availableProcessors", runtime.availableProcessors()
            ));

            // 请求统计
            metrics.put("requests", Map.of(
                "total", totalRequests.get(),
                "active", activeConnections.get()
            ));

            // 认证统计
            metrics.put("authentication", Map.of(
                "successes", authenticationSuccesses.get(),
                "failures", authenticationFailures.get()
            ));

            metrics.put("timestamp", Instant.now().toString());

            return metrics;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.internalServerError().build());
    }

    /**
     * 健康检查详细版本
     */
    @GetMapping("/health/detailed")
    public Mono<ResponseEntity<Health>> getDetailedHealth() {
        return Mono.fromCallable(() -> {
            try {
                long uptimeSeconds = Duration.between(startTime.get(), Instant.now()).toSeconds();

                Health.Builder builder = Health.up();

                // 添加详细组件信息
                builder.withDetail("uptime", formatUptime(uptimeSeconds))
                      .withDetail("statistics", Map.of(
                          "totalRequests", totalRequests.get(),
                          "activeConnections", activeConnections.get(),
                          "authenticationSuccesses", authenticationSuccesses.get(),
                          "authenticationFailures", authenticationFailures.get()
                      ))
                      .withDetail("runtime", Map.of(
                          "startTime", startTime.get().toString(),
                          "currentTime", Instant.now().toString()
                      ));

                return builder.build();
            } catch (Exception e) {
                log.error("详细健康检查失败", e);
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .withDetail("timestamp", Instant.now().toString())
                        .build();
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(500).body(Health.down().build()));
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("%d days, %02d:%02d:%02d", days, hours, minutes, secs);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
    }
}