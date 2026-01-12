package com.evcs.gateway.filter;

/**
 * Gateway security config to prevent accidental exposure of internal-only endpoints.
 */
public class InternalApiBlockProperties {

    /**
     * Whether to block incoming requests whose path starts with {@link #pathPrefix}.
     */
    private boolean enabled = true;

    /**
     * Path prefix for internal-only endpoints.
     */
    private String pathPrefix = "/internal/api/";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }
}
