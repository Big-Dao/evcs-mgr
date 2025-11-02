package com.evcs.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 熔断器服务
 * 实现服务熔断保护，防止级联故障
 */
@Slf4j
@Service
public class CircuitBreakerService {

    // 熔断器状态枚举
    public enum CircuitBreakerState {
        CLOSED,    // 关闭状态：正常工作
        OPEN,      // 打开状态：熔断中
        HALF_OPEN  // 半开状态：尝试恢复
    }

    // 熔断器配置
    public static class CircuitBreakerConfig {
        private final int failureThreshold;        // 失败阈值
        private final int successThreshold;        // 成功阈值（半开状态）
        private final Duration timeout;            // 超时时间
        private final Duration recoveryTimeout;    // 恢复等待时间

        public CircuitBreakerConfig(int failureThreshold, int successThreshold,
                                  Duration timeout, Duration recoveryTimeout) {
            this.failureThreshold = failureThreshold;
            this.successThreshold = successThreshold;
            this.timeout = timeout;
            this.recoveryTimeout = recoveryTimeout;
        }

        public int getFailureThreshold() { return failureThreshold; }
        public int getSuccessThreshold() { return successThreshold; }
        public Duration getTimeout() { return timeout; }
        public Duration getRecoveryTimeout() { return recoveryTimeout; }
    }

    // 默认配置
    private static final CircuitBreakerConfig DEFAULT_CONFIG = new CircuitBreakerConfig(
        5,      // 5次失败后熔断
        3,      // 半开状态需要3次成功才恢复
        Duration.ofSeconds(10),  // 单次请求超时10秒
        Duration.ofMinutes(1)    // 熔断后等待1分钟再尝试恢复
    );

    // 熔断器实例存储
    private final ConcurrentHashMap<String, CircuitBreakerInstance> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * 熔断器实例
     */
    public static class CircuitBreakerInstance {
        private final String serviceName;
        private final CircuitBreakerConfig config;
        private volatile CircuitBreakerState state;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final AtomicLong lastStateChangeTime = new AtomicLong(System.currentTimeMillis());
        private final AtomicLong requestCount = new AtomicLong(0);

        public CircuitBreakerInstance(String serviceName, CircuitBreakerConfig config) {
            this.serviceName = serviceName;
            this.config = config;
            this.state = CircuitBreakerState.CLOSED;
        }

        public synchronized boolean allowRequest() {
            requestCount.incrementAndGet();
            long currentTime = System.currentTimeMillis();

            switch (state) {
                case CLOSED:
                    return true;

                case OPEN:
                    // 检查是否可以尝试恢复
                    if (currentTime - lastStateChangeTime.get() > config.getRecoveryTimeout().toMillis()) {
                        log.info("Circuit breaker for {} transitioning to HALF_OPEN", serviceName);
                        state = CircuitBreakerState.HALF_OPEN;
                        successCount.set(0);
                        lastStateChangeTime.set(currentTime);
                        return true;
                    }
                    return false;

                case HALF_OPEN:
                    return true;

                default:
                    return false;
            }
        }

        public synchronized void onSuccess() {
            switch (state) {
                case CLOSED:
                    // 重置失败计数
                    failureCount.set(0);
                    break;

                case HALF_OPEN:
                    int currentSuccess = successCount.incrementAndGet();
                    if (currentSuccess >= config.getSuccessThreshold()) {
                        log.info("Circuit breaker for {} transitioning to CLOSED after {} successes",
                                serviceName, currentSuccess);
                        state = CircuitBreakerState.CLOSED;
                        failureCount.set(0);
                        successCount.set(0);
                        lastStateChangeTime.set(System.currentTimeMillis());
                    }
                    break;

                case OPEN:
                    // 在OPEN状态下不应该有成功调用
                    log.warn("Unexpected success call in OPEN state for service: {}", serviceName);
                    break;
            }
        }

        public synchronized void onFailure(Throwable throwable) {
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());

            switch (state) {
                case CLOSED:
                    if (failureCount.get() >= config.getFailureThreshold()) {
                        log.warn("Circuit breaker for {} transitioning to OPEN after {} failures",
                                serviceName, failureCount.get());
                        state = CircuitBreakerState.OPEN;
                        lastStateChangeTime.set(System.currentTimeMillis());
                    }
                    break;

                case HALF_OPEN:
                    log.warn("Circuit breaker for {} transitioning back to OPEN after failure in HALF_OPEN",
                            serviceName);
                    state = CircuitBreakerState.OPEN;
                    successCount.set(0);
                    lastStateChangeTime.set(System.currentTimeMillis());
                    break;

                case OPEN:
                    // 已经是OPEN状态，无需处理
                    break;
            }

            log.error("Service call failed for {}: {}", serviceName, throwable.getMessage());
        }

        public synchronized CircuitBreakerState getState() {
            return state;
        }

