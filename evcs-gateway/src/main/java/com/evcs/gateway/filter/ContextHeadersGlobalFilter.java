package com.evcs.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
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
 */
public class ContextHeadersGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_TYPE = "X-User-Type";

    private final ContextHeadersProperties properties;

    public ContextHeadersGlobalFilter(ContextHeadersProperties properties) {
        this.properties = Objects.requireNonNull(properties);
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
