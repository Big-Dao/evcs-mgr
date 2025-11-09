package com.evcs.station.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 充电桩服务测试类
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class, com.evcs.station.config.TestConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("充电桩服务测试")
class ChargerServiceTest extends BaseServiceTest {

    @Resource
    private IChargerService chargerService;

    @Resource
    private IStationService stationService;

    private Station testStation;

    @BeforeEach
    void setupStation() {
        // 创建测试充电站
        testStation = new Station();
        testStation.setStationCode(TestDataFactory.generateCode("STATION"));
        testStation.setStationName("测试充电站");
        testStation.setAddress("测试地址");
        testStation.setLatitude(39.9087);
        testStation.setLongitude(116.4089);
        testStation.setStatus(1);
        stationService.saveStation(testStation);
    }

    @Test
    @DisplayName("保存充电桩 - 正常流程")
    void testSaveCharger() {
        // Arrange
        Charger charger = new Charger();
        charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger.setChargerName("测试充电桩");
        charger.setStationId(testStation.getStationId());
        charger.setChargerType(1); // DC
        charger.setRatedPower(new java.math.BigDecimal("120.0"));
        charger.setStatus(1);
        charger.setSupportedProtocols("[\"OCPP1.6\"]");

        // Act
        boolean result = chargerService.saveCharger(charger);

        // Assert
        assertTrue(result);
        assertNotNull(charger.getId());
        assertEquals(DEFAULT_TENANT_ID, charger.getTenantId());
    }

    @Test
    @DisplayName("查询充电桩列表 - 按充电站ID查询")
    void testQueryChargersByStationId() {
        // Arrange - 创建多个充电桩
        for (int i = 0; i < 3; i++) {
            Charger charger = new Charger();
            charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
            charger.setChargerName("测试充电桩" + i);
            charger.setStationId(testStation.getStationId());
            charger.setChargerType(1);
            charger.setRatedPower(new java.math.BigDecimal("120.0"));
            charger.setStatus(1);
            charger.setSupportedProtocols("[\"OCPP1.6\"]");
            chargerService.saveCharger(charger);
        }

        // Act
        List<Charger> chargers = chargerService.getChargersByStationId(testStation.getStationId());

        // Assert
        assertNotNull(chargers);
        assertEquals(3, chargers.size());
        chargers.forEach(c -> assertEquals(testStation.getStationId(), c.getStationId()));
    }

