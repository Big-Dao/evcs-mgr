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
    
    /**
     * 获取充电站订单排名（Top N）
     */
    @InterceptorIgnore(tenantLine = "1")
    List<Map<String, Object>> getStationRanking(
            @Param("tenantId") Long tenantId,
            @Param("limit") Integer limit
    );
    
    /**
     * 获取充电桩利用率排名（Top N）
     */
    @InterceptorIgnore(tenantLine = "1")
    List<Map<String, Object>> getChargerUtilization(
            @Param("tenantId") Long tenantId,
            @Param("limit") Integer limit
    );

    /**
     * 统计充电桩状态分布
     */
    @InterceptorIgnore(tenantLine = "1")
    Map<String, Object> getChargerStatusStats(@Param("tenantId") Long tenantId);

    /**
     * 获取充电量趋势（最近N天）
     */
    @InterceptorIgnore(tenantLine = "1")
    List<Map<String, Object>> getChargingTrend(
            @Param("tenantId") Long tenantId,
            @Param("days") Integer days
    );

    /**
     * 获取收入趋势（最近N天）
     */
    @InterceptorIgnore(tenantLine = "1")
    List<Map<String, Object>> getRevenueTrend(
            @Param("tenantId") Long tenantId,
            @Param("days") Integer days
    );

    /**
     * 获取订单时段分布（按小时段聚合，当天）
     */
    @InterceptorIgnore(tenantLine = "1")
    List<Map<String, Object>> getOrderPeriodDistribution(
            @Param("tenantId") Long tenantId,
            @Param("date") java.time.LocalDate date,
            @Param("granularity") Integer granularity,
            @Param("stationId") Long stationId
    );
}
