package com.evcs.common.trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class TraceMdcTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("ensureTracePresent - missing both should generate and restore on close")
    void ensureTracePresent_shouldGenerateAndRestore() {
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("requestId"));

        String insideTrace;
        try (TraceMdc ignored = TraceMdc.ensureTracePresent()) {
            insideTrace = MDC.get("traceId");
            assertNotNull(insideTrace);
            assertFalse(insideTrace.isBlank());
            assertEquals(insideTrace, MDC.get("requestId"));
        }

        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    @DisplayName("ensureTracePresent - only traceId should backfill requestId and restore")
    void ensureTracePresent_shouldBackfillRequestIdAndRestore() {
        MDC.put("traceId", "trace-1");
        assertNull(MDC.get("requestId"));

        try (TraceMdc ignored = TraceMdc.ensureTracePresent()) {
            assertEquals("trace-1", MDC.get("traceId"));
            assertEquals("trace-1", MDC.get("requestId"));
        }

        assertEquals("trace-1", MDC.get("traceId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    @DisplayName("ensureTracePresent - only requestId should backfill traceId and restore")
    void ensureTracePresent_shouldBackfillTraceIdAndRestore() {
        MDC.put("requestId", "req-1");
        assertNull(MDC.get("traceId"));

        try (TraceMdc ignored = TraceMdc.ensureTracePresent()) {
            assertEquals("req-1", MDC.get("traceId"));
            assertEquals("req-1", MDC.get("requestId"));
        }

        assertNull(MDC.get("traceId"));
        assertEquals("req-1", MDC.get("requestId"));
    }

    @Test
    @DisplayName("withTraceId - should force both keys and restore on close")
    void withTraceId_shouldForceAndRestore() {
        MDC.put("traceId", "trace-prev");
        MDC.put("requestId", "req-prev");

        try (TraceMdc ignored = TraceMdc.withTraceId("trace-new")) {
            assertEquals("trace-new", MDC.get("traceId"));
            assertEquals("trace-new", MDC.get("requestId"));
        }

        assertEquals("trace-prev", MDC.get("traceId"));
        assertEquals("req-prev", MDC.get("requestId"));
    }
}
