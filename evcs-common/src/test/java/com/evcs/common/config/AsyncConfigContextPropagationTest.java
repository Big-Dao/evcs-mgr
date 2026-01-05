package com.evcs.common.config;

import com.evcs.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        AsyncConfig.class,
        TenantContextTaskDecorator.class,
        AsyncConfigContextPropagationTest.TestConfig.class
})
class AsyncConfigContextPropagationTest {

    @Autowired
    private AsyncProbe asyncProbe;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("AsyncConfig + @Async - 应传播 TenantContext 与 MDC(traceId/requestId) 且不泄漏")
    void testAsync_shouldPropagateTenantAndMdc_andNotLeak() throws Exception {
        // Arrange
        TenantContext.setTenantId(101L);
        TenantContext.setUserId(202L);
        MDC.put("traceId", "trace-xyz");
        MDC.put("requestId", "req-xyz");

        // Act
        ContextSnapshot snapshot = asyncProbe.probe().get(5, TimeUnit.SECONDS);

        // Assert - inside async task
        assertEquals(101L, snapshot.tenantId(), "Async task should see captured tenantId");
        assertEquals(202L, snapshot.userId(), "Async task should see captured userId");
        assertEquals("trace-xyz", snapshot.traceId(), "Async task should see captured MDC.traceId");
        assertEquals("req-xyz", snapshot.requestId(), "Async task should see captured MDC.requestId");

        // Assert - submitting thread unchanged
        assertEquals(101L, TenantContext.getTenantId(), "Submitting thread TenantContext must remain unchanged");
        assertEquals("trace-xyz", MDC.get("traceId"), "Submitting thread MDC.traceId must remain unchanged");

        // Assert - no leak: clear submitting thread context and run again; async task should observe nulls
        TenantContext.clear();
        MDC.clear();

        ContextSnapshot empty = asyncProbe.probe().get(5, TimeUnit.SECONDS);
        assertNull(empty.tenantId(), "Async task should not inherit previous tenantId after context cleared");
        assertNull(empty.userId(), "Async task should not inherit previous userId after context cleared");
        assertNull(empty.traceId(), "Async task should not inherit previous MDC.traceId after context cleared");
        assertNull(empty.requestId(), "Async task should not inherit previous MDC.requestId after context cleared");

        // Sanity: ensure test thread remains cleared
        assertNull(TenantContext.getTenantId(), "Test thread TenantContext should be cleared");
        assertNull(MDC.get("traceId"), "Test thread MDC.traceId should be cleared");
    }


    record ContextSnapshot(Long tenantId, Long userId, String traceId, String requestId) {
    }

    interface AsyncProbe {
        @Async("chargingExecutor")
        CompletableFuture<ContextSnapshot> probe();
    }

    @Configuration
    static class TestConfig {
        @Bean
        AsyncProbe asyncProbe() {
            return () -> CompletableFuture.completedFuture(
                new ContextSnapshot(
                    TenantContext.getTenantId(),
                    TenantContext.getUserId(),
                    MDC.get("traceId"),
                    MDC.get("requestId")
                )
            );
        }
    }
}
