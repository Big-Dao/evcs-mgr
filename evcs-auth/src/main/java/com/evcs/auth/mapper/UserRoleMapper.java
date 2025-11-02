package com.evcs.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.auth.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 用户角色关联 Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 根据用户ID查询角色ID列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} AND status = 1 AND tenant_id = #{tenantId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 根据用户ID查询角色编码列表
     */
    @Select("SELECT r.role_code FROM sys_user_role ur " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND ur.status = 1 AND r.status = 1 AND ur.tenant_id = #{tenantId}")
    Set<String> selectRoleCodesByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 根据角色编码查询角色ID列表
     */
    @Select("SELECT r.id FROM sys_role r WHERE r.role_code IN " +
            "<foreach collection='roleCodes' item='code' open='(' separator=',' close=')'>#{code}</foreach> " +
            "AND r.status = 1")
    List<Long> selectRoleIdsByRoleCodes(@Param("roleCodes") Set<String> roleCodes);

    /**
     * 根据角色ID列表查询用户ID列表
     */
    @Select("SELECT DISTINCT user_id FROM sys_user_role WHERE role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>#{roleId}</foreach> " +
            "AND status = 1 AND tenant_id = #{tenantId}")
    List<Long> selectUserIdsByRoleIds(@Param("roleIds") List<Long> roleIds, @Param("tenantId") Long tenantId);

    /**
     * 检查用户是否具有指定角色
     */
    @Select("SELECT COUNT(1) FROM sys_user_role ur " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.role_code = #{roleCode} " +
            "AND ur.status = 1 AND r.status = 1 AND ur.tenant_id = #{tenantId}")
    int countByUserIdAndRoleCode(@Param("userId") Long userId, @Param("roleCode") String roleCode, @Param("tenantId") Long tenantId);
}