package com.evcs.payment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

@Configuration
public class PaymentResilienceConfig {

    @Bean
    public CircuitBreaker orderServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .recordExceptions(ResourceAccessException.class, HttpServerErrorException.class)
            .build();

        return CircuitBreaker.of("orderService", config);
    }

    @Bean
    public Retry orderServiceRetry(OrderSyncConfig orderSyncConfig) {
        Duration base = Duration.ofMillis(Math.max(1L, orderSyncConfig.getRetryIntervalMs()));
        IntervalFunction intervalFunction = IntervalFunction.ofExponentialBackoff(base, 2.0);

        RetryConfig config = RetryConfig.custom()
            .maxAttempts(Math.max(1, orderSyncConfig.getMaxRetries()))
            .intervalFunction(intervalFunction)
            .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
            .build();

        return Retry.of("orderService", config);
    }

    @Bean
    public CircuitBreaker paymentMonitoringApiCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(15))
            .permittedNumberOfCallsInHalfOpenState(2)
            .recordExceptions(ResourceAccessException.class, HttpServerErrorException.class)
            .build();

        return CircuitBreaker.of("paymentMonitoringApi", config);
    }

    @Bean
    public Retry paymentMonitoringApiRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(200))
            .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
            .build();

        return Retry.of("paymentMonitoringApi", config);
    }
}
