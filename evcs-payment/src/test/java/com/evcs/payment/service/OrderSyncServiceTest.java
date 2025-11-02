package com.evcs.payment.service;

import com.evcs.payment.config.OrderSyncConfig;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.service.impl.OrderSyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单同步服务测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单同步服务测试")
class OrderSyncServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OrderSyncConfig orderSyncConfig;

    private OrderSyncServiceImpl orderSyncService;

    @BeforeEach
    void setUp() {
        orderSyncService = new OrderSyncServiceImpl(restTemplate, orderSyncConfig);
    }

    @Test
    @DisplayName("支付成功同步 - API调用成功")
    void testSyncPaymentSuccess_ApiSuccess() {
        // Given
        PaymentOrder paymentOrder = createTestPaymentOrder();

        when(orderSyncConfig.isDirectApiEnabled()).thenReturn(true);
        when(orderSyncConfig.getOrderServiceUrl()).thenReturn("http://localhost:8083/api");

        ResponseEntity<Map> successResponse = ResponseEntity.ok(Map.of("success", true));
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(successResponse);

        // When
        boolean result = orderSyncService.syncPaymentSuccess(paymentOrder);

        // Then
        assertTrue(result);

        // 验证API调用
        verify(restTemplate).postForEntity(
                eq("http://localhost:8083/api/payments/callback"),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("支付成功同步 - API调用失败，降级到消息队列")
    void testSyncPaymentSuccess_ApiFailure_Fallback() {
        // Given
        PaymentOrder paymentOrder = createTestPaymentOrder();

        when(orderSyncConfig.isDirectApiEnabled()).thenReturn(true);
        when(orderSyncConfig.getOrderServiceUrl()).thenReturn("http://localhost:8083/api");

        // 模拟API调用失败
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("API调用失败"));

        // When
        boolean result = orderSyncService.syncPaymentSuccess(paymentOrder);

        // Then
        assertTrue(result); // 降级处理应该返回true
    }

    @Test
    @DisplayName("支付成功同步 - 直接API调用禁用")
    void testSyncPaymentSuccess_ApiDisabled() {
        // Given
        PaymentOrder paymentOrder = createTestPaymentOrder();

        when(orderSyncConfig.isDirectApiEnabled()).thenReturn(false);

        // When
        boolean result = orderSyncService.syncPaymentSuccess(paymentOrder);

        // Then
        assertTrue(result); // 应该使用消息队列
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    @DisplayName("支付失败同步 - API调用成功")
    void testSyncPaymentFailure_ApiSuccess() {
        // Given
        PaymentOrder paymentOrder = createTestPaymentOrder();

        when(orderSyncConfig.isDirectApiEnabled()).thenReturn(true);
        when(orderSyncConfig.getOrderServiceUrl()).thenReturn("http://localhost:8083/api");

        ResponseEntity<Map> successResponse = ResponseEntity.ok(Map.of("success", true));
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(successResponse);

        // When
        boolean result = orderSyncService.syncPaymentFailure(paymentOrder, "余额不足");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("订单同步检查 - 未同步")
    void testIsOrderSynced_NotSynced() {
        // Given
        Long paymentOrderId = 1L;

        when(orderSyncConfig.isDirectApiEnabled()).thenReturn(false);

        // When
        boolean result = orderSyncService.isOrderSynced(paymentOrderId);

        // Then
        assertFalse(result);
    }

    /**
     * 创建测试用的支付订单
     */
    private PaymentOrder createTestPaymentOrder() {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(1L);
        paymentOrder.setOrderId(100L);
        paymentOrder.setTradeNo("ALIPAY_TRADE_001");
        paymentOrder.setAmount(new BigDecimal("100.00"));
        paymentOrder.setPaymentMethod("ALIPAY_APP");
        paymentOrder.setStatusEnum(PaymentStatus.SUCCESS);
        paymentOrder.setTenantId(1L);
        paymentOrder.setCreateBy(1L);
        paymentOrder.setPaidTime(LocalDateTime.now());
        paymentOrder.setCreateTime(LocalDateTime.now());
        paymentOrder.setUpdateBy(1L);
        return paymentOrder;
    }
}