package com.evcs.gateway.filter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceIdFilterConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "evcs.gateway.trace")
    public TraceIdFilterProperties traceIdFilterProperties() {
        return new TraceIdFilterProperties();
    }

    @Bean
    @ConditionalOnProperty(prefix = "evcs.gateway.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TraceIdGlobalFilter traceIdGlobalFilter(TraceIdFilterProperties properties) {
        return new TraceIdGlobalFilter(properties);
    }
}
