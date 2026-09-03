package com.evcs.order.config;

import com.evcs.common.internalapi.InternalApiTokenFilter;
import com.evcs.common.internalapi.InternalApiTokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内部 API 令牌防护配置（order 服务）。
 *
 * <p>复用 evcs-common 的通用实现；Bean 名带服务前缀，
 * 避免集成测试同时扫描多个服务包时的同名冲突。
 */
@Configuration("orderInternalApiTokenConfiguration")
@EnableConfigurationProperties(InternalApiTokenProperties.class)
public class InternalApiTokenConfiguration {

    @Bean("orderInternalApiTokenFilter")
    public InternalApiTokenFilter internalApiTokenFilter(InternalApiTokenProperties properties) {
        return new InternalApiTokenFilter(properties);
    }
}
