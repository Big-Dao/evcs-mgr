package com.evcs.gateway.config;

import com.evcs.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 从请求中提取 Bearer JWT 并校验。
 *
 * <p>校验通过返回认证令牌（含 userId/tenantId 主体）；无 token 或校验失败返回 empty，
 * 由入口返回 401。
 */
@RequiredArgsConstructor
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String token = extractBearerToken(exchange.getRequest().getHeaders());
        if (token == null) {
            return Mono.empty();
        }

        if (!jwtUtil.verifyToken(token)) {
            return Mono.empty();
        }

        Long userId = jwtUtil.getUserId(token);
        Long tenantId = jwtUtil.getTenantId(token);
        if (userId == null || tenantId == null) {
            return Mono.empty();
        }

        return Mono.just(new UsernamePasswordAuthenticationToken(
                new GatewaySecurityConfig.JwtPrincipal(userId, tenantId), null, null));
    }

    private static String extractBearerToken(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.startsWith("Bearer ")) {
            return null;
        }
        String token = value.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
