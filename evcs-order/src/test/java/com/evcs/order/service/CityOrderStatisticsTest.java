package com.evcs.order.service;

import com.evcs.order.domain.CityOrderStatistics;
import com.evcs.order.mapper.CityOrderStatisticsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CityOrderStatisticsTest {

    @Mock
    private CityOrderStatisticsMapper cityOrderStatisticsMapper;

    @InjectMocks
    private CityOrderStatisticsService cityOrderStatisticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCityOrderStatistics() {
        // Arrange
        CityOrderStatistics stats1 = new CityOrderStatistics();
        stats1.setCity("Beijing");
        stats1.setTotalOrders(100);
        stats1.setTotalRevenue(new BigDecimal("10000.00"));
        stats1.setTotalEnergy(500.0);

        CityOrderStatistics stats2 = new CityOrderStatistics();
        stats2.setCity("Shanghai");
        stats2.setTotalOrders(150);
        stats2.setTotalRevenue(new BigDecimal("15000.00"));
        stats2.setTotalEnergy(750.0);

        List<CityOrderStatistics> expectedStats = Arrays.asList(stats1, stats2);
        when(cityOrderStatisticsMapper.selectCityOrderStatistics()).thenReturn(expectedStats);

        // Act
        List<CityOrderStatistics> actualStats = cityOrderStatisticsService.getCityOrderStatistics();

        // Assert
        assertNotNull(actualStats);
        assertEquals(2, actualStats.size());
        assertEquals("Beijing", actualStats.get(0).getCity());
        assertEquals(100, actualStats.get(0).getTotalOrders());
        assertEquals(new BigDecimal("10000.00"), actualStats.get(0).getTotalRevenue());
        assertEquals(500.0, actualStats.get(0).getTotalEnergy());

        assertEquals("Shanghai", actualStats.get(1).getCity());
        assertEquals(150, actualStats.get(1).getTotalOrders());
        assertEquals(new BigDecimal("15000.00"), actualStats.get(1).getTotalRevenue());
        assertEquals(750.0, actualStats.get(1).getTotalEnergy());

        verify(cityOrderStatisticsMapper, times(1)).selectCityOrderStatistics();
    }

    @Test
    void testGetCityOrderStatisticsEmpty() {
        // Arrange
        when(cityOrderStatisticsMapper.selectCityOrderStatistics()).thenReturn(Arrays.asList());

        // Act
        List<CityOrderStatistics> actualStats = cityOrderStatisticsService.getCityOrderStatistics();

        // Assert
        assertNotNull(actualStats);
        assertTrue(actualStats.isEmpty());
        verify(cityOrderStatisticsMapper, times(1)).selectCityOrderStatistics();
    }

    @Test
    void testGetCityOrderStatisticsNull() {
        // Arrange
        when(cityOrderStatisticsMapper.selectCityOrderStatistics()).thenReturn(null);

        // Act
        List<CityOrderStatistics> actualStats = cityOrderStatisticsService.getCityOrderStatistics();

        // Assert
        assertNull(actualStats);
        verify(cityOrderStatisticsMapper, times(1)).selectCityOrderStatistics();
    }

    @Test
    void testCityOrderStatisticsValues() {
        // Arrange
        CityOrderStatistics stats = new CityOrderStatistics();
        stats.setCity("Guangzhou");
        stats.setTotalOrders(200);
        stats.setTotalRevenue(new BigDecimal("20000.00"));
        stats.setTotalEnergy(1000.0);

        // Assert
        assertEquals("Guangzhou", stats.getCity());
        assertEquals(200, stats.getTotalOrders());
        assertEquals(new BigDecimal("20000.00"), stats.getTotalRevenue());
        assertEquals(1000.0, stats.getTotalEnergy());
    }
}
