package com.evcs.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceIdGlobalFilterTest {

    @Test
    @DisplayName("缺失 trace/request id 时 - 网关应生成并写入请求与响应头")
    void shouldGenerateTraceIdWhenMissing() {
        // Arrange
        TraceIdFilterProperties properties = new TraceIdFilterProperties();
        properties.setEnabled(true);

        TraceIdGlobalFilter filter = new TraceIdGlobalFilter(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/ping").build()
        );

        AtomicReference<String> traceFromRequest = new AtomicReference<>();

        // Act
        filter.filter(exchange, ex -> {
            traceFromRequest.set(ex.getRequest().getHeaders().getFirst("X-Trace-Id"));
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return ex.getResponse().setComplete();
        }).block(Duration.ofSeconds(5));

        // Assert
        String requestTrace = traceFromRequest.get();
        assertNotNull(requestTrace, "请求头 X-Trace-Id 应被写入");
        assertTrue(!requestTrace.isBlank(), "请求头 X-Trace-Id 不应为空");

        String responseTrace = exchange.getResponse().getHeaders().getFirst("X-Trace-Id");
        String responseRequestId = exchange.getResponse().getHeaders().getFirst("X-Request-Id");
        assertEquals(requestTrace, responseTrace, "响应头 X-Trace-Id 应与请求一致");
        assertEquals(requestTrace, responseRequestId, "响应头 X-Request-Id 应与请求一致");
    }

    @Test
    @DisplayName("仅存在 X-Request-Id 时 - 网关应复用并同步到 X-Trace-Id")
    void shouldUseRequestIdWhenTraceIdMissing() {
        // Arrange
        TraceIdFilterProperties properties = new TraceIdFilterProperties();
        properties.setEnabled(true);

        TraceIdGlobalFilter filter = new TraceIdGlobalFilter(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/ping")
                .header("X-Request-Id", "req-123")
                .build()
        );

        AtomicReference<String> traceFromRequest = new AtomicReference<>();

        // Act
        filter.filter(exchange, ex -> {
            traceFromRequest.set(ex.getRequest().getHeaders().getFirst("X-Trace-Id"));
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return ex.getResponse().setComplete();
        }).block(Duration.ofSeconds(5));

        // Assert
        assertEquals("req-123", traceFromRequest.get(), "X-Trace-Id 应复用 X-Request-Id");
        assertEquals("req-123", exchange.getResponse().getHeaders().getFirst("X-Trace-Id"), "响应头 X-Trace-Id 应一致");
        assertEquals("req-123", exchange.getResponse().getHeaders().getFirst("X-Request-Id"), "响应头 X-Request-Id 应一致");
    }
}
