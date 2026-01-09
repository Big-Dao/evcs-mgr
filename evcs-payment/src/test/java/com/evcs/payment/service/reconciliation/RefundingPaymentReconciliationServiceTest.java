package com.evcs.payment.service.reconciliation;

import com.evcs.common.tenant.TenantContext;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.payment.PaymentServiceApplication;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.service.IPaymentService;
import com.evcs.payment.service.channel.WechatPayChannelService;
import com.evcs.payment.service.message.PaymentMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes = PaymentServiceApplication.class,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
        "evcs.payment.refunding-reconcile.min-age-minutes=0",
        "evcs.payment.refunding-reconcile.batch-size-per-tenant=50",
        "evcs.payment.refunding-reconcile.max-tenants-per-run=10"
    }
)
@ActiveProfiles("test")
@DisplayName("REFUNDING订单轮询补偿服务测试")
class RefundingPaymentReconciliationServiceTest extends BaseServiceTest {

    @Resource
    private IPaymentService paymentService;

    @Resource
    private RefundingPaymentReconciliationService reconciliationService;

    @MockBean
    private WechatPayChannelService wechatPayChannelService;

    @MockBean
    private PaymentMessageService paymentMessageService;

    @Test
    @DisplayName("轮询补偿 - 跨租户扫描REFUNDING订单并在渠道返回SUCCESS时应落库为REFUNDED")
    void testReconcileOnce_shouldFinalizeRefunded_whenChannelReturnsSuccess() {
        // Arrange
        Long tenant1 = 3001L;
        Long tenant2 = 3002L;

        doNothing().when(paymentMessageService).sendRefundSuccessMessage(any());

        when(wechatPayChannelService.queryRefund(eq("WXPR301_1234")))
            .thenReturn(buildRefundQueryResponse("SUCCESS", new BigDecimal("12.34")));
        when(wechatPayChannelService.queryRefund(eq("WXPR302_5678")))
            .thenReturn(buildRefundQueryResponse("SUCCESS", new BigDecimal("56.78")));

        Long paymentId1;
        Long paymentId2;

        try {
            TenantContext.setTenantId(tenant1);
            TenantContext.setUserId(1L);

            PaymentOrder order1 = new PaymentOrder();
            order1.setOrderId(301L);
            order1.setTradeNo("WXP301_aaaa");
            order1.setPaymentMethod(PaymentMethod.WECHAT_JSAPI.getCode());
            order1.setAmount(new BigDecimal("12.34"));
            order1.setStatusEnum(PaymentStatus.REFUNDING);
            order1.setRefundRequestNo("WXPR301_1234");
            order1.setRefundRequestAmount(new BigDecimal("12.34"));
            order1.setRefundRequestTime(LocalDateTime.now().minusMinutes(10));
            order1.setDescription("test");
            order1.setCreateTime(LocalDateTime.now().minusMinutes(10));
            assertTrue(paymentService.save(order1), "应能保存测试订单1");
            paymentId1 = order1.getId();

            TenantContext.setTenantId(tenant2);
            TenantContext.setUserId(1L);

            PaymentOrder order2 = new PaymentOrder();
            order2.setOrderId(302L);
            order2.setTradeNo("WXP302_bbbb");
            order2.setPaymentMethod(PaymentMethod.WECHAT_JSAPI.getCode());
            order2.setAmount(new BigDecimal("56.78"));
            order2.setStatusEnum(PaymentStatus.REFUNDING);
            order2.setRefundRequestNo("WXPR302_5678");
            order2.setRefundRequestAmount(new BigDecimal("56.78"));
            order2.setRefundRequestTime(LocalDateTime.now().minusMinutes(10));
            order2.setDescription("test");
            order2.setCreateTime(LocalDateTime.now().minusMinutes(10));
            assertTrue(paymentService.save(order2), "应能保存测试订单2");
            paymentId2 = order2.getId();
        } finally {
            TenantContext.clear();
        }

        // Act
        RefundingPaymentReconciliationService.RefundingPaymentReconciliationResult result =
            reconciliationService.reconcileOnce();

        // Assert
        assertNotNull(result, "应返回轮询结果");
        assertEquals(2, result.getTenantCount(), "应扫描到2个租户");
        assertEquals(2, result.getScannedOrderCount(), "应扫描到2笔订单");
        assertEquals(2, result.getFinalizedOrderCount(), "应将2笔订单最终态化");
        assertEquals(0, result.getErrorCount(), "不应有错误");

        try {
            TenantContext.setTenantId(tenant1);
            PaymentOrder updated1 = paymentService.getById(paymentId1);
            assertNotNull(updated1, "应能查询到更新后的订单1");
            assertEquals(PaymentStatus.REFUNDED, updated1.getStatusEnum(), "订单1应更新为REFUNDED");
            assertEquals(new BigDecimal("12.34"), updated1.getRefundAmount(), "订单1退款金额应正确");
            assertNull(updated1.getRefundRequestNo(), "订单1 refundRequestNo 应清理");

            TenantContext.setTenantId(tenant2);
            PaymentOrder updated2 = paymentService.getById(paymentId2);
            assertNotNull(updated2, "应能查询到更新后的订单2");
            assertEquals(PaymentStatus.REFUNDED, updated2.getStatusEnum(), "订单2应更新为REFUNDED");
            assertEquals(new BigDecimal("56.78"), updated2.getRefundAmount(), "订单2退款金额应正确");
            assertNull(updated2.getRefundRequestNo(), "订单2 refundRequestNo 应清理");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("轮询补偿 - 渠道返回PROCESSING时不应收敛订单状态")
    void testReconcileOnce_shouldNoop_whenChannelReturnsProcessing() {
        // Arrange
        Long tenantId = 3101L;

        when(wechatPayChannelService.queryRefund(eq("WXPR3101_0100")))
            .thenReturn(buildRefundQueryResponse("PROCESSING", new BigDecimal("1.00")));

        Long paymentId;
        try {
            TenantContext.setTenantId(tenantId);
            TenantContext.setUserId(1L);

            PaymentOrder order = new PaymentOrder();
            order.setOrderId(3101L);
            order.setTradeNo("WXP3101_cccc");
            order.setPaymentMethod(PaymentMethod.WECHAT_JSAPI.getCode());
            order.setAmount(new BigDecimal("9.99"));
            order.setStatusEnum(PaymentStatus.REFUNDING);
            order.setRefundRequestNo("WXPR3101_0100");
            order.setRefundRequestAmount(new BigDecimal("1.00"));
            order.setRefundRequestTime(LocalDateTime.now().minusMinutes(10));
            order.setDescription("test");
            order.setCreateTime(LocalDateTime.now().minusMinutes(10));
            assertTrue(paymentService.save(order), "应能保存测试订单");
            paymentId = order.getId();
        } finally {
            TenantContext.clear();
        }

        // Act
        RefundingPaymentReconciliationService.RefundingPaymentReconciliationResult result =
            reconciliationService.reconcileOnce();

        // Assert
        assertNotNull(result, "应返回轮询结果");
        assertEquals(1, result.getTenantCount(), "应扫描到1个租户");
        assertEquals(1, result.getScannedOrderCount(), "应扫描到1笔订单");
        assertEquals(0, result.getFinalizedOrderCount(), "PROCESSING不应计为最终态收敛");

        try {
            TenantContext.setTenantId(tenantId);
            PaymentOrder updated = paymentService.getById(paymentId);
            assertNotNull(updated, "应能查询到更新后的订单");
            assertEquals(PaymentStatus.REFUNDING, updated.getStatusEnum(), "订单应保持REFUNDING");
            assertEquals("WXPR3101_0100", updated.getRefundRequestNo(), "refundRequestNo 应保留");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("轮询补偿 - 渠道返回CLOSED时应回滚订单状态并清理退款请求字段")
    void testReconcileOnce_shouldRollbackToSuccess_whenChannelReturnsClosed() {
        // Arrange
        Long tenantId = 3201L;

        when(wechatPayChannelService.queryRefund(eq("WXPR3201_0500")))
            .thenReturn(buildRefundQueryResponse("CLOSED", new BigDecimal("5.00")));

        Long paymentId;
        try {
            TenantContext.setTenantId(tenantId);
            TenantContext.setUserId(1L);

            PaymentOrder order = new PaymentOrder();
            order.setOrderId(3201L);
            order.setTradeNo("WXP3201_dddd");
            order.setPaymentMethod(PaymentMethod.WECHAT_JSAPI.getCode());
            order.setAmount(new BigDecimal("10.00"));
            order.setStatusEnum(PaymentStatus.REFUNDING);
            order.setRefundRequestNo("WXPR3201_0500");
            order.setRefundRequestAmount(new BigDecimal("5.00"));
            order.setRefundRequestTime(LocalDateTime.now().minusMinutes(10));
            order.setDescription("test");
            order.setCreateTime(LocalDateTime.now().minusMinutes(10));
            assertTrue(paymentService.save(order), "应能保存测试订单");
            paymentId = order.getId();
        } finally {
            TenantContext.clear();
        }

        // Act
        RefundingPaymentReconciliationService.RefundingPaymentReconciliationResult result =
            reconciliationService.reconcileOnce();

        // Assert
        assertNotNull(result, "应返回轮询结果");
        assertEquals(1, result.getTenantCount(), "应扫描到1个租户");
        assertEquals(1, result.getScannedOrderCount(), "应扫描到1笔订单");
        assertEquals(1, result.getFinalizedOrderCount(), "CLOSED应触发回滚收敛");

        try {
            TenantContext.setTenantId(tenantId);
            PaymentOrder updated = paymentService.getById(paymentId);
            assertNotNull(updated, "应能查询到更新后的订单");
            assertEquals(PaymentStatus.SUCCESS, updated.getStatusEnum(), "订单应回滚为SUCCESS");
            assertNull(updated.getRefundRequestNo(), "refundRequestNo 应清理");
            assertNull(updated.getRefundRequestAmount(), "refundRequestAmount 应清理");
            assertNull(updated.getRefundRequestTime(), "refundRequestTime 应清理");
        } finally {
            TenantContext.clear();
        }
    }

    private RefundResponse buildRefundQueryResponse(String refundStatus, BigDecimal refundAmount) {
        RefundResponse resp = new RefundResponse();
        resp.setRefundStatus(refundStatus);
        resp.setRefundAmount(refundAmount);
        return resp;
    }
}
