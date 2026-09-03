package com.evcs.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContextHeadersGlobalFilterTest {

    @Test
    @DisplayName("有效 Bearer token 时 - 网关应注入 X-Tenant-Id 与 X-User-Id")
    void shouldInjectContextHeadersWhenTokenValid() {
        // Arrange
        ContextHeadersProperties properties = new ContextHeadersProperties();
        properties.setEnabled(true);
        properties.setJwtSecret("test-secret");

        ContextHeadersGlobalFilter filter = new ContextHeadersGlobalFilter(properties);

        String token = JWT.create()
            .withClaim("tenantId", 100L)
            .withClaim("userId", 200L)
            .sign(Algorithm.HMAC256("test-secret"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders")
                .header("Authorization", "Bearer " + token)
                .build()
        );

        AtomicReference<String> tenantId = new AtomicReference<>();
        AtomicReference<String> userId = new AtomicReference<>();

        // Act
        filter.filter(exchange, ex -> {
            tenantId.set(ex.getRequest().getHeaders().getFirst(ContextHeadersGlobalFilter.HEADER_TENANT_ID));
            userId.set(ex.getRequest().getHeaders().getFirst(ContextHeadersGlobalFilter.HEADER_USER_ID));
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return ex.getResponse().setComplete();
        }).block(Duration.ofSeconds(5));

        // Assert
        assertEquals("100", tenantId.get(), "X-Tenant-Id 应来自 token 的 tenantId claim");
        assertEquals("200", userId.get(), "X-User-Id 应来自 token 的 userId/sub");
    }

    @Test
    @DisplayName("stripClientContextHeaders=true 时 - 网关应移除客户端伪造头并覆盖写入")
    void shouldStripClientHeadersAndOverride() {
        // Arrange
        ContextHeadersProperties properties = new ContextHeadersProperties();
        properties.setEnabled(true);
        properties.setStripClientContextHeaders(true);
        properties.setJwtSecret("test-secret");

        ContextHeadersGlobalFilter filter = new ContextHeadersGlobalFilter(properties);

        String token = JWT.create()
            .withClaim("tenantId", 101L)
            .withClaim("userId", 201L)
            .sign(Algorithm.HMAC256("test-secret"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders")
                .header("Authorization", "Bearer " + token)
                .header(ContextHeadersGlobalFilter.HEADER_TENANT_ID, "999")
                .header(ContextHeadersGlobalFilter.HEADER_USER_ID, "888")
                .build()
        );

        AtomicReference<String> tenantId = new AtomicReference<>();
        AtomicReference<String> userId = new AtomicReference<>();

        // Act
        filter.filter(exchange, ex -> {
            tenantId.set(ex.getRequest().getHeaders().getFirst(ContextHeadersGlobalFilter.HEADER_TENANT_ID));
            userId.set(ex.getRequest().getHeaders().getFirst(ContextHeadersGlobalFilter.HEADER_USER_ID));
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return ex.getResponse().setComplete();
        }).block(Duration.ofSeconds(5));

        // Assert
        assertEquals("101", tenantId.get(), "X-Tenant-Id 应由网关覆盖写入");
        assertEquals("201", userId.get(), "X-User-Id 应由网关覆盖写入");
    }

    @Test
    @DisplayName("stripClientContextHeaders=true 时 - 网关应剥离客户端伪造的 X-Tenant-Type/X-Tenant-Ancestors")
    void shouldStripClientTenantTypeAndAncestorsHeaders() {
        // Arrange
        ContextHeadersProperties properties = new ContextHeadersProperties();
        properties.setEnabled(true);
        properties.setStripClientContextHeaders(true);
        properties.setJwtSecret("test-secret");

        ContextHeadersGlobalFilter filter = new ContextHeadersGlobalFilter(properties);

        String token = JWT.create()
            .withClaim("tenantId", 101L)
            .withClaim("userId", 201L)
            .sign(Algorithm.HMAC256("test-secret"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders")
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-Type", "1")
                .header("X-Tenant-Ancestors", ",5,")
                .build()
        );

        AtomicReference<String> tenantType = new AtomicReference<>();
        AtomicReference<String> ancestors = new AtomicReference<>();

        // Act
        filter.filter(exchange, ex -> {
            tenantType.set(ex.getRequest().getHeaders().getFirst("X-Tenant-Type"));
            ancestors.set(ex.getRequest().getHeaders().getFirst("X-Tenant-Ancestors"));
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return ex.getResponse().setComplete();
        }).block(Duration.ofSeconds(5));

        // Assert
        assertNull(tenantType.get(), "X-Tenant-Type 不来自 JWT claim，客户端伪造头必须被剥离");
        assertNull(ancestors.get(), "X-Tenant-Ancestors 不来自 JWT claim，客户端伪造头必须被剥离");
    }

    @Test
    @DisplayName("Bearer token 无效时 - 网关不应注入上下文头")
    void shouldNotInjectWhenTokenInvalid() {
        // Arrange
        ContextHeadersProperties properties = new ContextHeadersProperties();
        properties.setEnabled(true);
        properties.setJwtSecret("test-secret");

        ContextHeadersGlobalFilter filter = new ContextHeadersGlobalFilter(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders")
                .header("Authorization", "Bearer invalid")
                .build()
        );

        AtomicReference<String> tenantId = new AtomicReference<>();
        AtomicReference<String> userId = new AtomicReference<>();

        // Act
        filter.filter(exchange, ex -> {
            tenantId.set(ex.getRequest().getHeaders().getFirst(ContextHeadersGlobalFilter.HEADER_TENANT_ID));
            userId.set(ex.getRequest().getHeaders().getFirst(ContextHeadersGlobalFilter.HEADER_USER_ID));
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return ex.getResponse().setComplete();
        }).block(Duration.ofSeconds(5));

        // Assert
        assertNull(tenantId.get(), "token 无效时不应写入 X-Tenant-Id");
        assertNull(userId.get(), "token 无效时不应写入 X-User-Id");
    }
}
