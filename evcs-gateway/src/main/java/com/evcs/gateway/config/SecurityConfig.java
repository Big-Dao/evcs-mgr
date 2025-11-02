package com.evcs.gateway.config;

import com.evcs.gateway.security.CircuitBreakerService;
import com.evcs.gateway.security.EnhancedJwtAuthFilter;
import com.evcs.gateway.security.JwtBlacklistService;
import com.evcs.gateway.security.RateLimitingFilter;
import com.evcs.gateway.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 安全配置类
 * 集成所有安全组件：JWT认证、限流、熔断、审计等
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final EnhancedJwtAuthFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final SecurityAuditService auditService;
    private final JwtBlacklistService blacklistService;
    private final CircuitBreakerService circuitBreakerService;

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
     * 安全过滤器链配置
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)

            // 配置路径访问权限
            .authorizeExchange(exchanges -> exchanges
                // 允许访问的路径
                .pathMatchers(
                    "/auth/login",
                    "/auth/register",
                    "/auth/refresh",
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()

                // 管理员路径
                .pathMatchers("/actuator/**", "/admin/**").hasRole("ADMIN")

                // 充电站相关路径
                .pathMatchers("/api/v1/chargers/**").hasAnyRole("USER", "ADMIN", "OPERATOR")
                .pathMatchers("/api/v1/sessions/**").hasAnyRole("USER", "ADMIN", "OPERATOR")
                .pathMatchers("/api/v1/payments/**").hasAnyRole("USER", "ADMIN", "OPERATOR")

                // 协议相关路径
                .pathMatchers("/protocol/**").hasAnyRole("DEVICE", "ADMIN", "OPERATOR")

                // 其他所有路径需要认证
                .anyExchange().authenticated()
            )

            // 添加JWT认证过滤器
            .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)

            // 添加限流过滤器
            .addFilterAt(rateLimitingFilter, SecurityWebFiltersOrder.RATE_LIMIT)

            // 异常处理
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((exchange, ex) -> {
                    log.warn("Authentication failed for path: {}", exchange.getRequest().getPath());
                    auditService.logAccessDenied(
                        exchange.getRequest(),
                        "Authentication required",
                        null
                    );
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                })
                .accessDeniedHandler((exchange, denied) -> {
                    log.warn("Access denied for path: {}", exchange.getRequest().getPath());
                    auditService.logAccessDenied(
                        exchange.getRequest(),
                        "Access denied",
                        null
                    );
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                })
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
                corsConfig.addAllowedMethod(HttpMethod.resolve(method));
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
     * 路由配置（可选，也可以在application.yml中配置）
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
                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true))
                    .requestRateLimiter(config -> config
                        .setKeyResolver(exchange -> exchange.getRequest().getRemoteAddress().getAddress().getHostAddress())
                        .setRateLimiter(redisRateLimiter()))
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
                        .setBackoff(Duration.ofMillis(50), Duration.ofMillis(500), 1.5, false))
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

            .build();
    }

    /**
     * Redis限流器配置
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1); // replenishRate, burstCapacity, requestedTokens
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

    /**
     * 自定义Redis限流器
     */
    public static class RedisRateLimiter implements org.springframework.cloud.gateway.filter.ratelimit.KeyResolver {
        @Override
        public reactor.core.publisher.Mono<String> resolve(org.springframework.web.server.ServerWebExchange exchange) {
            return reactor.core.publisher.Mono.just(
                exchange.getRequest().getRemoteAddress() != null ?
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                    "unknown"
            );
        }
    }

    /**
     * 安全监控端点
     */
    @Bean
    public GlobalFilter securityMonitoringFilter() {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                var request = exchange.getRequest();
                var response = exchange.getResponse();

                // 记录慢请求
                if (duration > 5000) { // 超过5秒的请求
                    auditService.logSuspiciousActivity(
                        request,
                        String.format("Slow request detected: %dms for %s %s",
                            duration, request.getMethod(), request.getPath())
                    );
                }

                // 记录错误响应
                if (response.getStatusCode() != null &&
                    response.getStatusCode().is4xxClientError() ||
                    response.getStatusCode().is5xxServerError()) {
                    auditService.logSecurityError(
                        request,
                        new RuntimeException(String.format("HTTP error: %s", response.getStatusCode()))
                    );
                }
            }));
        };
    }
}