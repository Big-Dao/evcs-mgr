package com.evcs.tenant.config;

import com.evcs.tenant.security.TenantJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（tenant 服务）。
 *
 * <p>白名单放行健康检查/文档，其余端点要求认证；细粒度授权由 @PreAuthorize 提供。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class TenantSecurityConfig {

    private final TenantJwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean("tenantFilterChain")
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 显式绑定租户权限评估器：否则 hasPermission() 落入 DenyAllPermissionEvaluator，
     * 所有 @PreAuthorize("hasPermission(...)") 一律 403。
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            com.evcs.tenant.config.TenantRolePermissionEvaluator evaluator) {
        var handler = new org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(evaluator);
        return handler;
    }
}
