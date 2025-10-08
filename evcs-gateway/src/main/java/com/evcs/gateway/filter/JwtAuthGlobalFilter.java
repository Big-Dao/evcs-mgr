package com.evcs.gateway.filter;

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
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> WHITELIST = Set.of(
            "/auth/login",
            "/auth/refresh",
            "/doc.html",
            "/webjars/",
            "/v3/api-docs",
            "/swagger-ui/",
            "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        var path = request.getURI().getPath();
        
        // Normalize path to prevent path traversal attacks
        String normalizedPath = normalizePath(path);

        // Inject requestId if missing
        final String requestId = request.getHeaders().getFirst("X-Request-Id") != null 
            ? request.getHeaders().getFirst("X-Request-Id")
            : UUID.randomUUID().toString();
        
        if (request.getHeaders().getFirst("X-Request-Id") == null) {
            exchange.getResponse().getHeaders().add("X-Request-Id", requestId);
        }

        // Whitelist pass-through with normalized path
        boolean whitelisted = WHITELIST.stream().anyMatch(pattern -> 
            pattern.endsWith("/") ? normalizedPath.startsWith(pattern) : normalizedPath.equals(pattern)
        );
        if (whitelisted) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.verifyToken(token) || jwtUtil.isTokenExpired(token)) {
            return unauthorized(exchange, "Invalid or expired token");
        }

        Long userId = jwtUtil.getUserId(token);
        Long tenantId = jwtUtil.getTenantId(token);
        // propagate downstream as headers
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(httpHeaders -> {
                    if (userId != null) httpHeaders.set("X-User-Id", String.valueOf(userId));
                    if (tenantId != null) httpHeaders.set("X-Tenant-Id", String.valueOf(tenantId));
                    httpHeaders.set("X-Request-Id", requestId);
                }))
                .build();

        return chain.filter(mutated);
    }

    /**
     * Normalize path to prevent path traversal attacks
     * Removes /./ and /../ sequences
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        // Remove leading/trailing whitespace
        path = path.trim();
        
        // Split by / and rebuild path, skipping . and handling ..
        String[] segments = path.split("/");
        StringBuilder normalized = new StringBuilder();
        
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                // Remove last segment if exists
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

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // Use ObjectMapper to properly escape JSON
        Map<String, Object> body = Map.of("code", 401, "message", message);
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
        return -200; // run early
    }
}
