package com.evcs.payment.service.reconciliation;

import com.evcs.common.tenant.TenantContext;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.payment.PaymentServiceApplication;
import com.evcs.payment.dto.PaymentResponse;
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
        "evcs.payment.processing-reconcile.min-age-minutes=0",
        "evcs.payment.processing-reconcile.batch-size-per-tenant=50",
        "evcs.payment.processing-reconcile.max-tenants-per-run=10"
    }
)
@ActiveProfiles("test")
@DisplayName("PROCESSING订单轮询补偿服务测试")
class ProcessingPaymentReconciliationServiceTest extends BaseServiceTest {

    @Resource
    private IPaymentService paymentService;

    @Resource
    private ProcessingPaymentReconciliationService reconciliationService;

    @MockBean
    private WechatPayChannelService wechatPayChannelService;

    @MockBean
    private PaymentMessageService paymentMessageService;

    @Test
    @DisplayName("轮询补偿 - 跨租户扫描PROCESSING订单并在渠道返回SUCCESS时应落库为SUCCESS")
    void testReconcileOnce_shouldFinalizeSuccess_whenChannelReturnsSuccess() {
        // Arrange
        Long tenant1 = 2001L;
        Long tenant2 = 2002L;

        doNothing().when(paymentMessageService).sendPaymentSuccessMessage(any());
        doNothing().when(paymentMessageService).sendPaymentFailureMessage(any());

        when(wechatPayChannelService.queryPayment(eq("WXP11_aaaa1111")))
            .thenReturn(buildChannelQueryResponse(PaymentStatus.SUCCESS));
        when(wechatPayChannelService.queryPayment(eq("WXP12_bbbb2222")))
            .thenReturn(buildChannelQueryResponse(PaymentStatus.SUCCESS));

        Long paymentId1;
        Long paymentId2;

        try {
            TenantContext.setTenantId(tenant1);
            TenantContext.setUserId(1L);

            PaymentOrder order1 = new PaymentOrder();
            order1.setOrderId(11L);
            order1.setTradeNo("WXP11_aaaa1111");
            order1.setPaymentMethod(PaymentMethod.WECHAT_JSAPI.getCode());
            order1.setAmount(new BigDecimal("12.34"));
            order1.setStatusEnum(PaymentStatus.PROCESSING);
            order1.setDescription("test");
            order1.setCreateTime(LocalDateTime.now().minusMinutes(10));
            assertTrue(paymentService.save(order1), "应能保存测试订单1");
            paymentId1 = order1.getId();

            TenantContext.setTenantId(tenant2);
            TenantContext.setUserId(1L);

            PaymentOrder order2 = new PaymentOrder();
            order2.setOrderId(12L);
            order2.setTradeNo("WXP12_bbbb2222");
            order2.setPaymentMethod(PaymentMethod.WECHAT_JSAPI.getCode());
            order2.setAmount(new BigDecimal("56.78"));
            order2.setStatusEnum(PaymentStatus.PROCESSING);
            order2.setDescription("test");
            order2.setCreateTime(LocalDateTime.now().minusMinutes(10));
            assertTrue(paymentService.save(order2), "应能保存测试订单2");
            paymentId2 = order2.getId();
        } finally {
            TenantContext.clear();
        }

        // Act
        ProcessingPaymentReconciliationService.ProcessingPaymentReconciliationResult result =
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
            assertEquals(PaymentStatus.SUCCESS, updated1.getStatusEnum(), "订单1应更新为SUCCESS");
            assertNotNull(updated1.getPaidTime(), "订单1应填充支付时间");

            TenantContext.setTenantId(tenant2);
            PaymentOrder updated2 = paymentService.getById(paymentId2);
            assertNotNull(updated2, "应能查询到更新后的订单2");
            assertEquals(PaymentStatus.SUCCESS, updated2.getStatusEnum(), "订单2应更新为SUCCESS");
            assertNotNull(updated2.getPaidTime(), "订单2应填充支付时间");
        } finally {
            TenantContext.clear();
        }
    }

    private PaymentResponse buildChannelQueryResponse(PaymentStatus status) {
        PaymentResponse resp = new PaymentResponse();
        resp.setStatus(status);
        return resp;
    }
}
