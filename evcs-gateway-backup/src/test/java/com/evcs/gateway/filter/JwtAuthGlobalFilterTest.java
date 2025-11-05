package com.evcs.gateway.filter;

import com.evcs.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * JWT Authentication Filter Security Tests
 * Tests for Week 1 security hardening requirements
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthGlobalFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    private JwtAuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthGlobalFilter(jwtUtil);
        lenient().when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void testWhitelistedPathsDoNotRequireAuth() {
        // Test exact match whitelist
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(any(ServerWebExchange.class));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testWhitelistedPrefixPaths() {
        // Test prefix match whitelist (paths ending with /)
        MockServerHttpRequest request = MockServerHttpRequest.get("/webjars/swagger-ui/index.html").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(any(ServerWebExchange.class));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testPathTraversalAttackPrevention() {
        // Attempt path traversal to bypass whitelist
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/../api/stations")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Should normalize to /api/stations and require auth
        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectNextCount(0)
                .verifyComplete();

        // Should require authentication after normalization
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testPathTraversalWithDotSlash() {
        // Test /./ sequences are removed
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/./login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        // Should normalize to /auth/login and allow through
        verify(filterChain).filter(any(ServerWebExchange.class));
    }

    @Test
    void testMissingAuthorizationHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/stations").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectNextCount(0)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testInvalidAuthorizationHeaderFormat() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/stations")
                .header(HttpHeaders.AUTHORIZATION, "InvalidFormat token123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectNextCount(0)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testExpiredToken() {
        String token = "expired.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/stations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.isTokenExpired(token)).thenReturn(true);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectNextCount(0)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testInvalidToken() {
        String token = "invalid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/stations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.verifyToken(token)).thenReturn(false);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectNextCount(0)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testValidTokenWithUserAndTenantHeaders() {
        String token = "valid.jwt.token";
        Long userId = 123L;
        Long tenantId = 456L;

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/stations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtUtil.getUserId(token)).thenReturn(userId);
        when(jwtUtil.getTenantId(token)).thenReturn(tenantId);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(argThat(mutatedExchange -> {
            String userIdHeader = mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id");
            String tenantIdHeader = mutatedExchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            String requestIdHeader = mutatedExchange.getRequest().getHeaders().getFirst("X-Request-Id");

            return userId.toString().equals(userIdHeader) &&
                   tenantId.toString().equals(tenantIdHeader) &&
                   requestIdHeader != null;
        }));
    }

    @Test
    void testRequestIdInjection() {
        // Test that request ID is added when missing
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        String requestId = exchange.getResponse().getHeaders().getFirst("X-Request-Id");
        assertNotNull(requestId, "Request ID should be injected");
        assertTrue(requestId.length() > 0, "Request ID should not be empty");
    }

    @Test
    void testRequestIdPreserved() {
        // Test that existing request ID is preserved
        String existingRequestId = "existing-request-id-123";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/login")
                .header("X-Request-Id", existingRequestId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(argThat(mutatedExchange -> {
            String requestId = mutatedExchange.getRequest().getHeaders().getFirst("X-Request-Id");
            return existingRequestId.equals(requestId);
        }));
    }

    @Test
    void testFilterOrder() {
        assertEquals(-200, filter.getOrder(), "Filter should run early in the chain");
    }
}
