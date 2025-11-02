package com.evcs.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.auth.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色权限关联 Mapper
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 根据角色ID列表查询权限ID列表
     */
    @Select("SELECT DISTINCT permission_id FROM sys_role_permission WHERE role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>#{roleId}</foreach>")
    List<Long> selectPermissionIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据权限ID查询角色ID列表
     */
    @Select("SELECT DISTINCT role_id FROM sys_role_permission WHERE permission_id = #{permissionId}")
    List<Long> selectRoleIdsByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 检查角色是否具有指定权限
     */
    @Select("SELECT COUNT(1) FROM sys_role_permission WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    int countByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 根据角色ID删除权限关联
     */
    @Select("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限ID删除角色关联
     */
    @Select("DELETE FROM sys_role_permission WHERE permission_id = #{permissionId}")
    int deleteByPermissionId(@Param("permissionId") Long permissionId);
}