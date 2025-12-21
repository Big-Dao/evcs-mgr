package com.evcs.station.service;

import com.evcs.common.test.base.BaseTenantIsolationTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class, com.evcs.station.config.TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("充电枪口会话字段更新测试")
class ChargerConnectorSessionUpdateTest extends BaseTenantIsolationTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    @Resource
    private IChargerConnectorService chargerConnectorService;

    @Test
    @DisplayName("会话更新 - start/stop 应写入并清理枪口会话字段")
    void testSessionUpdate_shouldPersistAndClearSessionFields_whenStartAndStopReceived() throws Exception {
        // Arrange
        Long chargerId = runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("STATION"));
            station.setStationName("会话测试站");
            station.setAddress("地址");
            station.setStatus(1);
            stationService.saveStation(station);

            Charger charger = new Charger();
            charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
            charger.setChargerName("会话测试桩");
            charger.setStationId(station.getStationId());
            charger.setChargerType(1);
            charger.setRatedPower(new BigDecimal("120.0"));
            charger.setStatus(1);
            charger.setSupportedProtocols("[\"CLOUD_CHARGE\"]");
            charger.setGunCount(2);
            charger.setGunTypes("[\"CCS\",\"CHAdeMO\"]");
            chargerService.saveCharger(charger);

            chargerConnectorService.ensureConnectors(charger.getId());
            return charger.getId();
        });

        LocalDateTime startTime = LocalDateTime.now().minusMinutes(1);

        // Act
        runAsTenant(1L, () -> {
            boolean okStart = chargerConnectorService.updateSessionStart(
                chargerId,
                1,
                "SESSION_X",
                100L,
                startTime,
                0.0
            );
            assertTrue(okStart, "start 更新应成功");

            var c1 = chargerConnectorService.listByChargerId(chargerId)
                .stream()
                .filter(c -> Integer.valueOf(1).equals(c.getConnectorNo()))
                .findFirst()
                .orElseThrow();

            assertEquals("SESSION_X", c1.getCurrentSessionId());
            assertEquals(100L, c1.getCurrentUserId());
            assertNotNull(c1.getChargingStartTime());

            boolean okStop = chargerConnectorService.updateSessionStop(
                chargerId,
                1,
                "SESSION_X",
                12.5,
                60L
            );
            assertTrue(okStop, "stop 更新应成功");

            var c1After = chargerConnectorService.listByChargerId(chargerId)
                .stream()
                .filter(c -> Integer.valueOf(1).equals(c.getConnectorNo()))
                .findFirst()
                .orElseThrow();

            assertNull(c1After.getCurrentSessionId(), "stop 后应清理 currentSessionId");
            assertNull(c1After.getCurrentUserId(), "stop 后应清理 currentUserId");
            assertNull(c1After.getChargingStartTime(), "stop 后应清理 chargingStartTime");
            assertNotNull(c1After.getChargedEnergy());
            assertEquals(0, c1After.getChargedEnergy().compareTo(new BigDecimal("12.5")));
            assertEquals(60, c1After.getChargedDuration());
        });
    }
}
