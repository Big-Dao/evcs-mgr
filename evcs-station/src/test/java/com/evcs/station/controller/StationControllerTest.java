package com.evcs.station.controller;

import com.evcs.common.test.base.BaseControllerTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IStationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import jakarta.annotation.Resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 充电站Controller测试类
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class, com.evcs.station.config.TestConfig.class})
@DisplayName("充电站Controller测试")
@SuppressWarnings("null")
class StationControllerTest extends BaseControllerTest {

    @Resource
    private IStationService stationService;


    @Test
    @DisplayName("查询充电站列表 - 返回成功")
    void testListStations() throws Exception {
        // Arrange - 创建测试数据
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("测试充电站");
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        stationService.saveStation(station);

        // Act & Assert
        mockMvc.perform(get("/station")
                .param("current", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("根据ID查询充电站 - 返回成功")
    void testGetStationById() throws Exception {
        // Arrange
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("测试充电站");
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        stationService.saveStation(station);

        // Act & Assert
        mockMvc.perform(get("/station/" + station.getStationId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stationName").value("测试充电站"));
    }

    @Test
    @DisplayName("创建充电站 - 返回成功")
    void testCreateStation() throws Exception {
        // Arrange
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("新建充电站");
        station.setAddress("新建地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);

        // Act & Assert
        mockMvc.perform(post("/station")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(station)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("更新充电站 - 返回成功")
    void testUpdateStation() throws Exception {
        // Arrange - 先创建
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("原充电站名称");
        station.setAddress("原地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        stationService.saveStation(station);

        // 修改数据
        station.setStationName("更新后的充电站名称");
        station.setAddress("更新后的地址");

        // Act & Assert
        mockMvc.perform(put("/station/" + station.getStationId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(station)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除充电站 - 返回成功")
    void testDeleteStation() throws Exception {
        // Arrange
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("待删除充电站");
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        stationService.saveStation(station);

        // Act & Assert
        mockMvc.perform(delete("/station/" + station.getStationId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
