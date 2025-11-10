package com.evcs.auth.service.impl;

import com.evcs.auth.entity.Permission;
import com.evcs.auth.service.IPermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 简易内存权限服务实现：启动期返回空列表或基本模拟数据。
 * 后续可替换为数据库/MyBatis实现。
 */
@Service
public class PermissionServiceImpl implements IPermissionService {

    @Override
    public List<Permission> listByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 示例：为所有角色提供一个通配符权限，便于开发联调通过鉴权
        List<Permission> list = new ArrayList<>();
        Permission p = new Permission();
        p.setId(1L);
        p.setPerms("system:*");
        p.setName("System All");
        p.setStatus(1);
        list.add(p);
        return list;
    }
}

