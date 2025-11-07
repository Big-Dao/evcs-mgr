package com.evcs.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 响应式速率限制过滤器
 * 使用令牌桶算法，完全避免阻塞操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveRateLimitFilter implements GlobalFilter, Ordered {

    // 暂时禁用速率限制功能以排查问题
    private boolean rateLimitEnabled = false;

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${rate.limit.requests:100}")
    private int defaultRequestsPerMinute;

    @Value("${rate.limit.window:60}")
    private int defaultWindowSeconds;

    // rateLimitEnabled已在上面定义

    // 内存中的请求计数器（作为Redis的备用）
    private final Map<String, AtomicLong> memoryCounters = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!rateLimitEnabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String clientKey = getClientKey(request);
        String path = request.getURI().getPath();

        // 对不同的API路径设置不同的限制
        int requestsPerMinute = getRequestsPerMinute(path);
        int windowSeconds = getWindowSeconds(path);

        return checkRateLimitAsync(clientKey, requestsPerMinute, windowSeconds)
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    } else {
                        return rateLimitExceeded(exchange);
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("速率限制检查异常", throwable);
                    // 出错时允许通过，避免影响可用性
                    return chain.filter(exchange);
                });
    }

    /**
     * 获取客户端标识符
     */
    private String getClientKey(ServerHttpRequest request) {
        // 优先使用用户ID
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // 使用租户ID
        String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
        if (tenantId != null && !tenantId.isEmpty()) {
            return "tenant:" + tenantId;
        }

        // 使用IP地址
        String remoteAddr = request.getRemoteAddress() != null ?
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        return "ip:" + remoteAddr;
    }

    /**
     * 根据路径获取速率限制配置
     */
    private int getRequestsPerMinute(String path) {
        // 登录API有更严格的限制
        if (path.contains("/auth/login")) {
            return Math.min(defaultRequestsPerMinute / 10, 5); // 最多5次/分钟
        }

        // 文件上传API限制
        if (path.contains("/upload") || path.contains("/file")) {
            return Math.min(defaultRequestsPerMinute / 5, 20); // 最多20次/分钟
        }

        // 管理API限制
        if (path.contains("/admin") || path.contains("/manage")) {
            return Math.min(defaultRequestsPerMinute / 2, 50); // 最多50次/分钟
        }

        return defaultRequestsPerMinute;
    }

    /**
     * 根据路径获取时间窗口
     */
    private int getWindowSeconds(String path) {
        // 登录API使用更短的时间窗口
        if (path.contains("/auth/login")) {
            return 60; // 1分钟
        }

        return defaultWindowSeconds;
    }

    /**
     * 异步检查速率限制
     */
    private Mono<Boolean> checkRateLimitAsync(String clientKey, int maxRequests, int windowSeconds) {
        String redisKey = "rate_limit:" + clientKey;
        double currentTime = (double) System.currentTimeMillis() / 1000;
        double windowStart = currentTime - windowSeconds;

        // 首先尝试使用Redis
        return redisTemplate.opsForZSet()
                .count(redisKey, org.springframework.data.domain.Range.closed(windowStart, currentTime))
                .flatMap(currentCount -> {
                    if (currentCount < maxRequests) {
                        // 添加当前请求到时间窗口
                        return redisTemplate.opsForZSet()
                                .add(redisKey, String.valueOf(currentTime), currentTime)
                                .then(redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds + 10)))
                                .map(success -> true)
                                .defaultIfEmpty(true);
                    } else {
                        return Mono.just(false);
                    }
                })
                .timeout(Duration.ofMillis(100)) // Redis操作超时保护
                .onErrorResume(throwable -> {
                    log.debug("Redis速率限制检查失败，使用内存计数器: {}", throwable.getMessage());
                    return checkMemoryRateLimit(clientKey, maxRequests, windowSeconds);
                });
    }

    /**
     * 内存备用速率限制检查
     */
    private Mono<Boolean> checkMemoryRateLimit(String clientKey, int maxRequests, int windowSeconds) {
        return Mono.fromCallable(() -> {
            AtomicLong counter = memoryCounters.computeIfAbsent(clientKey, k -> new AtomicLong(0));
            long currentCount = counter.incrementAndGet();

            // 简单的计数器重置（每分钟重置一次）
            long currentMinute = System.currentTimeMillis() / 60000;
            String keyWithMinute = clientKey + ":" + currentMinute;

            if (!memoryCounters.containsKey(keyWithMinute)) {
                counter.set(0);
                memoryCounters.put(keyWithMinute, counter);
            }

            return currentCount <= maxRequests;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * 速率限制超出响应
     */
    private Mono<Void> rateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", "60");
        response.getHeaders().set("Cache-Control", "no-store");

        Map<String, Object> errorBody = Map.of(
            "code", 429,
            "message", "请求过于频繁，请稍后再试",
            "timestamp", Instant.now().toString(),
            "path", exchange.getRequest().getPath().value(),
            "retryAfter", 60
        );

        try {
            String jsonBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(errorBody);
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            var buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("序列化速率限制响应失败", e);
            String simpleBody = "{\"code\":429,\"message\":\"Too Many Requests\"}";
            byte[] bytes = simpleBody.getBytes(StandardCharsets.UTF_8);
            var buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }

    @Override
    public int getOrder() {
        return -90; // 在JWT过滤器之后执行
    }

    /**
     * 定期清理内存计数器
     */
    public void cleanupMemoryCounters() {
        Mono.fromRunnable(() -> {
            try {
                long currentMinute = System.currentTimeMillis() / 60000;

                // 移除过期的计数器（保留最近2分钟）
                memoryCounters.entrySet().removeIf(entry -> {
                    String key = entry.getKey();
                    if (key.contains(":")) {
                        try {
                            long minute = Long.parseLong(key.substring(key.lastIndexOf(":") + 1));
                            return currentMinute - minute > 2;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                    return false;
                });

                if (log.isDebugEnabled()) {
                    log.debug("内存速率限制计数器清理完成，当前计数器数量: {}", memoryCounters.size());
                }
            } catch (Exception e) {
                log.error("清理内存速率限制计数器失败", e);
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).subscribe();
    }
}