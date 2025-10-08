package com.evcs.auth.controller;

import com.evcs.auth.dto.LoginRequest;
import com.evcs.auth.dto.LoginResponse;
import com.evcs.auth.service.UserService;
import com.evcs.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户登录、注销等认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "用户名密码登录获取访问令牌")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success("登录成功", response);
    }
    
    /**
     * 用户注销
     */
    @Operation(summary = "用户注销", description = "注销当前用户会话")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            userService.logout(token);
        }
        return Result.success("注销成功");
    }
    
    /**
     * 刷新Token
     */
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        LoginResponse response = userService.refreshToken(refreshToken);
        return Result.success("Token刷新成功", response);
    }
    
    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/userinfo")
    public Result<LoginResponse.UserInfo> getUserInfo(HttpServletRequest request) {
        // TODO: 实现获取当前用户信息逻辑
        return Result.success("获取成功", null);
    }
    
    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}