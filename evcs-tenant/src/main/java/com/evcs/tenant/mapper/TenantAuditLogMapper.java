package com.evcs.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.tenant.entity.TenantAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 租户审计日志 Mapper
 */
@Mapper
public interface TenantAuditLogMapper extends BaseMapper<TenantAuditLog> {

    /**
     * 分页查询审计日志
     */
    @Select("<script>" +
            "SELECT * FROM tenant_audit_log " +
            "WHERE 1=1 " +
            "<if test='targetTenantId != null'>AND target_tenant_id = #{targetTenantId}</if> " +
            "<if test='action != null and action != \"\"'>AND action = #{action}</if> " +
            "<if test='startTime != null'>AND operate_time &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND operate_time &lt;= #{endTime}</if> " +
            "ORDER BY operate_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<TenantAuditLog> queryAuditLogPage(@Param("targetTenantId") Long targetTenantId,
                                           @Param("action") String action,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("offset") Integer offset,
                                           @Param("limit") Integer limit);

    /**
     * 统计审计日志数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM tenant_audit_log " +
            "WHERE 1=1 " +
            "<if test='targetTenantId != null'>AND target_tenant_id = #{targetTenantId}</if> " +
            "<if test='action != null and action != \"\"'>AND action = #{action}</if> " +
            "<if test='startTime != null'>AND operate_time &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND operate_time &lt;= #{endTime}</if> " +
            "</script>")
    long countAuditLog(@Param("targetTenantId") Long targetTenantId,
                      @Param("action") String action,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    /**
     * 查询跨层级操作统计
     */
    @Select("SELECT action, COUNT(*) as count, " +
            "SUM(CASE WHEN result = 'SUCCESS' THEN 1 ELSE 0 END) as success_count, " +
            "SUM(CASE WHEN result = 'FAILURE' THEN 1 ELSE 0 END) as failure_count " +
            "FROM tenant_audit_log " +
            "WHERE operator_tenant_id != target_tenant_id " +
            "AND operate_time >= #{startTime} " +
            "GROUP BY action")
    List<Map<String, Object>> statisticsCrossLayer(@Param("startTime") LocalDateTime startTime);
}
