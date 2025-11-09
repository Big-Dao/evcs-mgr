package com.evcs.payment.service;

import com.evcs.payment.config.TestConfig;
import com.evcs.payment.dto.ReconciliationRequest;
import com.evcs.payment.dto.ReconciliationResult;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.service.reconciliation.ReconciliationStatementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 对账服务集成测试
 */
@SpringBootTest(classes = {com.evcs.payment.PaymentServiceApplication.class, com.evcs.payment.config.TestConfig.class},
    properties = {"spring.autoconfigure.exclude=com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestConfig.class})
@DisplayName("对账服务集成测试")
class ReconciliationServiceIntegrationTest {

    @MockBean
    private PaymentOrderMapper paymentOrderMapper;

    @MockBean
    private ReconciliationStatementService reconciliationStatementService;

    @MockBean
    private IReconciliationService reconciliationService;

    @Test
    @DisplayName("测试支付宝对账功能")
    void testAlipayReconciliation() {
        // Arrange
        LocalDate reconciliationDate = LocalDate.now().minusDays(1);
        ReconciliationRequest request = new ReconciliationRequest();
        request.setChannel("alipay");
        request.setReconciliationDate(reconciliationDate);

        // 模拟系统中的支付订单
        List<PaymentOrder> systemOrders = new ArrayList<>();
        PaymentOrder order1 = createPaymentOrder(1L, "ALI2024110200001", new BigDecimal("100.00"), PaymentStatus.SUCCESS);
        PaymentOrder order2 = createPaymentOrder(2L, "ALI2024110200002", new BigDecimal("200.00"), PaymentStatus.SUCCESS);
        systemOrders.add(order1);
        systemOrders.add(order2);

        when(paymentOrderMapper.selectList(any())).thenReturn(systemOrders);

        // Act
        ReconciliationResult result = reconciliationService.reconcile(request);

        // Assert
        assertNotNull(result);

        verify(paymentOrderMapper, atLeastOnce()).selectList(any());
    }

    @Test
    @DisplayName("测试微信对账功能")
    void testWechatReconciliation() {
        // Arrange
        LocalDate reconciliationDate = LocalDate.now().minusDays(1);
        ReconciliationRequest request = new ReconciliationRequest();
        request.setChannel("wechat");
        request.setReconciliationDate(reconciliationDate);

        // 模拟系统中的支付订单
        List<PaymentOrder> systemOrders = new ArrayList<>();
        PaymentOrder order = createPaymentOrder(3L, "WX2024110200001", new BigDecimal("150.00"), PaymentStatus.SUCCESS);
        systemOrders.add(order);

        when(paymentOrderMapper.selectList(any())).thenReturn(systemOrders);

        // Act
        ReconciliationResult result = reconciliationService.reconcile(request);

        // Assert
        assertNotNull(result);

        verify(paymentOrderMapper, atLeastOnce()).selectList(any());
    }

    @Test
    @DisplayName("测试无订单的对账")
    void testReconciliationWithNoOrders() {
        // Arrange
        LocalDate reconciliationDate = LocalDate.now().minusDays(1);
        ReconciliationRequest request = new ReconciliationRequest();
        request.setChannel("alipay");
        request.setReconciliationDate(reconciliationDate);

        when(paymentOrderMapper.selectList(any())).thenReturn(new ArrayList<>());

        // Act
        ReconciliationResult result = reconciliationService.reconcile(request);

        // Assert
        assertNotNull(result);

        verify(paymentOrderMapper, atLeastOnce()).selectList(any());
    }

    @Test
    @DisplayName("测试对账异常处理")
    void testReconciliationWithException() {
        // Arrange
        LocalDate reconciliationDate = LocalDate.now().minusDays(1);
        ReconciliationRequest request = new ReconciliationRequest();
        request.setChannel("alipay");
        request.setReconciliationDate(reconciliationDate);

        // 模拟数据库异常
        when(paymentOrderMapper.selectList(any())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            reconciliationService.reconcile(request);
        });

        verify(paymentOrderMapper, atLeastOnce()).selectList(any());
    }

    /**
     * 创建支付订单
     */
    private PaymentOrder createPaymentOrder(Long id, String tradeNo, BigDecimal amount, PaymentStatus status) {
        PaymentOrder order = new PaymentOrder();
        order.setId(id);
        order.setTradeNo(tradeNo);
        order.setAmount(amount);
        order.setStatus(status.getCode());
        order.setOrderId(id + 1000L);
        order.setPaymentMethod(status == PaymentStatus.SUCCESS ? "ALIPAY_QR" : "WECHAT_NATIVE");
        order.setDescription("测试订单");
        order.setTenantId(1L);
        return order;
    }
}