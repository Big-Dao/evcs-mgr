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
 * 租户资源用量内部端点测试。
 *
 * <p>tenant 服务的配额校验需要站点/充电桩计数，这些数据归属 station 服务；
 * 通过内部端点提供（网关边缘封锁 + 内部令牌），取代跨服务直查数据库。
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class,
        com.evcs.station.config.TestConfig.class})
@TestPropertySource(properties = {
        "evcs.internal.api.enabled=true",
        "evcs.internal.api.token=station-internal-test-token-0123456789"
})
@DisplayName("租户资源用量内部端点")
class UsageCountInternalTest extends BaseControllerTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    private void saveStation(String code, Long tenantId) {
        TenantContext.setCurrentTenantId(tenantId);
        Station station = new Station();
        station.setStationCode(code);
        station.setStationName("用量统计-" + code);
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        stationService.saveStation(station);
    }

    private void saveCharger(String code, Long tenantId, Long stationId) {
        TenantContext.setCurrentTenantId(tenantId);
        Charger charger = new Charger();
        charger.setChargerCode(code);
        charger.setChargerName("用量桩-" + code);
        charger.setStationId(stationId);
        charger.setStatus(1);
        chargerService.save(charger);
    }

    @Test
    @DisplayName("内部令牌访问 - 应按租户聚合站点与充电桩计数")
    void shouldAggregateUsageCountsByTenant() throws Exception {
        saveStation("USAGE-ST-1", 1L);
        saveStation("USAGE-ST-2", 1L);
        saveCharger("USAGE-CH-1", 1L, 11L);

        saveStation("USAGE-ST-3", 2L);
        saveCharger("USAGE-CH-2", 2L, 12L);
        saveCharger("USAGE-CH-3", 2L, 12L);

        setUpTenantContext();

        mockMvc.perform(get("/internal/api/v1/usage-counts")
                        .header("X-Internal-Token", "station-internal-test-token-0123456789")
                        .param("tenantIds", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].tenantId").value(1))
                .andExpect(jsonPath("$.data[0].stationCount").value(2))
                .andExpect(jsonPath("$.data[0].chargerCount").value(1))
                .andExpect(jsonPath("$.data[1].tenantId").value(2))
                .andExpect(jsonPath("$.data[1].stationCount").value(1))
                .andExpect(jsonPath("$.data[1].chargerCount").value(2));
    }

    @Test
    @DisplayName("缺少内部令牌 - 应拒绝")
    void shouldRejectWithoutInternalToken() throws Exception {
        setUpTenantContext();

        mockMvc.perform(get("/internal/api/v1/usage-counts").param("tenantIds", "1"))
                .andExpect(status().isUnauthorized());
    }
}
