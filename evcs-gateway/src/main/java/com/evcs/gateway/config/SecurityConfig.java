package com.evcs.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 安全配置类
 * 简化版的安全配置，专注于基本功能
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)

            // 配置路径访问权限 - 暂时允许所有请求，后续可完善
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )

            .build();
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 设置允许的源
        corsConfig.addAllowedOrigin("http://localhost:3000");
        corsConfig.addAllowedOrigin("http://localhost:8080");
        corsConfig.addAllowedOrigin("http://localhost:8081");

        // 设置允许的方法
        corsConfig.addAllowedMethod(HttpMethod.GET);
        corsConfig.addAllowedMethod(HttpMethod.POST);
        corsConfig.addAllowedMethod(HttpMethod.PUT);
        corsConfig.addAllowedMethod(HttpMethod.DELETE);
        corsConfig.addAllowedMethod(HttpMethod.OPTIONS);

        // 设置允许的头部
        corsConfig.addAllowedHeader("*");

        // 其他配置
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        log.info("CORS configured with basic settings");

        return new CorsWebFilter(source);
    }

    /**
     * 路由配置
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 充电站服务路由
            .route("charger-service", r -> r.path("/api/v1/chargers/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .circuitBreaker(config -> config
                        .setName("charger-service")
                        .setFallbackUri("forward:/fallback/charger"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1)))
                )
                .uri("lb://evcs-charger")
            )

            // 用户服务路由
            .route("user-service", r -> r.path("/api/v1/users/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .circuitBreaker(config -> config
                        .setName("user-service")
                        .setFallbackUri("forward:/fallback/user"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(2)
                        .setBackoff(Duration.ofMillis(50), Duration.ofMillis(500)))
                )
                .uri("lb://evcs-user")
            )

            // 支付服务路由
            .route("payment-service", r -> r.path("/api/v1/payments/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .circuitBreaker(config -> config
                        .setName("payment-service")
                        .setFallbackUri("forward:/fallback/payment"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(1)
                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(2)))
                )
                .uri("lb://evcs-payment")
            )

            // 协议服务路由
            .route("protocol-service", r -> r.path("/protocol/**")
                .filters(f -> f
                    .circuitBreaker(config -> config
                        .setName("protocol-service")
                        .setFallbackUri("forward:/fallback/protocol"))
                )
                .uri("lb://evcs-protocol")
            )

            // 认证服务路由
            .route("auth-service", r -> r.path("/auth/**")
                .filters(f -> f.stripPrefix(1))
                .uri("lb://evcs-auth")
            )

            // 租户服务路由
            .route("tenant-service", r -> r.path("/tenant/**")
                .filters(f -> f.stripPrefix(1))
                .uri("lb://evcs-tenant")
            )

            // 订单服务路由
            .route("order-service", r -> r.path("/api/v1/orders/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .circuitBreaker(config -> config
                        .setName("order-service")
                        .setFallbackUri("forward:/fallback/order"))
                )
                .uri("lb://evcs-order")
            )

            .build();
    }
}