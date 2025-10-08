package com.evcs.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 用户信息
     */
    private UserInfo user;
    
    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private String phone;
        private String email;
        private String avatar;
        private Integer gender;
        private Long tenantId;
        private String tenantName;
        private List<String> roles;
        private List<String> permissions;
    }
}