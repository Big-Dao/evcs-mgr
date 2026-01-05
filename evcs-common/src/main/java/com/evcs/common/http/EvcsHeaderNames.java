package com.evcs.common.http;

/**
 * Standard header names used across EVCS services.
 */
public final class EvcsHeaderNames {

    private EvcsHeaderNames() {
    }

    public static final String TRACE_ID = "X-Trace-Id";
    public static final String REQUEST_ID = "X-Request-Id";

    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String USER_ID = "X-User-Id";

    public static final String TENANT_TYPE = "X-Tenant-Type";
    public static final String TENANT_ANCESTORS = "X-Tenant-Ancestors";
}
