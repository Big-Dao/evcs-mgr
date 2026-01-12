package com.evcs.gateway.filter;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

/**
 * Fail-fast guard: prevent any gateway route from exposing /internal/api/**.
 */
@Configuration
public class InternalApiRouteGuardConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "evcs.gateway.security.internal-api-route-guard")
    public InternalApiRouteGuardProperties internalApiRouteGuardProperties() {
        return new InternalApiRouteGuardProperties();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "evcs.gateway.security.internal-api-route-guard",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public ApplicationRunner internalApiRouteGuardRunner(
            RouteDefinitionLocator routeDefinitionLocator,
            InternalApiRouteGuardProperties properties
    ) {
        return new InternalApiRouteGuardRunner(routeDefinitionLocator, properties);
    }
}
