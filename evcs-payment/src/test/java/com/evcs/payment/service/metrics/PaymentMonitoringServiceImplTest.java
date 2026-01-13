package com.evcs.payment.service.metrics;

import com.evcs.payment.config.MonitoringHealthProperties;
import com.evcs.payment.dto.MetricsResponse;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.metrics.impl.PaymentMonitoringServiceImpl;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentMonitoringServiceImplTest {

    @Test
    @DisplayName("health - 未配置外部API端点时不应导致 Actuator DOWN")
    void health_whenExternalApiEndpointsNotConfigured_shouldBeUp() {
        // Arrange
        PaymentMetrics paymentMetrics = mock(PaymentMetrics.class);
        when(paymentMetrics.getPaymentSuccessRate()).thenReturn(100.0);
        when(paymentMetrics.getCallbackSuccessRate()).thenReturn(100.0);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        MonitoringHealthProperties props = new MonitoringHealthProperties();
        props.setDatabaseValidationQuery("SELECT 1");
        props.getApi().setEndpoints(java.util.Collections.emptyList());

        PaymentMonitoringServiceImpl service = new PaymentMonitoringServiceImpl(
            paymentMetrics,
            new SimpleMeterRegistry(),
            jdbcTemplate,
            props,
            CircuitBreaker.ofDefaults("paymentMonitoringApi"),
            Retry.ofDefaults("paymentMonitoringApi")
        );

        // Act
        Health health = service.health();

        // Assert
        assertNotNull(health);
        assertEquals(Status.UP, health.getStatus());
        assertEquals(MetricsResponse.Status.HEALTHY.getDescription(), health.getDetails().get("overall"));
    }

    @Test
    @DisplayName("health - WARNING 状态应保持 Actuator UP（仅在 UNHEALTHY 时 DOWN）")
    void health_whenOverallWarning_shouldBeUp() {
        // Arrange
        PaymentMetrics paymentMetrics = mock(PaymentMetrics.class);
        when(paymentMetrics.getPaymentSuccessRate()).thenReturn(85.0); // WARNING
        when(paymentMetrics.getCallbackSuccessRate()).thenReturn(100.0);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class))).thenReturn(1);

        MonitoringHealthProperties props = new MonitoringHealthProperties();
        props.getApi().setEndpoints(java.util.Collections.emptyList());

        PaymentMonitoringServiceImpl service = new PaymentMonitoringServiceImpl(
            paymentMetrics,
            new SimpleMeterRegistry(),
            jdbcTemplate,
            props,
            CircuitBreaker.ofDefaults("paymentMonitoringApi"),
            Retry.ofDefaults("paymentMonitoringApi")
        );

        // Act
        Health health = service.health();

        // Assert
        assertNotNull(health);
        assertEquals(Status.UP, health.getStatus());
        assertEquals(MetricsResponse.Status.WARNING.getDescription(), health.getDetails().get("overall"));
    }
}
