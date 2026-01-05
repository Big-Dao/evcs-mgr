package com.evcs.station.service;

import com.evcs.common.test.base.BaseTenantIsolationTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class, com.evcs.station.config.TestConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("充电枪口多租户隔离测试")
class ChargerConnectorTenantIsolationTest extends BaseTenantIsolationTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    @Resource
    private IChargerConnectorService chargerConnectorService;

    @Test
    @DisplayName("枪口落库 - 租户1创建的枪口不能被租户2访问")
    void testConnectorIsolationBetweenTenants() throws Exception {
        // Arrange - 租户1创建充电站 + 充电桩（2把枪）
        Long chargerId = runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("STATION_T1"));
            station.setStationName("租户1充电站");
            station.setAddress("地址");
            station.setStatus(1);
            stationService.saveStation(station);

            Charger charger = new Charger();
            charger.setChargerCode(TestDataFactory.generateCode("CHARGER_T1"));
            charger.setChargerName("租户1充电桩");
            charger.setStationId(station.getStationId());
            charger.setChargerType(1);
            charger.setRatedPower(new BigDecimal("120.0"));
            charger.setStatus(1);
            charger.setSupportedProtocols("[\"CLOUD_CHARGE\"]");
            charger.setGunCount(2);
            charger.setGunTypes("[\"CCS\",\"CHAdeMO\"]");
            chargerService.saveCharger(charger);

            // Act - 确保枪口存在
            var connectors = chargerConnectorService.ensureConnectors(charger.getId());

            // Assert - 租户1可见且数量正确
            assertNotNull(connectors);
            assertEquals(2, connectors.size(), "租户1应该创建2个枪口");
            connectors.forEach(c -> assertEquals(1L, c.getTenantId(), "枪口数据应属于租户1"));

            return charger.getId();
        });

        // Act & Assert - 租户2不能访问租户1的枪口
        runAsTenant(2L, () -> {
            var connectors = chargerConnectorService.ensureConnectors(chargerId);
            assertTrue(connectors.isEmpty(), "租户2不应该能为租户1的充电桩创建/查询枪口");

            var list = chargerConnectorService.listByChargerId(chargerId);
            assertTrue(list.isEmpty(), "租户2不应该能查询到租户1的枪口列表");
        });

        // Assert - 租户1仍可访问自己的枪口
        runAsTenant(1L, () -> {
            var list = chargerConnectorService.listByChargerId(chargerId);
            assertEquals(2, list.size(), "租户1应该能查询到自己的2个枪口");
            assertTrue(list.stream().anyMatch(c -> Integer.valueOf(1).equals(c.getConnectorNo())));
            assertTrue(list.stream().anyMatch(c -> Integer.valueOf(2).equals(c.getConnectorNo())));
        });
    }
}
