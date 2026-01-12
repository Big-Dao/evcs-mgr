package com.evcs.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalApiBlockWebFilterTest {

    @Test
    @DisplayName("enabled=true: /internal/api/** should be blocked with 404")
    void shouldBlockInternalApiWhenEnabled() {
        InternalApiBlockProperties properties = new InternalApiBlockProperties();
        properties.setEnabled(true);
        properties.setPathPrefix("/internal/api/");

        InternalApiBlockWebFilter filter = new InternalApiBlockWebFilter(properties);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = exchange -> {
            chainCalled.set(true);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        };

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/internal/api/v1/tenant-hierarchy/descendant").build()
        );

        filter.filter(exchange, chain).block(Duration.ofSeconds(5));

        assertFalse(chainCalled.get(), "Chain should not be invoked for internal paths");
        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("enabled=true: non-internal path should pass through")
    void shouldPassNonInternalPath() {
        InternalApiBlockProperties properties = new InternalApiBlockProperties();
        properties.setEnabled(true);
        properties.setPathPrefix("/internal/api/");

        InternalApiBlockWebFilter filter = new InternalApiBlockWebFilter(properties);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = exchange -> {
            chainCalled.set(true);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        };

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/auth/login").build()
        );

        filter.filter(exchange, chain).block(Duration.ofSeconds(5));

        assertTrue(chainCalled.get(), "Chain should be invoked for non-internal paths");
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("enabled=false: /internal/api/** should pass through")
    void shouldPassInternalApiWhenDisabled() {
        InternalApiBlockProperties properties = new InternalApiBlockProperties();
        properties.setEnabled(false);
        properties.setPathPrefix("/internal/api/");

        InternalApiBlockWebFilter filter = new InternalApiBlockWebFilter(properties);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = exchange -> {
            chainCalled.set(true);
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        };

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/internal/api/v1/tenant-hierarchy/descendant").build()
        );

        filter.filter(exchange, chain).block(Duration.ofSeconds(5));

        assertTrue(chainCalled.get(), "Chain should be invoked when blocking is disabled");
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }
}
