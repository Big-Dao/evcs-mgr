package com.evcs.common.http;

import com.evcs.common.tenant.TenantContext;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

/**
 * Utilities to propagate request-scoped context (tenant + trace) into outgoing HTTP calls.
 */
public final class OutgoingRequestContextHeaders {

    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    private OutgoingRequestContextHeaders() {
    }

    public static void applyTo(HttpHeaders headers) {
        if (headers == null) {
            return;
        }

        // Trace headers
        setIfAbsent(headers, EvcsHeaderNames.TRACE_ID, resolveTraceId());
        setIfAbsent(headers, EvcsHeaderNames.REQUEST_ID, resolveRequestId());

        // Tenant headers
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            setIfAbsent(headers, EvcsHeaderNames.TENANT_ID, String.valueOf(tenantId));
        }

        Long userId = TenantContext.getUserId();
        if (userId != null) {
            setIfAbsent(headers, EvcsHeaderNames.USER_ID, String.valueOf(userId));
        }

        Integer tenantType = TenantContext.getTenantType();
        if (tenantType != null) {
            setIfAbsent(headers, EvcsHeaderNames.TENANT_TYPE, String.valueOf(tenantType));
        }

        String ancestors = TenantContext.getTenantAncestors();
        if (ancestors != null && !ancestors.isBlank()) {
            setIfAbsent(headers, EvcsHeaderNames.TENANT_ANCESTORS, ancestors);
        }
    }

    private static void setIfAbsent(HttpHeaders headers, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!headers.containsKey(name)) {
            headers.set(name, value);
        }
    }

    private static String resolveTraceId() {
        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return MDC.get(MDC_REQUEST_ID_KEY);
    }

    private static String resolveRequestId() {
        String requestId = MDC.get(MDC_REQUEST_ID_KEY);
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return MDC.get(MDC_TRACE_ID_KEY);
    }
}
