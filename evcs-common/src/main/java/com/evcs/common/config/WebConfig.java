package com.evcs.common.config;

import com.evcs.common.filter.RequestIdFilter;
import com.evcs.common.tenant.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 注册拦截器、转换器等Web相关配置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final TenantInterceptor tenantInterceptor;
    
    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 静态资源
                        "/static/**",
                        "/public/**",
                        "/assets/**",
                        "/favicon.ico",
                        
                        // API文档
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        
                        // 健康检查
                        "/actuator/**",
                        
                        // 登录相关（无需租户上下文）
                        "/auth/login",
                        "/auth/refresh",
                        
                        // 其他不需要租户上下文的接口
                        "/tenant/check-code"
                )
                .order(1); // 设置拦截器执行顺序
    }
    
    /**
     * 注册RequestId过滤器
     * 为非网关服务生成X-Request-Id，以便与网关一致
     */
    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(0); // 最先执行，确保所有请求都有RequestId
        registration.setName("requestIdFilter");
        return registration;
    }
}