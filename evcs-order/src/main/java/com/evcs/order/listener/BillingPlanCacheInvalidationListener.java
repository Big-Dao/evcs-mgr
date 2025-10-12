package com.evcs.order.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 计费计划缓存失效监听器
 * 监听Redis Pub/Sub消息，清理本地实例的缓存
 * 
 * @author EVCS Team
 * @since M4 - Week 4 Performance Optimization
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPlanCacheInvalidationListener {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 接收缓存失效消息
     * 当其他实例修改计费计划时，通过此方法清理本地缓存
     * 
     * @param message 缓存键，格式: billing:plan:{tenantId}:{stationId}:{planId} 或 billing:plan:default:{stationId}
     */
    public void onMessage(String message) {
        try {
            log.debug("Received cache invalidation message: {}", message);
            
            // 删除本地Redis缓存
            Boolean deleted = redisTemplate.delete(message);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Successfully invalidated local cache for key: {}", message);
            } else {
                log.debug("Cache key not found in local instance: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to process cache invalidation message: {}", message, e);
        }
    }
}
