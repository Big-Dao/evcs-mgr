package com.evcs.station.controller;

import com.evcs.common.test.base.BaseControllerTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.common.tenant.TenantContext;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IChargerService;
import com.evcs.station.service.IStationService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.annotation.Resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class, com.evcs.station.config.TestConfig.class})
@DisplayName("充电枪口会话 Controller 测试")
@SuppressWarnings("null")
class ChargerConnectorSessionControllerTest extends BaseControllerTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

        private void resetTestContext() {
                TenantContext.setCurrentTenantId(getTestTenantId());
                TenantContext.setCurrentUserId(getTestUserId());
                var authorities = AuthorityUtils.createAuthorityList("ROLE_TEST");
                var authentication = new UsernamePasswordAuthenticationToken("test-user", "N/A", authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
        }

    @Test
    @DisplayName("按枪口开始/结束充电 - 应更新枪口会话字段")
    void testConnectorStartStop_shouldUpdateConnectorSessionFields() throws Exception {
        try {
            // Arrange
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("STATION"));
            station.setStationName("测试充电站");
            station.setAddress("地址");
            station.setStatus(1);
            stationService.saveStation(station);

            Charger charger = new Charger();
            charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
            charger.setChargerName("测试充电桩");
            charger.setStationId(station.getStationId());
            charger.setChargerType(1);
            charger.setRatedPower(new BigDecimal("120.0"));
            charger.setStatus(1);
            charger.setEnabled(1);
            charger.setSupportedProtocols("[\"CLOUD_CHARGE\"]");
            charger.setGunCount(2);
            charger.setGunTypes("[\"CCS\",\"CHAdeMO\"]");
            chargerService.saveCharger(charger);

            Long chargerId = charger.getId();

            // Act - start
            resetTestContext();
            mockMvc.perform(
                            post("/charger/" + chargerId + "/connectors/1/start")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .param("sessionId", "SESSION_100")
                                    .param("userId", String.valueOf(getTestUserId()))
                                    .param("initialEnergy", "0")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // Assert - session should exist
            resetTestContext();
            mockMvc.perform(get("/charger/" + chargerId + "/connectors")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].connectorNo").value(1))
                    .andExpect(jsonPath("$.data[0].currentSessionId").value("SESSION_100"))
                    .andExpect(jsonPath("$.data[0].currentUserId").value(getTestUserId()));

            // Act - stop
            resetTestContext();
            mockMvc.perform(
                            post("/charger/" + chargerId + "/connectors/1/stop")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .param("sessionId", "SESSION_100")
                                    .param("energy", "12.5")
                                    .param("duration", "60")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // Assert - session cleared
            resetTestContext();
            mockMvc.perform(get("/charger/" + chargerId + "/connectors")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].connectorNo").value(1))
                    .andExpect(jsonPath("$.data[0].currentSessionId").doesNotExist());
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
