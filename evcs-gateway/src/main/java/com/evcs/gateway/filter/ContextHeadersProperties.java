package com.evcs.gateway.filter;

/**
 * Gateway context headers injection settings.
 */
public class ContextHeadersProperties {

    private boolean enabled = false;

    /**
     * Shared secret for verifying legacy HMAC JWTs.
     *
     * <p>Note: production environments must override this via configuration.
     */
    private String jwtSecret = "evcs-secret-key";

    /**
     * If true, strip client-supplied context headers before forwarding.
     *
     * <p>This prevents header spoofing and should be enabled after migration.
     */
    private boolean stripClientContextHeaders = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public boolean isStripClientContextHeaders() {
        return stripClientContextHeaders;
    }

    public void setStripClientContextHeaders(boolean stripClientContextHeaders) {
        this.stripClientContextHeaders = stripClientContextHeaders;
    }
}
