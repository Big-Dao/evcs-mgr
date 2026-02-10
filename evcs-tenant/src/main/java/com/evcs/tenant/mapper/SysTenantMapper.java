package com.evcs.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.tenant.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    /**
     * 统计指定租户的子租户数量
     */
    @Select("SELECT COUNT(*) FROM sys_tenant WHERE parent_id = #{parentId} AND deleted = 0")
    int countByParentId(@Param("parentId") Long parentId);

    /**
     * 统计指定租户的站点数量
     */
    @Select("SELECT COUNT(*) FROM evcs_station WHERE tenant_id = #{tenantId} AND deleted = 0")
    int countStationsByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 统计指定租户的充电桩数量
     */
    @Select("SELECT COUNT(*) FROM evcs_charger WHERE tenant_id = #{tenantId} AND deleted = 0")
    int countChargersByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 统计多个租户的站点数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM evcs_station WHERE tenant_id IN " +
            "<foreach item='item' index='index' collection='tenantIds' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach> " +
            "AND deleted = 0" +
            "</script>")
    int countStationsByTenantIds(@Param("tenantIds") List<Long> tenantIds);

    /**
     * 统计多个租户的充电桩数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM evcs_charger WHERE tenant_id IN " +
            "<foreach item='item' index='index' collection='tenantIds' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach> " +
            "AND deleted = 0" +
            "</script>")
    int countChargersByTenantIds(@Param("tenantIds") List<Long> tenantIds);

    /**
     * 查询指定租户的所有后代租户ID（递归）
     */
    @Select("WITH RECURSIVE descendant_tree AS (" +
            "  SELECT id FROM sys_tenant WHERE id = #{tenantId} AND deleted = 0" +
            "  UNION ALL" +
            "  SELECT t.id FROM sys_tenant t INNER JOIN descendant_tree dt ON t.parent_id = dt.id WHERE t.deleted = 0" +
            ") SELECT id FROM descendant_tree WHERE id != #{tenantId}")
    List<Long> selectDescendantIds(@Param("tenantId") Long tenantId);
}
