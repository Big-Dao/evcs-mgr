package com.evcs.protocol.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class ProtocolResilienceConfig {

    private final ProtocolProperties protocolProperties;

    @Bean
    public CircuitBreaker stationServiceCircuitBreaker() {
        ProtocolProperties.CircuitBreakerPolicy policy = protocolProperties.getResilience().getStationServiceCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowSize(Math.max(1, policy.getSlidingWindowSize()))
            .minimumNumberOfCalls(Math.max(1, policy.getMinimumNumberOfCalls()))
            .failureRateThreshold(Math.max(0.0f, policy.getFailureRateThreshold()))
            .waitDurationInOpenState(Duration.ofMillis(Math.max(1L, policy.getWaitDurationInOpenStateMs())))
            .permittedNumberOfCallsInHalfOpenState(Math.max(1, policy.getPermittedNumberOfCallsInHalfOpenState()))
            // Retry only transient faults; do not retry/record 4xx business errors
            .recordExceptions(ResourceAccessException.class, HttpServerErrorException.class)
            .build();

        return CircuitBreaker.of("stationService", config);
    }

    @Bean
    public Retry stationServiceRetry() {
        ProtocolProperties.RetryConfig retry = protocolProperties.getResilience().getStationServiceRetry();
        if (retry == null) {
            // Backward compatibility: keep honoring historical evcs.protocol.retry.*
            retry = protocolProperties.getRetry();
        }

        int maxAttempts = retry.isEnabled() ? Math.max(1, retry.getMaxAttempts()) : 1;
        long baseDelayMs = Math.max(1L, retry.getDelay());
        long maxDelayMs = Math.max(1L, retry.getMaxDelay());
        double multiplier = retry.getMultiplier() > 0 ? retry.getMultiplier() : 2.0;

        IntervalFunction intervalFunction = attempt -> {
            // attempt starts at 1
            double factor = Math.pow(multiplier, Math.max(0, attempt - 1));
            long interval = (long) (baseDelayMs * factor);
            return Math.min(interval, maxDelayMs);
        };

        RetryConfig config = RetryConfig.custom()
            .maxAttempts(maxAttempts)
            .intervalFunction(intervalFunction)
            .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
            .build();

        return Retry.of("stationService", config);
    }
}
