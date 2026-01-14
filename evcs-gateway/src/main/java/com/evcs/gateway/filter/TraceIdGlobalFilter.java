package com.evcs.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Ensures {@code X-Trace-Id}/{@code X-Request-Id} are present and consistent at the gateway edge.
 *
 * <p>Rules:
 * <ul>
 *   <li>Prefer {@code X-Trace-Id}; fallback to {@code X-Request-Id}.</li>
 *   <li>If neither exists, generate a new UUID.</li>
 *   <li>Always set both headers to the chosen trace id for downstream consistency.</li>
 * </ul>
 */
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private final TraceIdFilterProperties properties;

    public TraceIdGlobalFilter(TraceIdFilterProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        final String traceHeader = normalizeHeaderName(properties.getTraceIdHeader(), "X-Trace-Id");
        final String requestHeader = normalizeHeaderName(properties.getRequestIdHeader(), "X-Request-Id");

        String resolvedTraceId = exchange.getRequest().getHeaders().getFirst(traceHeader);
        if (resolvedTraceId == null || resolvedTraceId.isBlank()) {
            resolvedTraceId = exchange.getRequest().getHeaders().getFirst(requestHeader);
        }
        if (resolvedTraceId == null || resolvedTraceId.isBlank()) {
            resolvedTraceId = UUID.randomUUID().toString();
        }

        final String traceId = resolvedTraceId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .headers(headers -> {
                headers.set(traceHeader, traceId);
                headers.set(requestHeader, traceId);
            })
            .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        mutatedExchange.getResponse().beforeCommit(() -> {
            mutatedExchange.getResponse().getHeaders().set(traceHeader, traceId);
            mutatedExchange.getResponse().getHeaders().set(requestHeader, traceId);
            return Mono.empty();
        });

        return chain.filter(mutatedExchange);
    }

    private static String normalizeHeaderName(String headerName, String fallback) {
        if (headerName == null || headerName.isBlank()) {
            return fallback;
        }
        return headerName.trim();
    }
}
