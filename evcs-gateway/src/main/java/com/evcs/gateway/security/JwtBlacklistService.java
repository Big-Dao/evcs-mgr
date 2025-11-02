package com.evcs.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * JWT令牌黑名单服务
 * 负责管理被注销的JWT令牌，防止令牌重用攻击
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKEN_PREFIX = "jwt:user:";
    private static final long DEFAULT_BLACKLIST_TTL = 24 * 60 * 60; // 24小时

    /**
     * 检查令牌是否在黑名单中
     *
     * @param token JWT令牌
     * @return true表示在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String key = BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking blacklist for token", e);
            return false;
        }
    }

    /**
     * 将令牌添加到黑名单
     *
     * @param token JWT令牌
     * @param ttl 生存时间（秒）
     */
    public void addToBlacklist(String token, long ttl) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Attempted to add null or empty token to blacklist");
            return;
        }

        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttl));
            log.info("Token added to blacklist: {} (TTL: {}s)", token.substring(0, Math.min(20, token.length())), ttl);
        } catch (Exception e) {
            log.error("Error adding token to blacklist", e);
        }
    }

    /**
     * 将令牌添加到黑名单（使用默认TTL）
     *
     * @param token JWT令牌
     */
    public void addToBlacklist(String token) {
        addToBlacklist(token, DEFAULT_BLACKLIST_TTL);
    }

    /**
     * 将用户的所有令牌添加到黑名单
     * 用于用户注销时，使该用户的所有JWT令牌失效
     *
     * @param userId 用户ID
     */
    public void blacklistUserTokens(Long userId) {
        if (userId == null) {
            log.warn("Attempted to blacklist tokens for null user");
            return;
        }

        try {
            String userKey = USER_TOKEN_PREFIX + userId;

            // 设置用户黑名单标记，有效期与JWT最长有效期相同
            redisTemplate.opsForValue().set(userKey, "blacklisted", Duration.ofDays(30));

            log.info("All tokens for user {} have been blacklisted", userId);
        } catch (Exception e) {
            log.error("Error blacklisting user tokens", e);
        }
    }

    /**
     * 检查用户是否被列入黑名单
     *
     * @param userId 用户ID
     * @return true表示用户被列入黑名单
     */
    public boolean isUserBlacklisted(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            String userKey = USER_TOKEN_PREFIX + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
        } catch (Exception e) {
            log.error("Error checking user blacklist status", e);
            return false;
        }
    }

    /**
     * 从黑名单中移除令牌
     *
     * @param token JWT令牌
     */
    public void removeFromBlacklist(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
            log.info("Token removed from blacklist: {}", token.substring(0, Math.min(20, token.length())));
        } catch (Exception e) {
            log.error("Error removing token from blacklist", e);
        }
    }

    /**
     * 从用户黑名单中移除用户
     *
     * @param userId 用户ID
     */
    public void removeUserFromBlacklist(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            String userKey = USER_TOKEN_PREFIX + userId;
            redisTemplate.delete(userKey);
            log.info("User {} removed from blacklist", userId);
        } catch (Exception e) {
            log.error("Error removing user from blacklist", e);
        }
    }

    /**
     * 清理过期的黑名单条目
     * 定期清理过期的令牌和用户黑名单记录
     */
    public void cleanupExpiredBlacklist() {
        try {
            // 清理过期的令牌黑名单
            // Redis会自动清理过期的键值，这里主要是日志记录
            log.debug("Starting expired blacklist cleanup");

            // 可以添加额外的清理逻辑，如统计清理数量等
        } catch (Exception e) {
            log.error("Error during blacklist cleanup", e);
        }
    }

    /**
     * 获取黑名单统计信息
     *
     * @return 黑名单统计
     */
    public BlacklistStatistics getStatistics() {
        try {
            // 统计令牌黑名单数量
            long tokenCount = redisTemplate.keys(BLACKLIST_PREFIX + "*").size();

            // 统计用户黑名单数量
            long userCount = redisTemplate.keys(USER_TOKEN_PREFIX + "*").size();

            return new BlacklistStatistics(tokenCount, userCount);
        } catch (Exception e) {
            log.error("Error getting blacklist statistics", e);
            return new BlacklistStatistics(0, 0);
        }
    }

    /**
     * 黑名单统计信息
     */
    public static class BlacklistStatistics {
        private final long blacklistedTokenCount;
        private final long blacklistedUserCount;
        private final long timestamp;

        public BlacklistStatistics(long blacklistedTokenCount, long blacklistedUserCount) {
            this.blacklistedTokenCount = blacklistedTokenCount;
            this.blacklistedUserCount = blacklistedUserCount;
            this.timestamp = System.currentTimeMillis();
        }

        public long getBlacklistedTokenCount() {
            return blacklistedTokenCount;
        }

        public long getBlacklistedUserCount() {
            return blacklistedUserCount;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("BlacklistStatistics{tokens=%d, users=%d, timestamp=%d}",
                    blacklistedTokenCount, blacklistedUserCount, timestamp);
        }
    }
}