package com.evcs.gateway.monitoring;

import com.evcs.gateway.security.CircuitBreakerService;
import com.evcs.gateway.security.RateLimitingService;
import com.evcs.gateway.security.SecurityAuditService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus指标收集服务
 * 收集和暴露系统运行指标
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusMetricsService {

    private final MeterRegistry meterRegistry;
    private final SecurityAuditService auditService;
    private final RateLimitingService rateLimitingService;
    private final CircuitBreakerService circuitBreakerService;

    // HTTP请求指标
    private Counter httpRequestsTotal;
    private Timer httpRequestTimer;
    private final AtomicLong activeConnections = new AtomicLong(0);

    // 安全相关指标
    private Counter authenticationSuccessCounter;
    private Counter authenticationFailureCounter;
    private Counter rateLimitCounter;
    private Counter suspiciousActivityCounter;
    private Counter accessDeniedCounter;

    // 业务指标
    private Counter chargerConnectionsTotal;
    private Counter chargingSessionsTotal;

    // 系统指标
    private Timer circuitBreakerTimer;

    @PostConstruct
    public void initializeMetrics() {
        initializeHttpMetrics();
        initializeSecurityMetrics();
        initializeBusinessMetrics();
        initializeSystemMetrics();

        log.info("Prometheus metrics initialized successfully");
    }

    /**
     * 初始化HTTP请求指标
     */
    private void initializeHttpMetrics() {
        httpRequestsTotal = Counter.builder("http_requests_total")
                .description("Total number of HTTP requests")
                .tag("application", "evcs-gateway")
                .register(meterRegistry);

        httpRequestTimer = Timer.builder("http_request_duration_seconds")
                .description("HTTP request duration in seconds")
                .tag("application", "evcs-gateway")
                .register(meterRegistry);

        Gauge.builder("http_active_connections")
                .description("Number of active HTTP connections")
                .tag("application", "evcs-gateway")
                .register(meterRegistry, activeConnections, AtomicLong::get);
    }

    /**
     * 初始化安全相关指标
     */
    private void initializeSecurityMetrics() {
        authenticationSuccessCounter = Counter.builder("security_authentication_success_total")
                .description("Total number of successful authentications")
                .tag("application", "evcs-gateway")
                .tag("type", "jwt")
                .register(meterRegistry);

        authenticationFailureCounter = Counter.builder("security_authentication_failure_total")
                .description("Total number of failed authentications")
                .tag("application", "evcs-gateway")
                .tag("type", "jwt")
                .register(meterRegistry);

        rateLimitCounter = Counter.builder("security_rate_limit_total")
                .description("Total number of rate limit violations")
                .tag("application", "evcs-gateway")
                .register(meterRegistry);

        suspiciousActivityCounter = Counter.builder("security_suspicious_activity_total")
                .description("Total number of suspicious activities detected")
                .tag("application", "evcs-gateway")
                .register(meterRegistry);

        accessDeniedCounter = Counter.builder("security_access_denied_total")
                .description("Total number of access denied events")
                .tag("application", "evcs-gateway")
                .register(meterRegistry);
    }

    /**
     * 初始化业务指标
     */
    private void initializeBusinessMetrics() {
        chargerConnectionsTotal = Counter.builder("business_charger_connections_total")
                .description("Total number of charger connections")
                .tag("application", "evcs-gateway")
                .tag("type", "ocpp")
                .register(meterRegistry);

        chargingSessionsTotal = Counter.builder("business_charging_sessions_total")
                .description("Total number of charging sessions")
                .tag("application", "evcs-gateway")
                .register(meterRegistry);

        Gauge.builder("business_active_chargers")
                .description("Number of active chargers")
                .tag("application", "evcs-gateway")
                .register(meterRegistry, this, PrometheusMetricsService::getActiveChargerCount);

        Gauge.builder("business_online_chargers")
                .description("Number of online chargers")
                .tag("application", "evcs-gateway")
                .register(meterRegistry, this, PrometheusMetricsService::getOnlineChargerCount);
    }

    /**
     * 初始化系统指标
     */
    private void initializeSystemMetrics() {
        Gauge.builder("system_circuit_breakers_open")
                .description("Number of open circuit breakers")
                .tag("application", "evcs-gateway")
                .register(meterRegistry, this, PrometheusMetricsService::getOpenCircuitBreakerCount);

        Gauge.builder("system_circuit_breakers_total")
                .description("Total number of circuit breakers")
                .tag("application", "evcs-gateway")
                .register(meterRegistry, this, PrometheusMetricsService::getTotalCircuitBreakerCount);

        circuitBreakerTimer = Timer.builder("system_circuit_breaker_response_time_seconds")
                .description("Circuit breaker response time in seconds")
                .tag("application", "evcs-gateway")
                .register(meterRegistry);
    }

    // HTTP指标记录方法
    public void recordHttpRequest(String method, String path, int statusCode, double duration) {
        Counter.builder("http_requests_total")
                .tag("application", "evcs-gateway")
                .tag("method", method)
                .tag("path", path)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();

        httpRequestTimer.record(Duration.ofMillis((long) (duration * 1000)));
    }

    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    // 安全指标记录方法
    public void recordAuthenticationSuccess() {
        authenticationSuccessCounter.increment();
    }

    public void recordAuthenticationFailure(String reason) {
        Counter.builder("security_authentication_failure_total")
                .tag("application", "evcs-gateway")
                .tag("type", "jwt")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordRateLimitViolation(String type, String identifier) {
        Counter.builder("security_rate_limit_total")
                .tag("application", "evcs-gateway")
                .tag("type", type)
                .tag("identifier", identifier)
                .register(meterRegistry)
                .increment();
    }

    public void recordSuspiciousActivity(String activity) {
        Counter.builder("security_suspicious_activity_total")
                .tag("application", "evcs-gateway")
                .tag("activity", activity)
                .register(meterRegistry)
                .increment();
    }

    public void recordAccessDenied(String reason) {
        Counter.builder("security_access_denied_total")
                .tag("application", "evcs-gateway")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    // 业务指标记录方法
    public void recordChargerConnection(String protocol) {
        Counter.builder("business_charger_connections_total")
                .tag("application", "evcs-gateway")
                .tag("type", "ocpp")
                .tag("protocol", protocol)
                .register(meterRegistry)
                .increment();
    }

    public void recordChargingSession(String status) {
        Counter.builder("business_charging_sessions_total")
                .tag("application", "evcs-gateway")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    // 系统指标记录方法
    public void recordCircuitBreakerOperation(String serviceName, String operation, double duration) {
        circuitBreakerTimer.record(Duration.ofMillis((long) (duration * 1000)));
    }

    // 获取指标值的辅助方法
    private double getActiveChargerCount() {
        try {
            // 这里应该从实际的服务获取数据
            // 临时返回模拟数据
            return 0.0;
        } catch (Exception e) {
            log.debug("Error getting active charger count", e);
            return 0.0;
        }
    }

    private double getOnlineChargerCount() {
        try {
            // 这里应该从实际的服务获取数据
            // 临时返回模拟数据
            return 0.0;
        } catch (Exception e) {
            log.debug("Error getting online charger count", e);
            return 0.0;
        }
    }

    private double getOpenCircuitBreakerCount() {
        try {
            return circuitBreakerService.getCircuitBreakers().values().stream()
                    .filter(cb -> cb.getState() == CircuitBreakerService.CircuitBreakerState.OPEN)
                    .count();
        } catch (Exception e) {
            log.debug("Error getting open circuit breaker count", e);
            return 0.0;
        }
    }

    private double getTotalCircuitBreakerCount() {
        try {
            return circuitBreakerService.getCircuitBreakers().size();
        } catch (Exception e) {
            log.debug("Error getting total circuit breaker count", e);
            return 0.0;
        }
    }

    /**
     * 自定义指标收集
     */
    public void collectCustomMetrics() {
        try {
            // 收集审计事件统计
            var auditStats = auditService.getAuditStatistics();
            Gauge.builder("security_audit_events_total")
                    .description("Total number of security audit events")
                    .tag("application", "evcs-gateway")
                    .register(meterRegistry, () -> {
                        try {
                            Object totalEvents = auditStats.get("totalEvents");
                            return totalEvents instanceof Number ? ((Number) totalEvents).doubleValue() : 0.0;
                        } catch (Exception e) {
                            return 0.0;
                        }
                    });

            // 收集限流统计
            var rateLimitStats = rateLimitingService.getStatistics("prometheus-sample", Duration.ofMinutes(1));
            Gauge.builder("security_rate_limit_requests")
                    .description("Number of requests in rate limit window")
                    .tag("application", "evcs-gateway")
                    .register(meterRegistry, () -> (double) rateLimitStats.getRequestCount());

        } catch (Exception e) {
            log.error("Error collecting custom metrics", e);
        }
    }

    /**
     * 获取指标摘要
     */
    public MetricsSummary getMetricsSummary() {
        return MetricsSummary.builder()
                .totalHttpRequests((int) httpRequestsTotal.count())
                .activeConnections(activeConnections.get())
                .authenticationSuccesses((int) authenticationSuccessCounter.count())
                .authenticationFailures((int) authenticationFailureCounter.count())
                .rateLimitViolations((int) rateLimitCounter.count())
                .suspiciousActivities((int) suspiciousActivityCounter.count())
                .openCircuitBreakers((int) getOpenCircuitBreakerCount())
                .totalCircuitBreakers((int) getTotalCircuitBreakerCount())
                .activeChargers((int) getActiveChargerCount())
                .onlineChargers((int) getOnlineChargerCount())
                .build();
    }

    /**
     * 指标摘要数据类
     */
    @lombok.Builder
    @lombok.Data
    public static class MetricsSummary {
        private int totalHttpRequests;
        private long activeConnections;
        private int authenticationSuccesses;
        private int authenticationFailures;
        private int rateLimitViolations;
        private int suspiciousActivities;
        private int openCircuitBreakers;
        private int totalCircuitBreakers;
        private int activeChargers;
        private int onlineChargers;
    }
}