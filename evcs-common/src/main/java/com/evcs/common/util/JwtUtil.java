package com.evcs.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT工具类
 *
 * <p>安全说明：
 * <ul>
 *   <li>{@code jwt.secret} 为必填配置，无默认值。缺失或过短（&lt; 32 字符）将在启动时失败，
 *       避免以弱密钥运行。</li>
 *   <li>生产环境应通过环境变量/密钥管理服务注入，禁止提交到版本库。</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire:7200}")
    private Long expire;

    @Value("${jwt.issuer:}")
    private String issuer;

    @Value("${jwt.audience:}")
    private String audience;

    @Value("${jwt.user-type:}")
    private String userType;

    /**
     * 启动校验：确保 JWT 密钥已配置且达到最小长度。
     * 缺失或过短都会让应用启动失败，避免以弱密钥运行。
     */
    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret 未配置。请通过环境变量或配置中心注入一个至少 32 字符的随机密钥。"
            );
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret 过短（当前 " + secret.length() + " 字符）。请使用至少 32 字符的随机密钥。"
            );
        }
        log.info("JwtUtil 初始化完成（issuer={}, expire={}s）", issuer, expire);
    }

    /**
     * 生成JWT Token
     */
    public String generateToken(Long userId, String username, Long tenantId) {
        Date expireDate = new Date(System.currentTimeMillis() + expire * 1000);

        var creator = JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(expireDate)
                .withIssuedAt(new Date())
                // Legacy claims (migration compatibility)
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withClaim("tenantId", tenantId);

        if (userId != null) {
            // Standard claim: subject
            creator = creator.withSubject(String.valueOf(userId));
        }
        if (issuer != null && !issuer.trim().isEmpty()) {
            creator = creator.withIssuer(issuer.trim());
        }

        List<String> audiences = parseAudienceList(audience);
        if (!audiences.isEmpty()) {
            creator = creator.withAudience(audiences.toArray(new String[0]));
        }

        if (userType != null && !userType.trim().isEmpty()) {
            creator = creator.withClaim("userType", userType.trim());
        }

        return creator.sign(Algorithm.HMAC256(secret));
    }

    /**
     * 生成JWT Token（含角色列表）。
     *
     * <p>角色写入 {@code roles} claim（字符串数组），便于下游服务（如 payment）本地鉴权，
     * 无需跨服务调用即可构造 Spring Security 认证对象。
     */
    public String generateToken(Long userId, String username, Long tenantId, List<String> roles) {
        return generateTokenWithRoles(userId, username, tenantId, roles);
    }

    private String generateTokenWithRoles(Long userId, String username, Long tenantId, List<String> roles) {
        Date expireDate = new Date(System.currentTimeMillis() + expire * 1000);

        var creator = JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(expireDate)
                .withIssuedAt(new Date())
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withClaim("tenantId", tenantId);

        if (userId != null) {
            creator = creator.withSubject(String.valueOf(userId));
        }
        if (issuer != null && !issuer.trim().isEmpty()) {
            creator = creator.withIssuer(issuer.trim());
        }
        List<String> audiences = parseAudienceList(audience);
        if (!audiences.isEmpty()) {
            creator = creator.withAudience(audiences.toArray(new String[0]));
        }
        if (userType != null && !userType.trim().isEmpty()) {
            creator = creator.withClaim("userType", userType.trim());
        }
        if (roles != null && !roles.isEmpty()) {
            creator = creator.withClaim("roles", roles);
        }

        return creator.sign(Algorithm.HMAC256(secret));
    }

    /**
     * 验证Token（签名 + 过期 + issuer + audience）。
     *
     * <p>issuer/audience 仅在配置时校验，未配置则跳过对应项。
     */
    public boolean verifyToken(String token) {
        try {
            var builder = JWT.require(Algorithm.HMAC256(secret));
            if (issuer != null && !issuer.trim().isEmpty()) {
                builder.withIssuer(issuer.trim());
            }
            List<String> audiences = parseAudienceList(audience);
            if (!audiences.isEmpty()) {
                builder.withAudience(audiences.toArray(new String[0]));
            }
            builder.build().verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取用户ID
     */
    public Long getUserId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            Long claimUserId = jwt.getClaim("userId").asLong();
            if (claimUserId != null) {
                return claimUserId;
            }
            String subject = jwt.getSubject();
            if (subject == null || subject.trim().isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException ex) {
                return null;
            }
        } catch (JWTDecodeException e) {
            log.error("获取用户ID失败", e);
            return null;
        }
    }

    /**
     * 获取用户名
     */
    public String getUsername(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("username").asString();
        } catch (JWTDecodeException e) {
            log.error("获取用户名失败", e);
            return null;
        }
    }

    /**
     * 获取租户ID
     */
    public Long getTenantId(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("tenantId").asLong();
        } catch (JWTDecodeException e) {
            log.error("获取租户ID失败", e);
            return null;
        }
    }

    /**
     * 获取用户角色列表（来自 token 的 roles claim）。
     *
     * <p>若 token 未携带 roles 或解析失败，返回空列表。
     */
    public List<String> getRoles(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            List<String> roles = jwt.getClaim("roles").asList(String.class);
            return roles != null ? roles : List.of();
        } catch (JWTDecodeException e) {
            log.error("获取用户角色失败", e);
            return List.of();
        }
    }

    /**
     * 检查Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt().before(new Date());
        } catch (JWTDecodeException e) {
            log.error("检查Token过期失败", e);
            return true;
        }
    }

    /**
     * 刷新Token
     */
    public String refreshToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            Long userId = getUserId(token);
            String username = jwt.getClaim("username").asString();
            Long tenantId = jwt.getClaim("tenantId").asLong();

            return generateToken(userId, username, tenantId);
        } catch (JWTDecodeException e) {
            log.error("刷新Token失败", e);
            return null;
        }
    }

    private static List<String> parseAudienceList(String configuredAudience) {
        if (configuredAudience == null || configuredAudience.trim().isEmpty()) {
            return List.of();
        }
        List<String> results = new ArrayList<>();
        Arrays.stream(configuredAudience.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .forEach(results::add);
        return results;
    }

    /**
     * 获取Token过期时间
     */
    public Instant getExpiration(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt().toInstant();
        } catch (JWTDecodeException e) {
            log.error("获取Token过期时间失败", e);
            return null;
        }
    }

}
