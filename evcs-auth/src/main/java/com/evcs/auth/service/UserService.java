package com.evcs.auth.service;

import com.evcs.auth.dto.LoginRequest;
import com.evcs.auth.dto.LoginResponse;
import com.evcs.auth.entity.User;
import com.evcs.common.page.PageQuery;
import com.evcs.common.page.PageResult;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 用户注销
     */
    void logout(String token);
    
    /**
     * 刷新Token
     */
    LoginResponse refreshToken(String refreshToken);
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);
    
    /**
     * 根据用户名和租户ID查询用户
     */
    User findByUsernameAndTenantId(String username, Long tenantId);
    
    /**
     * 创建用户
     */
    User createUser(User user);
    
    /**
     * 更新用户
     */
    User updateUser(User user);
    
    /**
     * 删除用户
     */
    void deleteUser(Long id);
    
    /**
     * 分页查询用户
     */
    PageResult<User> pageUsers(PageQuery pageQuery);
    
    /**
     * 重置密码
     */
    void resetPassword(Long userId, String newPassword);
    
    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 启用/禁用用户
     */
    void changeStatus(Long userId, Integer status);
}