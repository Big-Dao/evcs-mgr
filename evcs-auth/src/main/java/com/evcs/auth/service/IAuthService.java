package com.evcs.auth.service;

import com.evcs.auth.controller.dto.LoginResponse;
import com.evcs.auth.controller.dto.UserCreateRequest;

public interface IAuthService {
    /**
     * 用户登录
     * @param identifier 用户名/手机号/邮箱
     * @param password 密码
     * @return 登录响应信息
     */
    LoginResponse login(String identifier, String password);

    /**
     * 用户注册
     * @param request 注册请求
     */
    void register(UserCreateRequest request);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 刷新Token
     * @param token 旧Token
     * @return 新Token信息
     */
    LoginResponse refreshToken(String token);
    
    /**
     * 验证Token
     * @param token Token
     * @return 是否有效
     */
    boolean validateToken(String token);
}
