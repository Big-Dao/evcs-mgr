package com.evcs.tenant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "evcs.internal.api")
public class InternalApiTokenProperties {

    /**
     * Whether to enforce internal API token validation for /internal/api/**.
     */
    private boolean enabled = false;

    /**
     * Shared token for service-to-service internal API calls.
     */
    private String token;

    /**
     * Header name carrying the internal token.
     */
    private String headerName = "X-Internal-Token";

    /**
     * Request path prefix for internal APIs.
     */
    private String pathPrefix = "/internal/api/";
}
