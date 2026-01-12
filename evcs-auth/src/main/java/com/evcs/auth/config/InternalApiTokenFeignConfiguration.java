package com.evcs.auth.config;

import feign.RequestInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(InternalApiTokenProperties.class)
public class InternalApiTokenFeignConfiguration {

    @Bean
    public RequestInterceptor internalApiTokenRequestInterceptor(final InternalApiTokenProperties properties) {
        return template -> {
            if (!properties.isEnabled()) {
                return;
            }
            if (!StringUtils.hasText(properties.getToken())) {
                return;
            }
            template.header(properties.getHeaderName(), properties.getToken());
        };
    }
}
