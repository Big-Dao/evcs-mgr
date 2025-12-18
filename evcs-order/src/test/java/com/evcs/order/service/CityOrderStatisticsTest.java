package com.evcs.order.service;

import com.evcs.order.domain.Order;
import com.evcs.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityOrderStatisticsTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BillingService billingService;

    @InjectMocks
    private CityOrderStatisticsService cityOrderStatisticsService;

    private List<Order> mockOrders;

    @BeforeEach
    void setUp() {
        Order order1 = new Order();
        order1.setId(1L);
        order1.setCityId(100L);
        order1.setStationId(1001L);
        order1.setChargerId(10001L);
        order1.setPlanId(1L);
        order1.setStartTime(LocalDateTime.of(2024, 1, 1, 10, 0));
        order1.setEndTime(LocalDateTime.of(2024, 1, 1, 12, 0));
        order1.setEnergyKwh(50.0);

        when(billingService.calculateAmount(any(LocalDateTime.class), any(LocalDateTime.class), anyDouble(), anyLong(), anyLong(), anyLong()))
                .thenReturn(BigDecimal.valueOf(100.0));

        Order order2 = new Order();
        order2.setId(2L);
        order2.setCityId(100L);
        order2.setStationId(1001L);
        order2.setChargerId(10002L);
        order2.setPlanId(1L);
        order2.setStartTime(LocalDateTime.of(2024, 1, 1, 14, 0));
        order2.setEndTime(LocalDateTime.of(2024, 1, 1, 16, 0));
        order2.setEnergyKwh(30.0);

        Order order3 = new Order();
        order3.setId(3L);
        order3.setCityId(200L);
        order3.setStationId(2001L);
        order3.setChargerId(20001L);
        order3.setPlanId(2L);
        order3.setStartTime(LocalDateTime.of(2024, 1, 1, 9, 0));
        order3.setEndTime(LocalDateTime.of(2024, 1, 1, 11, 0));
        order3.setEnergyKwh(40.0);

        mockOrders = Arrays.asList(order1, order2, order3);
    }

    @Test
    void testGetOrderStatisticsByCity() {
        when(orderRepository.findAll()).thenReturn(mockOrders);

        Map<Long, CityOrderStatistics> result = cityOrderStatisticsService.getOrderStatisticsByCity();

        assertNotNull(result);
        assertEquals(2, result.size());

        CityOrderStatistics city100Stats = result.get(100L);
        assertNotNull(city100Stats);
        assertEquals(100L, city100Stats.getCityId());
        assertEquals(2, city100Stats.getTotalOrders());
        assertEquals(80.0, city100Stats.getTotalEnergyKwh());
        assertEquals(BigDecimal.valueOf(200.0), city100Stats.getTotalRevenue());

        CityOrderStatistics city200Stats = result.get(200L);
        assertNotNull(city200Stats);
        assertEquals(200L, city200Stats.getCityId());
        assertEquals(1, city200Stats.getTotalOrders());
        assertEquals(40.0, city200Stats.getTotalEnergyKwh());
        assertEquals(BigDecimal.valueOf(100.0), city200Stats.getTotalRevenue());
    }

    @Test
    void testGetOrderStatisticsByCityWithNoOrders() {
        when(orderRepository.findAll()).thenReturn(Arrays.asList());

        Map<Long, CityOrderStatistics> result = cityOrderStatisticsService.getOrderStatisticsByCity();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
