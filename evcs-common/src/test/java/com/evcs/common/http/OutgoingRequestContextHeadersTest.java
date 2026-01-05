package com.evcs.common.http;

import com.evcs.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

class OutgoingRequestContextHeadersTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("applyTo - 同时存在 traceId/requestId 时应分别写入对应 header")
    void testApplyTo_shouldSetTraceAndRequestHeaders_whenBothPresentInMdc() {
        // Arrange
        MDC.put("traceId", "trace-1");
        MDC.put("requestId", "req-1");
        HttpHeaders headers = new HttpHeaders();

        // Act
        OutgoingRequestContextHeaders.applyTo(headers);

        // Assert
        assertEquals("trace-1", headers.getFirst(EvcsHeaderNames.TRACE_ID), "X-Trace-Id should come from MDC.traceId");
        assertEquals("req-1", headers.getFirst(EvcsHeaderNames.REQUEST_ID), "X-Request-Id should come from MDC.requestId");
    }

    @Test
    @DisplayName("applyTo - 仅有 requestId 时 X-Trace-Id 应回退到 requestId")
    void testApplyTo_shouldFallbackTraceIdToRequestId_whenTraceIdMissing() {
        // Arrange
        MDC.put("requestId", "req-2");
        HttpHeaders headers = new HttpHeaders();

        // Act
        OutgoingRequestContextHeaders.applyTo(headers);

        // Assert
        assertEquals("req-2", headers.getFirst(EvcsHeaderNames.TRACE_ID), "X-Trace-Id should fallback to MDC.requestId");
        assertEquals("req-2", headers.getFirst(EvcsHeaderNames.REQUEST_ID), "X-Request-Id should come from MDC.requestId");
    }

    @Test
    @DisplayName("applyTo - 仅有 traceId 时 X-Request-Id 应回退到 traceId")
    void testApplyTo_shouldFallbackRequestIdToTraceId_whenRequestIdMissing() {
        // Arrange
        MDC.put("traceId", "trace-3");
        HttpHeaders headers = new HttpHeaders();

        // Act
        OutgoingRequestContextHeaders.applyTo(headers);

        // Assert
        assertEquals("trace-3", headers.getFirst(EvcsHeaderNames.TRACE_ID), "X-Trace-Id should come from MDC.traceId");
        assertEquals("trace-3", headers.getFirst(EvcsHeaderNames.REQUEST_ID), "X-Request-Id should fallback to MDC.traceId");
    }

    @Test
    @DisplayName("applyTo - headers 已存在时不应覆盖")
    void testApplyTo_shouldNotOverrideExistingHeaders_whenAlreadyPresent() {
        // Arrange
        MDC.put("traceId", "trace-new");
        MDC.put("requestId", "req-new");
        TenantContext.setTenantId(101L);

        HttpHeaders headers = new HttpHeaders();
        headers.set(EvcsHeaderNames.TRACE_ID, "trace-old");
        headers.set(EvcsHeaderNames.REQUEST_ID, "req-old");
        headers.set(EvcsHeaderNames.TENANT_ID, "999");

        // Act
        OutgoingRequestContextHeaders.applyTo(headers);

        // Assert
        assertEquals("trace-old", headers.getFirst(EvcsHeaderNames.TRACE_ID), "Existing X-Trace-Id should not be overridden");
        assertEquals("req-old", headers.getFirst(EvcsHeaderNames.REQUEST_ID), "Existing X-Request-Id should not be overridden");
        assertEquals("999", headers.getFirst(EvcsHeaderNames.TENANT_ID), "Existing X-Tenant-Id should not be overridden");
    }

    @Test
    @DisplayName("applyTo - TenantContext 存在时应写入租户相关 header")
    void testApplyTo_shouldSetTenantHeaders_whenTenantContextPresent() {
        // Arrange
        TenantContext.setTenantId(11L);
        TenantContext.setUserId(22L);
        TenantContext.setTenantType(2);
        TenantContext.setTenantAncestors("1/11");

        HttpHeaders headers = new HttpHeaders();

        // Act
        OutgoingRequestContextHeaders.applyTo(headers);

        // Assert
        assertEquals("11", headers.getFirst(EvcsHeaderNames.TENANT_ID), "X-Tenant-Id should come from TenantContext.tenantId");
        assertEquals("22", headers.getFirst(EvcsHeaderNames.USER_ID), "X-User-Id should come from TenantContext.userId");
        assertEquals("2", headers.getFirst(EvcsHeaderNames.TENANT_TYPE), "X-Tenant-Type should come from TenantContext.tenantType");
        assertEquals("1/11", headers.getFirst(EvcsHeaderNames.TENANT_ANCESTORS), "X-Tenant-Ancestors should come from TenantContext.tenantAncestors");
    }
}
