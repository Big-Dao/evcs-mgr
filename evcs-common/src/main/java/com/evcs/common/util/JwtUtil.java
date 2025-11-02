package com.evcs.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:evcs-secret-key}")
    private String secret;
    
    @Value("${jwt.expire:7200}")
    private Long expire;
    
    /**
     * 生成JWT Token
     */
    public String generateToken(Long userId, String username, Long tenantId) {
        Date expireDate = new Date(System.currentTimeMillis() + expire * 1000);
        
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withClaim("tenantId", tenantId)
                .withExpiresAt(expireDate)
                .withIssuedAt(new Date())
                .sign(Algorithm.HMAC256(secret));
    }
    
    /**
     * 验证Token
     */
    public boolean verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            verifier.verify(token);
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
            return jwt.getClaim("userId").asLong();
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
            Long userId = jwt.getClaim("userId").asLong();
            String username = jwt.getClaim("username").asString();
            Long tenantId = jwt.getClaim("tenantId").asLong();

            return generateToken(userId, username, tenantId);
        } catch (JWTDecodeException e) {
            log.error("刷新Token失败", e);
            return null;
        }
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

    /**
     * 获取用户角色
     */
    public String getRoles(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            List<String> roles = jwt.getClaim("roles").asList(String.class);
            return roles != null ? String.join(",", roles) : "";
        } catch (Exception e) {
            log.error("获取用户角色失败", e);
            return "";
        }
    }
}
