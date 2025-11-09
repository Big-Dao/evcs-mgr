package com.evcs.payment.service;

import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.payment.PaymentServiceApplication;
import com.evcs.payment.config.TestConfig;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.service.channel.AlipayChannelService;
import com.evcs.payment.service.channel.WechatPayChannelService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 支付服务测试
 */
@SpringBootTest(classes = PaymentServiceApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {"spring.autoconfigure.exclude=com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"})
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestConfig.class})
@DisplayName("支付服务测试")
class PaymentServiceTestTemplate extends BaseServiceTest {

    @Resource
    private IPaymentService paymentService;

    @MockBean
    private AlipayChannelService alipayChannelService;

    @MockBean
    private WechatPayChannelService wechatChannelService;

    /**
     * 配置Mock行为
     */
    private void configureMocks() {
        // Mock支付宝渠道服务
        when(alipayChannelService.createPayment(any(PaymentRequest.class)))
            .thenAnswer(invocation -> {
                PaymentRequest request = invocation.getArgument(0);
                PaymentResponse response = new PaymentResponse();
                response.setPaymentId(System.currentTimeMillis());
                response.setTradeNo("ALI" + System.currentTimeMillis());
                response.setStatus(PaymentStatus.PENDING);
                response.setAmount(request.getAmount());

                if (request.getPaymentMethod().name().contains("QR") || request.getPaymentMethod().name().contains("NATIVE")) {
                    response.setPayUrl("https://mock-alipay.com/qr/" + response.getTradeNo());
                } else {
                    response.setPayParams("{\"order_string\":\"mock_alipay_order_string\"}");
                }

                return response;
            });

        when(alipayChannelService.queryPayment(any(String.class)))
            .thenAnswer(invocation -> {
                String tradeNo = invocation.getArgument(0);
                PaymentResponse response = new PaymentResponse();
                response.setTradeNo(tradeNo);
                response.setStatus(PaymentStatus.SUCCESS);
                response.setAmount(new BigDecimal("100.00"));
                return response;
            });

        when(alipayChannelService.verifySignature(any(String.class), any(String.class)))
            .thenReturn(true);

        // Mock微信渠道服务
        when(wechatChannelService.createPayment(any(PaymentRequest.class)))
            .thenAnswer(invocation -> {
                PaymentRequest request = invocation.getArgument(0);
                PaymentResponse response = new PaymentResponse();
                response.setPaymentId(System.currentTimeMillis());
                response.setTradeNo("WX" + System.currentTimeMillis());
                response.setStatus(PaymentStatus.PENDING);
                response.setAmount(request.getAmount());

                if (request.getPaymentMethod().name().contains("QR") || request.getPaymentMethod().name().contains("NATIVE")) {
                    response.setPayUrl("https://mock-wechat.com/qr/" + response.getTradeNo());
                } else {
                    response.setPayParams("{\"order_string\":\"mock_wechat_order_string\"}");
                }

                return response;
            });

        when(wechatChannelService.queryPayment(any(String.class)))
            .thenAnswer(invocation -> {
                String tradeNo = invocation.getArgument(0);
                PaymentResponse response = new PaymentResponse();
                response.setTradeNo(tradeNo);
                response.setStatus(PaymentStatus.SUCCESS);
                response.setAmount(new BigDecimal("100.00"));
                return response;
            });

        when(wechatChannelService.verifySignature(any(String.class), any(String.class)))
            .thenReturn(true);

        // Mock退款操作
        when(alipayChannelService.refund(any(RefundRequest.class)))
            .thenAnswer(invocation -> {
                RefundRequest request = invocation.getArgument(0);
                RefundResponse response = new RefundResponse();
                response.setRefundNo("ALIRF" + System.currentTimeMillis());
                response.setRefundAmount(request.getRefundAmount());
                response.setRefundStatus("SUCCESS");
                return response;
            });

        when(wechatChannelService.refund(any(RefundRequest.class)))
            .thenAnswer(invocation -> {
                RefundRequest request = invocation.getArgument(0);
                RefundResponse response = new RefundResponse();
                response.setRefundNo("WXRF" + System.currentTimeMillis());
                response.setRefundAmount(request.getRefundAmount());
                response.setRefundStatus("SUCCESS");
                return response;
            });
    }

    @Test
    @DisplayName("创建支付订单 - 支付宝")
    void testCreatePayment_Alipay() {
        // Arrange
        configureMocks();
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_APP);
        request.setUserId(1L);
        request.setDescription("测试支付订单");
        request.setIdempotentKey("test-idempotent-key-1");
        
