package com.evcs.protocol.websocket;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class OCPPSessionManagerTraceMdcTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("@Scheduled entrypoint - should not leak MDC requestId when backfilled")
    void scheduled_shouldNotLeakBackfilledRequestId() {
        OCPPSessionManager manager = new OCPPSessionManager();

        MDC.put("traceId", "trace-1");
        assertNull(MDC.get("requestId"));

        manager.cleanupInactiveSessions();

        // ensureTracePresent may backfill requestId during execution,
        // but it must be restored after returning.
        assertEquals("trace-1", MDC.get("traceId"));
        assertNull(MDC.get("requestId"));
    }
}
