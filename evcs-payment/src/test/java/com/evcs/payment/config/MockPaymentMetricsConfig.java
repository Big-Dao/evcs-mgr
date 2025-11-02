package com.evcs.payment.config;

import com.evcs.payment.metrics.PaymentMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Mock PaymentMetrics for testing environment
 * Provides no-op implementation to avoid metrics dependencies
 */
@TestConfiguration
public class MockPaymentMetricsConfig {

    @Bean
    @Primary
    public PaymentMetrics mockPaymentMetrics(MeterRegistry meterRegistry) {
        return new NoOpPaymentMetrics(meterRegistry);
    }

    /**
     * No-op implementation of PaymentMetrics for testing
     */
    public static class NoOpPaymentMetrics extends PaymentMetrics {
        public NoOpPaymentMetrics(MeterRegistry meterRegistry) {
            super(meterRegistry);
        }

        @Override
        public void recordPaymentRequest() {
            // No-op for testing
        }

        @Override
        public void recordPaymentSuccess(String channel, Long amount) {
            // No-op for testing
        }

        @Override
        public void recordPaymentFailure(String channel) {
            // No-op for testing
        }

        @Override
        public void recordCallbackReceived() {
            // No-op for testing
        }

        @Override
        public void recordCallbackSuccess() {
            // No-op for testing
        }

        @Override
        public void recordCallbackFailure() {
            // No-op for testing
        }

        @Override
        public void recordRefundRequest() {
            // No-op for testing
        }

        @Override
        public void recordRefundSuccess(Long amount) {
            // No-op for testing
        }

        @Override
        public void recordRefundFailure() {
            // No-op for testing
        }
    }
}