    @Test
    @DisplayName("查询充电桩分页 - 正常查询")
    void testQueryChargerPage() {
        // Arrange
        Page<Charger> page = new Page<>(1, 10);
        Charger queryParam = new Charger();
        queryParam.setStationId(testStation.getStationId());

        // Act
        IPage<Charger> result = chargerService.queryChargerPage(page, queryParam);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("更新充电桩 - 正常流程")
    void testUpdateCharger() {
        // Arrange - 创建充电桩
        Charger charger = new Charger();
        charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger.setChargerName("原充电桩名称");
        charger.setStationId(testStation.getStationId());
        charger.setChargerType(1);
        charger.setRatedPower(new java.math.BigDecimal("120.0"));
        charger.setStatus(1);
        charger.setSupportedProtocols("[\"OCPP1.6\"]");
        chargerService.saveCharger(charger);

        // Act - 更新充电桩
        charger.setChargerName("更新后的充电桩名称");
        charger.setRatedPower(new java.math.BigDecimal("150.0"));
        boolean result = chargerService.updateCharger(charger);

        // Assert
        assertTrue(result);
        Charger updated = chargerService.getById(charger.getId());
        assertEquals("更新后的充电桩名称", updated.getChargerName());
        assertEquals(0, new java.math.BigDecimal("150.0").compareTo(updated.getRatedPower()));
    }

    @Test
    @DisplayName("修改充电桩状态 - 启用和停用")
    void testChangeChargerStatus() {
        // Arrange
        Charger charger = new Charger();
        charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger.setChargerName("测试充电桩");
        charger.setStationId(testStation.getStationId());
        charger.setChargerType(1);
        charger.setRatedPower(new java.math.BigDecimal("120.0"));
        charger.setStatus(1);
        charger.setSupportedProtocols("[\"OCPP1.6\"]");
        chargerService.saveCharger(charger);

        // Act - 停用
        boolean result = chargerService.changeStatus(charger.getId(), 0);

        // Assert
        assertTrue(result);
        Charger updated = chargerService.getById(charger.getId());
        assertEquals(0, updated.getEnabled());

        // Act - 启用
        result = chargerService.changeStatus(charger.getId(), 1);

        // Assert
        assertTrue(result);
        updated = chargerService.getById(charger.getId());
        assertEquals(1, updated.getEnabled());
    }

    @Test
    @DisplayName("删除充电桩 - 逻辑删除")
    void testDeleteCharger() {
        // Arrange
        Charger charger = new Charger();
        charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger.setChargerName("测试充电桩");
        charger.setStationId(testStation.getStationId());
        charger.setChargerType(1);
        charger.setRatedPower(new java.math.BigDecimal("120.0"));
        charger.setStatus(1);
        charger.setSupportedProtocols("[\"OCPP1.6\"]");
        chargerService.saveCharger(charger);
        Long chargerId = charger.getId();

        // Act
        boolean result = chargerService.deleteCharger(chargerId);

        // Assert
        assertTrue(result);
        Charger deleted = chargerService.getById(chargerId);
        assertNull(deleted, "逻辑删除后查询应返回null");
    }

    @Test
    @DisplayName("检查充电桩编码是否存在")
    void testCheckChargerCodeExists() {
        // Arrange
        String testCode = TestDataFactory.generateCode("CHARGER");
        Charger charger = new Charger();
        charger.setChargerCode(testCode);
        charger.setChargerName("测试充电桩");
        charger.setStationId(testStation.getStationId());
        charger.setChargerType(1);
        charger.setRatedPower(new java.math.BigDecimal("120.0"));
        charger.setStatus(1);
        charger.setSupportedProtocols("[\"OCPP1.6\"]");
        chargerService.saveCharger(charger);

        // Act & Assert - 编码存在
        boolean exists = chargerService.checkChargerCodeExists(testCode, null);
        assertTrue(exists, "编码应该存在");

        // Act & Assert - 编码不存在
        boolean notExists = chargerService.checkChargerCodeExists("NOTEXIST", null);
        assertFalse(notExists, "编码应该不存在");

        // Act & Assert - 排除自己
        boolean excludeSelf = chargerService.checkChargerCodeExists(testCode, charger.getId());
        assertFalse(excludeSelf, "排除自己后应该不存在");
    }

    @Test
    @DisplayName("查询离线充电桩")
    void testGetOfflineChargers() {
        // Arrange - 创建充电桩，但不设置心跳
        Charger charger = new Charger();
        charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger.setChargerName("测试充电桩");
        charger.setStationId(testStation.getStationId());
        charger.setChargerType(1);
        charger.setRatedPower(new java.math.BigDecimal("120.0"));
        charger.setStatus(1);
        charger.setSupportedProtocols("[\"OCPP1.6\"]");
        chargerService.saveCharger(charger);

        // Act
        List<Charger> offlineChargers = chargerService.getOfflineChargers(5);

        // Assert
        assertNotNull(offlineChargers);
        // 可能包含刚创建的充电桩（如果没有心跳时间）
    }

    @Test
    @DisplayName("批量更新充电桩状态")
    void testBatchUpdateStatus() {
        // Arrange - 创建多个充电桩
        Charger charger1 = createTestCharger("CHARGER1");
        Charger charger2 = createTestCharger("CHARGER2");
        List<Long> chargerIds = List.of(charger1.getId(), charger2.getId());

        // Act
        boolean result = chargerService.batchUpdateStatus(chargerIds, 0);

        // Assert
        assertTrue(result);
        assertEquals(0, chargerService.getById(charger1.getId()).getStatus());
        assertEquals(0, chargerService.getById(charger2.getId()).getStatus());
    }

    private Charger createTestCharger(String codePrefix) {
        Charger charger = new Charger();
        charger.setChargerCode(TestDataFactory.generateCode(codePrefix));
        charger.setChargerName("测试充电桩-" + codePrefix);
        charger.setStationId(testStation.getStationId());
        charger.setChargerType(1);
        charger.setRatedPower(new java.math.BigDecimal("120.0"));
        charger.setStatus(1);
        charger.setSupportedProtocols("[\"OCPP1.6\"]");
        chargerService.saveCharger(charger);
        return charger;
    }
}
