package com.evcs.payment.config;

import com.evcs.payment.security.PaymentJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（payment 服务）。
 *
 * <p>职责：
 * <ul>
 *   <li>白名单放行公开端点（健康检查/文档）。</li>
 *   <li>其余端点要求认证；细粒度授权由 {@code @PreAuthorize} 提供（现已真正生效）。</li>
 *   <li>无状态会话；JWT 过滤器在 {@link UsernamePasswordAuthenticationFilter} 之前插入。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class PaymentSecurityConfig {

    private final PaymentJwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // 只放行非敏感 actuator 端点，禁止前缀匹配避免暴露 env/heapdump
                                "/actuator/health",
                                "/actuator/info",
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                // 测试专用端点（仅测试 classpath 存在，生产无影响）
                                "/__test/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
