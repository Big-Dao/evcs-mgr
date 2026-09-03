package com.evcs.order.controller;

import com.evcs.common.test.base.BaseControllerTest;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.service.IChargingOrderService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单控制器响应契约测试。
 *
 * <p>订单查询接口必须返回 DTO：paymentTradeId（第三方交易号）、version、deleted、审计人字段
 * 属于 Entity 内部结构，不得出现在 API 响应中。
 */
@SpringBootTest(classes = {com.evcs.order.OrderServiceApplication.class,
        com.evcs.order.config.TestConfig.class})
@AutoConfigureMockMvc
@DisplayName("订单控制器响应契约")
class OrderControllerTest extends BaseControllerTest {

    @Resource
    private IChargingOrderService orderService;

    @Test
    @DisplayName("按ID查询订单 - 响应不得包含 Entity 内部字段")
    void getByIdShouldNotExposeInternalFields() throws Exception {
        ChargingOrder order = new ChargingOrder();
        order.setSessionId("DTO-CTRL-S1");
        order.setStationId(11L);
        order.setChargerId(21L);
        order.setStatus(10);
        order.setAmount(new BigDecimal("12.34"));
        order.setPaymentTradeId("internal-trade-no-001");
        assertNotNull(orderService.save(order), "测试订单应保存成功");

        setUpTenantContext();

        mockMvc.perform(get("/order/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value("DTO-CTRL-S1"))
                .andExpect(jsonPath("$.data.amount").value(12.34))
                .andExpect(jsonPath("$.data.paymentTradeId").doesNotExist())
                .andExpect(jsonPath("$.data.version").doesNotExist())
                .andExpect(jsonPath("$.data.deleted").doesNotExist())
                .andExpect(jsonPath("$.data.createBy").doesNotExist())
                .andExpect(jsonPath("$.data.updateBy").doesNotExist());
    }

    @Test
    @DisplayName("按会话查询订单 - 响应不得包含 Entity 内部字段")
    void bySessionShouldNotExposeInternalFields() throws Exception {
        ChargingOrder order = new ChargingOrder();
        order.setSessionId("DTO-CTRL-S2");
        order.setStatus(1);
        order.setPaymentTradeId("internal-trade-no-002");
        assertNotNull(orderService.save(order), "测试订单应保存成功");

        setUpTenantContext();

        mockMvc.perform(get("/order/by-session").param("sessionId", "DTO-CTRL-S2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value("DTO-CTRL-S2"))
                .andExpect(jsonPath("$.data.paymentTradeId").doesNotExist())
                .andExpect(jsonPath("$.data.version").doesNotExist());
    }
}
