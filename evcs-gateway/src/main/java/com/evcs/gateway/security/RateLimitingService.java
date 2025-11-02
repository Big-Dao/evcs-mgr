package com.evcs.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流服务
 * 提供基于Redis的分布式限流功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityAuditService auditService;

    // 限流键前缀
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String IP_RATE_LIMIT_PREFIX = RATE_LIMIT_PREFIX + "ip:";
    private static final String USER_RATE_LIMIT_PREFIX = RATE_LIMIT_PREFIX + "user:";
    private static final String API_RATE_LIMIT_PREFIX = RATE_LIMIT_PREFIX + "api:";

    // 默认限流配置
    private static final int DEFAULT_IP_LIMIT = 100; // 每分钟100次请求
    private static final int DEFAULT_USER_LIMIT = 200; // 每分钟200次请求
    private static final int DEFAULT_API_LIMIT = 50; // 每分钟50次请求
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);

    /**
     * 检查IP限流
     */
    public RateLimitResult checkIpRateLimit(String ipAddress, String apiPath) {
        return checkRateLimit(
            IP_RATE_LIMIT_PREFIX + ipAddress,
            DEFAULT_IP_LIMIT,
            DEFAULT_WINDOW,
            "IP",
            ipAddress,
            apiPath
        );
    }

    /**
     * 检查用户限流
     */
    public RateLimitResult checkUserRateLimit(Long userId, String apiPath) {
        return checkRateLimit(
            USER_RATE_LIMIT_PREFIX + userId,
            DEFAULT_USER_LIMIT,
            DEFAULT_WINDOW,
            "User",
            String.valueOf(userId),
            apiPath
        );
    }

    /**
     * 检查API限流
     */
    public RateLimitResult checkApiRateLimit(String apiPath) {
        return checkRateLimit(
            API_RATE_LIMIT_PREFIX + apiPath.replaceAll("/", "_"),
            DEFAULT_API_LIMIT,
            DEFAULT_WINDOW,
            "API",
            apiPath,
            apiPath
        );
    }

    /**
     * 检查自定义限流
     */
    public RateLimitResult checkCustomRateLimit(String key, int limit, Duration window,
                                               String limitType, String identifier, String apiPath) {
        return checkRateLimit(key, limit, window, limitType, identifier, apiPath);
    }

    /**
     * 核心限流检查逻辑 - 滑动窗口算法
     */
    private RateLimitResult checkRateLimit(String key, int limit, Duration window,
                                          String limitType, String identifier, String apiPath) {
        try {
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - window.toMillis();

            // 获取当前窗口内的请求计数
            String countKey = key + ":count";
            String windowKey = key + ":window";

            // 清理过期的请求记录
            redisTemplate.opsForZSet().removeRangeByScore(windowKey, 0, windowStart);

            // 获取当前计数
            Long currentCount = redisTemplate.opsForZSet().count(windowKey, windowStart, currentTime);
            int requestCount = currentCount != null ? currentCount.intValue() : 0;

            if (requestCount >= limit) {
                // 限流触发
                RateLimitResult result = new RateLimitResult(
                    false,
                    requestCount,
                    limit,
                    window,
                    System.currentTimeMillis() + window.toMillis()
                );

                log.warn("Rate limit exceeded for {}: {} on path {}", limitType, identifier, apiPath);

                // 记录限流事件到审计
                // 这里可以添加审计记录逻辑

                return result;
            }

            // 记录当前请求
            redisTemplate.opsForZSet().add(windowKey, String.valueOf(currentTime), currentTime);
            redisTemplate.expire(windowKey, window);

            // 更新计数器
            redisTemplate.opsForValue().set(countKey, String.valueOf(requestCount + 1), window);

            return new RateLimitResult(
                true,
                requestCount + 1,
                limit,
                window,
                -1
            );

        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // 发生异常时允许通过，避免影响正常业务
            return new RateLimitResult(true, 0, limit, window, -1);
        }
    }

    /**
     * 获取限流统计信息
     */
    public RateLimitStatistics getStatistics(String key, Duration window) {
        try {
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - window.toMillis();
            String windowKey = key + ":window";

            // 清理过期记录
            redisTemplate.opsForZSet().removeRangeByScore(windowKey, 0, windowStart);

            // 获取当前计数
            Long currentCount = redisTemplate.opsForZSet().count(windowKey, windowStart, currentTime);
            int requestCount = currentCount != null ? currentCount.intValue() : 0;

            // 获取时间分布
            var timeDistribution = redisTemplate.opsForZSet().rangeWithScores(windowKey, 0, -1);

            long currentTimestamp = System.currentTimeMillis();
            return new RateLimitStatistics(
                key,
                requestCount,
                window,
                timeDistribution != null ? timeDistribution.size() : 0,
                currentTimestamp
            );

        } catch (Exception e) {
            log.error("Error getting rate limit statistics for key: {}", key, e);
            return new RateLimitStatistics(key, 0, window, 0, System.currentTimeMillis());
        }
    }

    /**
     * 重置限流计数
     */
    public boolean resetRateLimit(String key) {
        try {
            redisTemplate.delete(key + ":count");
            redisTemplate.delete(key + ":window");
            log.info("Rate limit reset for key: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Error resetting rate limit for key: {}", key, e);
            return false;
        }
    }

    /**
     * 检查是否在黑名单中
     */
    public boolean isBlacklisted(String identifier) {
        try {
            String blacklistKey = RATE_LIMIT_PREFIX + "blacklist:" + identifier;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.error("Error checking blacklist for: {}", identifier, e);
            return false;
        }
    }

    /**
     * 添加到黑名单
     */
    public void addToBlacklist(String identifier, Duration duration) {
        try {
            String blacklistKey = RATE_LIMIT_PREFIX + "blacklist:" + identifier;
            redisTemplate.opsForValue().set(blacklistKey, "blacklisted", duration);
            log.warn("Added to blacklist: {} for duration: {}", identifier, duration);
        } catch (Exception e) {
            log.error("Error adding to blacklist: {}", identifier, e);
        }
    }

    /**
     * 从黑名单移除
     */
    public void removeFromBlacklist(String identifier) {
        try {
            String blacklistKey = RATE_LIMIT_PREFIX + "blacklist:" + identifier;
            redisTemplate.delete(blacklistKey);
            log.info("Removed from blacklist: {}", identifier);
        } catch (Exception e) {
            log.error("Error removing from blacklist: {}", identifier, e);
        }
    }

    /**
     * 限流结果类
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final int currentRequests;
        private final int limit;
        private final Duration window;
        private final long retryAfter;

        public RateLimitResult(boolean allowed, int currentRequests, int limit,
                             Duration window, long retryAfter) {
            this.allowed = allowed;
            this.currentRequests = currentRequests;
            this.limit = limit;
            this.window = window;
            this.retryAfter = retryAfter;
        }

        public boolean isAllowed() { return allowed; }
        public int getCurrentRequests() { return currentRequests; }
        public int getLimit() { return limit; }
        public Duration getWindow() { return window; }
        public long getRetryAfter() { return retryAfter; }
        public boolean shouldRetry() { return retryAfter > 0; }

        @Override
        public String toString() {
            return String.format("RateLimitResult{allowed=%s, current=%d/%d, window=%ds, retryAfter=%d}",
                    allowed, currentRequests, limit, window.getSeconds(), retryAfter);
        }
    }

    /**
     * 限流统计信息类
     */
    public static class RateLimitStatistics {
        private final String key;
        private final int requestCount;
        private final Duration window;
        private final int uniqueRequests;
        private final long timestamp;

        public RateLimitStatistics(String key, int requestCount, Duration window,
                                 int uniqueRequests, long timestamp) {
            this.key = key;
            this.requestCount = requestCount;
            this.window = window;
            this.uniqueRequests = uniqueRequests;
            this.timestamp = timestamp;
        }

        public String getKey() { return key; }
        public int getRequestCount() { return requestCount; }
        public Duration getWindow() { return window; }
        public int getUniqueRequests() { return uniqueRequests; }
        public long getTimestamp() { return timestamp; }
        public double getRequestsPerSecond() {
            return window.getSeconds() > 0 ? (double) requestCount / window.getSeconds() : 0;
        }

        @Override
        public String toString() {
            return String.format("RateLimitStatistics{key=%s, requests=%d, window=%ds, unique=%d, rps=%.2f}",
                    key, requestCount, window.getSeconds(), uniqueRequests, getRequestsPerSecond());
        }
    }
}