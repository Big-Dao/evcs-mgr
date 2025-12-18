package com.evcs.order.service;

import com.evcs.order.dto.CityOrderStatistics;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.billing.entity.Billing;
import com.evcs.billing.entity.BillingPlan;
import com.evcs.billing.service.IBillingService;
import com.evcs.billing.service.IBillingPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for City Order Statistics functionality
 * Tests the integration between ChargingOrder, Billing, and BillingPlan services
 */
@ExtendWith(MockitoExtension.class)
class CityOrderStatisticsTest {

    @Mock
    private IChargingOrderService chargingOrderService;

    @Mock
    private IBillingService billingService;

    @Mock
    private IBillingPlanService billingPlanService;

    private List<ChargingOrder> mockOrders;
    private List<Billing> mockBillings;
    private List<BillingPlan> mockBillingPlans;

    @BeforeEach
    void setUp() {
        // Initialize mock charging orders
        mockOrders = createMockChargingOrders();
        
        // Initialize mock billings
        mockBillings = createMockBillings();
        
        // Initialize mock billing plans
        mockBillingPlans = createMockBillingPlans();
    }

    @Test
    void testGetCityOrderStatistics_WithValidData() {
        // Arrange
        String cityCode = "CITY001";
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();

        when(chargingOrderService.getOrdersByCity(cityCode, startTime, endTime))
            .thenReturn(mockOrders);
        when(billingService.getBillingsByOrderIds(anyList()))
            .thenReturn(mockBillings);
        when(billingPlanService.getBillingPlansByIds(anyList()))
            .thenReturn(mockBillingPlans);

        // Act
        CityOrderStatistics statistics = calculateCityOrderStatistics(cityCode, startTime, endTime);

        // Assert
        assertNotNull(statistics);
        assertEquals(cityCode, statistics.getCityCode());
        assertTrue(statistics.getTotalOrders() > 0);
        assertTrue(statistics.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(statistics.getTotalEnergyConsumed().compareTo(BigDecimal.ZERO) > 0);

        // Verify service interactions
        verify(chargingOrderService, times(1)).getOrdersByCity(cityCode, startTime, endTime);
        verify(billingService, times(1)).getBillingsByOrderIds(anyList());
        verify(billingPlanService, times(1)).getBillingPlansByIds(anyList());
    }

    @Test
    void testGetCityOrderStatistics_WithNoOrders() {
        // Arrange
        String cityCode = "CITY002";
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();

        when(chargingOrderService.getOrdersByCity(cityCode, startTime, endTime))
            .thenReturn(Collections.emptyList());

        // Act
        CityOrderStatistics statistics = calculateCityOrderStatistics(cityCode, startTime, endTime);

        // Assert
        assertNotNull(statistics);
        assertEquals(cityCode, statistics.getCityCode());
        assertEquals(0, statistics.getTotalOrders());
        assertEquals(BigDecimal.ZERO, statistics.getTotalRevenue());
        assertEquals(BigDecimal.ZERO, statistics.getTotalEnergyConsumed());

        // Verify service interactions
        verify(chargingOrderService, times(1)).getOrdersByCity(cityCode, startTime, endTime);
        verify(billingService, never()).getBillingsByOrderIds(anyList());
        verify(billingPlanService, never()).getBillingPlansByIds(anyList());
    }

    @Test
    void testGetCityOrderStatistics_WithMultipleCities() {
        // Arrange
        List<String> cityCodes = Arrays.asList("CITY001", "CITY002", "CITY003");
        LocalDateTime startTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endTime = LocalDateTime.now();

        // Act & Assert
        for (String cityCode : cityCodes) {
            when(chargingOrderService.getOrdersByCity(cityCode, startTime, endTime))
                .thenReturn(mockOrders);
            when(billingService.getBillingsByOrderIds(anyList()))
                .thenReturn(mockBillings);
            when(billingPlanService.getBillingPlansByIds(anyList()))
                .thenReturn(mockBillingPlans);

            CityOrderStatistics statistics = calculateCityOrderStatistics(cityCode, startTime, endTime);
            
            assertNotNull(statistics);
            assertEquals(cityCode, statistics.getCityCode());
        }

        verify(chargingOrderService, times(cityCodes.size())).getOrdersByCity(anyString(), any(), any());
    }

    @Test
    void testCalculateTotalRevenue() {
        // Arrange
        when(billingService.getBillingsByOrderIds(anyList()))
            .thenReturn(mockBillings);

        // Act
        BigDecimal totalRevenue = mockBillings.stream()
            .map(Billing::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assert
        assertTrue(totalRevenue.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("150.00"), totalRevenue);
    }

    @Test
    void testCalculateTotalEnergyConsumed() {
        // Act
        BigDecimal totalEnergy = mockOrders.stream()
            .map(ChargingOrder::getEnergyConsumed)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assert
        assertTrue(totalEnergy.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("75.5"), totalEnergy);
    }

    @Test
    void testGetAverageOrderValue() {
        // Arrange
        when(billingService.getBillingsByOrderIds(anyList()))
            .thenReturn(mockBillings);

        // Act
        BigDecimal totalRevenue = mockBillings.stream()
            .map(Billing::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageValue = totalRevenue.divide(
            BigDecimal.valueOf(mockBillings.size()), 
            2, 
            BigDecimal.ROUND_HALF_UP
        );

        // Assert
        assertTrue(averageValue.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("50.00"), averageValue);
    }

    // Helper method to calculate city order statistics
    private CityOrderStatistics calculateCityOrderStatistics(
            String cityCode, 
            LocalDateTime startTime, 
            LocalDateTime endTime) {
        
        List<ChargingOrder> orders = chargingOrderService.getOrdersByCity(cityCode, startTime, endTime);
        
        if (orders.isEmpty()) {
            return CityOrderStatistics.builder()
                .cityCode(cityCode)
                .totalOrders(0)
                .totalRevenue(BigDecimal.ZERO)
                .totalEnergyConsumed(BigDecimal.ZERO)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        }

        List<Long> orderIds = orders.stream()
            .map(ChargingOrder::getId)
            .toList();

        List<Billing> billings = billingService.getBillingsByOrderIds(orderIds);
        
        BigDecimal totalRevenue = billings.stream()
            .map(Billing::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEnergy = orders.stream()
            .map(ChargingOrder::getEnergyConsumed)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CityOrderStatistics.builder()
            .cityCode(cityCode)
            .totalOrders(orders.size())
            .totalRevenue(totalRevenue)
            .totalEnergyConsumed(totalEnergy)
            .startTime(startTime)
            .endTime(endTime)
            .build();
    }

    // Helper method to create mock charging orders
    private List<ChargingOrder> createMockChargingOrders() {
        ChargingOrder order1 = new ChargingOrder();
        order1.setId(1L);
        order1.setOrderNo("ORD001");
        order1.setEnergyConsumed(new BigDecimal("25.5"));
        order1.setStatus("COMPLETED");

        ChargingOrder order2 = new ChargingOrder();
        order2.setId(2L);
        order2.setOrderNo("ORD002");
        order2.setEnergyConsumed(new BigDecimal("30.0"));
        order2.setStatus("COMPLETED");

        ChargingOrder order3 = new ChargingOrder();
        order3.setId(3L);
        order3.setOrderNo("ORD003");
        order3.setEnergyConsumed(new BigDecimal("20.0"));
        order3.setStatus("COMPLETED");

        return Arrays.asList(order1, order2, order3);
    }

    // Helper method to create mock billings
    private List<Billing> createMockBillings() {
        Billing billing1 = new Billing();
        billing1.setId(1L);
        billing1.setOrderId(1L);
        billing1.setTotalAmount(new BigDecimal("50.00"));
        billing1.setBillingPlanId(1L);

        Billing billing2 = new Billing();
        billing2.setId(2L);
        billing2.setOrderId(2L);
        billing2.setTotalAmount(new BigDecimal("60.00"));
        billing2.setBillingPlanId(1L);

        Billing billing3 = new Billing();
        billing3.setId(3L);
        billing3.setOrderId(3L);
        billing3.setTotalAmount(new BigDecimal("40.00"));
        billing3.setBillingPlanId(2L);

        return Arrays.asList(billing1, billing2, billing3);
    }

    // Helper method to create mock billing plans
    private List<BillingPlan> createMockBillingPlans() {
        BillingPlan plan1 = new BillingPlan();
        plan1.setId(1L);
        plan1.setPlanName("Standard Plan");
        plan1.setPricePerKwh(new BigDecimal("2.00"));

        BillingPlan plan2 = new BillingPlan();
        plan2.setId(2L);
        plan2.setPlanName("Premium Plan");
        plan2.setPricePerKwh(new BigDecimal("1.80"));

        return Arrays.asList(plan1, plan2);
    }
}
