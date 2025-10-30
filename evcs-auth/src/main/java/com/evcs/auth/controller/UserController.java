package com.evcs.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.auth.entity.User;
import com.evcs.auth.mapper.UserMapper;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.page.PageResult;
import com.evcs.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户管理控制器
 */
@Slf4j
@Tag(name = "用户管理", description = "用户的查询、创建、更新、删除等操作")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 获取用户列表
     */
    @Operation(summary = "获取用户列表", description = "分页查询用户列表")
    @GetMapping("/list")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "只能查询本租户用户")
    public Result<PageResult<User>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(User::getUsername, username);
        }
        if (StringUtils.isNotBlank(realName)) {
            queryWrapper.like(User::getRealName, realName);
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }
        
        queryWrapper.orderByDesc(User::getCreateTime);
        
        Page<User> page = new Page<>(current, size);
        IPage<User> result = userMapper.selectPage(page, queryWrapper);
        
        PageResult<User> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setPage((int) result.getCurrent());
        pageResult.setSize((int) result.getSize());
        pageResult.setPages((int) result.getPages());
        
        return Result.success("查询成功", pageResult);
    }
    
    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    @GetMapping("/{id}")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "只能查询本租户用户")
    public Result<User> getById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.failure("用户不存在");
        }
        // 清除敏感信息
        user.setPassword(null);
        return Result.success("查询成功", user);
    }
    
    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "只能在本租户创建用户")
    public Result<User> create(@Valid @RequestBody User user) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(checkWrapper) > 0) {
            return Result.failure("用户名已存在");
        }
        
        // 加密密码
        if (StringUtils.isNotBlank(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        int inserted = userMapper.insert(user);
        if (inserted > 0) {
            // 清除密码
            user.setPassword(null);
            return Result.success("创建成功", user);
        }
        return Result.failure("创建失败");
    }
    
    /**
     * 更新用户
     */
    @Operation(summary = "更新用户", description = "更新用户信息")
    @PutMapping("/{id}")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "只能更新本租户用户")
    public Result<User> update(@PathVariable Long id, @Valid @RequestBody User user) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            return Result.failure("用户不存在");
        }
        
        user.setId(id);
        
        // 如果修改了密码，需要加密
        if (StringUtils.isNotBlank(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // 不修改密码
            user.setPassword(null);
        }
        
        int updated = userMapper.updateById(user);
        if (updated > 0) {
            user.setPassword(null);
            return Result.success("更新成功", user);
        }
        return Result.failure("更新失败");
    }
    
    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "删除用户")
    @DeleteMapping("/{id}")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "只能删除本租户用户")
    public Result<Void> delete(@PathVariable Long id) {
        int deleted = userMapper.deleteById(id);
        if (deleted > 0) {
            return Result.success("删除成功");
        }
        return Result.failure("删除失败");
    }
}
