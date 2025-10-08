package com.evcs.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF
            .csrf(csrf -> csrf.disable())
            // 无状态Session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 请求授权配置
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            // 禁用默认登录页面
            .formLogin(form -> form.disable())
            // 禁用HTTP Basic认证
            .httpBasic(basic -> basic.disable());
            
        return http.build();
    }
}