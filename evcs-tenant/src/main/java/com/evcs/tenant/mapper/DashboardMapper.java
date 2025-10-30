package com.evcs.tenant.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dashboard统计Mapper
 */
@Mapper
public interface DashboardMapper {
    
    /**
     * 统计租户数量（当前租户及其子租户）
     */
    Long countTenants(@Param("tenantId") Long tenantId);
    
    /**
     * 统计用户数量
     */
    @InterceptorIgnore(tenantLine = "1")
    Long countUsers(@Param("tenantId") Long tenantId);
    
    /**
     * 统计充电站数量
     */
    @InterceptorIgnore(tenantLine = "1")
    Long countStations(@Param("tenantId") Long tenantId);
    
    /**
     * 统计充电桩数量
     */
    @InterceptorIgnore(tenantLine = "1")
    Long countChargers(@Param("tenantId") Long tenantId);
    
    /**
     * 统计今日订单数
     */
    @InterceptorIgnore(tenantLine = "1")
    Long countTodayOrders(
            @Param("tenantId") Long tenantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 统计今日充电量
     */
    @InterceptorIgnore(tenantLine = "1")
    BigDecimal sumTodayChargingAmount(
            @Param("tenantId") Long tenantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 统计今日收入
     */
    @InterceptorIgnore(tenantLine = "1")
    BigDecimal sumTodayRevenue(
            @Param("tenantId") Long tenantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 获取最近订单列表
     */
    @InterceptorIgnore(tenantLine = "1")
    List<Map<String, Object>> getRecentOrders(
            @Param("tenantId") Long tenantId,
            @Param("limit") Integer limit
    );
}
