package com.evcs.gateway.config;

import com.evcs.common.config.AsyncConfig;
import com.evcs.common.config.TenantContextTaskDecorator;
import com.evcs.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringJUnitConfig(classes = {
    TenantContextTaskDecorator.class,
    AsyncConfig.class,
    ReactorSchedulerConfig.class
})
class ReactorSchedulerContextPropagationTest {

    @Autowired
    private Scheduler evcsReactorScheduler;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("Reactor publishOn(evcsReactorScheduler) - 应传播 TenantContext 与 MDC 且不泄漏")
    void testPublishOn_shouldPropagateTenantAndMdc_andNotLeak() {
        // Arrange
        TenantContext.setTenantId(101L);
        TenantContext.setUserId(202L);
        MDC.put("traceId", "trace-reactor-xyz");
        MDC.put("requestId", "req-reactor-xyz");

        // Act
        ContextSnapshot snapshot = Mono.just("start")
            .publishOn(evcsReactorScheduler)
            .map(ignored -> new ContextSnapshot(
                TenantContext.getTenantId(),
                TenantContext.getUserId(),
                MDC.get("traceId"),
                MDC.get("requestId")
            ))
            .block(Duration.ofSeconds(5));

        // Assert
        assertNotNull(snapshot, "Reactive pipeline should produce a snapshot");
        assertEquals(101L, snapshot.tenantId(), "publishOn should propagate tenantId");
        assertEquals(202L, snapshot.userId(), "publishOn should propagate userId");
        assertEquals("trace-reactor-xyz", snapshot.traceId(), "publishOn should propagate MDC.traceId");
        assertEquals("req-reactor-xyz", snapshot.requestId(), "publishOn should propagate MDC.requestId");

        // Assert - no leak
        TenantContext.clear();
        MDC.clear();

        ContextSnapshot empty = Mono.just("start")
            .publishOn(evcsReactorScheduler)
            .map(ignored -> new ContextSnapshot(
                TenantContext.getTenantId(),
                TenantContext.getUserId(),
                MDC.get("traceId"),
                MDC.get("requestId")
            ))
            .block(Duration.ofSeconds(5));

        assertNotNull(empty, "Reactive pipeline should produce a snapshot");
        assertNull(empty.tenantId(), "publishOn should not inherit previous tenantId after context cleared");
        assertNull(empty.userId(), "publishOn should not inherit previous userId after context cleared");
        assertNull(empty.traceId(), "publishOn should not inherit previous MDC.traceId after context cleared");
        assertNull(empty.requestId(), "publishOn should not inherit previous MDC.requestId after context cleared");
    }

    record ContextSnapshot(Long tenantId, Long userId, String traceId, String requestId) {}
}
