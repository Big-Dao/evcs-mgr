package com.evcs.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
