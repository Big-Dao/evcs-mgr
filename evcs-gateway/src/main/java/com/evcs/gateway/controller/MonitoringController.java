package com.evcs.gateway.controller;

import com.evcs.gateway.monitoring.HealthCheckService;
import com.evcs.gateway.monitoring.PrometheusMetricsService;
import com.evcs.gateway.security.CircuitBreakerService;
import com.evcs.gateway.security.JwtBlacklistService;
import com.evcs.gateway.security.RateLimitingService;
import com.evcs.gateway.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 监控控制器
 * 提供系统监控和健康检查API
 */
@Slf4j
@RestController
@RequestMapping("/actuator")
@RequiredArgsConstructor
public class MonitoringController implements HealthIndicator {

    private final HealthCheckService healthCheckService;
    private final PrometheusMetricsService metricsService;
    private final SecurityAuditService auditService;
    private final RateLimitingService rateLimitingService;
    private final CircuitBreakerService circuitBreakerService;
    private final JwtBlacklistService blacklistService;

    /**
     * Spring Boot Actuator健康检查
     */
    @Override
    public Health health() {
        try {
            var healthResult = healthCheckService.performSystemHealthCheck();

            Health.Builder builder = healthResult.isHealthy() ? Health.up() : Health.down();

            if (healthResult.isDegraded()) {
                builder.status("DEGRADED");
            }

            builder.withDetail("components", healthResult.getComponents())
                   .withDetail("uptime", healthResult.getUptime())
                   .withDetail("timestamp", healthResult.getTimestamp());

            return builder.build();

        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<HealthCheckService.HealthCheckResult> getDetailedHealth() {
        try {
            var healthResult = healthCheckService.performSystemHealthCheck();
            return ResponseEntity.ok(healthResult);
        } catch (Exception e) {
            log.error("Detailed health check failed", e);
            return ResponseEntity.internalServerError()
                    .body(HealthCheckService.HealthCheckResult.builder()
                            .status("DOWN")
                            .components(Map.of("system", HealthCheckService.ComponentHealth.builder()
                                    .status("DOWN")
                                    .details(Map.of("error", e.getMessage()))
                                    .build()))
                            .uptime("Unknown")
                            .timestamp(java.time.LocalDateTime.now())
                            .build());
        }
    }

    /**
     * 异步健康检查
     */
    @GetMapping("/health/async")
    public CompletableFuture<ResponseEntity<HealthCheckService.HealthCheckResult>> getAsyncHealth() {
        return healthCheckService.performAsyncHealthCheck()
                .thenApply(healthResult -> {
                    if (healthResult.isHealthy()) {
                        return ResponseEntity.ok(healthResult);
                    } else if (healthResult.isDegraded()) {
                        return ResponseEntity.status(206).body(healthResult); // 206 Partial Content
                    } else {
                        return ResponseEntity.status(503).body(healthResult); // 503 Service Unavailable
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Async health check failed", throwable);
                    return ResponseEntity.internalServerError()
                            .body(HealthCheckService.HealthCheckResult.builder()
                                    .status("DOWN")
                                    .components(Map.of("system", HealthCheckService.ComponentHealth.builder()
                                            .status("DOWN")
                                            .details(Map.of("error", throwable.getMessage()))
                                            .build()))
                                    .uptime("Unknown")
                                    .timestamp(java.time.LocalDateTime.now())
                                    .build());
                });
    }

    /**
     * 清理健康检查缓存
     */
    @PostMapping("/health/clear-cache")
    public ResponseEntity<Map<String, Object>> clearHealthCache() {
        try {
            healthCheckService.clearCache();
            return ResponseEntity.ok(Map.of(
                    "message", "Health check cache cleared successfully",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Failed to clear health cache", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Failed to clear health cache: " + e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    /**
     * 获取Prometheus指标
     */
    @GetMapping("/prometheus")
    public ResponseEntity<String> getPrometheusMetrics() {
        try {
            // 触发自定义指标收集
            metricsService.collectCustomMetrics();

            // 这里应该返回Prometheus格式的指标
            // 由于我们使用的是Micrometer，指标会通过/actuator/prometheus端点自动暴露
            // 这里返回一个简单的指标摘要
            var summary = metricsService.getMetricsSummary();

            StringBuilder metrics = new StringBuilder();
            metrics.append("# HELP evcs_gateway_metrics_summary System metrics summary\n");
            metrics.append("# TYPE evcs_gateway_metrics_summary gauge\n");
            metrics.append("evcs_gateway_metrics_summary{status=\"total_requests\"} ").append(summary.getTotalHttpRequests()).append("\n");
            metrics.append("evcs_gateway_metrics_summary{status=\"active_connections\"} ").append(summary.getActiveConnections()).append("\n");
            metrics.append("evcs_gateway_metrics_summary{status=\"auth_successes\"} ").append(summary.getAuthenticationSuccesses()).append("\n");
            metrics.append("evcs_gateway_metrics_summary{status=\"auth_failures\"} ").append(summary.getAuthenticationFailures()).append("\n");
            metrics.append("evcs_gateway_metrics_summary{status=\"rate_limit_violations\"} ").append(summary.getRateLimitViolations()).append("\n");
            metrics.append("evcs_gateway_metrics_summary{status=\"suspicious_activities\"} ").append(summary.getSuspiciousActivities()).append("\n");
            metrics.append("evcs_gateway_metrics_summary{status=\"open_circuit_breakers\"} ").append(summary.getOpenCircuitBreakers()).append("\n");
            metrics.append("evcs_gateway_metrics_summary{status=\"total_circuit_breakers\"} ").append(summary.getTotalCircuitBreakers()).append("\n");

            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; version=0.0.4")
                    .body(metrics.toString());

        } catch (Exception e) {
            log.error("Failed to generate Prometheus metrics", e);
            return ResponseEntity.internalServerError()
                    .body("# ERROR generating metrics: " + e.getMessage());
        }
    }

    /**
     * 获取指标摘要
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<PrometheusMetricsService.MetricsSummary> getMetricsSummary() {
        try {
            var summary = metricsService.getMetricsSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Failed to get metrics summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            // 应用信息
            info.put("application", Map.of(
                    "name", "EVCS Gateway",
                    "version", "1.0.0",
                    "description", "Electric Vehicle Charging Station Management Gateway",
                    "uptime", getUptime()
            ));

            // 运行时信息
            Runtime runtime = Runtime.getRuntime();
            info.put("runtime", Map.of(
                    "javaVersion", System.getProperty("java.version"),
                    "javaVendor", System.getProperty("java.vendor"),
                    "osName", System.getProperty("os.name"),
                    "osVersion", System.getProperty("os.version"),
                    "availableProcessors", runtime.availableProcessors(),
                    "maxMemory", runtime.maxMemory(),
                    "totalMemory", runtime.totalMemory(),
                    "freeMemory", runtime.freeMemory()
            ));

            // 系统状态
            var healthResult = healthCheckService.performSystemHealthCheck();
            info.put("health", Map.of(
                    "status", healthResult.getStatus(),
                    "components", healthResult.getComponents().size()
            ));

            // 监控状态
            var metricsSummary = metricsService.getMetricsSummary();
            info.put("metrics", Map.of(
                    "totalRequests", metricsSummary.getTotalHttpRequests(),
                    "activeConnections", metricsSummary.getActiveConnections(),
                    "rateLimitViolations", metricsSummary.getRateLimitViolations()
            ));

            info.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            log.error("Failed to get system info", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    /**
     * 获取安全监控指标
     */
    @GetMapping("/gateway/security/metrics")
    public ResponseEntity<Map<String, Object>> getSecurityMetrics() {
        try {
            Map<String, Object> securityMetrics = new HashMap<>();

            // 审计指标
            var auditStats = auditService.getAuditStatistics();
            securityMetrics.put("audit", auditStats);

            // 限流指标
            var rateLimitStats = rateLimitingService.getStatistics("security-metrics", Duration.ofMinutes(5));
            securityMetrics.put("rateLimit", Map.of(
                    "requestCount", rateLimitStats.getRequestCount(),
                    "window", "5 minutes"
            ));

            // 熔断器指标
            Map<String, Object> circuitBreakerMetrics = new HashMap<>();
            circuitBreakerService.getCircuitBreakers().forEach((serviceName, instance) -> {
                var stats = instance.getStatistics();
                circuitBreakerMetrics.put(serviceName, Map.of(
                        "state", stats.getState(),
                        "failureCount", stats.getFailureCount(),
                        "successCount", stats.getSuccessCount(),
                        "requestCount", stats.getRequestCount(),
                        "failureRate", String.format("%.2f%%", stats.getFailureRate() * 100)
                ));
            });
            securityMetrics.put("circuitBreakers", circuitBreakerMetrics);

            securityMetrics.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(securityMetrics);

        } catch (Exception e) {
            log.error("Failed to get security metrics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    /**
     * 性能监控
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            Map<String, Object> performance = new HashMap<>();

            // JVM性能指标
            Runtime runtime = Runtime.getRuntime();
            performance.put("jvm", Map.of(
                    "memoryUsage", Map.of(
                            "used", runtime.totalMemory() - runtime.freeMemory(),
                            "free", runtime.freeMemory(),
                            "total", runtime.totalMemory(),
                            "max", runtime.maxMemory(),
                            "usagePercentage", String.format("%.2f%%",
                                    ((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100)
                    ),
                    "availableProcessors", runtime.availableProcessors()
            ));

            // 系统性能指标
            com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();

            performance.put("system", Map.of(
                    "systemLoadAverage", osBean.getSystemLoadAverage(),
                    "processCpuLoad", String.format("%.2f%%", osBean.getProcessCpuLoad() * 100),
                    "systemCpuLoad", String.format("%.2f%%", osBean.getSystemCpuLoad() * 100),
                    "totalPhysicalMemorySize", osBean.getTotalPhysicalMemorySize(),
                    "freePhysicalMemorySize", osBean.getFreePhysicalMemorySize()
            ));

            performance.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(performance);

        } catch (Exception e) {
            log.error("Failed to get performance metrics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    /**
     * 获取运行时间
     */
    private String getUptime() {
        // 这里应该从实际的启动时间计算
        // 临时返回模拟数据
        return "0 days, 00:00:00";
    }
}