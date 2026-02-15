package com.evcs.payment.controller;

import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.service.IPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("支付控制器测试")
class PaymentControllerTest {

    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @InjectMocks
    private PaymentController paymentController;

    @Mock
    private IPaymentService paymentService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("创建支付订单 - 应返回成功结果")
    void testCreatePayment_shouldReturnSuccess() throws Exception {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(1L);
        response.setTradeNo("TRADE_10001");
        response.setAmount(new BigDecimal("12.34"));
        response.setStatus(PaymentStatus.PENDING);

        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(response);

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1001L);
        request.setAmount(new BigDecimal("12.34"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_APP);
        request.setUserId(2001L);

        mockMvc.perform(post("/payment/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.tradeNo").value("TRADE_10001"));
    }

    @Test
    @DisplayName("查询支付状态 - 不存在时应返回失败")
    void testQueryPayment_shouldReturnFailure_whenNotFound() throws Exception {
        when(paymentService.queryPayment("TRADE_NOT_FOUND")).thenReturn(null);

        mockMvc.perform(get("/payment/query/TRADE_NOT_FOUND"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("支付订单不存在"));
    }

    @Test
    @DisplayName("退款 - 应返回成功结果")
    void testRefund_shouldReturnSuccess() throws Exception {
        RefundResponse response = new RefundResponse();
        response.setRefundNo("REFUND_9001");
        response.setRefundAmount(new BigDecimal("8.88"));
        response.setRefundStatus("SUCCESS");

        when(paymentService.refund(any(RefundRequest.class))).thenReturn(response);

        RefundRequest request = new RefundRequest();
        request.setPaymentId(10L);
        request.setRefundAmount(new BigDecimal("8.88"));
        request.setRefundReason("测试退款");

        mockMvc.perform(post("/payment/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.refundNo").value("REFUND_9001"));
    }
}
