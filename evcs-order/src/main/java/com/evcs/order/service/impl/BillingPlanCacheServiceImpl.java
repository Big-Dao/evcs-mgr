package com.evcs.order.service.impl;

import com.evcs.common.tenant.TenantContext;
import com.evcs.order.entity.BillingPlan;
import com.evcs.order.entity.BillingPlanSegment;
import com.evcs.order.service.IBillingPlanCacheService;
import com.evcs.order.service.IBillingPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 计费计划缓存服务实现
 * 使用Redis进行分布式缓存，通过Pub/Sub实现缓存失效广播
 * 
 * @author EVCS Team
 * @since M4 - Week 4 Performance Optimization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingPlanCacheServiceImpl implements IBillingPlanCacheService {
    
    private final IBillingPlanService billingPlanService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CACHE_PREFIX = "billing:plan:";
    private static final String DEFAULT_PREFIX = "billing:plan:default:";
    private static final String SEGMENTS_PREFIX = "billing:plan:segments:";
    private static final String INVALIDATE_TOPIC = "billing:plan:update";
    private static final long CACHE_TTL_HOURS = 1;
    
    @Override
    public BillingPlan getByStationAndId(Long stationId, Long planId) {
        if (stationId == null || planId == null) {
            return null;
        }
        
        String cacheKey = buildCacheKey(stationId, planId);
        
        // 尝试从缓存获取
        BillingPlan cached = (BillingPlan) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for billing plan: stationId={}, planId={}", stationId, planId);
            return cached;
        }
        
        // 缓存未命中，从数据库加载
        log.debug("Cache miss for billing plan: stationId={}, planId={}", stationId, planId);
        BillingPlan plan = billingPlanService.getById(planId);
        
        if (plan != null && plan.getStationId() != null && plan.getStationId().equals(stationId)) {
            // 放入缓存
            redisTemplate.opsForValue().set(cacheKey, plan, CACHE_TTL_HOURS, TimeUnit.HOURS);
            return plan;
        }
        
        return null;
    }
    
    @Override
    public BillingPlan getDefaultByStation(Long stationId) {
        if (stationId == null) {
            return null;
        }
        
        String cacheKey = DEFAULT_PREFIX + stationId;
        
        // 尝试从缓存获取
        BillingPlan cached = (BillingPlan) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for default billing plan: stationId={}", stationId);
            return cached;
        }
        
        // 缓存未命中，从数据库加载
        log.debug("Cache miss for default billing plan: stationId={}", stationId);
        BillingPlan plan = billingPlanService.getChargerPlan(null, stationId);
        
        if (plan != null) {
            // 放入缓存
            redisTemplate.opsForValue().set(cacheKey, plan, CACHE_TTL_HOURS, TimeUnit.HOURS);
        }
        
        return plan;
    }
    
    @Override
    public List<BillingPlanSegment> getSegments(Long planId) {
        if (planId == null) {
            return null;
        }
        
        String cacheKey = SEGMENTS_PREFIX + planId;
        
        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<BillingPlanSegment> cached = (List<BillingPlanSegment>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for billing plan segments: planId={}", planId);
            return cached;
        }
        
        // 缓存未命中，从数据库加载
        log.debug("Cache miss for billing plan segments: planId={}", planId);
        List<BillingPlanSegment> segments = billingPlanService.listSegments(planId);
        
        if (segments != null && !segments.isEmpty()) {
            // 放入缓存
            redisTemplate.opsForValue().set(cacheKey, segments, CACHE_TTL_HOURS, TimeUnit.HOURS);
        }
        
        return segments;
    }
    
    @Override
    public void invalidate(Long stationId, Long planId) {
        if (stationId == null || planId == null) {
            return;
        }
        
        String cacheKey = buildCacheKey(stationId, planId);
        redisTemplate.delete(cacheKey);
        
        // 广播缓存失效消息到其他实例
        broadcastInvalidation(cacheKey);
        
        log.info("Invalidated billing plan cache: stationId={}, planId={}", stationId, planId);
    }
    
    @Override
    public void invalidateDefault(Long stationId) {
        if (stationId == null) {
            return;
        }
        
        String cacheKey = DEFAULT_PREFIX + stationId;
        redisTemplate.delete(cacheKey);
        
        // 广播缓存失效消息到其他实例
        broadcastInvalidation(cacheKey);
        
        log.info("Invalidated default billing plan cache: stationId={}", stationId);
    }
    
    @Override
    public void invalidateSegments(Long planId) {
        if (planId == null) {
            return;
        }
        
        String cacheKey = SEGMENTS_PREFIX + planId;
        redisTemplate.delete(cacheKey);
        
        // 广播缓存失效消息到其他实例
        broadcastInvalidation(cacheKey);
        
        // 同时清理内存缓存
        billingPlanService.evictCache(planId);
        
        log.info("Invalidated billing plan segments cache: planId={}", planId);
    }
    
    @Override
    public void preloadHotStations(List<Long> stationIds) {
        if (stationIds == null || stationIds.isEmpty()) {
            return;
        }
        
        log.info("Preloading billing plans for {} hot stations", stationIds.size());
        
        for (Long stationId : stationIds) {
            try {
                // 预加载默认计划
                getDefaultByStation(stationId);
                log.debug("Preloaded default plan for station: {}", stationId);
            } catch (Exception e) {
                log.error("Failed to preload plan for station: {}", stationId, e);
            }
        }
        
        log.info("Completed preloading billing plans");
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(Long stationId, Long planId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        return CACHE_PREFIX + tenantId + ":" + stationId + ":" + planId;
    }
    
    /**
     * 广播缓存失效消息
     */
    private void broadcastInvalidation(String cacheKey) {
        try {
            redisTemplate.convertAndSend(INVALIDATE_TOPIC, cacheKey);
            log.debug("Broadcasted cache invalidation: {}", cacheKey);
        } catch (Exception e) {
            log.error("Failed to broadcast cache invalidation: {}", cacheKey, e);
        }
    }
}
