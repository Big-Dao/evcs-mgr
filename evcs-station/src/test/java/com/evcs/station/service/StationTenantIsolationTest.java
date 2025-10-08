package com.evcs.station.service;

import com.evcs.common.test.base.BaseTenantIsolationTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Station;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 充电站多租户隔离测试
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class},
                webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("充电站多租户隔离测试")
class StationTenantIsolationTest extends BaseTenantIsolationTest {

    @Resource
    private IStationService stationService;

    @Test
    @DisplayName("数据隔离 - 租户1的充电站不能被租户2访问")
    void testStationIsolationBetweenTenants() {
        // Arrange & Act - 租户1创建充电站
        Long stationId = runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("TENANT1"));
            station.setStationName("租户1的充电站");
            station.setAddress("租户1地址");
            station.setLatitude(39.9087);
            station.setLongitude(116.4089);
            station.setStatus(1);
            stationService.saveStation(station);
            return station.getStationId();
        });

        // Assert - 租户2不能访问租户1的充电站
        runAsTenant(2L, () -> {
            Station station = stationService.getById(stationId);
            assertTenantIsolation(station, "租户2不应该能访问租户1的充电站");
        });

        // Assert - 租户1可以访问自己的充电站
        runAsTenant(1L, () -> {
            Station station = stationService.getById(stationId);
            assertNotNull(station, "租户1应该能访问自己的充电站");
            assertEquals("租户1的充电站", station.getStationName());
        });
    }

    @Test
    @DisplayName("查询隔离 - 列表查询只返回当前租户的充电站")
    void testQueryIsolation() {
        // Arrange - 租户1创建2个充电站
        runAsTenant(1L, () -> {
            for (int i = 0; i < 2; i++) {
                Station station = new Station();
                station.setStationCode(TestDataFactory.generateCode("TENANT1"));
                station.setStationName("租户1充电站" + i);
                station.setAddress("地址" + i);
                station.setStatus(1);
                stationService.saveStation(station);
            }
        });

        // Arrange - 租户2创建3个充电站
        runAsTenant(2L, () -> {
            for (int i = 0; i < 3; i++) {
                Station station = new Station();
                station.setStationCode(TestDataFactory.generateCode("TENANT2"));
                station.setStationName("租户2充电站" + i);
                station.setAddress("地址" + i);
                station.setStatus(1);
                stationService.saveStation(station);
            }
        });

        // Assert - 租户1只能查询到自己的2个充电站
        runAsTenant(1L, () -> {
            var list = stationService.list();
            // 由于可能有其他测试数据，我们只验证至少有2个
            assertTrue(list.size() >= 2, "租户1应该至少能查询到2个充电站");
            list.forEach(station -> {
                assertEquals(1L, station.getTenantId(), "所有充电站都应该属于租户1");
            });
        });

        // Assert - 租户2只能查询到自己的3个充电站
        runAsTenant(2L, () -> {
            var list = stationService.list();
            // 由于可能有其他测试数据，我们只验证至少有3个
            assertTrue(list.size() >= 3, "租户2应该至少能查询到3个充电站");
            list.forEach(station -> {
                assertEquals(2L, station.getTenantId(), "所有充电站都应该属于租户2");
            });
        });
    }

    @Test
    @DisplayName("更新隔离 - 租户不能更新其他租户的充电站")
    void testUpdateIsolation() {
        // Arrange - 租户1创建充电站
        Long stationId = runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("TENANT1"));
            station.setStationName("原始名称");
            station.setAddress("原始地址");
            station.setStatus(1);
            stationService.saveStation(station);
            return station.getStationId();
        });

        // Act & Assert - 租户2尝试更新租户1的充电站
        runAsTenant(2L, () -> {
            Station station = new Station();
            station.setStationId(stationId);
            station.setStationName("尝试修改");
            station.setAddress("尝试修改地址");
            
            // 由于租户隔离，update应该不会更新任何记录
            boolean result = stationService.updateStation(station);
            // 根据实际实现，这里可能返回false或抛出异常
            // 如果返回false，表示没有更新任何记录（因为找不到该ID的记录）
        });

        // Assert - 验证租户1的充电站未被修改
        runAsTenant(1L, () -> {
            Station station = stationService.getById(stationId);
            assertNotNull(station);
            assertEquals("原始名称", station.getStationName(), "充电站名称不应该被修改");
            assertEquals("原始地址", station.getAddress(), "充电站地址不应该被修改");
        });
    }

    @Test
    @DisplayName("删除隔离 - 租户不能删除其他租户的充电站")
    void testDeleteIsolation() {
        // Arrange - 租户1创建充电站
        Long stationId = runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("TENANT1"));
            station.setStationName("待删除充电站");
            station.setAddress("地址");
            station.setStatus(1);
            stationService.saveStation(station);
            return station.getStationId();
        });

        // Act & Assert - 租户2尝试删除租户1的充电站
        runAsTenant(2L, () -> {
            // 由于租户隔离，删除应该不会删除任何记录
            boolean result = stationService.deleteStation(stationId);
            // 根据实际实现，这里可能返回false或不影响任何记录
        });

        // Assert - 验证租户1的充电站仍然存在
        runAsTenant(1L, () -> {
            Station station = stationService.getById(stationId);
            assertNotNull(station, "充电站不应该被删除");
        });
    }

    @Test
    @DisplayName("编码唯一性 - 不同租户可以使用相同的充电站编码")
    void testCodeUniquenessAcrossTenants() {
        String sameCode = TestDataFactory.generateCode("SHARED");

        // Arrange - 租户1创建充电站
        runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(sameCode);
            station.setStationName("租户1的充电站");
            station.setAddress("地址1");
            station.setStatus(1);
            stationService.saveStation(station);
        });

        // Act & Assert - 租户2使用相同编码创建充电站应该成功
        runAsTenant(2L, () -> {
            Station station = new Station();
            station.setStationCode(sameCode);
            station.setStationName("租户2的充电站");
            station.setAddress("地址2");
            station.setStatus(1);
            
            // 不同租户可以使用相同编码
            boolean result = stationService.saveStation(station);
            assertTrue(result, "不同租户应该可以使用相同的充电站编码");
        });

        // Assert - 验证两个充电站都存在且属于不同租户
        runAsTenant(1L, () -> {
            boolean exists = stationService.checkStationCodeExists(sameCode, null);
            assertTrue(exists, "租户1应该能查到自己的充电站编码");
        });

        runAsTenant(2L, () -> {
            boolean exists = stationService.checkStationCodeExists(sameCode, null);
            assertTrue(exists, "租户2应该能查到自己的充电站编码");
        });
    }
}
