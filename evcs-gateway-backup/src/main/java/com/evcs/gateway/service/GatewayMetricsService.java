package com.evcs.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 网关指标收集服务
 * 提供线程安全的统计数据收集功能
 */
@Slf4j
@Service
public class GatewayMetricsService {

    // 请求统计
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong completedRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);

    // 认证统计
    private final AtomicLong authenticationAttempts = new AtomicLong(0);
    private final AtomicLong authenticationSuccesses = new AtomicLong(0);
    private final AtomicLong authenticationFailures = new AtomicLong(0);

    // 安全统计
    private final AtomicLong rateLimitViolations = new AtomicLong(0);
    private final AtomicLong suspiciousActivities = new AtomicLong(0);

    // 路由统计
    private final AtomicReference<Instant> startTime = new AtomicReference<>(Instant.now());

    /**
     * 记录请求开始
     */
    public void recordRequestStart() {
        totalRequests.incrementAndGet();
        activeConnections.incrementAndGet();
    }

    /**
     * 记录请求完成
     */
    public void recordRequestComplete(boolean success) {
        completedRequests.incrementAndGet();
        if (!success) {
            failedRequests.incrementAndGet();
        }
        activeConnections.decrementAndGet();
    }

    /**
     * 记录认证尝试
     */
    public void recordAuthenticationAttempt() {
        authenticationAttempts.incrementAndGet();
    }

    /**
     * 记录认证成功
     */
    public void recordAuthenticationSuccess() {
        authenticationSuccesses.incrementAndGet();
    }

    /**
     * 记录认证失败
     */
    public void recordAuthenticationFailure() {
        authenticationFailures.incrementAndGet();
    }

    /**
     * 记录速率限制违规
     */
    public void recordRateLimitViolation() {
        rateLimitViolations.incrementAndGet();
    }

    /**
     * 记录可疑活动
     */
    public void recordSuspiciousActivity() {
        suspiciousActivities.incrementAndGet();
    }

    /**
     * 获取请求统计
     */
    public RequestStats getRequestStats() {
        return RequestStats.builder()
                .totalRequests(totalRequests.get())
                .activeConnections(activeConnections.get())
                .completedRequests(completedRequests.get())
                .failedRequests(failedRequests.get())
                .successRate(calculateSuccessRate())
                .build();
    }

    /**
     * 获取认证统计
     */
    public AuthenticationStats getAuthenticationStats() {
        return AuthenticationStats.builder()
                .attempts(authenticationAttempts.get())
                .successes(authenticationSuccesses.get())
                .failures(authenticationFailures.get())
                .successRate(calculateAuthSuccessRate())
                .build();
    }

    /**
     * 获取安全统计
     */
    public SecurityStats getSecurityStats() {
        return SecurityStats.builder()
                .rateLimitViolations(rateLimitViolations.get())
                .suspiciousActivities(suspiciousActivities.get())
                .build();
    }

    /**
     * 获取系统运行时间
     */
    public Duration getUptime() {
        return Duration.between(startTime.get(), Instant.now());
    }

    /**
     * 重置所有统计数据
     */
    public void resetAllStats() {
        totalRequests.set(0);
        activeConnections.set(0);
        completedRequests.set(0);
        failedRequests.set(0);
        authenticationAttempts.set(0);
        authenticationSuccesses.set(0);
        authenticationFailures.set(0);
        rateLimitViolations.set(0);
        suspiciousActivities.set(0);
        startTime.set(Instant.now());

        log.info("所有统计数据已重置");
    }

    /**
     * 计算成功率
     */
    private double calculateSuccessRate() {
        long completed = completedRequests.get();
        long failed = failedRequests.get();
        long total = completed + failed;

        if (total == 0) {
            return 100.0;
        }

        return (double) completed / total * 100;
    }

    /**
     * 计算认证成功率
     */
    private double calculateAuthSuccessRate() {
        long successes = authenticationSuccesses.get();
        long attempts = authenticationAttempts.get();

        if (attempts == 0) {
            return 100.0;
        }

        return (double) successes / attempts * 100;
    }

    /**
     * 请求统计数据
     */
    public static class RequestStats {
        private final long totalRequests;
        private final long activeConnections;
        private final long completedRequests;
        private final long failedRequests;
        private final double successRate;

        private RequestStats(Builder builder) {
            this.totalRequests = builder.totalRequests;
            this.activeConnections = builder.activeConnections;
            this.completedRequests = builder.completedRequests;
            this.failedRequests = builder.failedRequests;
            this.successRate = builder.successRate;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getTotalRequests() { return totalRequests; }
        public long getActiveConnections() { return activeConnections; }
        public long getCompletedRequests() { return completedRequests; }
        public long getFailedRequests() { return failedRequests; }
        public double getSuccessRate() { return successRate; }

        public static class Builder {
            private long totalRequests;
            private long activeConnections;
            private long completedRequests;
            private long failedRequests;
            private double successRate;

            public Builder totalRequests(long totalRequests) {
                this.totalRequests = totalRequests;
                return this;
            }

            public Builder activeConnections(long activeConnections) {
                this.activeConnections = activeConnections;
                return this;
            }

            public Builder completedRequests(long completedRequests) {
                this.completedRequests = completedRequests;
                return this;
            }

            public Builder failedRequests(long failedRequests) {
                this.failedRequests = failedRequests;
                return this;
            }

            public Builder successRate(double successRate) {
                this.successRate = successRate;
                return this;
            }

            public RequestStats build() {
                return new RequestStats(this);
            }
        }
    }

    /**
     * 认证统计数据
     */
    public static class AuthenticationStats {
        private final long attempts;
        private final long successes;
        private final long failures;
        private final double successRate;

        private AuthenticationStats(Builder builder) {
            this.attempts = builder.attempts;
            this.successes = builder.successes;
            this.failures = builder.failures;
            this.successRate = builder.successRate;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getAttempts() { return attempts; }
        public long getSuccesses() { return successes; }
        public long getFailures() { return failures; }
        public double getSuccessRate() { return successRate; }

        public static class Builder {
            private long attempts;
            private long successes;
            private long failures;
            private double successRate;

            public Builder attempts(long attempts) {
                this.attempts = attempts;
                return this;
            }

            public Builder successes(long successes) {
                this.successes = successes;
                return this;
            }

            public Builder failures(long failures) {
                this.failures = failures;
                return this;
            }

            public Builder successRate(double successRate) {
                this.successRate = successRate;
                return this;
            }

            public AuthenticationStats build() {
                return new AuthenticationStats(this);
            }
        }
    }

    /**
     * 安全统计数据
     */
    public static class SecurityStats {
        private final long rateLimitViolations;
        private final long suspiciousActivities;

        private SecurityStats(Builder builder) {
            this.rateLimitViolations = builder.rateLimitViolations;
            this.suspiciousActivities = builder.suspiciousActivities;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getRateLimitViolations() { return rateLimitViolations; }
        public long getSuspiciousActivities() { return suspiciousActivities; }

        public static class Builder {
            private long rateLimitViolations;
            private long suspiciousActivities;

            public Builder rateLimitViolations(long rateLimitViolations) {
                this.rateLimitViolations = rateLimitViolations;
                return this;
            }

            public Builder suspiciousActivities(long suspiciousActivities) {
                this.suspiciousActivities = suspiciousActivities;
                return this;
            }

            public SecurityStats build() {
                return new SecurityStats(this);
            }
        }
    }
}