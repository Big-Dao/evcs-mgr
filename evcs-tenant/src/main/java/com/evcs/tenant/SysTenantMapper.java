package com.evcs.tenant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.tenant.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {
    
    /**
     * 统计指定租户在某个表中的数据量
     */
    @Select("SELECT COUNT(*) FROM ${tableName} WHERE tenant_id = #{tenantId}")
    Long countByTenantId(@Param("tableName") String tableName, @Param("tenantId") Long tenantId);
}
