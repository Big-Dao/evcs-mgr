package com.evcs.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.auth.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限（菜单） Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
