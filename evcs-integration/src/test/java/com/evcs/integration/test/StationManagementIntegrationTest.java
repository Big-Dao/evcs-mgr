package com.evcs.integration.test;

import com.evcs.common.test.base.BaseIntegrationTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IChargerService;
import com.evcs.station.service.IStationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 充电站管理集成测试
 * 测试充电站和充电桩的完整业务流程
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class, com.evcs.integration.config.TestConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("充电站管理集成测试")
class StationManagementIntegrationTest extends BaseIntegrationTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    @Test
    @DisplayName("端到端测试：创建充电站并添加充电桩")
    void testCreateStationWithChargers() {
        // 1. 创建充电站
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("集成测试充电站");
        station.setAddress("北京市海淀区测试路123号");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        
        boolean stationSaved = stationService.saveStation(station);
        assertTrue(stationSaved, "充电站应该保存成功");
        assertNotNull(station.getStationId(), "充电站ID应该被生成");
        assertEquals(DEFAULT_TENANT_ID, station.getTenantId(), "充电站应该属于默认租户");

        // 2. 为充电站添加充电桩
        Charger charger1 = new Charger();
        charger1.setStationId(station.getStationId());
        charger1.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger1.setChargerName("1号充电桩");
        charger1.setChargerType(1); // 直流快充
        charger1.setStatus(1); // 空闲
        charger1.setRatedPower(new BigDecimal("60.0"));
        
        boolean charger1Saved = chargerService.saveCharger(charger1);
        assertTrue(charger1Saved, "充电桩1应该保存成功");
        assertNotNull(charger1.getId(), "充电桩1 ID应该被生成");
        assertEquals(DEFAULT_TENANT_ID, charger1.getTenantId(), "充电桩1应该属于默认租户");

        // 3. 添加第二个充电桩
        Charger charger2 = new Charger();
        charger2.setStationId(station.getStationId());
        charger2.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger2.setChargerName("2号充电桩");
        charger2.setChargerType(2); // 交流慢充
        charger2.setStatus(1);
        charger2.setRatedPower(new BigDecimal("7.0"));
        
        boolean charger2Saved = chargerService.saveCharger(charger2);
        assertTrue(charger2Saved, "充电桩2应该保存成功");

        // 4. 验证可以查询到充电站及其充电桩
        Station retrievedStation = stationService.getById(station.getStationId());
        assertNotNull(retrievedStation, "应该能查询到充电站");
        assertEquals(station.getStationCode(), retrievedStation.getStationCode());

        // 5. 验证充电桩属于该充电站
        Charger retrievedCharger1 = chargerService.getById(charger1.getId());
        assertNotNull(retrievedCharger1, "应该能查询到充电桩1");
        assertEquals(station.getStationId(), retrievedCharger1.getStationId(), "充电桩1应该属于创建的充电站");

        Charger retrievedCharger2 = chargerService.getById(charger2.getId());
        assertNotNull(retrievedCharger2, "应该能查询到充电桩2");
        assertEquals(station.getStationId(), retrievedCharger2.getStationId(), "充电桩2应该属于创建的充电站");
    }

    @Test
    @DisplayName("测试充电站状态更新流程")
    void testStationStatusUpdate() {
        // 1. 创建充电站
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("状态测试充电站");
        station.setAddress("上海市浦东新区测试路456号");
        station.setLatitude(31.2304);
        station.setLongitude(121.4737);
        station.setStatus(1); // 初始状态：运营中
        
        stationService.saveStation(station);
        assertNotNull(station.getStationId());

        // 2. 更新充电站状态为维护中
        Station updateStation = stationService.getById(station.getStationId());
        updateStation.setStatus(2); // 维护中
        boolean updated = stationService.updateStation(updateStation);
        assertTrue(updated, "充电站状态应该更新成功");

        // 3. 验证状态已更新
        Station verifyStation = stationService.getById(station.getStationId());
        assertEquals(2, verifyStation.getStatus(), "充电站状态应该是维护中");

        // 4. 再次更新为停止运营
        verifyStation.setStatus(0); // 停止运营
        updated = stationService.updateStation(verifyStation);
        assertTrue(updated, "充电站状态应该更新成功");

        // 5. 最终验证
        Station finalStation = stationService.getById(station.getStationId());
        assertEquals(0, finalStation.getStatus(), "充电站状态应该是停止运营");
    }

    @Test
    @DisplayName("测试充电桩状态变化流程")
    void testChargerStatusTransition() {
        // 1. 创建充电站
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("充电桩测试充电站");
        station.setAddress("广州市天河区测试路789号");
        station.setLatitude(23.1291);
        station.setLongitude(113.2644);
        station.setStatus(1);
        
        stationService.saveStation(station);

        // 2. 创建充电桩（初始状态：空闲）
        Charger charger = new Charger();
        charger.setStationId(station.getStationId());
        charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger.setChargerName("测试充电桩");
        charger.setChargerType(1);
        charger.setStatus(1); // 空闲
        charger.setRatedPower(new BigDecimal("120.0"));
        
        chargerService.saveCharger(charger);
        assertNotNull(charger.getId());

        // 3. 模拟充电流程：空闲 -> 充电中
        Charger updateCharger = chargerService.getById(charger.getId());
        updateCharger.setStatus(2); // 充电中
        boolean updated = chargerService.updateCharger(updateCharger);
        assertTrue(updated, "充电桩状态应该更新为充电中");

        // 4. 验证状态
        Charger chargingCharger = chargerService.getById(charger.getId());
        assertEquals(2, chargingCharger.getStatus(), "充电桩应该处于充电中状态");

        // 5. 充电完成：充电中 -> 空闲
        chargingCharger.setStatus(1);
        updated = chargerService.updateCharger(chargingCharger);
        assertTrue(updated, "充电桩状态应该恢复为空闲");

        // 6. 最终验证
        Charger finalCharger = chargerService.getById(charger.getId());
        assertEquals(1, finalCharger.getStatus(), "充电桩应该恢复空闲状态");
    }

    @Test
    @DisplayName("测试删除充电站（级联处理）")
    void testDeleteStationWithChargers() {
        // 1. 创建充电站
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("待删除充电站");
        station.setAddress("深圳市南山区测试路321号");
        station.setLatitude(22.5333);
        station.setLongitude(114.1333);
        station.setStatus(0); // 停止运营
        
        stationService.saveStation(station);
        Long stationId = station.getStationId();

        // 2. 添加充电桩
        Charger charger = new Charger();
        charger.setStationId(stationId);
        charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger.setChargerName("待删除充电桩");
        charger.setChargerType(1);
        charger.setStatus(0); // 离线
        charger.setRatedPower(new BigDecimal("60.0"));
        
        chargerService.saveCharger(charger);
        Long chargerId = charger.getId();

        // 3. 首先删除充电桩(实际业务中应先删除充电桩再删除充电站)
        boolean chargerDeleted = chargerService.deleteCharger(chargerId);
        assertTrue(chargerDeleted, "充电桩应该删除成功");

        // 4. 验证充电桩已删除（使用软删除，查询不到）
        Charger deletedCharger = chargerService.getById(chargerId);
        assertNull(deletedCharger, "删除后应该查询不到充电桩");

        // 5. 删除充电站
        boolean stationDeleted = stationService.deleteStation(stationId);
        assertTrue(stationDeleted, "充电站应该删除成功");

        // 6. 验证充电站已删除
        Station deletedStation = stationService.getById(stationId);
        assertNull(deletedStation, "删除后应该查询不到充电站");
    }
}
