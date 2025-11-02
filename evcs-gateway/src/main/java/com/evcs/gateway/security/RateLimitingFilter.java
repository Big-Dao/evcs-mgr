package com.evcs.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 限流过滤器
 * 基于Redis实现分布式限流功能
 */
@Slf4j
@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private SecurityAuditService auditService;

    public RateLimitingFilter() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("limit", "windowSeconds", "type");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // 获取客户端IP
            String clientIp = getClientIpAddress(request);

            // 检查黑名单
            if (rateLimitingService.isBlacklisted(clientIp)) {
                log.warn("Blacklisted IP access attempt: {}", clientIp);
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                auditService.logSuspiciousActivity(request, "Blacklisted IP access attempt: " + clientIp);
                return response.setComplete();
            }

            RateLimitingService.RateLimitResult result = null;

            // 根据限流类型执行不同的限流策略
            switch (config.getType().toLowerCase()) {
                case "ip":
                    result = rateLimitingService.checkIpRateLimit(clientIp, request.getPath().value());
                    break;
                case "api":
                    result = rateLimitingService.checkApiRateLimit(request.getPath().value());
                    break;
                case "custom":
                    String key = "custom:" + request.getPath().value();
                    Duration window = Duration.ofSeconds(config.getWindowSeconds());
                    result = rateLimitingService.checkCustomRateLimit(
                        key, config.getLimit(), window, "Custom", clientIp, request.getPath().value()
                    );
                    break;
                default:
                    // 默认使用IP限流
                    result = rateLimitingService.checkIpRateLimit(clientIp, request.getPath().value());
            }

            if (result != null && !result.isAllowed()) {
                // 限流触发
                log.warn("Rate limit exceeded for IP: {} on path: {}. Current: {}/{}",
                        clientIp, request.getPath().value(), result.getCurrentRequests(), result.getLimit());

                // 设置响应头
                response.getHeaders().add("X-RateLimit-Limit", String.valueOf(result.getLimit()));
                response.getHeaders().add("X-RateLimit-Remaining", "0");
                response.getHeaders().add("X-RateLimit-Reset", String.valueOf(result.getRetryAfter()));

                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

                // 记录审计事件
                auditService.logRateLimitExceeded(
                    request,
                    config.getType().toUpperCase() + "_LIMIT",
                    String.format("Limit: %d per %ds", result.getLimit(), result.getWindow().getSeconds())
                );

                // 如果频繁触发限流，加入临时黑名单
                if (result.getCurrentRequests() > result.getLimit() * 2) {
                    rateLimitingService.addToBlacklist(clientIp, Duration.ofMinutes(5));
                    log.warn("IP {} added to temporary blacklist due to excessive rate limit violations", clientIp);
                }

                return response.setComplete();
            }

            // 允许通过，添加响应头
            if (result != null) {
                response.getHeaders().add("X-RateLimit-Limit", String.valueOf(result.getLimit()));
                response.getHeaders().add("X-RateLimit-Remaining",
                    String.valueOf(Math.max(0, result.getLimit() - result.getCurrentRequests())));
            }

            return chain.filter(exchange);

        }, Ordered.HIGHEST_PRECEDENCE);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        try {
            var remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return remoteAddress.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            log.debug("Error getting remote address", e);
        }
        return "unknown";
    }

    /**
     * 配置类
     */
    public static class Config {
        private int limit = 100; // 默认限制
        private int windowSeconds = 60; // 默认窗口时间（秒）
        private String type = "ip"; // 限流类型：ip, api, custom

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        @Override
        public String toString() {
            return String.format("RateLimitConfig{limit=%d, window=%ds, type='%s'}", limit, windowSeconds, type);
        }
    }
}