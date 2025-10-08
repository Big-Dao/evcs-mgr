package com.evcs.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.auth.dto.LoginRequest;
import com.evcs.auth.dto.LoginResponse;
import com.evcs.auth.entity.User;
import com.evcs.auth.mapper.UserMapper;
import com.evcs.auth.service.UserService;
import com.evcs.common.exception.BusinessException;
import com.evcs.common.page.PageQuery;
import com.evcs.common.page.PageResult;
import com.evcs.common.result.ResultCode;
import com.evcs.common.tenant.TenantContext;
import com.evcs.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        User user = findByUsernameAndTenantId(request.getUsername(), request.getTenantId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }
        
        // 设置租户上下文（用于后续操作）
        TenantContext.setTenantId(user.getTenantId());
        TenantContext.setUserId(user.getId());
        
        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());
        
        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(7200L); // 2小时
        
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setGender(user.getGender());
        userInfo.setTenantId(user.getTenantId());
        
        response.setUser(userInfo);
        
        return response;
    }
    
    @Override
    public void logout(String token) {
        // 将Token加入黑名单
        String key = "blacklist:token:" + token;
        stringRedisTemplate.opsForValue().set(key, "1", 2, TimeUnit.HOURS);
    }
    
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // TODO: 实现Token刷新逻辑
        throw new BusinessException("Token刷新功能待实现");
    }
    
    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        wrapper.eq(User::getDeleted, 0);
        return userMapper.selectOne(wrapper);
    }
    
    @Override
    public User findByUsernameAndTenantId(String username, Long tenantId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (tenantId != null) {
            wrapper.eq(User::getTenantId, tenantId);
        }
        wrapper.eq(User::getDeleted, 0);
        return userMapper.selectOne(wrapper);
    }
    
    @Override
    @Transactional
    public User createUser(User user) {
        // 检查用户名是否存在
        if (findByUsernameAndTenantId(user.getUsername(), user.getTenantId()) != null) {
            throw new BusinessException("用户名已存在");
        }
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        userMapper.insert(user);
        return user;
    }
    
    @Override
    @Transactional
    public User updateUser(User user) {
        User existingUser = userMapper.selectById(user.getId());
        if (existingUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }
    
    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        user.setDeleted(1);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
    
    @Override
    public PageResult<User> pageUsers(PageQuery pageQuery) {
        Page<User> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        
        if (pageQuery.getSortBy() != null) {
            if ("desc".equalsIgnoreCase(pageQuery.getSortDir())) {
                wrapper.orderByDesc(User::getCreateTime);
            } else {
                wrapper.orderByAsc(User::getCreateTime);
            }
        }
        
        IPage<User> result = userMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), pageQuery.getPage(), pageQuery.getSize());
    }
    
    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
    
    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
    
    @Override
    @Transactional
    public void changeStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}