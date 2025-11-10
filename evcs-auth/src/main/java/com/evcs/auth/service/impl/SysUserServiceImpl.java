package com.evcs.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.auth.entity.Role;
import com.evcs.auth.entity.SysUser;
import com.evcs.auth.entity.SysUserRole;
import com.evcs.auth.mapper.SysRoleMapper;
import com.evcs.auth.mapper.SysUserMapper;
import com.evcs.auth.mapper.SysUserRoleMapper;
import com.evcs.auth.service.ISysUserService;
import com.evcs.auth.service.dto.UserQuery;
import com.evcs.common.exception.TenantContextMissingException;
import com.evcs.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public IPage<SysUser> pageUsers(UserQuery query, Long tenantId) {
        ensureTenant(tenantId);
        Page<SysUser> page = new Page<>(Math.max(query.getCurrent(), 1), Math.max(query.getSize(), 1));
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getDeleted, 0)
                .like(StringUtils.isNotBlank(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.isNotBlank(query.getRealName()), SysUser::getRealName, query.getRealName())
                .eq(Objects.nonNull(query.getStatus()), SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getCreateTime);
        this.page(page, wrapper);

        if (page.getTotal() == 0 && !page.getRecords().isEmpty()) {
            LambdaQueryWrapper<SysUser> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(SysUser::getTenantId, tenantId)
                    .eq(SysUser::getDeleted, 0)
                    .like(StringUtils.isNotBlank(query.getUsername()), SysUser::getUsername, query.getUsername())
                    .like(StringUtils.isNotBlank(query.getRealName()), SysUser::getRealName, query.getRealName())
                    .eq(Objects.nonNull(query.getStatus()), SysUser::getStatus, query.getStatus());
            long total = this.count(countWrapper);
            page.setTotal(total);
            long pages = total == 0 ? 0 : (total + page.getSize() - 1) / page.getSize();
            page.setPages(pages);
        }

        return page;
    }

    @Override
    public SysUser getUserByIdWithTenant(Long id, Long tenantId) {
        ensureTenant(tenantId);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getId, id)
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getDeleted, 0);
        return this.getOne(wrapper, false);
    }

    @Override
    public List<String> listRoleCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return roleMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds, Long tenantId, Long operatorId) {
        ensureTenant(tenantId);
        SysUser user = getUserByIdWithTenant(userId, tenantId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 清理旧关系
        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, tenantId);
        userRoleMapper.delete(deleteWrapper);

        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        List<Long> validIds = roleMapper.selectValidRoleIds(roleIds, tenantId);
        if (validIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Set<Long> uniqueIds = validIds.stream().collect(Collectors.toSet());
        for (Long roleId : uniqueIds) {
            SysUserRole entity = new SysUserRole();
            entity.setUserId(userId);
            entity.setRoleId(roleId);
            entity.setTenantId(tenantId);
            entity.setCreateTime(now);
            entity.setCreateBy(operatorId);
            userRoleMapper.insert(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String encodedPassword, Long tenantId, Long operatorId) {
        ensureTenant(tenantId);
        SysUser user = getUserByIdWithTenant(userId, tenantId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(encodedPassword);
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(operatorId);
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(SysUser user, Long tenantId) {
        ensureTenant(tenantId);
        validateUsernameUnique(user.getUsername(), tenantId, null);
        user.setTenantId(tenantId);
        user.setDeleted(0);
        this.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user, Long tenantId) {
        ensureTenant(tenantId);
        if (user.getId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        SysUser existing = getUserByIdWithTenant(user.getId(), tenantId);
        if (existing == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (StringUtils.isNotBlank(user.getUsername()) && !user.getUsername().equals(existing.getUsername())) {
            validateUsernameUnique(user.getUsername(), tenantId, user.getId());
        }
        user.setTenantId(tenantId);
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId, Long tenantId) {
        ensureTenant(tenantId);
        SysUser existing = getUserByIdWithTenant(userId, tenantId);
        if (existing == null) {
            return;
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, tenantId);
        userRoleMapper.delete(wrapper);
        this.removeById(userId);
    }

    @Override
    public SysUser getByUsername(String username, Long tenantId) {
        if (StringUtils.isBlank(username) || tenantId == null) {
            return null;
        }
        ensureTenant(tenantId);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
                .last("LIMIT 1");
        return this.getOne(wrapper, false);
    }

    private void ensureTenant(Long tenantId) {
        if (tenantId == null) {
            throw new TenantContextMissingException("缺少租户信息");
        }
        TenantContext.setTenantId(tenantId);
    }

    private void validateUsernameUnique(String username, Long tenantId, Long excludeUserId) {
        if (StringUtils.isBlank(username)) {
            return;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0);
        if (excludeUserId != null) {
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        long count = this.count(wrapper);
        if (count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }
}
