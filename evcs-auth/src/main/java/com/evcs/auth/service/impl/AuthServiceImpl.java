package com.evcs.auth.service.impl;

import com.evcs.auth.controller.dto.LoginResponse;
import com.evcs.auth.controller.dto.UserCreateRequest;
import com.evcs.auth.entity.SysUser;
import com.evcs.auth.service.IAuthService;
import com.evcs.auth.service.ISysUserService;
import com.evcs.common.exception.BusinessException;
import com.evcs.common.result.ResultCode;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.common.tenant.TenantContext;
import com.evcs.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final ISysUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expire:7200}")
    private Long jwtExpireSeconds;

    @Override
    public LoginResponse login(String identifier, String password) {
        SysUser user;
        try {
            // 登录阶段允许跨租户查询，根据登录标识定位用户
            CustomTenantLineHandler.disableTenantFilter();
            user = userService.getByIdentifier(identifier);
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }

        if (user == null || user.getTenantId() == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "账号或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "账号或密码错误");
        }

        TenantContext.setTenantId(user.getTenantId());
        TenantContext.setUserId(user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());
        List<String> roles = userService.listRoleCodes(user.getId());

        Map<String, Object> userInfo = buildUserInfo(user, roles);

        long expiresIn = jwtExpireSeconds;
        Instant expiresAt = jwtUtil.getExpiration(token);
        if (expiresAt != null) {
            expiresIn = Math.max(0, ChronoUnit.SECONDS.between(Instant.now(), expiresAt));
        }

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userInfo)
                .build();
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            throw new BusinessException(401, "未登录");
        }

        SysUser user = userService.getUserByIdWithTenant(userId, tenantId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        List<String> roles = userService.listRoleCodes(userId);
        return buildUserInfo(user, roles);
    }

    private Map<String, Object> buildUserInfo(SysUser user, List<String> roles) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("tenantId", user.getTenantId());
        userInfo.put("username", user.getUsername());
        userInfo.put("identifier", user.getLoginIdentifier());
        userInfo.put("realName", user.getRealName());
        userInfo.put("status", user.getStatus());
        userInfo.put("userType", user.getUserType());
        if (!CollectionUtils.isEmpty(roles)) {
            userInfo.put("roles", roles);
        }
        return userInfo;
    }

    @Override
    public void register(UserCreateRequest request) {
        // 检查用户名是否已存在
        SysUser existingUser = userService.getByIdentifier(request.getLoginIdentifier());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1); // 默认启用
        
        Long tenantId = TenantContext.getTenantId();
        // 如果没有租户上下文，这里可能会有问题，但在Service层我们假设上下文已由Controller或Filter设置
        
        userService.createUser(user, tenantId);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userService.getUserByIdWithTenant(userId, TenantContext.getTenantId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        
        userService.resetPassword(userId, passwordEncoder.encode(newPassword), user.getTenantId(), userId);
    }

    @Override
    public LoginResponse refreshToken(String token) {
        if (!jwtUtil.verifyToken(token)) {
             throw new BusinessException(401, "Token无效或已过期");
        }
        
        String newToken = jwtUtil.refreshToken(token);
        if (newToken == null) {
            throw new BusinessException(401, "Token刷新失败");
        }
        
        return LoginResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpireSeconds)
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.verifyToken(token);
    }
}
