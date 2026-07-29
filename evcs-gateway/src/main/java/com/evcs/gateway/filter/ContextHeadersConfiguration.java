package com.evcs.gateway.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关可信上下文头注入配置。
 *
 * <p>启用后（默认启用）：
 * <ul>
 *   <li>从请求 JWT 中派生 tenant/user 上下文，注入 {@code X-Tenant-Id}/{@code X-User-Id}/{@code X-User-Type} 头。</li>
 *   <li>默认剥离客户端同名的伪造头（{@code stripClientContextHeaders=true}），确保下游只见到可信头。</li>
 * </ul>
 *
 * <p>前缀：{@code evcs.gateway.security.context-headers}。
 */
@Configuration
public class ContextHeadersConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "evcs.gateway.security.context-headers")
    public ContextHeadersProperties contextHeadersProperties() {
        return new ContextHeadersProperties();
    }

    @Bean
    public ContextHeadersGlobalFilter contextHeadersGlobalFilter(ContextHeadersProperties properties) {
        return new ContextHeadersGlobalFilter(properties);
    }
}
