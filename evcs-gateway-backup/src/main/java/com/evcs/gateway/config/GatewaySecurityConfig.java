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

    // 路由配置暂时禁用，使用YAML配置以避免冲突
    // CORS配置已移至CorsConfig类，避免Bean名称冲突

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

    // 临时禁用复杂的JWT过滤器以排查问题
    // 需要在@Bean配置中排除这些过滤器

    // GlobalFilter暂时禁用以排查问题
    // /**
    //  * 安全监控过滤器
    //  */
    // @Bean
    // public GlobalFilter securityMonitoringFilter() { ... }

    // /**
    //  * 安全响应头过滤器
    //  */
    // @Bean
    // public GlobalFilter securityHeadersFilter() { ... }

  }