package com.evcs.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 临时简化的JWT认证过滤器 - 用于排查问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveJwtAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 临时完全禁用JWT认证，只做基本日志记录
        log.info("Request received: {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getPath());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级
    }
}