package com.evcs.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.auth.entity.Permission;

import java.util.List;

/**
 * 权限（菜单）服务接口
 */
public interface IPermissionService extends IService<Permission> {
    
    /**
     * 查询所有菜单权限（树形结构）
     */
    List<Permission> listMenuTree();
    
    /**
     * 查询用户的菜单权限
     */
    List<Permission> listUserMenus(Long userId);
}
