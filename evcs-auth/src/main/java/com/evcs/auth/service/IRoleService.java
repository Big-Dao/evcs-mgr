package com.evcs.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.auth.entity.Role;

import java.util.List;
import java.util.Set;

/**
 * 角色服务接口
 */
public interface IRoleService extends IService<Role> {
    
    /**
     * 分页查询角色列表
     */
    IPage<Role> pageRoles(Page<Role> page);
    
    /**
     * 查询所有角色列表
     */
    List<Role> listAllRoles();

    /**
     * 根据角色编码集合查询角色列表
     */
    List<Role> listByRoleCodes(Set<String> roleCodes);
}
