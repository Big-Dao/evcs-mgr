package com.evcs.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.auth.entity.Role;
import java.util.List;
import java.util.Set;

/**
 * 角色服务接口
 */
public interface IRoleService extends IService<Role> {

    /**
     * 根据角色编码列表查询角色
     */
    List<Role> listByRoleCodes(Set<String> roleCodes);

    /**
     * 分页查询角色
     */
    List<Role> pageRoles(Object page);

    /**
     * 查询所有角色
     */
    List<Role> listAllRoles();
}