package com.evcs.gateway.filter;

/**
 * Gateway context headers injection settings.
 *
 * <p>前缀：{@code evcs.gateway.security.context-headers}。
 */
public class ContextHeadersProperties {

    /**
     * 是否启用可信上下文头注入。默认启用：网关从 JWT 派生 tenant/user 头注入下游。
     */
    private boolean enabled = true;

    /**
     * Shared secret for verifying legacy HMAC JWTs.
     *
     * <p>必填，无默认值。生产环境必须通过
     * {@code evcs.gateway.security.context-headers.jwt-secret} 注入至少 32 字符的随机密钥。
     * 缺失或过短将在网关过滤器启用时导致启动失败。
     */
    private String jwtSecret;

    /**
     * 若 true（默认），在注入可信头之前先剥离客户端同名的伪造头。
     * 这防止客户端通过 X-Tenant-Id 等头伪造租户身份，强烈建议保持开启。
     */
    private boolean stripClientContextHeaders = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public boolean isStripClientContextHeaders() {
        return stripClientContextHeaders;
    }

    public void setStripClientContextHeaders(boolean stripClientContextHeaders) {
        this.stripClientContextHeaders = stripClientContextHeaders;
    }
}
