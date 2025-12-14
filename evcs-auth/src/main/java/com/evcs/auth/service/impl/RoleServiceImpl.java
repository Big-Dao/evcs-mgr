package com.evcs.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.auth.entity.Role;
import com.evcs.auth.mapper.SysRoleMapper;
import com.evcs.auth.service.IRoleService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 角色服务实现类
 */
@Service
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, Role> implements IRoleService {

    @Override
    public List<Role> listByRoleCodes(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return list(new QueryWrapper<Role>().in("role_code", roleCodes));
    }

    @Override
    public List<Role> pageRoles(Object page) {
        // 暂不使用此旧接口，建议直接使用 IService 的 page 方法
        return Collections.emptyList();
    }

    @Override
    public List<Role> listAllRoles() {
        return list();
    }
}

