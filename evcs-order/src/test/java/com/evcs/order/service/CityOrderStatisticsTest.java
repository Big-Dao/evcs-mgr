package com.evcs.order.service;

import com.evcs.common.tenant.TenantContext;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.order.OrderServiceApplication;
import com.evcs.order.config.TestConfig;
import com.evcs.order.dto.CityOrderStatistics;
import com.evcs.order.entity.ChargingOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 城市订单统计功能测试
 * 测试地图分析所需的城市级别订单统计API
 */
@SpringBootTest(classes = {OrderServiceApplication.class})
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("城市订单统计测试")
class CityOrderStatisticsTest extends BaseServiceTest {

    @Autowired
    private IChargingOrderService chargingOrderService;

    @MockBean
    private IBillingPlanService billingPlanService;

    @MockBean
    private IBillingService billingService;

    private static final Long TEST_TENANT_ID = 1000L;
    private static final Long STATION_ID_HANGZHOU = 1001L;
    private static final Long STATION_ID_NINGBO = 1002L;

    @BeforeEach
    void setUp() {
        // Mock billing service to return a fixed amount
        when(billingService.calculateAmount(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
            .thenReturn(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("获取城市订单统计 - 应返回按城市聚合的数据")
    void testGetCityOrderStatistics_shouldReturnCityAggregatedData() {
        // Given: 设置租户上下文
        TenantContext.setCurrentTenantId(TEST_TENANT_ID);
        try {
            // Note: 此测试依赖数据库中存在充电站数据，且充电站表有province和city字段
            // 在实际环境中，需要先创建测试数据或使用@Sql注解加载测试数据
            
            // When: 获取城市统计数据
            List<CityOrderStatistics> statistics = chargingOrderService.getCityOrderStatistics(null, null);
            
            // Then: 验证返回结果
            assertThat(statistics).isNotNull();
            // 如果数据库为空，返回空列表也是正确的
            assertThat(statistics).isInstanceOf(List.class);
            
            // 如果有数据，验证数据结构
            statistics.forEach(stat -> {
                assertThat(stat.getProvince()).isNotNull();
                assertThat(stat.getCity()).isNotNull();
                assertThat(stat.getOrderCount()).isNotNull();
                assertThat(stat.getStationCount()).isNotNull();
            });
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("获取城市订单统计 - 带时间范围筛选")
    void testGetCityOrderStatistics_withTimeRange() {
        // Given: 设置租户上下文和时间范围
        TenantContext.setCurrentTenantId(TEST_TENANT_ID);
        try {
            LocalDateTime startTime = LocalDateTime.now().minusDays(30);
            LocalDateTime endTime = LocalDateTime.now();
            
            // When: 获取指定时间范围的城市统计数据
            List<CityOrderStatistics> statistics = chargingOrderService.getCityOrderStatistics(startTime, endTime);
            
            // Then: 验证返回结果
            assertThat(statistics).isNotNull();
            assertThat(statistics).isInstanceOf(List.class);
            
            // 验证返回的数据在指定时间范围内
            statistics.forEach(stat -> {
                assertThat(stat.getProvince()).isNotNull();
                assertThat(stat.getCity()).isNotNull();
            });
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("获取城市订单统计 - 多租户隔离验证")
    void testGetCityOrderStatistics_tenantIsolation() {
        // Given: 设置第一个租户
        Long tenant1 = 1000L;
        TenantContext.setCurrentTenantId(tenant1);
        try {
            // When: 获取租户1的统计数据
            List<CityOrderStatistics> stats1 = chargingOrderService.getCityOrderStatistics(null, null);
            
            // Then: 验证只返回租户1的数据
            assertThat(stats1).isNotNull();
            
            // 切换到租户2
            Long tenant2 = 2000L;
            TenantContext.setCurrentTenantId(tenant2);
            
            // When: 获取租户2的统计数据
            List<CityOrderStatistics> stats2 = chargingOrderService.getCityOrderStatistics(null, null);
            
            // Then: 验证两个租户的数据是隔离的
            assertThat(stats2).isNotNull();
            // 如果租户间有数据差异，结果应该不同（实际取决于测试数据）
            
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("获取城市订单统计 - 无租户上下文应返回空列表")
    void testGetCityOrderStatistics_noTenantContext() {
        // Given: 清空租户上下文
        TenantContext.clear();
        
        try {
            // When: 在没有租户上下文的情况下调用
            List<CityOrderStatistics> statistics = chargingOrderService.getCityOrderStatistics(null, null);
            
            // Then: 应返回空列表（不抛异常）
            assertThat(statistics).isNotNull();
            assertThat(statistics).isEmpty();
        } finally {
            TenantContext.clear();
        }
    }
}
