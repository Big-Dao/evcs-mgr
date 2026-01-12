package com.evcs.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "evcs.internal.api")
public class InternalApiTokenProperties {

    private boolean enabled = false;

    private String token;

    private String headerName = "X-Internal-Token";
}
