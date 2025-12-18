package com.evcs.order.service;

import com.evcs.common.exception.BusinessException;
import com.evcs.common.tenant.TenantContext;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.order.OrderServiceApplication;
import com.evcs.order.config.TestConfig;
import com.evcs.order.dto.CityOrderStatistics;
import com.evcs.order.mapper.ChargingOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 城市订单统计功能测试
 * 测试地图分析所需的城市级别订单统计API
 * 使用Mock方式隔离跨模块依赖
 */
@SpringBootTest(classes = {OrderServiceApplication.class})
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("城市订单统计测试")
class CityOrderStatisticsTest extends BaseServiceTest {

    @Autowired
    private IChargingOrderService chargingOrderService;

    @MockBean
    private ChargingOrderMapper chargingOrderMapper;

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
        // Given: 设置租户上下文和Mock数据
        TenantContext.setCurrentTenantId(TEST_TENANT_ID);
        
        CityOrderStatistics stat1 = new CityOrderStatistics();
        stat1.setProvince("北京市");
        stat1.setCity("北京市");
        stat1.setOrderCount(100L);
        stat1.setStationCount(10L);
        stat1.setTotalEnergy(new BigDecimal("1000.0"));
        stat1.setTotalAmount(new BigDecimal("5000.0"));
        
        CityOrderStatistics stat2 = new CityOrderStatistics();
        stat2.setProvince("上海市");
        stat2.setCity("上海市");
        stat2.setOrderCount(80L);
        stat2.setStationCount(8L);
        stat2.setTotalEnergy(new BigDecimal("800.0"));
        stat2.setTotalAmount(new BigDecimal("4000.0"));
        
        when(chargingOrderMapper.getCityOrderStatistics(eq(TEST_TENANT_ID), any(), any()))
            .thenReturn(Arrays.asList(stat1, stat2));
        
        try {
            // When: 获取城市统计数据
            List<CityOrderStatistics> statistics = chargingOrderService.getCityOrderStatistics(null, null);
            
            // Then: 验证返回结果
            assertThat(statistics).isNotNull();
            assertThat(statistics).hasSize(2);
            
            // 验证第一个城市统计
            assertThat(statistics.get(0).getProvince()).isEqualTo("北京市");
            assertThat(statistics.get(0).getCity()).isEqualTo("北京市");
            assertThat(statistics.get(0).getOrderCount()).isEqualTo(100L);
            assertThat(statistics.get(0).getStationCount()).isEqualTo(10L);
            assertThat(statistics.get(0).getTotalEnergy()).isEqualByComparingTo(new BigDecimal("1000.0"));
            assertThat(statistics.get(0).getTotalAmount()).isEqualByComparingTo(new BigDecimal("5000.0"));
            
            // 验证第二个城市统计
            assertThat(statistics.get(1).getProvince()).isEqualTo("上海市");
            assertThat(statistics.get(1).getCity()).isEqualTo("上海市");
            assertThat(statistics.get(1).getOrderCount()).isEqualTo(80L);
            assertThat(statistics.get(1).getStationCount()).isEqualTo(8L);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("获取城市订单统计 - 带时间范围筛选")
    void testGetCityOrderStatistics_withTimeRange() {
        // Given: 设置租户上下文和时间范围
        TenantContext.setCurrentTenantId(TEST_TENANT_ID);
        LocalDateTime startTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endTime = LocalDateTime.now();
        
        // Mock返回空列表
        when(chargingOrderMapper.getCityOrderStatistics(eq(TEST_TENANT_ID), eq(startTime), eq(endTime)))
            .thenReturn(Collections.emptyList());
        
        try {
            // When: 获取指定时间范围的城市统计数据
            List<CityOrderStatistics> statistics = chargingOrderService.getCityOrderStatistics(startTime, endTime);
            
            // Then: 验证返回结果
            assertThat(statistics).isNotNull();
            assertThat(statistics).isEmpty();
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