        public CircuitBreakerStatistics getStatistics() {
            return new CircuitBreakerStatistics(
                serviceName,
                state,
                failureCount.get(),
                successCount.get(),
                requestCount.get(),
                lastFailureTime.get(),
                lastStateChangeTime.get()
            );
        }
    }

    /**
     * 获取熔断器实例
     */
    private CircuitBreakerInstance getCircuitBreaker(String serviceName) {
        return circuitBreakers.computeIfAbsent(serviceName,
            name -> new CircuitBreakerInstance(name, DEFAULT_CONFIG));
    }

    /**
     * 获取自定义配置的熔断器实例
     */
    private CircuitBreakerInstance getCircuitBreaker(String serviceName, CircuitBreakerConfig config) {
        return circuitBreakers.computeIfAbsent(serviceName,
            name -> new CircuitBreakerInstance(name, config));
    }

    /**
     * 检查是否允许请求
     */
    public boolean allowRequest(String serviceName) {
        CircuitBreakerInstance circuitBreaker = getCircuitBreaker(serviceName);
        boolean allowed = circuitBreaker.allowRequest();

        if (!allowed) {
            log.debug("Circuit breaker blocking request to service: {}", serviceName);
        }

        return allowed;
    }

    /**
     * 记录成功调用
     */
    public void recordSuccess(String serviceName) {
        CircuitBreakerInstance circuitBreaker = getCircuitBreaker(serviceName);
        circuitBreaker.onSuccess();
    }

    /**
     * 记录失败调用
     */
    public void recordFailure(String serviceName, Throwable throwable) {
        CircuitBreakerInstance circuitBreaker = getCircuitBreaker(serviceName);
        circuitBreaker.onFailure(throwable);
    }

    /**
     * 获取熔断器状态
     */
    public CircuitBreakerState getState(String serviceName) {
        CircuitBreakerInstance circuitBreaker = getCircuitBreakers().get(serviceName);
        return circuitBreaker != null ? circuitBreaker.getState() : CircuitBreakerState.CLOSED;
    }

    /**
     * 获取所有熔断器
     */
    public ConcurrentHashMap<String, CircuitBreakerInstance> getCircuitBreakers() {
        return circuitBreakers;
    }

    /**
     * 获取熔断器统计信息
     */
    public CircuitBreakerStatistics getStatistics(String serviceName) {
        CircuitBreakerInstance circuitBreaker = getCircuitBreakers().get(serviceName);
        return circuitBreaker != null ? circuitBreaker.getStatistics() : null;
    }

    /**
     * 重置熔断器
     */
    public void reset(String serviceName) {
        CircuitBreakerInstance circuitBreaker = getCircuitBreaker(serviceName);
        synchronized (circuitBreaker) {
            circuitBreaker.state = CircuitBreakerState.CLOSED;
            circuitBreaker.failureCount.set(0);
            circuitBreaker.successCount.set(0);
            circuitBreaker.lastStateChangeTime.set(System.currentTimeMillis());
        }
        log.info("Circuit breaker reset for service: {}", serviceName);
    }

    /**
     * 手动打开熔断器
     */
    public void forceOpen(String serviceName) {
        CircuitBreakerInstance circuitBreaker = getCircuitBreaker(serviceName);
        synchronized (circuitBreaker) {
            circuitBreaker.state = CircuitBreakerState.OPEN;
            circuitBreaker.lastStateChangeTime.set(System.currentTimeMillis());
        }
        log.warn("Circuit breaker forced open for service: {}", serviceName);
    }

    /**
     * 手动关闭熔断器
     */
    public void forceClose(String serviceName) {
        CircuitBreakerInstance circuitBreaker = getCircuitBreaker(serviceName);
        synchronized (circuitBreaker) {
            circuitBreaker.state = CircuitBreakerState.CLOSED;
            circuitBreaker.failureCount.set(0);
            circuitBreaker.successCount.set(0);
            circuitBreaker.lastStateChangeTime.set(System.currentTimeMillis());
        }
        log.info("Circuit breaker forced closed for service: {}", serviceName);
    }

    /**
     * 熔断器统计信息类
     */
    public static class CircuitBreakerStatistics {
        private final String serviceName;
        private final CircuitBreakerState state;
        private final int failureCount;
        private final int successCount;
        private final long requestCount;
        private final long lastFailureTime;
        private final long lastStateChangeTime;

        public CircuitBreakerStatistics(String serviceName, CircuitBreakerState state,
                                      int failureCount, int successCount, long requestCount,
                                      long lastFailureTime, long lastStateChangeTime) {
            this.serviceName = serviceName;
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.requestCount = requestCount;
            this.lastFailureTime = lastFailureTime;
            this.lastStateChangeTime = lastStateChangeTime;
        }

        public String getServiceName() { return serviceName; }
        public CircuitBreakerState getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public long getRequestCount() { return requestCount; }
        public long getLastFailureTime() { return lastFailureTime; }
        public long getLastStateChangeTime() { return lastStateChangeTime; }
        public double getFailureRate() {
            return requestCount > 0 ? (double) failureCount / requestCount : 0;
        }

        @Override
        public String toString() {
            return String.format("CircuitBreakerStats{service=%s, state=%s, failures=%d, successes=%d, requests=%d, failureRate=%.2f%%}",
                    serviceName, state, failureCount, successCount, requestCount, getFailureRate() * 100);
        }
    }
}