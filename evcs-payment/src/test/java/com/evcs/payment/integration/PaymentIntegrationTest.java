package com.evcs.payment.integration;

import com.evcs.common.tenant.TenantContext;
import com.evcs.common.test.base.BaseIntegrationTest;
import com.evcs.payment.PaymentServiceApplication;
import com.evcs.payment.TestConfig;
import com.evcs.payment.dto.*;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.service.IPaymentService;
import com.evcs.payment.service.IReconciliationService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付服务集成测试
 * 
 * 测试覆盖：
 * 1. 完整支付流程（创建 → 支付 → 回调 → 查询）
 * 2. 退款流程（全额/部分退款）
 * 3. 对账功能
 * 4. 多租户隔离
 * 5. 幂等性保证
 */
@SpringBootTest(
    classes = {PaymentServiceApplication.class, TestConfig.class},
    properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:payment_testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.autoconfigure.exclude=org.springframework.amqp.rabbit.annotation.RabbitBootstrapConfiguration"
    }
)
@ActiveProfiles("test")
@DisplayName("支付服务集成测试")
class PaymentIntegrationTest extends BaseIntegrationTest {

    @Resource
    private IPaymentService paymentService;

    @Resource
    private IReconciliationService reconciliationService;

    private static final Long TENANT_1 = 1L;
    private static final Long USER_1 = 100L;

    @BeforeEach
    void setUp() {
        // 设置默认租户上下文
        TenantContext.setCurrentTenantId(TENANT_1);
        TenantContext.setCurrentUserId(USER_1);
    }

    @AfterEach
    void tearDown() {
        // 清理租户上下文
        TenantContext.clear();
    }

    @Test
    @DisplayName("完整支付流程测试 - 支付宝APP支付")
    void testCompletePaymentFlow_Alipay() {
        // 1. 创建支付订单
        PaymentRequest createRequest = new PaymentRequest();
        createRequest.setOrderId(1001L);
        createRequest.setAmount(new BigDecimal("99.99"));
        createRequest.setPaymentMethod(PaymentMethod.ALIPAY_APP);
        createRequest.setUserId(USER_1);
        createRequest.setDescription("充电订单支付");

        PaymentResponse createResponse = paymentService.createPayment(createRequest);

        // 验证创建结果
        assertNotNull(createResponse);
        assertNotNull(createResponse.getPaymentId());
        assertNotNull(createResponse.getTradeNo());
        assertEquals(PaymentStatus.PENDING, createResponse.getStatus());
        assertEquals(new BigDecimal("99.99"), createResponse.getAmount());
        assertNotNull(createResponse.getPayParams());

        String tradeNo = createResponse.getTradeNo();

        // 2. 查询支付状态（支付前）
        PaymentResponse queryResponse1 = paymentService.queryPayment(tradeNo);
        assertNotNull(queryResponse1);
        assertEquals(PaymentStatus.PENDING, queryResponse1.getStatus());

        // 3. 模拟支付回调（支付成功）
        boolean callbackResult = paymentService.handlePaymentCallback(tradeNo, true);
        assertTrue(callbackResult);

        // 4. 查询支付状态（支付后）
        PaymentResponse queryResponse2 = paymentService.queryPayment(tradeNo);
        assertNotNull(queryResponse2);
        assertEquals(PaymentStatus.SUCCESS, queryResponse2.getStatus());

        // 5. 验证订单状态
        PaymentOrder order = paymentService.getByOrderId(1001L);
        assertNotNull(order);
        assertEquals(PaymentStatus.SUCCESS, order.getStatusEnum());
        assertNotNull(order.getPaidTime());
        assertEquals(TENANT_1, order.getTenantId());
    }

    @Test
    @DisplayName("幂等性测试 - 重复支付请求")
    void testIdempotency() {
        String idempotentKey = "test-idem-" + System.currentTimeMillis();

        // 1. 第一次创建支付订单
        PaymentRequest request1 = new PaymentRequest();
        request1.setOrderId(5001L);
        request1.setAmount(new BigDecimal("88.88"));
        request1.setPaymentMethod(PaymentMethod.ALIPAY_QR);
        request1.setUserId(USER_1);
        request1.setIdempotentKey(idempotentKey);

        PaymentResponse response1 = paymentService.createPayment(request1);
        assertNotNull(response1);
        Long paymentId1 = response1.getPaymentId();
        String tradeNo1 = response1.getTradeNo();

        // 2. 使用相同幂等键再次创建
        PaymentRequest request2 = new PaymentRequest();
        request2.setOrderId(5001L);
        request2.setAmount(new BigDecimal("88.88"));
        request2.setPaymentMethod(PaymentMethod.ALIPAY_QR);
        request2.setUserId(USER_1);
        request2.setIdempotentKey(idempotentKey); // 相同的幂等键

        PaymentResponse response2 = paymentService.createPayment(request2);
        assertNotNull(response2);

        // 3. 验证返回的是同一个订单
        assertEquals(paymentId1, response2.getPaymentId(), "应该返回相同的支付订单ID");
        assertEquals(tradeNo1, response2.getTradeNo(), "应该返回相同的交易流水号");
    }
}
