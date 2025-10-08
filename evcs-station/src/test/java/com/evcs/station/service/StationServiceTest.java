package com.evcs.station.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IStationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;

import jakarta.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 充电站服务测试类
 * 使用新的测试框架基类
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class},
                webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureWebMvc
@DisplayName("充电站服务测试")
class StationServiceTest extends BaseServiceTest {

    @Resource
    private IStationService stationService;

    @Test
    @DisplayName("保存充电站 - 正常流程")
    void testSaveStation() {
        // Arrange - 准备测试数据
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("测试充电站");
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);

        // Act - 执行测试
        boolean result = stationService.saveStation(station);

        // Assert - 验证结果
        assertTrue(result);
        assertNotNull(station.getStationId());
        assertEquals(DEFAULT_TENANT_ID, station.getTenantId());
    }

    @Test
    @DisplayName("查询充电站列表 - 分页查询")
    void testQueryStationPage() {
        // Arrange - 准备分页参数
        Page<Station> page = new Page<>(1, 10);
        Station queryParam = new Station();
        queryParam.setStatus(1);

        // Act - 执行查询
        IPage<Station> result = stationService.queryStationPage(page, queryParam);

        // Assert - 验证结果
        assertNotNull(result);
        assertTrue(result.getSize() > 0);
    }

    @Test
    @DisplayName("查询附近充电站 - 地理位置查询")
    void testGetNearbyStations() {
        // Arrange - 准备位置参数
        Double latitude = 39.9087;
        Double longitude = 116.4089;
        Double radius = 10.0;
        Integer limit = 10;

        // Act - 执行查询
        List<Station> stations = stationService.getNearbyStations(latitude, longitude, radius, limit);

        // Assert - 验证结果
        assertNotNull(stations);
        assertTrue(stations.size() <= limit);
    }

    @Test
    @DisplayName("检查充电站编码是否存在")
    void testCheckStationCodeExists() {
        // Arrange - 创建测试充电站
        String testCode = TestDataFactory.generateCode("STATION");
        Station station = new Station();
        station.setStationCode(testCode);
        station.setStationName("测试充电站2");
        station.setAddress("测试地址2");
        stationService.saveStation(station);

        // Act & Assert - 测试编码存在
        boolean exists = stationService.checkStationCodeExists(testCode, null);
        assertTrue(exists, "编码应该存在");

        // Act & Assert - 测试编码不存在
        boolean notExists = stationService.checkStationCodeExists("NOTEXIST", null);
        assertFalse(notExists, "编码应该不存在");

        // Act & Assert - 测试排除自己
        boolean excludeSelf = stationService.checkStationCodeExists(testCode, station.getStationId());
        assertFalse(excludeSelf, "排除自己后应该不存在");
    }

    @Test
    @DisplayName("更新充电站 - 正常流程")
    void testUpdateStation() {
        // Arrange - 创建测试充电站
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("测试充电站3");
        station.setAddress("测试地址3");
        stationService.saveStation(station);

        // Act - 更新充电站
        station.setStationName("更新后的充电站名称");
        station.setAddress("更新后的地址");
        boolean result = stationService.updateStation(station);

        // Assert - 验证结果
        assertTrue(result);

        // 查询验证
        Station updated = stationService.getById(station.getStationId());
        assertEquals("更新后的充电站名称", updated.getStationName());
        assertEquals("更新后的地址", updated.getAddress());
    }

    @Test
    @DisplayName("修改充电站状态 - 启用和停用")
    void testChangeStatus() {
        // Arrange - 创建测试充电站
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("测试充电站4");
        station.setAddress("测试地址4");
        stationService.saveStation(station);

        // Act - 停用充电站
        boolean result = stationService.changeStatus(station.getStationId(), 0);

        // Assert - 验证状态
        assertTrue(result);
        Station updated = stationService.getById(station.getStationId());
        assertEquals(0, updated.getStatus(), "状态应该为停用");

        // Act - 启用充电站
        result = stationService.changeStatus(station.getStationId(), 1);

        // Assert - 验证状态
        assertTrue(result);
        updated = stationService.getById(station.getStationId());
        assertEquals(1, updated.getStatus(), "状态应该为启用");
    }

    @Test
    @DisplayName("删除充电站 - 逻辑删除")
    void testDeleteStation() {
        // Arrange - 创建测试充电站
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("测试充电站5");
        station.setAddress("测试地址5");
        stationService.saveStation(station);
        Long stationId = station.getStationId();

        // Act - 删除充电站
        boolean result = stationService.deleteStation(stationId);

        // Assert - 验证删除（逻辑删除）
        assertTrue(result);
        Station deleted = stationService.getById(stationId);
        assertNull(deleted, "逻辑删除后查询应返回null");
    }
}