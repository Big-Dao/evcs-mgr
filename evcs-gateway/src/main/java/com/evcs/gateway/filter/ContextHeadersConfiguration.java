package com.evcs.gateway.filter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextHeadersConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "evcs.gateway.security.context-headers")
    public ContextHeadersProperties contextHeadersProperties() {
        return new ContextHeadersProperties();
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "evcs.gateway.security.context-headers",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
    )
    public ContextHeadersGlobalFilter contextHeadersGlobalFilter(ContextHeadersProperties properties) {
        return new ContextHeadersGlobalFilter(properties);
    }
}
