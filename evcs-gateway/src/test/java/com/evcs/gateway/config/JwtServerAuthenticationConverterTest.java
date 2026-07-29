package com.evcs.gateway.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.evcs.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JwtServerAuthenticationConverter 测试。
 * 验证 Bearer JWT 的提取与校验逻辑。
 */
class JwtServerAuthenticationConverterTest {

    private JwtUtil jwtUtil;
    private JwtServerAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        converter = new JwtServerAuthenticationConverter(jwtUtil);
    }

    @Test
    @DisplayName("有效 JWT 时应返回认证令牌（含 userId/tenantId 主体）")
    void shouldReturnAuthenticationWhenJwtValid() {
        String token = JWT.create()
                .withClaim("tenantId", 100L)
                .withClaim("userId", 200L)
                .sign(Algorithm.HMAC256("test-secret"));

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(200L);
        when(jwtUtil.getTenantId(token)).thenReturn(100L);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .build());

        AtomicReference<org.springframework.security.core.Authentication> captured = new AtomicReference<>();

        converter.convert(exchange).doOnNext(captured::set).block(Duration.ofSeconds(5));

        assertNotNull(captured.get());
        GatewaySecurityConfig.JwtPrincipal principal =
                (GatewaySecurityConfig.JwtPrincipal) captured.get().getPrincipal();
        assertNotNull(principal);
    }

    @Test
    @DisplayName("无 Authorization 头时应返回 empty（触发 401）")
    void shouldReturnEmptyWhenNoAuthHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders").build());

        AtomicReference<Boolean> emitted = new AtomicReference<>(false);

        converter.convert(exchange).doOnNext(a -> emitted.set(true)).block(Duration.ofSeconds(5));

        assertFalse(emitted.get(), "无 token 时应不发射任何认证令牌");
    }

    @Test
    @DisplayName("非 Bearer 格式的 Authorization 头应返回 empty")
    void shouldReturnEmptyWhenNotBearer() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Basic dXNlcjpwYXNz")
                        .build());

        AtomicReference<Boolean> emitted = new AtomicReference<>(false);

        converter.convert(exchange).doOnNext(a -> emitted.set(true)).block(Duration.ofSeconds(5));

        assertFalse(emitted.get());
    }

    @Test
    @DisplayName("JWT 校验失败时应返回 empty（触发 401）")
    void shouldReturnEmptyWhenJwtInvalid() {
        String token = "invalid.jwt.token";

        when(jwtUtil.verifyToken(token)).thenReturn(false);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .build());

        AtomicReference<Boolean> emitted = new AtomicReference<>(false);

        converter.convert(exchange).doOnNext(a -> emitted.set(true)).block(Duration.ofSeconds(5));

        assertFalse(emitted.get());
    }

    @Test
    @DisplayName("JWT 缺少 userId/tenantId 时应返回 empty")
    void shouldReturnEmptyWhenJwtMissingClaims() {
        String token = JWT.create()
                .withClaim("username", "user")
                .sign(Algorithm.HMAC256("test-secret"));

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(null);
        when(jwtUtil.getTenantId(token)).thenReturn(null);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .build());

        AtomicReference<Boolean> emitted = new AtomicReference<>(false);

        converter.convert(exchange).doOnNext(a -> emitted.set(true)).block(Duration.ofSeconds(5));

        assertFalse(emitted.get());
    }

    @Test
    @DisplayName("空/空白 Bearer token 应返回 empty")
    void shouldReturnEmptyWhenBearerTokenBlank() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Bearer    ")
                        .build());

        AtomicReference<Boolean> emitted = new AtomicReference<>(false);

        converter.convert(exchange).doOnNext(a -> emitted.set(true)).block(Duration.ofSeconds(5));

        assertFalse(emitted.get());
        verify(jwtUtil, never()).verifyToken(anyString());
    }
}
