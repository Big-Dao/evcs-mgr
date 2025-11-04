package com.evcs.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 网关安全配置
 * 集成限流、熔断、CORS等安全功能
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    // CORS配置已移至CorsConfig类，避免Bean名称冲突

    /**
     * 路由配置（简化版，移除熔断器）
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 认证服务路由
            .route("auth-service", r -> r.path("/api/auth/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://evcs-auth")
            )

            // 租户服务路由
            .route("tenant-service", r -> r.path("/api/tenant/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://evcs-tenant")
            )

            // 充电站服务路由
            .route("station-service", r -> r.path("/api/station/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://evcs-station")
            )

            // 订单服务路由
            .route("order-service", r -> r.path("/api/order/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://evcs-order")
            )

            // 支付服务路由
            .route("payment-service", r -> r.path("/api/payment/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://evcs-payment")
            )

            // 协议服务路由
            .route("protocol-service", r -> r.path("/api/protocol/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://evcs-protocol")
            )

            // Dashboard API路由
            .route("dashboard-api", r -> r.path("/api/dashboard/**")
                .filters(f -> f.stripPrefix(2))
                .uri("lb://evcs-tenant")
            )

            .build();
    }

    /**
     * 禁用CSRF保护的Spring Security配置
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
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

  }