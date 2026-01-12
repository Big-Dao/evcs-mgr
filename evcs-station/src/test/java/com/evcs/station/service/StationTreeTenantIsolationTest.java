package com.evcs.station.service;

import com.evcs.common.test.base.BaseTenantIsolationTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.dto.ChargingStationTreeDTO;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = {com.evcs.station.StationServiceApplication.class, com.evcs.station.config.TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@DisplayName("充电站树形列表多租户隔离测试")
class StationTreeTenantIsolationTest extends BaseTenantIsolationTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    @Resource
    private IChargerConnectorService chargerConnectorService;

    @Test
    @DisplayName("树形列表隔离 - 仅返回当前租户站点与下挂设备")
    void treeShouldBeTenantIsolated() throws Exception {
        // tenant 1 data
        runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("T1_ST"));
            station.setStationName("T1 Station");
            station.setAddress("T1 Address");
            station.setLatitude(39.9);
            station.setLongitude(116.4);
            station.setStatus(1);
            stationService.saveStation(station);

            Charger charger = new Charger();
            charger.setChargerCode(TestDataFactory.generateCode("T1_CH"));
            charger.setChargerName("T1 Charger");
            charger.setStationId(station.getStationId());
            charger.setChargerType(1);
            charger.setGunCount(2);
            charger.setEnabled(1);
            charger.setStatus(1);
            assertTrue(chargerService.saveCharger(charger));

            chargerConnectorService.ensureConnectors(charger.getId());
        });

        // tenant 2 data
        runAsTenant(2L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("T2_ST"));
            station.setStationName("T2 Station");
            station.setAddress("T2 Address");
            station.setLatitude(31.2);
            station.setLongitude(121.5);
            station.setStatus(1);
            stationService.saveStation(station);

            Charger charger = new Charger();
            charger.setChargerCode(TestDataFactory.generateCode("T2_CH"));
            charger.setChargerName("T2 Charger");
            charger.setStationId(station.getStationId());
            charger.setChargerType(1);
            charger.setGunCount(1);
            charger.setEnabled(1);
            charger.setStatus(1);
            assertTrue(chargerService.saveCharger(charger));

            chargerConnectorService.ensureConnectors(charger.getId());
        });

        runAsTenant(1L, () -> {
            List<ChargingStationTreeDTO> tree = stationService.listChargingStationTree(new Station());
            assertNotNull(tree);
            assertFalse(tree.isEmpty(), "tenant 1 should have stations in tree");

            // Validate only tenant 1 station codes appear
            tree.forEach(node -> {
                assertNotNull(node.getStationId());
                assertNotNull(node.getStationCode());
                assertTrue(node.getStationCode().startsWith("T1_ST"), "should not leak other tenant station");
                node.getChargers().forEach(ch -> {
                    assertNotNull(ch.getChargerId());
                    assertNotNull(ch.getChargerCode());
                    assertTrue(ch.getChargerCode().startsWith("T1_CH"), "should not leak other tenant charger");
                    // connectors are auto-created, just verify list exists
                    assertNotNull(ch.getConnectors());
                });
            });
        });

        runAsTenant(2L, () -> {
            List<ChargingStationTreeDTO> tree = stationService.listChargingStationTree(new Station());
            assertNotNull(tree);
            assertFalse(tree.isEmpty(), "tenant 2 should have stations in tree");

            tree.forEach(node -> {
                assertNotNull(node.getStationCode());
                assertTrue(node.getStationCode().startsWith("T2_ST"), "should not leak other tenant station");
                node.getChargers().forEach(ch -> {
                    assertNotNull(ch.getChargerCode());
                    assertTrue(ch.getChargerCode().startsWith("T2_CH"), "should not leak other tenant charger");
                    assertNotNull(ch.getConnectors());
                });
            });
        });
    }
}
