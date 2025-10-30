package com.evcs.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.auth.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户Mapper
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
