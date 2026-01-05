package com.evcs.payment.service;

import com.evcs.payment.client.OrderServiceClient;
import com.evcs.payment.config.OrderSyncConfig;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.mapper.PaymentSyncRecordMapper;
import com.evcs.payment.service.impl.OrderSyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private OrderServiceClient orderServiceClient;

    @Mock
    private OrderSyncConfig orderSyncConfig;

    @Mock
    private PaymentSyncRecordMapper paymentSyncRecordMapper;

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    private OrderSyncServiceImpl orderSyncService;

    @BeforeEach
    void setUp() {
        orderSyncService = new OrderSyncServiceImpl(orderServiceClient, orderSyncConfig, paymentSyncRecordMapper, paymentOrderMapper);
    }

    @Test
    @DisplayName("支付成功同步 - API调用成功")
    void testSyncPaymentSuccess_ApiSuccess() {
        // Given
        PaymentOrder paymentOrder = createTestPaymentOrder();

        when(orderSyncConfig.isDirectApiEnabled()).thenReturn(true);
        when(orderServiceClient.notifyPaymentCallback(any(PaymentOrder.class), eq(true))).thenReturn(true);

        // When
        boolean result = orderSyncService.syncPaymentSuccess(paymentOrder);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("支付成功同步 - API调用失败，降级到消息队列")
    void testSyncPaymentSuccess_ApiFailure_Fallback() {
        // Given
        PaymentOrder paymentOrder = createTestPaymentOrder();

        when(orderSyncConfig.isDirectApiEnabled()).thenReturn(true);
        when(orderServiceClient.notifyPaymentCallback(any(PaymentOrder.class), eq(true)))
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
        verify(orderServiceClient, never()).notifyPaymentCallback(any(PaymentOrder.class), anyBoolean());
    }

    @Test
    @DisplayName("支付失败同步 - API调用成功")
    void testSyncPaymentFailure_ApiSuccess() {
        // Given
        PaymentOrder paymentOrder = createTestPaymentOrder();

        when(orderSyncConfig.isDirectApiEnabled()).thenReturn(true);
        when(orderServiceClient.notifyPaymentCallback(any(PaymentOrder.class), eq(false))).thenReturn(true);

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

        // 模拟本地记录查询返回0
        when(paymentSyncRecordMapper.selectCount(any())).thenReturn(0L);
        
        // 模拟API检查未启用或未查到
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