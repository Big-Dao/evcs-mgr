package com.evcs.tenant.config;

import com.evcs.common.tenant.TenantContext;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 测试配置类
 * 排除Knife4j自动配置以避免javax.servlet依赖问题
 *
 * @author EVCS
 * @since 2025-01-07
 */
@TestConfiguration
@EnableAutoConfiguration(
    excludeName = "com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration"
)
public class TestConfig {

    /**
     * 测试用过滤器：在 TenantInterceptor 之前设置默认租户上下文，
     * 避免 MockMvc 请求因缺少 X-Tenant-Id 头而 500。
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public OncePerRequestFilter testTenantContextFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                if (TenantContext.getCurrentTenantId() == null) {
                    TenantContext.setCurrentTenantId(1L);
                    TenantContext.setCurrentUserId(1L);
                }
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    TenantContext.clear();
                }
            }
        };
    }
}
