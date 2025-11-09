package com.evcs.auth.service;

import com.evcs.auth.entity.Permission;
import java.util.List;

/**
 * 权限服务接口
 */
public interface IPermissionService {

    /**
     * 根据角色ID列表查询权限
     */
    List<Permission> listByRoleIds(List<Long> roleIds);
}