package com.evcs.common.trace;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Utility for setting a traceId/requestId pair into MDC for non-request entrypoints
 * (e.g. scheduled jobs and MQ consumers) and restoring previous values on close.
 *
 * <p>Compatibility strategy:
 * <ul>
 *   <li>MDC uses keys: traceId / requestId</li>
 *   <li>Prefer traceId; requestId is kept for backward compatibility</li>
 * </ul>
 */
public final class TraceMdc implements AutoCloseable {

    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    private final String previousTraceId;
    private final String previousRequestId;

    private TraceMdc(String previousTraceId, String previousRequestId) {
        this.previousTraceId = previousTraceId;
        this.previousRequestId = previousRequestId;
    }

    /**
     * Ensure MDC has a usable trace id during the current scope.
     * <ul>
     *   <li>If both traceId/requestId are missing, generate a new one</li>
     *   <li>If only one exists, copy it to the other for consistency</li>
     *   <li>If both exist, leave them unchanged</li>
     * </ul>
     */
    public static TraceMdc ensureTracePresent() {
        String previousTraceId = MDC.get(MDC_TRACE_ID_KEY);
        String previousRequestId = MDC.get(MDC_REQUEST_ID_KEY);

        String traceId = normalize(previousTraceId);
        String requestId = normalize(previousRequestId);

        if (traceId == null && requestId == null) {
            String generated = generateTraceId();
            MDC.put(MDC_TRACE_ID_KEY, generated);
            MDC.put(MDC_REQUEST_ID_KEY, generated);
            return new TraceMdc(previousTraceId, previousRequestId);
        }

        if (traceId == null) {
            MDC.put(MDC_TRACE_ID_KEY, requestId);
        }
        if (requestId == null) {
            MDC.put(MDC_REQUEST_ID_KEY, traceId);
        }

        return new TraceMdc(previousTraceId, previousRequestId);
    }

    /**
     * Force MDC traceId/requestId to the given value for the current scope.
     * If blank/null, a new trace id is generated.
     */
    public static TraceMdc withTraceId(String traceId) {
        String previousTraceId = MDC.get(MDC_TRACE_ID_KEY);
        String previousRequestId = MDC.get(MDC_REQUEST_ID_KEY);

        String resolved = normalize(traceId);
        if (resolved == null) {
            resolved = generateTraceId();
        }

        MDC.put(MDC_TRACE_ID_KEY, resolved);
        MDC.put(MDC_REQUEST_ID_KEY, resolved);
        return new TraceMdc(previousTraceId, previousRequestId);
    }

    public static TraceMdc withNewTraceId() {
        return withTraceId(generateTraceId());
    }

    @Override
    public void close() {
        restore(MDC_TRACE_ID_KEY, previousTraceId);
        restore(MDC_REQUEST_ID_KEY, previousRequestId);
    }

    private static void restore(String key, String previousValue) {
        if (previousValue == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, previousValue);
        }
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
