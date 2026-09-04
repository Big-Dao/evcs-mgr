package com.evcs.gateway.config;

import com.evcs.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关安全配置（Spring Cloud Gateway + Spring Security reactive）。
 *
 * <p>职责：
 * <ul>
 *   <li>白名单路径（登录/刷新/健康检查/文档）放行，无需认证。</li>
 *   <li>其余路径要求有效的 Bearer JWT；无效或缺失返回 401。</li>
 *   <li>无状态：不维护会话（{@code NoOpServerSecurityContextRepository}），JWT 自包含。</li>
 * </ul>
 *
 * <p>注意：网关只做入口鉴权。细粒度授权（@PreAuthorize）在各下游服务内执行。
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final JwtUtil jwtUtil;

    /** 放行路径：登录、刷新、健康检查、API 文档等公开接口。 */
    private static final String[] WHITELIST_PATHS = {
            "/auth/login",
            "/auth/refresh",
            // 只放行非敏感 actuator 端点（health/info），禁止前缀匹配避免暴露 env/heapdump/beans
            "/actuator/health",
            "/actuator/info",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/favicon.ico",
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(WHITELIST_PATHS).permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        })
                );

        // 在认证过滤器之前插入自定义 JWT 过滤器。
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(jwtAuthenticationManager());
        jwtFilter.setServerAuthenticationConverter(new JwtServerAuthenticationConverter(jwtUtil));
        // 认证成功时不写入安全上下文（无状态），仅放行让后续过滤器/路由继续。
        jwtFilter.setAuthenticationSuccessHandler((webFilterExchange, authentication) ->
                Mono.empty());

        http.addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

    @Bean
    public ReactiveAuthenticationManager jwtAuthenticationManager() {
        return authentication -> {
            if (authentication instanceof UsernamePasswordAuthenticationToken token
                    && token.getPrincipal() instanceof JwtPrincipal principal) {
                // 已被 JwtServerAuthenticationConverter 校验过 JWT，直接标记认证成功。
                return Mono.just(UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
                );
            }
            return Mono.error(new org.springframework.security.core.AuthenticationException("Invalid JWT") {
            });
        };
    }

    /**
     * 简单的 JWT 持有者身份标识。
     */
    public record JwtPrincipal(Long userId, Long tenantId) {
    }
}
