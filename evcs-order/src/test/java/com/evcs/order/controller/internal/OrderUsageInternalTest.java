package com.evcs.order.controller.internal;

import com.evcs.common.test.base.BaseControllerTest;
import com.evcs.common.tenant.TenantContext;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.service.IChargingOrderService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单资源用量内部端点测试。
 *
 * <p>tenant 服务的删除预检需要订单计数，这些数据归属 order 服务；
 * 通过内部端点提供（网关边缘封锁 + 内部令牌），取代跨服务直查数据库。
 */
@SpringBootTest(classes = {com.evcs.order.OrderServiceApplication.class,
        com.evcs.order.config.TestConfig.class})
@TestPropertySource(properties = {
        "evcs.internal.api.enabled=true",
        "evcs.internal.api.token=order-internal-test-token-0123456789"
})
@AutoConfigureMockMvc
@DisplayName("订单资源用量内部端点")
class OrderUsageInternalTest extends BaseControllerTest {

    @Resource
    private IChargingOrderService orderService;

    private void saveOrder(String sessionId, Long tenantId) {
        TenantContext.setCurrentTenantId(tenantId);
        ChargingOrder order = new ChargingOrder();
        order.setSessionId(sessionId);
        order.setStationId(11L);
        order.setChargerId(21L);
        order.setStatus(1);
        order.setAmount(new BigDecimal("1.00"));
        orderService.save(order);
    }

    @Test
    @DisplayName("内部令牌访问 - 应按租户聚合订单计数")
    void shouldAggregateOrderCountsByTenant() throws Exception {
        saveOrder("USAGE-ORD-1", 1L);
        saveOrder("USAGE-ORD-2", 1L);
        saveOrder("USAGE-ORD-3", 2L);

        setUpTenantContext();

        mockMvc.perform(get("/internal/api/v1/order-usage-counts")
                        .header("X-Internal-Token", "order-internal-test-token-0123456789")
                        .param("tenantIds", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].tenantId").value(1))
                .andExpect(jsonPath("$.data[0].orderCount").value(2))
                .andExpect(jsonPath("$.data[1].tenantId").value(2))
                .andExpect(jsonPath("$.data[1].orderCount").value(1));
    }

    @Test
    @DisplayName("缺少内部令牌 - 应拒绝")
    void shouldRejectWithoutInternalToken() throws Exception {
        setUpTenantContext();

        mockMvc.perform(get("/internal/api/v1/order-usage-counts").param("tenantIds", "1"))
                .andExpect(status().isUnauthorized());
    }
}
