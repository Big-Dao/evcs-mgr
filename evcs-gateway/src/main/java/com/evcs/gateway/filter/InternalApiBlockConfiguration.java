package com.evcs.gateway.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

/**
 * Configures a gateway edge filter to block /internal/api/** access.
 */
@Configuration
public class InternalApiBlockConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "evcs.gateway.security.internal-api-block")
    public InternalApiBlockProperties internalApiBlockProperties() {
        return new InternalApiBlockProperties();
    }

    @Bean
    public WebFilter internalApiBlockWebFilter(InternalApiBlockProperties properties) {
        return new InternalApiBlockWebFilter(properties);
    }
}
