package com.evcs.order.service;

import com.evcs.common.exception.BusinessException;
import com.evcs.common.utils.TenantContext;
import com.evcs.order.domain.ChargingOrder;
import com.evcs.order.dto.CityOrderStatisticsDTO;
import com.evcs.order.mapper.ChargingOrderMapper;
import com.evcs.order.service.impl.ChargingOrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 城市订单统计测试
 */
@ExtendWith(MockitoExtension.class)
class CityOrderStatisticsTest {

    @Mock
    private ChargingOrderMapper chargingOrderMapper;

    @InjectMocks
    private ChargingOrderServiceImpl chargingOrderService;

    @BeforeEach
    void setUp() {
        // 每个测试前确保清空租户上下文
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        // 每个测试后清理租户上下文
        TenantContext.clear();
    }

    @Test
    @DisplayName("获取城市订单统计 - 成功场景")
    void testGetCityOrderStatistics_success() {
        // Given: 设置租户ID
        TenantContext.setTenantId(1L);
        
        // Mock数据
        CityOrderStatisticsDTO dto1 = new CityOrderStatisticsDTO();
        dto1.setCityCode("110000");
        dto1.setCityName("北京市");
        dto1.setOrderCount(100L);
        dto1.setTotalAmount(new BigDecimal("10000.00"));

        CityOrderStatisticsDTO dto2 = new CityOrderStatisticsDTO();
        dto2.setCityCode("310000");
        dto2.setCityName("上海市");
        dto2.setOrderCount(80L);
        dto2.setTotalAmount(new BigDecimal("8000.00"));

        List<CityOrderStatisticsDTO> mockResult = Arrays.asList(dto1, dto2);
        
        when(chargingOrderMapper.getCityOrderStatistics(any(ChargingOrder.class)))
            .thenReturn(mockResult);

        try {
            // When: 调用服务方法
            List<CityOrderStatisticsDTO> result = 
                chargingOrderService.getCityOrderStatistics(null, null);

            // Then: 验证结果
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCityCode()).isEqualTo("110000");
            assertThat(result.get(0).getCityName()).isEqualTo("北京市");
            assertThat(result.get(0).getOrderCount()).isEqualTo(100L);
            assertThat(result.get(0).getTotalAmount()).isEqualTo(new BigDecimal("10000.00"));
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
