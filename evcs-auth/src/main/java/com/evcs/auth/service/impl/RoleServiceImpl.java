package com.evcs.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.auth.entity.Role;
import com.evcs.auth.mapper.RoleMapper;
import com.evcs.auth.mapper.UserRoleMapper;
import com.evcs.auth.service.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    private final UserRoleMapper userRoleMapper;
    
    @Override
    public IPage<Role> pageRoles(Page<Role> page) {
        QueryWrapper<Role> qw = new QueryWrapper<>();
        qw.eq("status", 1); // 只查询启用的角色
        qw.orderByAsc("sort");
        qw.orderByDesc("create_time");
        return page(page, qw);
    }
    
    @Override
    public List<Role> listAllRoles() {
        QueryWrapper<Role> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        qw.orderByAsc("sort");
        return list(qw);
    }

    @Override
    public List<Role> listByRoleCodes(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoleMapper.selectRoleIdsByRoleCodes(roleCodes);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        QueryWrapper<Role> qw = new QueryWrapper<>();
        qw.in("id", roleIds);
        qw.eq("status", 1);
        qw.orderByAsc("sort");
        return list(qw);
    }
}
