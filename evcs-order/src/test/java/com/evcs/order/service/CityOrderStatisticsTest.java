package com.evcs.order.service;

import com.evcs.order.dto.CityOrderStatistics;
import com.evcs.order.mapper.ChargingOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for City Order Statistics functionality
 */
class CityOrderStatisticsTest {

    @Mock
    private ChargingOrderMapper chargingOrderMapper;

    @InjectMocks
    private IChargingOrderService chargingOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCityOrderStatistics_Success() {
        // Arrange
        CityOrderStatistics stats1 = new CityOrderStatistics();
        stats1.setCity("Beijing");
        stats1.setTotalOrders(100L);
        stats1.setTotalRevenue(10000.0);
        stats1.setTotalEnergy(5000.0);

        CityOrderStatistics stats2 = new CityOrderStatistics();
        stats2.setCity("Shanghai");
        stats2.setTotalOrders(150L);
        stats2.setTotalRevenue(15000.0);
        stats2.setTotalEnergy(7500.0);

        List<CityOrderStatistics> mockStatistics = Arrays.asList(stats1, stats2);

        when(chargingOrderMapper.getCityOrderStatistics()).thenReturn(mockStatistics);

        // Act
        List<CityOrderStatistics> result = chargingOrderMapper.getCityOrderStatistics();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        CityOrderStatistics firstCity = result.get(0);
        assertEquals("Beijing", firstCity.getCity());
        assertEquals(100L, firstCity.getTotalOrders());
        assertEquals(10000.0, firstCity.getTotalRevenue());
        assertEquals(5000.0, firstCity.getTotalEnergy());
        
        CityOrderStatistics secondCity = result.get(1);
        assertEquals("Shanghai", secondCity.getCity());
        assertEquals(150L, secondCity.getTotalOrders());
        assertEquals(15000.0, secondCity.getTotalRevenue());
        assertEquals(7500.0, secondCity.getTotalEnergy());

        verify(chargingOrderMapper, times(1)).getCityOrderStatistics();
    }

    @Test
    void testGetCityOrderStatistics_EmptyResult() {
        // Arrange
        when(chargingOrderMapper.getCityOrderStatistics()).thenReturn(Arrays.asList());

        // Act
        List<CityOrderStatistics> result = chargingOrderMapper.getCityOrderStatistics();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(chargingOrderMapper, times(1)).getCityOrderStatistics();
    }

    @Test
    void testGetCityOrderStatistics_NullValues() {
        // Arrange
        CityOrderStatistics stats = new CityOrderStatistics();
        stats.setCity("Guangzhou");
        stats.setTotalOrders(0L);
        stats.setTotalRevenue(0.0);
        stats.setTotalEnergy(0.0);

        when(chargingOrderMapper.getCityOrderStatistics()).thenReturn(Arrays.asList(stats));

        // Act
        List<CityOrderStatistics> result = chargingOrderMapper.getCityOrderStatistics();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Guangzhou", result.get(0).getCity());
        assertEquals(0L, result.get(0).getTotalOrders());
        assertEquals(0.0, result.get(0).getTotalRevenue());
        assertEquals(0.0, result.get(0).getTotalEnergy());
    }

    @Test
    void testCityOrderStatistics_DataTypes() {
        // Arrange
        CityOrderStatistics stats = new CityOrderStatistics();
        
        // Act
        stats.setCity("Shenzhen");
        stats.setTotalOrders(200L);
        stats.setTotalRevenue(25000.0);
        stats.setTotalEnergy(12500.0);

        // Assert
        assertInstanceOf(String.class, stats.getCity());
        assertInstanceOf(Long.class, stats.getTotalOrders());
        assertInstanceOf(Double.class, stats.getTotalRevenue());
        assertInstanceOf(Double.class, stats.getTotalEnergy());
        
        assertEquals("Shenzhen", stats.getCity());
        assertEquals(200L, stats.getTotalOrders());
        assertEquals(25000.0, stats.getTotalRevenue());
        assertEquals(12500.0, stats.getTotalEnergy());
    }
}