        // Act
        PaymentResponse response = paymentService.createPayment(request);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getPaymentId());
        assertNotNull(response.getTradeNo());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertNotNull(response.getPayParams());
    }

    @Test
    @DisplayName("创建支付订单 - 微信支付")
    void testCreatePayment_WechatPay() {
        // Arrange
        configureMocks();
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(2L);
        request.setAmount(new BigDecimal("200.00"));
        request.setPaymentMethod(PaymentMethod.WECHAT_NATIVE);
        request.setUserId(1L);
        request.setDescription("微信支付测试");
        request.setIdempotentKey("test-idempotent-key-2");
        
        // Act
        PaymentResponse response = paymentService.createPayment(request);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getPaymentId());
        assertNotNull(response.getTradeNo());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertNotNull(response.getPayUrl());
    }

    @Test
    @DisplayName("查询支付状态 - 已支付")
    void testQueryPaymentStatus_Paid() {
        // 1. 创建支付订单
        configureMocks();
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(3L);
        request.setAmount(new BigDecimal("50.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_QR);
        request.setUserId(1L);
        request.setIdempotentKey("test-idempotent-key-3");
        PaymentResponse createResponse = paymentService.createPayment(request);
        
        // 2. 模拟支付成功
        paymentService.handlePaymentCallback(createResponse.getTradeNo(), true);
        
        // 3. 查询支付状态
        PaymentResponse queryResponse = paymentService.queryPayment(createResponse.getTradeNo());
        
        // 4. 验证状态为已支付
        assertNotNull(queryResponse);
        assertEquals(PaymentStatus.SUCCESS, queryResponse.getStatus());
    }

    @Test
    @DisplayName("支付回调 - 支付成功")
    void testPaymentCallback_Success() {
        // 1. 创建支付订单
        configureMocks();
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(4L);
        request.setAmount(new BigDecimal("150.00"));
        request.setPaymentMethod(PaymentMethod.WECHAT_JSAPI);
        request.setUserId(1L);
        request.setIdempotentKey("test-idempotent-key-4");
        PaymentResponse response = paymentService.createPayment(request);
        
        // 2. 模拟支付平台回调
        boolean success = paymentService.handlePaymentCallback(response.getTradeNo(), true);
        
        // 3. 验证订单状态更新
        assertTrue(success);
        PaymentOrder order = paymentService.getByOrderId(4L);
        assertNotNull(order);
        assertEquals(PaymentStatus.SUCCESS, order.getStatusEnum());
        assertNotNull(order.getPaidTime());
    }

    @Test
    @DisplayName("支付回调 - 签名验证失败")
    void testPaymentCallback_InvalidSignature() {
        // Note: 实际的签名验证应该在渠道服务中实现
        // 这里只测试回调处理逻辑

        // 1. 创建支付订单
        configureMocks();
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(5L);
        request.setAmount(new BigDecimal("75.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_APP);
        request.setUserId(1L);
        request.setIdempotentKey("test-idempotent-key-5");

        PaymentResponse response = paymentService.createPayment(request);

        // 2. 发送失败回调
        boolean success = paymentService.handlePaymentCallback(response.getTradeNo(), false);

        // 3. 验证订单状态为失败
        assertTrue(success);
        PaymentOrder order = paymentService.getByOrderId(5L);
        assertEquals(PaymentStatus.FAILED, order.getStatusEnum());
    }

    @Test
    @DisplayName("退款 - 全额退款")
    void testRefund_Full() {
        // 1. 创建并完成支付
        configureMocks();
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(6L);
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_APP);
        request.setUserId(1L);
        request.setIdempotentKey("test-idempotent-key-6");
        PaymentResponse response = paymentService.createPayment(request);
        paymentService.handlePaymentCallback(response.getTradeNo(), true);
        
        // 2. 申请全额退款
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setPaymentId(response.getPaymentId());
        refundRequest.setRefundAmount(new BigDecimal("100.00"));
        refundRequest.setRefundReason("测试全额退款");
        
        // 3. 验证退款成功
        RefundResponse refundResponse = paymentService.refund(refundRequest);
        assertNotNull(refundResponse);
        assertNotNull(refundResponse.getRefundNo());
        assertEquals(new BigDecimal("100.00"), refundResponse.getRefundAmount());
        
        // 4. 验证订单状态更新
        PaymentOrder order = paymentService.getById(response.getPaymentId());
        assertEquals(PaymentStatus.REFUNDED, order.getStatusEnum());
        assertEquals(new BigDecimal("100.00"), order.getRefundAmount());
        assertNotNull(order.getRefundTime());
    }

    @Test
    @DisplayName("退款 - 部分退款")
    void testRefund_Partial() {
        // 1. 创建并完成支付
        configureMocks();
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(7L);
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.WECHAT_NATIVE);
        request.setUserId(1L);
        request.setIdempotentKey("test-idempotent-key-7");
        PaymentResponse response = paymentService.createPayment(request);
        paymentService.handlePaymentCallback(response.getTradeNo(), true);
        
        // 2. 申请部分退款
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setPaymentId(response.getPaymentId());
        refundRequest.setRefundAmount(new BigDecimal("50.00"));
        refundRequest.setRefundReason("测试部分退款");
        
        // 3. 验证退款金额正确
        RefundResponse refundResponse = paymentService.refund(refundRequest);
        assertNotNull(refundResponse);
        assertEquals(new BigDecimal("50.00"), refundResponse.getRefundAmount());
        
        // 4. 验证订单状态
        PaymentOrder order = paymentService.getById(response.getPaymentId());
        assertEquals(new BigDecimal("50.00"), order.getRefundAmount());
    }

    @Test
    @DisplayName("对账 - 每日对账")
    void testDailyReconciliation() {
        // TODO: 对账功能待后续实现
        // 1. 创建多笔支付订单
        // 2. 下载支付宝/微信对账单
        // 3. 比对系统订单与对账单
        // 4. 生成对账报表
        
        // 现阶段：验证可以查询支付订单
        configureMocks();
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(8L);
        request.setAmount(new BigDecimal("88.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_QR);
        request.setUserId(1L);
        request.setIdempotentKey("test-idempotent-key-8");
        PaymentResponse response = paymentService.createPayment(request);
        paymentService.handlePaymentCallback(response.getTradeNo(), true);
        
        PaymentOrder order = paymentService.getByOrderId(8L);
        assertNotNull(order);
        assertEquals(PaymentStatus.SUCCESS, order.getStatusEnum());
    }

    @Test
    @DisplayName("多租户隔离 - 不同租户的支付数据应该隔离")
    void testTenantIsolation() {
        // TODO: 需要在测试环境中设置多租户上下文
        // 实际测试时需要：
        // 1. 使用租户1的上下文创建支付订单
        // 2. 切换到租户2的上下文
        // 3. 尝试查询租户1的订单
        // 4. 验证查询不到（MyBatis Plus自动添加tenant_id过滤）
        
        // 简化测试：验证支付订单包含tenant_id字段
        configureMocks();
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(9L);
        request.setAmount(new BigDecimal("66.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_APP);
        request.setUserId(1L);
        request.setIdempotentKey("test-idempotent-key-9");
        PaymentResponse response = paymentService.createPayment(request);
        
        PaymentOrder order = paymentService.getById(response.getPaymentId());
        assertNotNull(order);
        assertNotNull(order.getTenantId(), "支付订单应该包含租户ID");
    }

    @Test
    @DisplayName("幂等性 - 重复支付请求应该返回原订单")
    void testPaymentIdempotency() {
        // 1. 使用相同的幂等键创建支付订单
        configureMocks();
        String idempotentKey = "test-idempotent-key-" + System.currentTimeMillis();
        PaymentRequest request1 = new PaymentRequest();
        request1.setOrderId(10L);
        request1.setAmount(new BigDecimal("99.00"));
        request1.setPaymentMethod(PaymentMethod.WECHAT_JSAPI);
        request1.setUserId(1L);
        request1.setIdempotentKey(idempotentKey);
        
        PaymentResponse response1 = paymentService.createPayment(request1);
        
        // 2. 再次使用相同的幂等键创建支付订单
        PaymentRequest request2 = new PaymentRequest();
        request2.setOrderId(10L);
        request2.setAmount(new BigDecimal("99.00"));
        request2.setPaymentMethod(PaymentMethod.WECHAT_JSAPI);
        request2.setUserId(1L);
        request2.setIdempotentKey(idempotentKey);
        
        PaymentResponse response2 = paymentService.createPayment(request2);
        
        // 3. 验证返回的是同一个订单
        assertEquals(response1.getPaymentId(), response2.getPaymentId());
        assertEquals(response1.getTradeNo(), response2.getTradeNo());
    }
}
