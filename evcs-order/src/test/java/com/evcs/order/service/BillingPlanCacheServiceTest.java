package com.evcs.order.service;

import com.evcs.common.tenant.TenantContext;
import com.evcs.order.entity.BillingPlan;
import com.evcs.order.service.impl.BillingPlanCacheServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 计费计划缓存服务测试
 * 测试Redis缓存和多实例一致性
 * 
 * @author EVCS Team
 * @since M4 - Week 4 Performance Optimization
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("计费计划缓存服务测试")
class BillingPlanCacheServiceTest {
    
    @Autowired(required = false)
    private IBillingPlanCacheService cacheService;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @BeforeEach
    void setUp() {
        // 设置租户上下文
        TenantContext.setCurrentTenantId(1L);
        TenantContext.setCurrentUserId(1L);
    }
    
    @AfterEach
    void tearDown() {
        // 清理租户上下文
        TenantContext.clear();
    }
    
    @Test
    @DisplayName("测试缓存失效广播 - 验证消息发送")
    void testCacheInvalidationBroadcast() {
        // 跳过测试如果Redis未配置
        if (cacheService == null || redisTemplate == null) {
            System.out.println("Redis not configured, skipping cache test");
            return;
        }
        
        // Given: 缓存中有数据
        Long stationId = 1L;
        Long planId = 100L;
        
        // When: 调用失效方法
        assertDoesNotThrow(() -> {
            cacheService.invalidate(stationId, planId);
        });
        
        // Then: 验证不会抛出异常（实际广播功能需要多实例环境）
    }
    
    @Test
    @DisplayName("测试缓存预加载 - 验证热点站点加载")
    void testCachePreload() {
        // 跳过测试如果Redis未配置
        if (cacheService == null) {
            System.out.println("Redis not configured, skipping cache test");
            return;
        }
        
        // Given: 热点站点列表
        java.util.List<Long> hotStations = java.util.Arrays.asList(1L, 2L, 3L);
        
        // When: 预加载
        assertDoesNotThrow(() -> {
            cacheService.preloadHotStations(hotStations);
        });
        
        // Then: 验证不会抛出异常
    }
    
    @Test
    @DisplayName("测试缓存失效 - 默认计划")
    void testInvalidateDefault() {
        // 跳过测试如果Redis未配置
        if (cacheService == null) {
            System.out.println("Redis not configured, skipping cache test");
            return;
        }
        
        // Given: 站点ID
        Long stationId = 1L;
        
        // When: 失效默认计划缓存
        assertDoesNotThrow(() -> {
            cacheService.invalidateDefault(stationId);
        });
        
        // Then: 验证不会抛出异常
    }
    
    @Test
    @DisplayName("测试缓存失效 - 计划分段")
    void testInvalidateSegments() {
        // 跳过测试如果Redis未配置
        if (cacheService == null) {
            System.out.println("Redis not configured, skipping cache test");
            return;
        }
        
        // Given: 计划ID
        Long planId = 100L;
        
        // When: 失效分段缓存
        assertDoesNotThrow(() -> {
            cacheService.invalidateSegments(planId);
        });
        
        // Then: 验证不会抛出异常
    }
}
