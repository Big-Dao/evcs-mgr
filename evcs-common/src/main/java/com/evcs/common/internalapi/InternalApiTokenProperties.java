package com.evcs.common.internalapi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内部 API 共享令牌配置（服务间 /internal/api/** 调用的防御纵深）。
 *
 * <p>外部访问由网关在边缘封锁 /internal/api/** 前缀；服务内由
 * {@link InternalApiTokenFilter} 校验共享令牌。新服务直接复用本配置类；
 * evcs-tenant / evcs-station 历史上各有一份等价实现。
 */
@Data
@ConfigurationProperties(prefix = "evcs.internal.api")
public class InternalApiTokenProperties {

    /**
     * Whether to enforce internal API token validation for /internal/api/**.
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

    /**
     * Request path prefix for internal APIs.
     */
    private String pathPrefix = "/internal/api/";
}
