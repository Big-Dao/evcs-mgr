package com.evcs.order.service;

import com.evcs.common.exception.BusinessException;
import com.evcs.common.tenant.TenantContext;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.order.OrderServiceApplication;
import com.evcs.order.config.TestConfig;
import com.evcs.order.dto.CityOrderStatistics;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
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

    @BeforeEach
    void setUp() {
        // Mock billing service to return a fixed amount
        when(billingService.calculateAmount(
            any(LocalDateTime.class), 
            any(LocalDateTime.class), 
            anyDouble(), 
            anyLong(), 
            anyLong(), 
            anyLong()))
            .thenReturn(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("获取城市订单统计 - 应返回按城市聚合的数据")
    void testGetCityOrderStatistics_shouldReturnCityAggregatedData() {
        // Given: 设置租户上下文
        TenantContext.setCurrentTenantId(TEST_TENANT_ID);
        try {
            // When: 获取城市统计数据
            List<CityOrderStatistics> statistics = chargingOrderService.getCityOrderStatistics(null, null);
            
            // Then: 验证返回结果
            assertThat(statistics).isNotNull();
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
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("获取城市订单统计 - 无租户上下文应抛出异常")
    void testGetCityOrderStatistics_noTenantContext() {
        // Given: 清空租户上下文
        TenantContext.clear();
        
        try {
            // When & Then: 在没有租户上下文的情况下调用应抛出BusinessException
            assertThatThrownBy(() -> chargingOrderService.getCityOrderStatistics(null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("缺少租户信息");
        } finally {
            TenantContext.clear();
        }
    }
}
