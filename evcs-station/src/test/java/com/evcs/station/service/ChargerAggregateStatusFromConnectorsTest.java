package com.evcs.station.service;

import com.evcs.common.test.base.BaseTenantIsolationTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.ChargerConnector;
import com.evcs.station.entity.Station;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@DisplayName("充电桩状态聚合测试（由枪口状态聚合）")
class ChargerAggregateStatusFromConnectorsTest extends BaseTenantIsolationTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    @Resource
    private IChargerConnectorService chargerConnectorService;

    @Test
    @DisplayName("枪口状态变更 - 应同步刷新充电桩聚合状态（覆盖0-5全部状态，故障>充电中>维护>预约>空闲>离线）")
    void testUpdateConnectorStatus_shouldSyncChargerAggregateStatus_withPriorityOrder() throws Exception {
        // Arrange
        Long chargerId = runAsTenant(1L, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("STATION"));
            station.setStationName("聚合测试站");
            station.setAddress("地址");
            station.setStatus(1);
            stationService.saveStation(station);

            Charger charger = new Charger();
            charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
            charger.setChargerName("聚合测试桩");
            charger.setStationId(station.getStationId());
            charger.setChargerType(1);
            charger.setRatedPower(new BigDecimal("120.0"));
            charger.setStatus(1);
            charger.setGunCount(2);
            charger.setGunTypes("[\"CCS\",\"CHAdeMO\"]");
            chargerService.saveCharger(charger);

            chargerConnectorService.ensureConnectors(charger.getId());
            return charger.getId();
        });

        LocalDateTime now = LocalDateTime.now().withNano(0);

        // Act & Assert
        runAsTenant(1L, () -> {
            // Arrange - 枪口基础信息应齐全（类型/编号）
            List<ChargerConnector> initialConnectors = chargerConnectorService.listByChargerId(chargerId);
            assertNotNull(initialConnectors);
            assertEquals(2, initialConnectors.size(), "应自动创建 2 个枪口记录");
            ChargerConnector c1 = getConnector(initialConnectors, 1);
            ChargerConnector c2 = getConnector(initialConnectors, 2);
            assertEquals(1, c1.getConnectorNo());
            assertEquals("CCS", c1.getConnectorType(), "connector#1 枪口类型应与 gunTypes 对应");
            assertEquals(2, c2.getConnectorNo());
            assertEquals("CHAdeMO", c2.getConnectorType(), "connector#2 枪口类型应与 gunTypes 对应");

            // 先全部离线
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 1, 0, null, null, now),
                "connector#1 设置为离线应成功"
            );
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 2, 0, null, null, now),
                "connector#2 设置为离线应成功"
            );

            List<ChargerConnector> offlineConnectors = chargerConnectorService.listByChargerId(chargerId);
            ChargerConnector offline1 = getConnector(offlineConnectors, 1);
            ChargerConnector offline2 = getConnector(offlineConnectors, 2);
            assertEquals(0, offline1.getStatus(), "connector#1 状态应为离线");
            assertEquals(0, offline2.getStatus(), "connector#2 状态应为离线");
            assertEquals(now, offline1.getLastHeartbeat() != null ? offline1.getLastHeartbeat().withNano(0) : null,
                "connector#1 最后心跳应更新");
            assertEquals(now, offline2.getLastHeartbeat() != null ? offline2.getLastHeartbeat().withNano(0) : null,
                "connector#2 最后心跳应更新");
            assertNull(offline1.getFaultCode(), "非故障状态时故障码应为空");
            assertNull(offline1.getFaultDescription(), "非故障状态时故障描述应为空");
            assertNull(offline2.getFaultCode(), "非故障状态时故障码应为空");
            assertNull(offline2.getFaultDescription(), "非故障状态时故障描述应为空");

            Charger afterAllOffline0 = chargerService.getById(chargerId);
            assertNotNull(afterAllOffline0);
            assertEquals(0, afterAllOffline0.getStatus(), "全部枪口离线，桩应为离线");

            // 覆盖：空闲（1）
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 1, 1, null, null, now),
                "connector#1 设置为空闲应成功"
            );

            ChargerConnector idle1 = getConnector(chargerConnectorService.listByChargerId(chargerId), 1);
            assertEquals(1, idle1.getStatus(), "connector#1 状态应为空闲");
            assertEquals(now, idle1.getLastHeartbeat() != null ? idle1.getLastHeartbeat().withNano(0) : null,
                "connector#1 最后心跳应更新");

            Charger afterIdle = chargerService.getById(chargerId);
            assertNotNull(afterIdle);
            assertEquals(1, afterIdle.getStatus(), "存在空闲且无更高优先级状态，桩应为空闲");

            // 覆盖：预约中（5）优先于空闲
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 2, 5, null, null, now),
                "connector#2 设置为预约中应成功"
            );

            ChargerConnector reserved2 = getConnector(chargerConnectorService.listByChargerId(chargerId), 2);
            assertEquals(5, reserved2.getStatus(), "connector#2 状态应为预约中");

            Charger afterReserved = chargerService.getById(chargerId);
            assertNotNull(afterReserved);
            assertEquals(5, afterReserved.getStatus(), "任一枪口预约中，且无更高优先级状态，桩应为预约中");

            // 覆盖：维护（4）优先于预约
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 1, 4, null, null, now),
                "connector#1 设置为维护应成功"
            );

            ChargerConnector maintenance1 = getConnector(chargerConnectorService.listByChargerId(chargerId), 1);
            assertEquals(4, maintenance1.getStatus(), "connector#1 状态应为维护");

            Charger afterMaintenance = chargerService.getById(chargerId);
            assertNotNull(afterMaintenance);
            assertEquals(4, afterMaintenance.getStatus(), "任一枪口维护，桩应为维护（优先于预约）");

            // 覆盖：充电中（2）优先于维护
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 1, 2, null, null, now),
                "connector#1 设置为充电中应成功"
            );

            ChargerConnector charging1 = getConnector(chargerConnectorService.listByChargerId(chargerId), 1);
            assertEquals(2, charging1.getStatus(), "connector#1 状态应为充电中");

            // 会话开始（覆盖：会话ID/开始充电/已充电量/已充时长）
            LocalDateTime startTime = now.minusMinutes(7);
            assertTrue(
                chargerConnectorService.updateSessionStart(
                    chargerId,
                    1,
                    "S001",
                    1001L,
                    startTime,
                    1.23
                ),
                "connector#1 会话开始应成功"
            );
            ChargerConnector afterSessionStart = getConnector(chargerConnectorService.listByChargerId(chargerId), 1);
            assertEquals("S001", afterSessionStart.getCurrentSessionId(), "会话开始后应写入会话ID");
            assertEquals(1001L, afterSessionStart.getCurrentUserId(), "会话开始后应写入当前用户ID");
            assertEquals(startTime, afterSessionStart.getChargingStartTime() != null ? afterSessionStart.getChargingStartTime().withNano(0) : null,
                "会话开始后应写入开始充电时间");
            assertNotNull(afterSessionStart.getChargedEnergy(), "会话开始后已充电量应有值");
            assertEquals(0, afterSessionStart.getChargedEnergy().compareTo(BigDecimal.valueOf(1.23)), "会话开始后已充电量应写入初始值");
            assertEquals(0, afterSessionStart.getChargedDuration(), "会话开始后已充时长应初始化为 0");

            Charger afterCharging = chargerService.getById(chargerId);
            assertNotNull(afterCharging);
            assertEquals(2, afterCharging.getStatus(), "任一枪口充电中，桩应为充电中");

            // 覆盖：故障（3）优先级最高
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 2, 3, "E001", "OverTemp", now),
                "connector#2 设置为故障应成功"
            );

            ChargerConnector fault2 = getConnector(chargerConnectorService.listByChargerId(chargerId), 2);
            assertEquals(3, fault2.getStatus(), "connector#2 状态应为故障");
            assertEquals("E001", fault2.getFaultCode(), "故障状态应写入故障码");
            assertEquals("OverTemp", fault2.getFaultDescription(), "故障状态应写入故障描述");

            Charger afterFault = chargerService.getById(chargerId);
            assertNotNull(afterFault);
            assertEquals(3, afterFault.getStatus(), "任一枪口故障，桩应为故障（优先级最高）");

            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 2, 1, null, null, now),
                "connector#2 恢复为空闲应成功"
            );

            ChargerConnector recover2 = getConnector(chargerConnectorService.listByChargerId(chargerId), 2);
            assertEquals(1, recover2.getStatus(), "connector#2 恢复后状态应为空闲");
            assertNull(recover2.getFaultCode(), "故障恢复后故障码应清空");
            assertNull(recover2.getFaultDescription(), "故障恢复后故障描述应清空");

            Charger afterRecover = chargerService.getById(chargerId);
            assertNotNull(afterRecover);
            assertEquals(2, afterRecover.getStatus(), "故障恢复后仍有枪口充电中，桩应回到充电中"
            );

            // 会话结束（覆盖：结束后会话字段清理 + 已充电量/已充时长更新）
            assertTrue(
                chargerConnectorService.updateSessionStop(chargerId, 1, "S001", 12.34, 42L),
                "connector#1 会话结束应成功"
            );
            ChargerConnector afterSessionStop = getConnector(chargerConnectorService.listByChargerId(chargerId), 1);
            assertNull(afterSessionStop.getCurrentSessionId(), "会话结束后会话ID应清理");
            assertNull(afterSessionStop.getCurrentUserId(), "会话结束后当前用户ID应清理");
            assertNull(afterSessionStop.getChargingStartTime(), "会话结束后开始充电时间应清理");
            assertNotNull(afterSessionStop.getChargedEnergy(), "会话结束后已充电量应有值");
            assertEquals(0, afterSessionStop.getChargedEnergy().compareTo(BigDecimal.valueOf(12.34)), "会话结束后已充电量应更新");
            assertEquals(42, afterSessionStop.getChargedDuration(), "会话结束后已充时长应更新");

            // 覆盖：充电结束后回退到预约（connector#2 先设置为预约）
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 2, 5, null, null, now),
                "connector#2 重新设置为预约中应成功"
            );
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 1, 1, null, null, now),
                "connector#1 结束充电设置为空闲应成功"
            );
            Charger afterChargingStop = chargerService.getById(chargerId);
            assertNotNull(afterChargingStop);
            assertEquals(5, afterChargingStop.getStatus(), "充电结束后仍存在预约中，桩应为预约中");

            // 全部回到空闲
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 2, 1, null, null, now),
                "connector#2 设置为空闲应成功"
            );
            Charger afterAllIdle = chargerService.getById(chargerId);
            assertNotNull(afterAllIdle);
            assertEquals(1, afterAllIdle.getStatus(), "全部枪口空闲，桩应为空闲");

            // 最终：全部离线
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 1, 0, null, null, now),
                "connector#1 设置为离线应成功"
            );
            assertTrue(
                chargerConnectorService.updateStatus(chargerId, 2, 0, null, null, now),
                "connector#2 设置为离线应成功"
            );
            Charger afterAllOffline = chargerService.getById(chargerId);
            assertNotNull(afterAllOffline);
            assertEquals(0, afterAllOffline.getStatus(), "全部枪口离线，桩应为离线");
        });
    }

    private static ChargerConnector getConnector(List<ChargerConnector> connectors, int connectorNo) {
        return connectors.stream()
            .filter(c -> c != null && c.getConnectorNo() != null && c.getConnectorNo() == connectorNo)
            .findFirst()
            .orElseThrow(() -> new AssertionError("未找到 connectorNo=" + connectorNo + " 的枪口记录"));
    }
}
