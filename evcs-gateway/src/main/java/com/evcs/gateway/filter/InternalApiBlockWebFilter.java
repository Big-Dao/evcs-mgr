package com.evcs.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.evcs.common.result.Result;
import com.evcs.common.result.ResultCode;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Defense-in-depth: block access to internal endpoints at the gateway edge.
 * <p>
 * Internal endpoints are intended for service-to-service calls only, and should not
 * be exposed via gateway routes.
 */
public class InternalApiBlockWebFilter implements WebFilter, Ordered {

    private final InternalApiBlockProperties properties;

    public InternalApiBlockWebFilter(InternalApiBlockProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String pathPrefix = properties.getPathPrefix();
        if (pathPrefix == null || pathPrefix.isBlank()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (path == null || !path.startsWith(pathPrefix)) {
            return chain.filter(exchange);
        }

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        Result<Void> body = Result.failure(ResultCode.NOT_FOUND);
        byte[] bytes = JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
