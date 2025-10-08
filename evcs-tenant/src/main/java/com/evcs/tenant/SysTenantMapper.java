package com.evcs.tenant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.tenant.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {
}
