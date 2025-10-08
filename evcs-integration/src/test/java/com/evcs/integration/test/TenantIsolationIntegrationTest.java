package com.evcs.integration.test;

import com.evcs.common.test.base.BaseTenantIsolationTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IStationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多租户隔离集成测试
 * 验证不同租户之间的数据完全隔离
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class},
                webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("多租户隔离集成测试")
class TenantIsolationIntegrationTest extends BaseTenantIsolationTest {

    @Resource
    private IStationService stationService;

    @Test
    @DisplayName("测试不同租户的数据隔离")
    void testDataIsolationBetweenTenants() {
        // 租户1创建充电站
        Long stationIdTenant1 = runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("T1-STATION"));
            station.setStationName("租户1的充电站");
            station.setAddress("租户1地址");
            station.setLatitude(39.9087);
            station.setLongitude(116.4089);
            station.setStatus(1);
            
            stationService.saveStation(station);
            assertNotNull(station.getStationId(), "租户1充电站ID应该被生成");
            assertEquals(1L, station.getTenantId(), "充电站应该属于租户1");
            
            return station.getStationId();
        });

        // 租户2创建充电站
        Long stationIdTenant2 = runAsTenant(2L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("T2-STATION"));
            station.setStationName("租户2的充电站");
            station.setAddress("租户2地址");
            station.setLatitude(31.2304);
            station.setLongitude(121.4737);
            station.setStatus(1);
            
            stationService.saveStation(station);
            assertNotNull(station.getStationId(), "租户2充电站ID应该被生成");
            assertEquals(2L, station.getTenantId(), "充电站应该属于租户2");
            
            return station.getStationId();
        });

        // 租户2不能访问租户1的数据
        runAsTenant(2L, () -> {
            Station station = stationService.getStationById(stationIdTenant1);
            assertTenantIsolation(station, "租户2不应该能访问租户1的充电站");
        });

        // 租户1不能访问租户2的数据
        runAsTenant(1L, () -> {
            Station station = stationService.getStationById(stationIdTenant2);
            assertTenantIsolation(station, "租户1不应该能访问租户2的充电站");
        });

        // 租户1可以访问自己的数据
        runAsTenant(1L, () -> {
            Station station = stationService.getStationById(stationIdTenant1);
            assertNotNull(station, "租户1应该能访问自己的充电站");
            assertEquals("租户1的充电站", station.getStationName());
        });

        // 租户2可以访问自己的数据
        runAsTenant(2L, () -> {
            Station station = stationService.getStationById(stationIdTenant2);
            assertNotNull(station, "租户2应该能访问自己的充电站");
            assertEquals("租户2的充电站", station.getStationName());
        });
    }

    @Test
    @DisplayName("测试租户上下文在并发场景下的隔离")
    void testTenantContextIsolationInConcurrency() throws InterruptedException {
        // 创建多个线程模拟并发请求
        Thread thread1 = new Thread(() -> {
            runAsTenant(1L, () -> {
                Station station = new Station();
                station.setStationCode(TestDataFactory.generateCode("CONCURRENT-T1"));
                station.setStationName("并发测试-租户1");
                station.setAddress("租户1并发地址");
                station.setLatitude(39.9087);
                station.setLongitude(116.4089);
                station.setStatus(1);
                
                stationService.saveStation(station);
                assertEquals(1L, station.getTenantId(), "并发场景下租户1的数据应该属于租户1");
                
                // 模拟一些处理时间
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });

        Thread thread2 = new Thread(() -> {
            runAsTenant(2L, () -> {
                Station station = new Station();
                station.setStationCode(TestDataFactory.generateCode("CONCURRENT-T2"));
                station.setStationName("并发测试-租户2");
                station.setAddress("租户2并发地址");
                station.setLatitude(31.2304);
                station.setLongitude(121.4737);
                station.setStatus(1);
                
                stationService.saveStation(station);
                assertEquals(2L, station.getTenantId(), "并发场景下租户2的数据应该属于租户2");
                
                // 模拟一些处理时间
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });

        // 启动并等待两个线程完成
        thread1.start();
        thread2.start();
        thread1.join(5000);
        thread2.join(5000);
        
        assertFalse(thread1.isAlive(), "线程1应该已完成");
        assertFalse(thread2.isAlive(), "线程2应该已完成");
    }

    @Test
    @DisplayName("测试租户上下文缺失时的异常处理")
    void testMissingTenantContextException() {
        // 清除租户上下文
        clearTenantContext();
        
        // 尝试在没有租户上下文的情况下执行操作
        assertThrows(Exception.class, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("NO-TENANT"));
            station.setStationName("无租户上下文测试");
            station.setAddress("测试地址");
            station.setLatitude(39.9087);
            station.setLongitude(116.4089);
            station.setStatus(1);
            
            // 这应该抛出异常，因为没有租户上下文
            stationService.saveStation(station);
        }, "在没有租户上下文时应该抛出异常");
    }

    @Test
    @DisplayName("测试租户切换时的上下文正确性")
    void testTenantContextSwitching() {
        // 创建租户1的数据
        Long stationId1 = runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("SWITCH-T1"));
            station.setStationName("切换测试-租户1");
            station.setAddress("租户1地址");
            station.setLatitude(39.9087);
            station.setLongitude(116.4089);
            station.setStatus(1);
            
            stationService.saveStation(station);
            return station.getStationId();
        });

        // 切换到租户2
        runAsTenant(2L, () -> {
            // 验证租户上下文已切换
            assertTenantContext(2L, "租户上下文应该已切换到租户2");
            
            // 尝试访问租户1的数据
            Station station = stationService.getStationById(stationId1);
            assertNull(station, "在租户2的上下文中不应该能访问租户1的数据");
        });

        // 切换回租户1
        runAsTenant(1L, () -> {
            // 验证租户上下文已切换回来
            assertTenantContext(1L, "租户上下文应该已切换回租户1");
            
            // 验证可以访问自己的数据
            Station station = stationService.getStationById(stationId1);
            assertNotNull(station, "在租户1的上下文中应该能访问自己的数据");
            assertEquals("切换测试-租户1", station.getStationName());
        });
    }

    @Test
    @DisplayName("测试租户数据查询列表隔离")
    void testTenantDataListIsolation() {
        // 租户1创建多个充电站
        runAsTenant(1L, () -> {
            for (int i = 1; i <= 3; i++) {
                Station station = new Station();
                station.setStationCode(TestDataFactory.generateCode("T1-LIST"));
                station.setStationName("租户1充电站" + i);
                station.setAddress("租户1地址" + i);
                station.setLatitude(39.9087);
                station.setLongitude(116.4089);
                station.setStatus(1);
                stationService.saveStation(station);
            }
        });

        // 租户2创建多个充电站
        runAsTenant(2L, () -> {
            for (int i = 1; i <= 2; i++) {
                Station station = new Station();
                station.setStationCode(TestDataFactory.generateCode("T2-LIST"));
                station.setStationName("租户2充电站" + i);
                station.setAddress("租户2地址" + i);
                station.setLatitude(31.2304);
                station.setLongitude(121.4737);
                station.setStatus(1);
                stationService.saveStation(station);
            }
        });

        // 验证租户1只能查询到自己的数据
        runAsTenant(1L, () -> {
            var stations = stationService.listStations();
            assertNotNull(stations, "应该能查询到充电站列表");
            // 至少有3个（因为可能有之前测试创建的数据）
            assertTrue(stations.size() >= 3, "租户1应该至少能查到3个自己的充电站");
            // 验证所有数据都属于租户1
            stations.forEach(s -> assertEquals(1L, s.getTenantId(), "所有充电站都应该属于租户1"));
        });

        // 验证租户2只能查询到自己的数据
        runAsTenant(2L, () -> {
            var stations = stationService.listStations();
            assertNotNull(stations, "应该能查询到充电站列表");
            // 至少有2个
            assertTrue(stations.size() >= 2, "租户2应该至少能查到2个自己的充电站");
            // 验证所有数据都属于租户2
            stations.forEach(s -> assertEquals(2L, s.getTenantId(), "所有充电站都应该属于租户2"));
        });
    }
}
