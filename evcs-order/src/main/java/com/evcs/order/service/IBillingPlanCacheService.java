package com.evcs.order.service;

import com.evcs.order.entity.BillingPlan;
import com.evcs.order.entity.BillingPlanSegment;

import java.util.List;

/**
 * 计费计划缓存服务接口
 * 提供Redis分布式缓存和缓存失效广播功能
 * 
 * @author EVCS Team
 * @since M4 - Week 4 Performance Optimization
 */
public interface IBillingPlanCacheService {
    
    /**
     * 根据站点ID和计划ID获取计费计划（带缓存）
     * 缓存键: billing:plan:{stationId}:{planId}
     * TTL: 1小时
     * 
     * @param stationId 站点ID
     * @param planId 计划ID
     * @return 计费计划，不存在返回null
     */
    BillingPlan getByStationAndId(Long stationId, Long planId);
    
    /**
     * 获取站点默认计费计划（带缓存）
     * 缓存键: billing:plan:default:{stationId}
     * TTL: 1小时
     * 
     * @param stationId 站点ID
     * @return 默认计费计划，不存在返回null
     */
    BillingPlan getDefaultByStation(Long stationId);
    
    /**
     * 获取计费计划的分段配置（带缓存）
     * 缓存键: billing:plan:segments:{planId}
     * TTL: 1小时
     * 
     * @param planId 计划ID
     * @return 分段配置列表
     */
    List<BillingPlanSegment> getSegments(Long planId);
    
    /**
     * 使计费计划缓存失效
     * 同时通过Redis Pub/Sub广播到所有实例
     * 
     * @param stationId 站点ID
     * @param planId 计划ID
     */
    void invalidate(Long stationId, Long planId);
    
    /**
     * 使站点默认计划缓存失效
     * 
     * @param stationId 站点ID
     */
    void invalidateDefault(Long stationId);
    
    /**
     * 使计划分段缓存失效
     * 
     * @param planId 计划ID
     */
    void invalidateSegments(Long planId);
    
    /**
     * 应用启动时预加载热点站点的计费计划
     * 
     * @param stationIds 热点站点ID列表
     */
    void preloadHotStations(List<Long> stationIds);
}
