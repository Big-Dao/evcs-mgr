package com.evcs.order.config;

import com.evcs.common.tenant.TenantContext;
import com.evcs.order.service.IBillingPlanCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

/**
 * 缓存预热启动器
 * 应用启动时预加载热点站点的计费计划到Redis缓存
 * 
 * @author EVCS Team
 * @since M4 - Week 4 Performance Optimization
 */
@Slf4j
@Component
@Profile("!test")  // 测试环境排除此组件
@RequiredArgsConstructor
public class CachePreloadRunner implements ApplicationRunner {
    
    private final IBillingPlanCacheService billingPlanCacheService;

    @Value("${evcs.tenant.default-tenant-id:1}")
    private Long defaultTenantId;

    @Value("${evcs.order.cache.hot-stations:1,2,3,4,5}")
    private List<Long> hotStationIds;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting cache preload for hot stations...");
        
        try {
            TenantContext.setCurrentTenantId(defaultTenantId);
            
            if (hotStationIds != null && !hotStationIds.isEmpty()) {
                log.info("Preloading {} hot stations: {}", hotStationIds.size(), hotStationIds);
                billingPlanCacheService.preloadHotStations(hotStationIds);
            } else {
                log.info("No hot stations configured for preload");
            }
            
            log.info("Cache preload completed successfully");
        } catch (Exception e) {
            log.error("Failed to preload cache", e);
            // 不抛出异常，避免影响应用启动
        } finally {
            TenantContext.clear();
        }
    }
}
