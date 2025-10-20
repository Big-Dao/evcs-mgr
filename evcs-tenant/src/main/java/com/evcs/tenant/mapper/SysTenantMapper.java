package com.evcs.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.tenant.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 租户Mapper接口
 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {
    
    /**
     * 统计指定表中某租户的记录数
     * 
     * @param tableName 表名
     * @param tenantId 租户ID
     * @return 记录数
     */
    @Select("SELECT COUNT(*) FROM ${tableName} WHERE tenant_id = #{tenantId} AND deleted = 0")
    Long countByTenantId(@Param("tableName") String tableName, @Param("tenantId") Long tenantId);
}
