package com.evcs.gateway.filter;

/**
 * Reactive gateway trace id propagation settings.
 */
public class TraceIdFilterProperties {

    private boolean enabled = true;

    /**
     * Header name for the preferred trace id.
     */
    private String traceIdHeader = "X-Trace-Id";

    /**
     * Header name for legacy/request id compatibility.
     */
    private String requestIdHeader = "X-Request-Id";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTraceIdHeader() {
        return traceIdHeader;
    }

    public void setTraceIdHeader(String traceIdHeader) {
        this.traceIdHeader = traceIdHeader;
    }

    public String getRequestIdHeader() {
        return requestIdHeader;
    }

    public void setRequestIdHeader(String requestIdHeader) {
        this.requestIdHeader = requestIdHeader;
    }
}
