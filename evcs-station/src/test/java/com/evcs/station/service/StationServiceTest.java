package com.evcs.station.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.tenant.TenantContext;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IStationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 充电站服务测试类
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class},
                webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@AutoConfigureWebMvc
@Transactional
class StationServiceTest {

    @Resource
    private IStationService stationService;

    @BeforeEach
    void setUp() {
        // 设置测试租户上下文
        TenantContext.setCurrentTenantId(1L);
        TenantContext.setCurrentUserId(1L);
    }

    @Test
    void testSaveStation() {
        // 准备测试数据
        Station station = new Station();
        station.setStationCode("TEST001");
        station.setStationName("测试充电站");
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        // station.setOperatorName("测试运营商");
        station.setStatus(1);

        // 执行测试
        boolean result = stationService.saveStation(station);

        // 验证结果
        assertTrue(result);
        assertNotNull(station.getStationId());
        assertEquals(1L, station.getTenantId());
    }

    @Test
    void testQueryStationPage() {
        // 准备分页参数
        Page<Station> page = new Page<>(1, 10);
        Station queryParam = new Station();
        queryParam.setStatus(1);

        // 执行查询
        IPage<Station> result = stationService.queryStationPage(page, queryParam);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getSize() > 0);
    }

    @Test
    void testGetNearbyStations() {
        // 准备位置参数
        Double latitude = 39.9087;
        Double longitude = 116.4089;
        Double radius = 10.0;
        Integer limit = 10;

        // 执行查询
        List<Station> stations = stationService.getNearbyStations(latitude, longitude, radius, limit);

        // 验证结果
        assertNotNull(stations);
        assertTrue(stations.size() <= limit);
    }

    @Test
    void testCheckStationCodeExists() {
        // 创建测试充电站
        Station station = new Station();
        station.setStationCode("TEST002");
        station.setStationName("测试充电站2");
        station.setAddress("测试地址2");
        stationService.saveStation(station);

        // 测试编码存在
        boolean exists = stationService.checkStationCodeExists("TEST002", null);
        assertTrue(exists);

        // 测试编码不存在
        boolean notExists = stationService.checkStationCodeExists("NOTEXIST", null);
        assertFalse(notExists);

        // 测试排除自己
        boolean excludeSelf = stationService.checkStationCodeExists("TEST002", station.getStationId());
        assertFalse(excludeSelf);
    }

    @Test
    void testUpdateStation() {
        // 创建测试充电站
        Station station = new Station();
        station.setStationCode("TEST003");
        station.setStationName("测试充电站3");
        station.setAddress("测试地址3");
        stationService.saveStation(station);

        // 更新充电站
        station.setStationName("更新后的充电站名称");
        station.setAddress("更新后的地址");
        boolean result = stationService.updateStation(station);

        // 验证结果
        assertTrue(result);

        // 查询验证
        Station updated = stationService.getById(station.getStationId());
        assertEquals("更新后的充电站名称", updated.getStationName());
        assertEquals("更新后的地址", updated.getAddress());
    }

    @Test
    void testChangeStatus() {
        // 创建测试充电站
        Station station = new Station();
        station.setStationCode("TEST004");
        station.setStationName("测试充电站4");
        station.setAddress("测试地址4");
        stationService.saveStation(station);

        // 停用充电站
        boolean result = stationService.changeStatus(station.getStationId(), 0);
        assertTrue(result);

        // 验证状态
        Station updated = stationService.getById(station.getStationId());
        assertEquals(0, updated.getStatus());

        // 启用充电站
        result = stationService.changeStatus(station.getStationId(), 1);
        assertTrue(result);

        // 验证状态
        updated = stationService.getById(station.getStationId());
        assertEquals(1, updated.getStatus());
    }

    @Test
    void testDeleteStation() {
        // 创建测试充电站
        Station station = new Station();
        station.setStationCode("TEST005");
        station.setStationName("测试充电站5");
        station.setAddress("测试地址5");
        stationService.saveStation(station);

        Long stationId = station.getStationId();

        // 删除充电站
        boolean result = stationService.deleteStation(stationId);
        assertTrue(result);

        // 验证删除（逻辑删除）
        Station deleted = stationService.getById(stationId);
        assertNull(deleted); // MyBatis Plus逻辑删除后查询返回null
    }
}