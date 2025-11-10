package com.evcs.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.role_code FROM sys_role r " +
            "JOIN sys_user_role ur ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND (r.deleted IS NULL OR r.deleted = 0)")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select({
            "<script>",
            "SELECT r.id FROM sys_role r",
            "WHERE r.tenant_id = #{tenantId}",
            "AND (r.deleted IS NULL OR r.deleted = 0)",
            "<if test='roleIds != null and roleIds.size > 0'>",
            "AND r.id IN",
            "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</if>",
            "</script>"
    })
    List<Long> selectValidRoleIds(@Param("roleIds") List<Long> roleIds,
                                  @Param("tenantId") Long tenantId);
}
