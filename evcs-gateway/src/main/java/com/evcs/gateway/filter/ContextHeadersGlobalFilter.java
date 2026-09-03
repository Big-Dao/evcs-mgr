package com.evcs.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Derives and injects trusted user/tenant context headers from JWT.
 *
 * <p>Current behavior is migration-safe:
 * <ul>
 *   <li>When enabled, if a valid Bearer token exists, inject headers derived from token claims.</li>
 *   <li>Optionally strip client-supplied context headers to prevent spoofing.</li>
 *   <li>Does not enforce authentication yet (no 401/403), only context enrichment.</li>
 * </ul>
 *
 * <p>安全说明：启用时（enabled=true）要求 {@code jwt-secret} 已配置且 &ge; 32 字符，
 * 否则启动失败。这避免了"启用过滤器却以空/弱密钥运行"的隐性安全退化。
 */
public class ContextHeadersGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_TYPE = "X-User-Type";
    public static final String HEADER_TENANT_TYPE = "X-Tenant-Type";
    public static final String HEADER_TENANT_ANCESTORS = "X-Tenant-Ancestors";

    private final ContextHeadersProperties properties;

    public ContextHeadersGlobalFilter(ContextHeadersProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    /**
     * 启用时校验 JWT 密钥强度。缺失或过短都会让网关启动失败，
     * 避免过滤器启用却以空/弱密钥静默放行。
     */
    @PostConstruct
    void validateSecretStrength() {
        if (!properties.isEnabled()) {
            return;
        }
        String jwtSecret = properties.getJwtSecret();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "evcs.gateway.security.context-headers.jwt-secret 未配置。"
                            + "启用 context-headers 时必须注入至少 32 字符的随机密钥。"
            );
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "evcs.gateway.security.context-headers.jwt-secret 过短（当前 "
                            + jwtSecret.length() + " 字符）。请使用至少 32 字符的随机密钥。"
            );
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String token = extractBearerToken(exchange.getRequest().getHeaders());
        DecodedJWT jwt = token == null ? null : verifyAndDecode(token);

        Long tenantId = jwt == null ? null : readTenantId(jwt);
        Long userId = jwt == null ? null : readUserId(jwt);
        String userType = jwt == null ? null : readUserType(jwt);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .headers(headers -> {
                if (properties.isStripClientContextHeaders()) {
                    headers.remove(HEADER_TENANT_ID);
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_TYPE);
                    // 租户类型/祖先链不来自 JWT claim，客户端伪造即租户越权入口，必须剥离
                    headers.remove(HEADER_TENANT_TYPE);
                    headers.remove(HEADER_TENANT_ANCESTORS);
                }

                if (tenantId != null) {
                    headers.set(HEADER_TENANT_ID, String.valueOf(tenantId));
                }
                if (userId != null) {
                    headers.set(HEADER_USER_ID, String.valueOf(userId));
                }
                if (userType != null && !userType.isBlank()) {
                    headers.set(HEADER_USER_TYPE, userType);
                }
            })
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private DecodedJWT verifyAndDecode(String token) {
        String jwtSecret = properties.getJwtSecret();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            return null;
        }

        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (JWTVerificationException ex) {
            return null;
        }
    }

    private static String extractBearerToken(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.startsWith("Bearer ")) {
            return null;
        }
        String token = value.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static Long readTenantId(DecodedJWT jwt) {
        Long tenantId = jwt.getClaim("tenantId").asLong();
        return tenantId;
    }

    private static Long readUserId(DecodedJWT jwt) {
        // Preferred: standard subject.
        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException ignored) {
                // fallback to legacy claim.
            }
        }

        // Legacy compatibility: older tokens use userId claim.
        return jwt.getClaim("userId").asLong();
    }

    private static String readUserType(DecodedJWT jwt) {
        return jwt.getClaim("userType").asString();
    }
}
