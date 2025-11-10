package com.evcs.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.evcs.auth.controller.dto.AssignRolesRequest;
import com.evcs.auth.controller.dto.PageResponse;
import com.evcs.auth.controller.dto.ResetPasswordRequest;
import com.evcs.auth.controller.dto.UserCreateRequest;
import com.evcs.auth.controller.dto.UserResponse;
import com.evcs.auth.controller.dto.UserUpdateRequest;
import com.evcs.auth.entity.SysUser;
import com.evcs.auth.service.ISysUserService;
import com.evcs.auth.service.dto.UserQuery;
import com.evcs.common.result.Result;
import com.evcs.common.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final ISysUserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public Result<PageResponse<UserResponse>> listUsers(@RequestParam(required = false) String username,
                                                        @RequestParam(required = false) String realName,
                                                        @RequestParam(required = false) Integer status,
                                                        @RequestParam(defaultValue = "1") @Min(1) long current,
                                                        @RequestParam(defaultValue = "10") @Min(1) long size,
                                                        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
                                                        @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        Long tenantId = resolveTenantId(tenantHeader);
        if (tenantId == null) {
            return Result.failure("缺少租户信息");
        }
        bindContext(tenantId, resolveUserId(userHeader));

        UserQuery query = new UserQuery();
        query.setUsername(username);
        query.setRealName(realName);
        query.setStatus(status);
        query.setCurrent(current);
        query.setSize(size);

        IPage<SysUser> page = userService.pageUsers(query, tenantId);
        List<UserResponse> responses = page.getRecords().stream()
                .map(user -> toResponse(user, userService.listRoleCodes(user.getId())))
                .toList();

        PageResponse<UserResponse> pageResponse = PageResponse.<UserResponse>builder()
                .records(responses)
                .total(page.getTotal())
                .size(page.getSize())
                .current(page.getCurrent())
                .pages(page.getPages())
                .build();

        return Result.success("查询成功", pageResponse);
    }

    @GetMapping("/{id}")
    public Result<UserResponse> getUser(@PathVariable Long id,
                                        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
                                        @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        Long tenantId = resolveTenantId(tenantHeader);
        if (tenantId == null) {
            return Result.failure("缺少租户信息");
        }
        bindContext(tenantId, resolveUserId(userHeader));

        SysUser user = userService.getUserByIdWithTenant(id, tenantId);
        if (user == null) {
            return Result.failure("用户不存在");
        }
        return Result.success("查询成功", toResponse(user, userService.listRoleCodes(id)));
    }

    @PostMapping
    public Result<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request,
                                           @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
                                           @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        Long tenantId = request.getTenantId() != null ? request.getTenantId() : resolveTenantId(tenantHeader);
        if (tenantId == null) {
            return Result.failure("缺少租户信息");
        }
        Long operatorId = resolveUserId(userHeader);
        bindContext(tenantId, operatorId);

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setStatus(request.getStatus());
        user.setUserType(request.getUserType() == null ? 2 : request.getUserType());
        user.setCreateBy(operatorId);
        user.setUpdateBy(operatorId);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userService.createUser(user, tenantId);

        return Result.success("创建成功", toResponse(user, Collections.emptyList()));
    }

    @PutMapping("/{id}")
    public Result<UserResponse> updateUser(@PathVariable Long id,
                                           @Valid @RequestBody UserUpdateRequest request,
                                           @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
                                           @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        Long tenantId = resolveTenantId(tenantHeader);
        if (tenantId == null) {
            return Result.failure("缺少租户信息");
        }
        Long operatorId = resolveUserId(userHeader);
        bindContext(tenantId, operatorId);

        SysUser update = new SysUser();
        update.setId(id);
        if (StringUtils.isNotBlank(request.getUsername())) {
            update.setUsername(request.getUsername());
        }
        if (StringUtils.isNotBlank(request.getRealName())) {
            update.setRealName(request.getRealName());
        }
        if (StringUtils.isNotBlank(request.getPhone())) {
            update.setPhone(request.getPhone());
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            update.setEmail(request.getEmail());
        }
        if (request.getGender() != null) {
            update.setGender(request.getGender());
        }
        if (request.getStatus() != null) {
            update.setStatus(request.getStatus());
        }
        if (request.getUserType() != null) {
            update.setUserType(request.getUserType());
        }
        update.setUpdateBy(operatorId);
        update.setUpdateTime(LocalDateTime.now());

        userService.updateUser(update, tenantId);
        SysUser updated = userService.getUserByIdWithTenant(id, tenantId);
        return Result.success("更新成功", toResponse(updated, userService.listRoleCodes(id)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id,
                                   @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
                                   @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        Long tenantId = resolveTenantId(tenantHeader);
        if (tenantId == null) {
            return Result.failure("缺少租户信息");
        }
        bindContext(tenantId, resolveUserId(userHeader));
        userService.deleteUser(id, tenantId);
        return Result.success("删除成功");
    }

    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody ResetPasswordRequest request,
                                      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
                                      @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        Long tenantId = resolveTenantId(tenantHeader);
        if (tenantId == null) {
            return Result.failure("缺少租户信息");
        }
        Long operatorId = resolveUserId(userHeader);
        bindContext(tenantId, operatorId);

        String encoded = passwordEncoder.encode(request.getNewPassword());
        userService.resetPassword(id, encoded, tenantId, operatorId);
        return Result.success("密码重置成功");
    }

    @GetMapping("/{id}/roles")
    public Result<List<String>> getUserRoles(@PathVariable Long id,
                                             @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
                                             @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        Long tenantId = resolveTenantId(tenantHeader);
        if (tenantId == null) {
            return Result.failure("缺少租户信息");
        }
        bindContext(tenantId, resolveUserId(userHeader));
        SysUser user = userService.getUserByIdWithTenant(id, tenantId);
        if (user == null) {
            return Result.failure("用户不存在");
        }
        return Result.success("查询成功", userService.listRoleCodes(id));
    }

    @PostMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id,
                                    @Valid @RequestBody AssignRolesRequest requestBody,
                                    @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
                                    @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
        Long tenantId = resolveTenantId(tenantHeader);
        if (tenantId == null) {
            return Result.failure("缺少租户信息");
        }
        Long operatorId = resolveUserId(userHeader);
        bindContext(tenantId, operatorId);

        List<Long> roleIds = requestBody.getRoleIds();
        if (CollectionUtils.isEmpty(roleIds)) {
            userService.assignRoles(id, Collections.emptyList(), tenantId, operatorId);
        } else {
            userService.assignRoles(id, roleIds, tenantId, operatorId);
        }
        return Result.success("角色分配成功");
    }

    private UserResponse toResponse(SysUser user, List<String> roles) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .gender(user.getGender())
                .status(user.getStatus())
                .userType(user.getUserType())
                .tenantId(user.getTenantId())
                .avatar(user.getAvatar())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .roles(roles)
                .build();
    }

    private Long resolveTenantId(String header) {
        if (!StringUtils.isNotBlank(header)) {
            return null;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException ex) {
            log.warn("无法解析租户ID: {}", header);
            return null;
        }
    }

    private Long resolveUserId(String header) {
        if (!StringUtils.isNotBlank(header)) {
            return null;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException ex) {
            log.warn("无法解析用户ID: {}", header);
            return null;
        }
    }

    private void bindContext(Long tenantId, Long userId) {
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        if (userId != null) {
            TenantContext.setUserId(userId);
        }
    }
}

