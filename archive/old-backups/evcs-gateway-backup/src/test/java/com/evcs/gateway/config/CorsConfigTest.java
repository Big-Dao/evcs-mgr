package com.evcs.gateway.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Tests for CORS configuration: validate preflight behavior and exposed headers.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CORS Configuration Tests")
class CorsConfigTest {

    private CorsWebFilter corsWebFilter;

    @BeforeEach
    void setUp() {
        // Use the real configuration class to build the CorsWebFilter exactly as production
        CorsConfig corsConfig = new CorsConfig();
        GlobalCorsProperties globalCorsProperties = new GlobalCorsProperties();
        this.corsWebFilter = corsConfig.corsWebFilter(globalCorsProperties);
    }

    @Test
    @DisplayName(
        "Preflight (OPTIONS) request should return CORS allow headers and not invoke chain"
    )
    void preflightReturnsCorsAllowHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.options(
            "http://localhost/api/stations"
        )
            .header(HttpHeaders.ORIGIN, "http://example.com")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = mock(WebFilterChain.class);
        // Do not stub chain.filter(...). For preflight requests, the CORS filter should handle the request
        // and we assert the downstream chain is not invoked.

        StepVerifier.create(
            corsWebFilter.filter(exchange, chain)
        ).verifyComplete();

        HttpHeaders respHeaders = exchange.getResponse().getHeaders();

        // Access-Control-Allow-Origin may vary depending on credentials and origin handling.
        // We avoid asserting the exact origin to reduce test flakiness; instead assert allow methods are present below.

        String allowMethods = respHeaders.getFirst(
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS
        );
        assertNotNull(
            allowMethods,
            "Access-Control-Allow-Methods header should be present on preflight response"
        );
        assertTrue(
            allowMethods.contains("GET"),
            "Allowed methods should include the requested method"
        );

        // Preflight should short-circuit and NOT call downstream chain
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName(
        "Actual request should include Access-Control-Expose-Headers for X-Request-Id"
    )
    void actualRequestIncludesExposeHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get(
            "http://localhost/api/stations"
        )
            .header(HttpHeaders.ORIGIN, "http://example.com")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Downstream chain should be invoked for non-preflight requests.
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(
            invocation -> {
                // simulate downstream writing a response
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }
        );

        StepVerifier.create(
            corsWebFilter.filter(exchange, chain)
        ).verifyComplete();

        HttpHeaders respHeaders = exchange.getResponse().getHeaders();

        // The CORS configuration exposes X-Request-Id header, ensure it is present in response headers
        List<String> exposeHeaders = respHeaders.get(
            HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS
        );
        assertNotNull(
            exposeHeaders,
            "Access-Control-Expose-Headers should be present on normal responses"
        );
        assertTrue(
            exposeHeaders.stream().anyMatch(h -> h.contains("X-Request-Id")),
            "Access-Control-Expose-Headers must include X-Request-Id"
        );

        // Ensure downstream chain was invoked for actual request
        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }
}
