package com.evcs.payment.service.channel;

import com.evcs.payment.config.AlipayConfig;
import com.evcs.payment.config.TestConfig;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付宝渠道服务测试
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestConfig.class})
@DisplayName("支付宝渠道服务测试")
class AlipayChannelServiceTest {

    @Resource
    private AlipayChannelService alipayChannelService;

    @Test
    @DisplayName("测试支付宝APP支付创建")
    void testCreateAppPayment() {
        // Arrange
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1L);
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_APP);
        request.setUserId(1L);
        request.setDescription("测试支付宝APP支付");
        request.setIdempotentKey("test-app-pay");

        // Act
        PaymentResponse response = alipayChannelService.createPayment(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getTradeNo());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertNotNull(response.getPayParams());

        // 验证交易号格式
        assertTrue(response.getTradeNo().startsWith("ALI"));
    }

    @Test
    @DisplayName("测试支付宝扫码支付创建")
    void testCreateQrPayment() {
        // Arrange
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(2L);
        request.setAmount(new BigDecimal("200.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_QR);
        request.setUserId(1L);
        request.setDescription("测试支付宝扫码支付");
        request.setIdempotentKey("test-qr-pay");

        // Act
        PaymentResponse response = alipayChannelService.createPayment(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getTradeNo());
        assertEquals(new BigDecimal("200.00"), response.getAmount());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertNotNull(response.getPayUrl());

        // 验证交易号格式
        assertTrue(response.getTradeNo().startsWith("ALI"));
    }

    @Test
    @DisplayName("测试支付宝支付状态查询")
    void testQueryPayment() {
        // Arrange
        String tradeNo = "ALI" + System.currentTimeMillis() + "000001";

        // Act
        PaymentResponse response = alipayChannelService.queryPayment(tradeNo);

        // Assert
        assertNotNull(response);
        assertEquals(tradeNo, response.getTradeNo());
        assertNotNull(response.getStatus());
    }

    @Test
    @DisplayName("测试支付宝退款")
    void testRefund() {
        // Arrange
        RefundRequest request = new RefundRequest();
        request.setPaymentId(1L);
        request.setRefundAmount(new BigDecimal("50.00"));
        request.setRefundReason("测试退款");

        // Act
        RefundResponse response = alipayChannelService.refund(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getRefundNo());
        assertEquals(new BigDecimal("50.00"), response.getRefundAmount());
        assertEquals("SUCCESS", response.getRefundStatus());
    }

    @Test
    @DisplayName("测试支付宝签名验证")
    void testVerifySignature() {
        // Arrange
        String data = "test_data";
        String signature = "test_signature";

        // Act
        boolean result = alipayChannelService.verifySignature(data, signature);

        // Assert
        assertTrue(result); // 目前使用模拟实现
    }

    @Test
    @DisplayName("测试获取渠道名称")
    void testGetChannelName() {
        // Act
        String channelName = alipayChannelService.getChannelName();

        // Assert
        assertEquals("alipay", channelName);
    }
}