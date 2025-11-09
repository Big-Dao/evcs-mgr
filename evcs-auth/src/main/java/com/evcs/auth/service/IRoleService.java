package com.evcs.auth.service;

import com.evcs.auth.entity.Role;
import java.util.List;
import java.util.Set;

/**
 * 角色服务接口
 */
public interface IRoleService {

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

    /**
     * 根据ID查询角色
     */
    Role getById(Long id);

    /**
     * 保存角色
     */
    boolean save(Role role);

    /**
     * 根据ID更新角色
     */
    boolean updateById(Role role);

    /**
     * 根据ID删除角色
     */
    boolean removeById(Long id);
}