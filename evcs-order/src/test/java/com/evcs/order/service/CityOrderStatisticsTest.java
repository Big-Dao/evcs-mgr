package com.evcs.order.service;

import com.evcs.common.context.TenantContext;
import com.evcs.common.exception.BusinessException;
import com.evcs.order.domain.entity.ChargingOrder;
import com.evcs.order.domain.vo.CityOrderStatisticsVO;
import com.evcs.order.mapper.ChargingOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 城市订单统计测试类
 */
@ExtendWith(MockitoExtension.class)
class CityOrderStatisticsTest {

    @Mock
    private ChargingOrderMapper chargingOrderMapper;

    @InjectMocks
    private ChargingOrderService chargingOrderService;

    @BeforeEach
    void setUp() {
        // 设置默认租户上下文
        TenantContext.setTenantId(1L);
    }

    @Test
    @DisplayName("获取城市订单统计 - 正常场景")
    void testGetCityOrderStatistics_success() {
        // Given: 准备测试数据
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        ChargingOrder order1 = new ChargingOrder();
        order1.setCity("北京");
        order1.setOrderAmount(new BigDecimal("100.00"));
        
        ChargingOrder order2 = new ChargingOrder();
        order2.setCity("北京");
        order2.setOrderAmount(new BigDecimal("150.00"));
        
        ChargingOrder order3 = new ChargingOrder();
        order3.setCity("上海");
        order3.setOrderAmount(new BigDecimal("200.00"));
        
        List<ChargingOrder> orders = Arrays.asList(order1, order2, order3);
        
        when(chargingOrderMapper.selectList(any())).thenReturn(orders);
        
        // When: 调用统计方法
        List<CityOrderStatisticsVO> result = chargingOrderService.getCityOrderStatistics(startDate, endDate);
        
        // Then: 验证结果
        assertThat(result).hasSize(2);
        
        CityOrderStatisticsVO beijing = result.stream()
            .filter(vo -> "北京".equals(vo.getCity()))
            .findFirst()
            .orElse(null);
        assertThat(beijing).isNotNull();
        assertThat(beijing.getOrderCount()).isEqualTo(2);
        assertThat(beijing.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        
        CityOrderStatisticsVO shanghai = result.stream()
            .filter(vo -> "上海".equals(vo.getCity()))
            .findFirst()
            .orElse(null);
        assertThat(shanghai).isNotNull();
        assertThat(shanghai.getOrderCount()).isEqualTo(1);
        assertThat(shanghai.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("获取城市订单统计 - 空数据")
    void testGetCityOrderStatistics_emptyData() {
        // Given: 没有订单数据
        when(chargingOrderMapper.selectList(any())).thenReturn(Arrays.asList());
        
        // When: 调用统计方法
        List<CityOrderStatisticsVO> result = chargingOrderService.getCityOrderStatistics(null, null);
        
        // Then: 返回空列表
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("获取城市订单统计 - 无租户上下文应抛出异常")
    void testGetCityOrderStatistics_noTenantContext() {
        // Given: 清空租户上下文
        TenantContext.clear();
        
        try {
            // When & Then: 在没有租户上下文的情况下调用应抛出BusinessException
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> 
                chargingOrderService.getCityOrderStatistics(null, null))
                .isInstanceOf(com.evcs.common.exception.BusinessException.class)
                .hasMessageContaining("缺少租户信息");
        } finally {
            TenantContext.clear();
        }
    }
}