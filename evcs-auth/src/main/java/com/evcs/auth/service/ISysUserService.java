package com.evcs.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.auth.entity.SysUser;
import com.evcs.auth.service.dto.UserQuery;

import java.util.List;

public interface ISysUserService extends IService<SysUser> {

    IPage<SysUser> pageUsers(UserQuery query, Long tenantId);

    SysUser getUserByIdWithTenant(Long id, Long tenantId);

    List<String> listRoleCodes(Long userId);

    void assignRoles(Long userId, List<Long> roleIds, Long tenantId, Long operatorId);

    void resetPassword(Long userId, String encodedPassword, Long tenantId, Long operatorId);

    void createUser(SysUser user, Long tenantId);

    void updateUser(SysUser user, Long tenantId);

    void deleteUser(Long userId, Long tenantId);

    SysUser getByUsername(String username, Long tenantId);
}
