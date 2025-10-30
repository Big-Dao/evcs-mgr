package com.evcs.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.auth.entity.Permission;
import com.evcs.auth.mapper.PermissionMapper;
import com.evcs.auth.service.IPermissionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限（菜单）服务实现
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {
    
    @Override
    public List<Permission> listMenuTree() {
        QueryWrapper<Permission> qw = new QueryWrapper<>();
        qw.eq("type", 1); // 只查询菜单类型
        qw.eq("status", 1); // 只查询启用的
        qw.eq("visible", 1); // 只查询可见的
        qw.orderByAsc("sort");
        qw.orderByAsc("create_time");
        return list(qw);
    }
    
    @Override
    public List<Permission> listUserMenus(Long userId) {
        // TODO: 根据用户角色查询权限
        // 当前简单实现：返回所有菜单
        return listMenuTree();
    }
}
