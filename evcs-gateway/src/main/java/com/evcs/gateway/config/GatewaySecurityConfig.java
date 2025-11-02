package com.evcs.gateway.config;

import com.evcs.gateway.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * 网关安全配置
 * 集成限流、熔断、CORS等安全功能
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final RateLimitingFilter rateLimitingFilter;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private List<String> allowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    /**
     * CORS配置
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 设置允许的源
        allowedOrigins.forEach(origin -> {
            if (isValidOrigin(origin)) {
                corsConfig.addAllowedOrigin(origin);
                log.debug("Added allowed origin: {}", origin);
            } else {
                log.warn("Invalid origin configuration: {}", origin);
            }
        });

        // 设置允许的方法
        allowedMethods.forEach(method -> {
            try {
                HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
                corsConfig.addAllowedMethod(httpMethod);
            } catch (Exception e) {
                log.warn("Invalid HTTP method: {}", method);
            }
        });

        // 设置允许的头部
        if (allowedHeaders.contains("*")) {
            corsConfig.addAllowedHeader("*");
        } else {
            allowedHeaders.forEach(corsConfig::addAllowedHeader);
        }

        // 其他配置
        corsConfig.setAllowCredentials(allowCredentials);
        corsConfig.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        log.info("CORS configured with origins: {}, methods: {}, credentials: {}",
                allowedOrigins, allowedMethods, allowCredentials);

        return new CorsWebFilter(source);
    }

    /**
     * 路由配置（包含熔断和重试机制）
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
                        .setRetries(3))
                    .filter(rateLimitingFilter.apply(new RateLimitingFilter.Config()))
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
                        .setRetries(2))
                    .filter(rateLimitingFilter.apply(new RateLimitingFilter.Config()))
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
                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(2), 2, true))
                    .filter(rateLimitingFilter.apply(new RateLimitingFilter.Config()))
                )
                .uri("lb://evcs-payment")
            )

            // 协议服务路由
            .route("protocol-service", r -> r.path("/protocol/**")
                .filters(f -> f
                    .circuitBreaker(config -> config
                        .setName("protocol-service")
                        .setFallbackUri("forward:/fallback/protocol"))
                    .filter(rateLimitingFilter.apply(new RateLimitingFilter.Config()))
                )
                .uri("lb://evcs-protocol")
            )

            // 认证服务路由
            .route("auth-service", r -> r.path("/auth/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .circuitBreaker(config -> config
                        .setName("auth-service")
                        .setFallbackUri("forward:/fallback/auth"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(2))
                )
                .uri("lb://evcs-auth")
            )

            .build();
    }

    /**
     * 安全监控过滤器
     */
    @Bean
    public GlobalFilter securityMonitoringFilter() {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            var request = exchange.getRequest();

            // 记录请求开始
            log.debug("Processing request: {} {}", request.getMethod(), request.getPath());

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                var response = exchange.getResponse();

                // 记录慢请求
                if (duration > 5000) { // 超过5秒的请求
                    log.warn("Slow request detected: {}ms for {} {}",
                            duration, request.getMethod(), request.getPath());
                }

                // 记录错误响应
                if (response.getStatusCode() != null &&
                    (response.getStatusCode().is4xxClientError() ||
                     response.getStatusCode().is5xxServerError())) {
                    log.warn("HTTP error response: {} for {} {}",
                            response.getStatusCode(), request.getMethod(), request.getPath());
                }

                // 记录请求完成
                log.debug("Request completed: {} {} - {} ({}ms)",
                        request.getMethod(), request.getPath(),
                        response.getStatusCode() != null ? response.getStatusCode() : "UNKNOWN",
                        duration);
            }));
        };
    }

    /**
     * 安全响应头过滤器
     */
    @Bean
    public GlobalFilter securityHeadersFilter() {
        return (exchange, chain) -> {
            var response = exchange.getResponse();

            // 添加安全响应头
            response.getHeaders().add("X-Content-Type-Options", "nosniff");
            response.getHeaders().add("X-Frame-Options", "DENY");
            response.getHeaders().add("X-XSS-Protection", "1; mode=block");
            response.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
            response.getHeaders().add("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");

            return chain.filter(exchange);
        };
    }

    /**
     * 验证Origin是否有效
     */
    private boolean isValidOrigin(String origin) {
        if (origin == null || origin.trim().isEmpty()) {
            return false;
        }

        // 基本格式检查
        return origin.matches("^https?://[a-zA-Z0-9.-]+(?::\\d+)?(?:/.*)?$") ||
               origin.equals("localhost") ||
               origin.startsWith("http://localhost") ||
               origin.startsWith("https://localhost");
    }
}