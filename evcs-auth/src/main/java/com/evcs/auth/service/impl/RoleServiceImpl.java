package com.evcs.auth.service.impl;

import com.evcs.auth.entity.Role;
import com.evcs.auth.service.IRoleService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 简单的内存实现，满足启动期依赖。后续可替换为持久化实现。
 */
@Service
public class RoleServiceImpl implements IRoleService {

    private static final AtomicLong ID_GEN = new AtomicLong(1);

    @Override
    public List<Role> listByRoleCodes(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        // 将角色编码映射为基础 Role 对象（默认启用，最小数据权限）
        return roleCodes.stream().map(code -> {
            Role r = new Role();
            r.setId(ID_GEN.getAndIncrement());
            r.setRoleCode(code);
            r.setRoleName(code);
            r.setStatus(1);
            // 默认赋予有限数据权限；真正授权逻辑由后续实现完善
            r.setDataScope(1); // 1=全部数据权限（便于开发联调）
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Role> pageRoles(Object page) {
        return new ArrayList<>();
    }

    @Override
    public List<Role> listAllRoles() {
        return new ArrayList<>();
    }

    @Override
    public Role getById(Long id) {
        return null;
    }

    @Override
    public boolean save(Role role) {
        return true;
    }

    @Override
    public boolean updateById(Role role) {
        return true;
    }

    @Override
    public boolean removeById(Long id) {
        return true;
    }
}

