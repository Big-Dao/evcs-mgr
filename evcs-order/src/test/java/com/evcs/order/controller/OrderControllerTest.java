package com.evcs.order.controller;

import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.service.IChargingOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("订单控制器测试")
class OrderControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private OrderController orderController;

    @Mock
    private IChargingOrderService orderService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    @Test
    @DisplayName("按会话查询 - 存在订单时应返回成功")
    void testBySession_shouldReturnOrder_whenSessionExists() throws Exception {
        ChargingOrder order = new ChargingOrder();
        order.setId(101L);
        order.setSessionId("SESSION_001");
        order.setUserId(9001L);

        when(orderService.getBySessionId(eq("SESSION_001"))).thenReturn(order);

        mockMvc.perform(get("/order/by-session").param("sessionId", "SESSION_001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(101))
            .andExpect(jsonPath("$.data.sessionId").value("SESSION_001"));
    }

    @Test
    @DisplayName("支付回调 - 成功回调应返回 true")
    void testPaymentCallback_shouldReturnTrue_whenSuccess() throws Exception {
        when(orderService.paymentCallback(eq("TRADE_1001"), eq(true))).thenReturn(true);

        mockMvc.perform(post("/order/payment/callback")
                .param("tradeId", "TRADE_1001")
                .param("success", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value(true));
    }
}