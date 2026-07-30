package com.evcs.auth.config;

import com.evcs.auth.security.AuthJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（auth 服务）。
 *
 * <p>职责：
 * <ul>
 *   <li>白名单放行公开端点（登录/刷新/健康检查/文档）。</li>
 *   <li>其余端点要求认证；细粒度授权由 {@code @PreAuthorize} 提供。</li>
 *   <li>无状态会话；JWT 过滤器在 {@link UsernamePasswordAuthenticationFilter} 之前插入。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private final AuthJwtAuthenticationFilter jwtAuthenticationFilter;

    /** 密码编码器 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 安全过滤链 */
    @Bean("authFilterChain")
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（无状态 JWT）
                .csrf(csrf -> csrf.disable())
                // 无状态Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 请求授权
                .authorizeHttpRequests(auth -> auth
                        // 公开端点：登录、刷新、健康检查、API 文档（白名单最小化，业务端点需认证）
                        .requestMatchers(
                                "/auth/login",
                                "/auth/refresh",
                                "/health", "/health/**",
                                // 只放行非敏感 actuator 端点，禁止前缀匹配避免暴露 env/heapdump
                                "/actuator/health",
                                "/actuator/info",
                                "/api/health", "/api/health/**",
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        // 其余端点要求认证（含 /auth/logout、/auth/userinfo 等业务端点）
                        .anyRequest().authenticated()
                )
                // 禁用默认登录页与 HTTP Basic
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // 插入 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
