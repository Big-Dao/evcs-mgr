package com.evcs.gateway.security;

import com.evcs.common.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 增强的JWT认证过滤器
 * 集成了JWT黑名单、令牌刷新、安全日志等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnhancedJwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final JwtBlacklistService blacklistService;
    private final SecurityAuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> WHITELIST = Set.of(
            "/auth/login",
            "/auth/refresh",
            "/api/auth/login",
            "/api/auth/refresh",
            "/doc.html",
            "/webjars/",
            "/v3/api-docs",
            "/swagger-ui/",
            "/actuator/health",
            "/actuator/info"
    );

    private static final long TOKEN_REFRESH_THRESHOLD_SECONDS = 300; // 5分钟内即将过期的令牌

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 规范化路径以防止路径遍历攻击
        String normalizedPath = normalizePath(path);

        // 注入请求ID（如果缺失）
        final String requestId = injectRequestId(exchange);

        // 白名单路径直接通过
        if (isWhitelisted(normalizedPath)) {
            return chain.filter(exchange);
        }

        // 记录安全事件
        auditService.logSecurityEvent(request, "AUTH_REQUEST_RECEIVED");

        // 提取和验证JWT令牌
        return extractAndValidateToken(request)
                .flatMap(tokenValidationResult -> {
                    if (tokenValidationResult.isValid()) {
                        // 检查令牌是否即将过期，如果需要则刷新
                        if (shouldRefreshToken(tokenValidationResult.getToken())) {
                            String newToken = jwtUtil.refreshToken(tokenValidationResult.getToken());
                            if (newToken != null) {
                                exchange.getResponse().getHeaders().add("X-New-Token", newToken);
                                log.info("Token refreshed for request: {}", requestId);
                            }
                        }

                        // 传播用户信息到下游服务
                        ServerWebExchange mutated = propagateUserInfo(exchange, tokenValidationResult);

                        // 记录认证成功事件
                        auditService.logAuthenticationSuccess(request, tokenValidationResult.getUserId());

                        return chain.filter(mutated);
                    } else {
                        // 认证失败，记录安全事件
                        auditService.logAuthenticationFailure(request, tokenValidationResult.getError());

                        return unauthorized(exchange, tokenValidationResult.getError(), tokenValidationResult.getUserId());
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("Unexpected error during JWT authentication", throwable);
                    auditService.logSecurityError(request, throwable);
                    return unauthorized(exchange, "Authentication service error", null);
                });
    }

    /**
     * 提取和验证JWT令牌
     */
    private Mono<TokenValidationResult> extractAndValidateToken(ServerHttpRequest request) {
        return Mono.fromCallable(() -> {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // 检查认证头
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new TokenValidationResult(false, null, "Missing or invalid Authorization header", null);
            }

            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                return new TokenValidationResult(false, null, "Token is empty", null);
            }

            // 检查令牌格式
            if (!isValidTokenFormat(token)) {
                return new TokenValidationResult(false, null, "Invalid token format", null);
            }

            // 检查令牌是否在黑名单中
            if (blacklistService.isBlacklisted(token)) {
                return new TokenValidationResult(false, null, "Token has been blacklisted", null);
            }

            // 检查用户是否被列入黑名单
            Long userId = jwtUtil.getUserId(token);
            if (userId != null && blacklistService.isUserBlacklisted(userId)) {
                return new TokenValidationResult(false, token, "User has been blacklisted", userId);
            }

            // 验证令牌签名和过期时间
            if (!jwtUtil.verifyToken(token)) {
                return new TokenValidationResult(false, token, "Invalid token signature", userId);
            }

            if (jwtUtil.isTokenExpired(token)) {
                return new TokenValidationResult(false, token, "Token has expired", userId);
            }

            return new TokenValidationResult(true, token, null, userId);
        });
    }

    /**
     * 注入请求ID
     */
    private String injectRequestId(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = request.getHeaders().getFirst("X-Request-Id");

        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
            exchange.getRequest().mutate().headers(headers -> headers.set("X-Request-Id", requestId));
        }

        return requestId;
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(pattern ->
                pattern.endsWith("/") ? path.startsWith(pattern) : path.equals(pattern));
    }

    /**
     * 规范化路径以防止路径遍历攻击
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        path = path.trim();

        String[] segments = path.split("/");
        StringBuilder normalized = new StringBuilder();

        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                int lastSlash = normalized.lastIndexOf("/");
                if (lastSlash > 0) {
                    normalized.setLength(lastSlash);
                }
                continue;
            }
            normalized.append("/").append(segment);
        }

        return normalized.length() == 0 ? "/" : normalized.toString();
    }

    /**
     * 检查令牌格式是否有效
     */
    private boolean isValidTokenFormat(String token) {
        try {
            // JWT令牌应该包含3个部分，用点分隔
            String[] parts = token.split("\\.");
            return parts.length == 3;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否需要刷新令牌
     */
    private boolean shouldRefreshToken(String token) {
        try {
            Instant expiration = jwtUtil.getExpiration(token);
            if (expiration != null) {
                Duration timeUntilExpiration = Duration.between(Instant.now(), expiration);
                return timeUntilExpiration.getSeconds() < TOKEN_REFRESH_THRESHOLD_SECONDS;
            }
        } catch (Exception e) {
            log.debug("Unable to check token expiration for refresh", e);
        }
        return false;
    }

    /**
     * 传播用户信息到下游服务
     */
    private ServerWebExchange propagateUserInfo(ServerWebExchange exchange, TokenValidationResult validation) {
        String token = validation.getToken();
        Long userId = validation.getUserId();
        Long tenantId = jwtUtil.getTenantId(token);
        String username = jwtUtil.getUsername(token);
        String roles = jwtUtil.getRoles(token);

        return exchange.mutate()
                .request(r -> r.headers(httpHeaders -> {
                    httpHeaders.set("X-User-Id", String.valueOf(userId));
                    if (tenantId != null) {
                        httpHeaders.set("X-Tenant-Id", String.valueOf(tenantId));
                    }
                    if (username != null) {
                        httpHeaders.set("X-Username", username);
                    }
                    if (roles != null) {
                        httpHeaders.set("X-User-Roles", roles);
                    }
                }))
                .build();
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message, Long userId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "code", 401,
                "message", message,
                "timestamp", Instant.now().toString(),
                "path", exchange.getRequest().getPath().value(),
                "requestId", exchange.getRequest().getHeaders().getFirst("X-Request-Id")
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            bytes = "{\"code\":401,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }

        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -200; // 高优先级，在其他过滤器之前执行
    }

    /**
     * 令牌验证结果
     */
    private static class TokenValidationResult {
        private final boolean valid;
        private final String token;
        private final String error;
        private final Long userId;

        public TokenValidationResult(boolean valid, String token, String error, Long userId) {
            this.valid = valid;
            this.token = token;
            this.error = error;
            this.userId = userId;
        }

        public boolean isValid() { return valid; }
        public String getToken() { return token; }
        public String getError() { return error; }
        public Long getUserId() { return userId; }
    }
}