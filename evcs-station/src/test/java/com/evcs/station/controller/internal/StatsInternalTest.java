package com.evcs.station.controller.internal;

import com.evcs.common.test.base.BaseControllerTest;
import com.evcs.common.tenant.TenantContext;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IChargerService;
import com.evcs.station.service.IStationService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 站点/充电桩统计内部端点测试。
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class,
        com.evcs.station.config.TestConfig.class})
@TestPropertySource(properties = {
        "evcs.internal.api.enabled=true",
        "evcs.internal.api.token=station-internal-test-token-0123456789"
})
@DisplayName("站点统计内部端点")
class StatsInternalTest extends BaseControllerTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    private void saveStation(String code, Long tenantId) {
        TenantContext.setCurrentTenantId(tenantId);
        Station station = new Station();
        station.setStationCode(code);
        station.setStationName("统计站-" + code);
        station.setAddress("测试地址");
        station.setLatitude(39.9);
        station.setLongitude(116.4);
        station.setStatus(1);
        stationService.saveStation(station);
    }

    private void saveCharger(String code, Long tenantId, Integer status) {
        TenantContext.setCurrentTenantId(tenantId);
        Charger charger = new Charger();
        charger.setChargerCode(code);
        charger.setChargerName("统计桩-" + code);
        charger.setStationId(11L);
        charger.setStatus(status);
        chargerService.save(charger);
    }

    @Test
    @DisplayName("站点名称批量查询 - 应返回指定租户的站点")
    void shouldReturnStationNames() throws Exception {
        saveStation("STATS-ST-1", 1L);
        setUpTenantContext();

        mockMvc.perform(get("/internal/api/v1/stats/stations/names")
                        .header("X-Internal-Token", "station-internal-test-token-0123456789")
                        .param("tenantIds", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.stationName == '统计站-STATS-ST-1')]").exists());
    }

    @Test
    @DisplayName("桩状态统计 - 应按语义聚合在线/离线/充电中/空闲")
    void shouldAggregateChargerStatusStats() throws Exception {
        saveCharger("STATS-CH-1", 1L, 1); // 空闲：online + idle
        saveCharger("STATS-CH-2", 1L, 2); // 充电中：online + charging
        saveCharger("STATS-CH-3", 1L, 0); // 离线
        setUpTenantContext();

        mockMvc.perform(get("/internal/api/v1/stats/chargers/status-stats")
                        .header("X-Internal-Token", "station-internal-test-token-0123456789")
                        .param("tenantIds", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.online").value(2))
                .andExpect(jsonPath("$.data.offline").value(1))
                .andExpect(jsonPath("$.data.charging").value(1))
                .andExpect(jsonPath("$.data.idle").value(1));
    }
}
