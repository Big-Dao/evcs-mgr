package com.evcs.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evcs.order.client.StationDirectoryClient;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.common.tenant.TenantContext;
import com.evcs.order.dto.OrderDTO;
import com.evcs.order.entity.ChargingOrder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 订单表反范式化测试：站点名称/省市与充电桩编码在写入时经 station 内部 API
 * 解析并冗余到订单行，订单查询不再 JOIN station 服务表。
 */
@SpringBootTest(classes = {com.evcs.order.OrderServiceApplication.class,
        com.evcs.order.config.TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("订单站点信息反范式化")
class OrderStationDenormalizationTest extends BaseServiceTest {

    @Resource
    private IChargingOrderService orderService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private StationDirectoryClient stationDirectoryClient;

    @BeforeEach
    void setUpMocks() {
        when(stationDirectoryClient.getStationBrief(11L)).thenReturn(new StationDirectoryClient.StationBrief(
                11L, "反范式测试站", "北京市", "北京市"));
        when(stationDirectoryClient.getChargerCode(21L)).thenReturn("DENORM-CH-1");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("建单 - 应把站点名称/省市与桩编码冗余到订单行")
    void createOrderShouldCarryDenormalizedStationInfo() {
        TenantContext.setCurrentTenantId(1L);
        assertTrue(orderService.createOrderOnStart(11L, 21L, "DENORM-S1", 31L, null),
                "建单应成功");

        ChargingOrder saved = orderService.getBySessionId("DENORM-S1");
        assertNotNull(saved);
        assertEquals("反范式测试站", saved.getStationName());
        assertEquals("北京市", saved.getProvince());
        assertEquals("北京市", saved.getCity());
        assertEquals("DENORM-CH-1", saved.getChargerCode());
    }

    @Test
    @DisplayName("订单列表 - 站点名称来自订单行本身（无 station 服务表 JOIN）")
    void orderListShouldReadDenormalizedColumns() {
        TenantContext.setCurrentTenantId(1L);
        assertTrue(orderService.createOrderOnStart(11L, 21L, "DENORM-S2", 31L, null));

        ChargingOrder query = new ChargingOrder();
        query.setSessionId("DENORM-S2");
        Page<OrderDTO> page = new Page<>(1, 10);
        List<OrderDTO> orders = orderService.getOrderPage(page, query).getRecords();

        assertEquals(1, orders.size());
        assertEquals("反范式测试站", orders.get(0).getStationName());
        assertEquals("DENORM-CH-1", orders.get(0).getChargerCode());
    }

    @Test
    @DisplayName("建单 - 站点服务不可用时应放行（展示字段为空）")
    void createOrderShouldProceedWhenStationUnavailable() {
        TenantContext.setCurrentTenantId(1L);
        assertTrue(orderService.createOrderOnStart(99L, 99L, "DENORM-S3", 31L, null),
                "站点信息解析失败不应阻断建单");

        ChargingOrder saved = orderService.getBySessionId("DENORM-S3");
        assertNotNull(saved);
        org.junit.jupiter.api.Assertions.assertNull(saved.getStationName());
    }
}
