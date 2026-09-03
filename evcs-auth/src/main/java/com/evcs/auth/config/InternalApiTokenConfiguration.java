package com.evcs.auth.config;

import com.evcs.common.internalapi.InternalApiTokenFilter;
import com.evcs.common.internalapi.InternalApiTokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内部 API 令牌防护配置（auth 服务）。
 *
 * <p>复用 evcs-common 的通用实现；Bean 名带服务前缀，避免跨服务扫描冲突。
 */
@Configuration("authInternalApiTokenConfiguration")
@EnableConfigurationProperties(InternalApiTokenProperties.class)
public class InternalApiTokenConfiguration {

    @Bean("authInternalApiTokenFilter")
    public InternalApiTokenFilter internalApiTokenFilter(InternalApiTokenProperties properties) {
        return new InternalApiTokenFilter(properties);
    }
}
