package com.evcs.auth.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录DTO
 */
@Data
public class LoginRequest {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 租户ID（可选，如果不提供则为超级管理员登录）
     */
    private Long tenantId;
    
    /**
     * 验证码
     */
    private String captcha;
    
    /**
     * 验证码键值
     */
    private String captchaKey;
}