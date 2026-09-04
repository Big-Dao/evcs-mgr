package com.evcs.station.controller;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 站点/充电桩输入契约测试（批量赋值防护）。
 *
 * <p>创建/更新端点必须使用输入 DTO：租户归属、逻辑删除、审计人、
 * 设备运行时字段（心跳/会话/累计量/功率温度等）不得由请求体注入。
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class,
        com.evcs.station.config.TestConfig.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@DisplayName("站点/充电桩输入契约")
class StationChargerInputContractTest extends BaseControllerTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    @Test
    @DisplayName("创建站点 - 请求体不得注入租户/删除/审计字段")
    void createStationShouldIgnoreInternalFields() throws Exception {
        String payload = """
            {
                "stationCode": "INPUT-CTRACT-ST",
                "stationName": "输入契约站",
                "address": "测试地址",
                "latitude": 39.9,
                "longitude": 116.4,
                "status": 1,
                "tenantId": 99,
                "deleted": 1,
                "createBy": 777,
                "updateBy": 777
            }
            """;

        mockMvc.perform(post("/station")
                        .header("X-Tenant-Id", "1")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk());

        TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID); // 拦截器请求后清空了上下文，查询前重设
        Station saved = stationService.lambdaQuery()
                .eq(Station::getStationCode, "INPUT-CTRACT-ST")
                .one();
        org.junit.jupiter.api.Assertions.assertNotNull(saved);
        org.junit.jupiter.api.Assertions.assertEquals(DEFAULT_TENANT_ID, saved.getTenantId(),
                "租户归属必须来自上下文，不得由请求体注入");
        org.junit.jupiter.api.Assertions.assertNotEquals(Integer.valueOf(1), saved.getDeleted(),
                "deleted 不得由请求体注入");
        org.junit.jupiter.api.Assertions.assertNotEquals(777L, saved.getCreateBy(),
                "createBy 不得由请求体注入");
    }

    @Test
    @DisplayName("创建充电桩 - 设备运行时字段不得由请求体注入")
    void createChargerShouldIgnoreRuntimeFields() throws Exception {
        TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
        Station host = new Station();
        host.setStationCode("INPUT-HOST-ST");
        host.setStationName("宿主站");
        host.setAddress("测试地址");
        host.setLatitude(39.9);
        host.setLongitude(116.4);
        host.setStatus(1);
        stationService.saveStation(host);
        long hostStationId = host.getStationId();
        TenantContext.clear();

        String payload = """
            {
                "chargerCode": "INPUT-CTRACT-CH",
                "chargerName": "输入契约桩",
                "stationId": %d,
                "status": 1,
                "lastHeartbeat": "2020-01-01T00:00:00",
                "currentSessionId": "hijacked-session",
                "totalChargingSessions": 9999,
                "totalChargingEnergy": 8888,
                "temperature": 99
            }
            """.formatted(hostStationId);

        mockMvc.perform(post("/charger")
                        .header("X-Tenant-Id", "1")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk());

        TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID); // 拦截器请求后清空了上下文，查询前重设
        Charger saved = chargerService.lambdaQuery()
                .eq(Charger::getChargerCode, "INPUT-CTRACT-CH")
                .one();
        org.junit.jupiter.api.Assertions.assertNotNull(saved);
        assertNull(saved.getLastHeartbeat(), "运行时心跳不得由请求体注入");
        assertNull(saved.getCurrentSessionId(), "运行时会话不得由请求体注入");
        org.junit.jupiter.api.Assertions.assertNotEquals(9999L,
                saved.getTotalChargingSessions() == null ? 0 : saved.getTotalChargingSessions(),
                "累计会话数不得由请求体注入");
        assertNull(saved.getTemperature(), "温度等实时量测不得由请求体注入");
        assertEquals("INPUT-CTRACT-CH", saved.getChargerCode());
    }
}
