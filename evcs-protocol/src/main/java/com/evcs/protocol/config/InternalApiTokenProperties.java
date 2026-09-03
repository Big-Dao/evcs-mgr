package com.evcs.protocol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内部 API 调用配置：调用其他服务的 /internal/api/** 端点时携带共享内部令牌。
 */
@Data
@ConfigurationProperties(prefix = "evcs.internal.api")
public class InternalApiTokenProperties {

    /**
     * Whether to attach the internal API token for /internal/api/** calls.
     */
    private boolean enabled = false;

    /**
     * Shared token for service-to-service internal API calls.
     */
    private String token;

    /**
     * Header name carrying the internal token.
     */
    private String headerName = "X-Internal-Token";
}
