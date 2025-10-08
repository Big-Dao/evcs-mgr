package com.evcs.common.config;

import com.evcs.common.tenant.TenantInterceptor;
import lombok.RequiredArgsConstructor;
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

    // TODO: 可增加一个 Servlet Filter 为非网关服务生成 X-Request-Id，以便与网关一致

    }
}