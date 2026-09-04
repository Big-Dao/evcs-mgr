package com.evcs.payment.service.channel;

import com.evcs.payment.config.TestConfig;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.dto.WechatPaymentOptions;
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
 * 微信支付渠道服务测试
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
})
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestConfig.class})
@DisplayName("微信支付渠道服务测试")
class WechatPayChannelServiceTest {

    @Resource
    private WechatPayChannelService wechatPayChannelService;

    @Test
    @DisplayName("测试微信Native支付创建")
    void testCreateNativePayment() {
        // Arrange
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(2L);
        request.setAmount(new BigDecimal("50.00"));
        request.setPaymentMethod(PaymentMethod.WECHAT_NATIVE);
        request.setUserId(1L);
        request.setDescription("测试微信Native支付");
        request.setIdempotentKey("test-wx-native");

        WechatPaymentOptions options = new WechatPaymentOptions();
        options.setAppId("wx8888888888888888");
        request.setWechatOptions(options);

        // Act
        PaymentResponse response = wechatPayChannelService.createPayment(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getTradeNo());
        assertTrue(response.getTradeNo().startsWith("WXP"));
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertNotNull(response.getPayUrl());
        assertTrue(response.getPayUrl().contains("mock"));
    }

    @Test
    @DisplayName("测试微信JSAPI支付创建")
    void testCreateJsapiPayment() {
        // Arrange
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(3L);
        request.setAmount(new BigDecimal("10.00"));
        request.setPaymentMethod(PaymentMethod.WECHAT_JSAPI);
        request.setUserId(1L);
        request.setDescription("测试微信JSAPI支付");

        WechatPaymentOptions options = new WechatPaymentOptions();
        options.setAppId("wx8888888888888888");
        options.setOpenId("oUpF8uMuAJO_M2pxb1Q9zNjWeS6o");
        request.setWechatOptions(options);

        // Act
        PaymentResponse response = wechatPayChannelService.createPayment(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getTradeNo());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertNotNull(response.getPayParams());
        assertTrue(response.getPayParams().contains("mock"));
    }

    @Test
    @DisplayName("测试微信支付状态查询")
    void testQueryPayment() {
        // Arrange
        String tradeNo = "WXP12345678";

        // Act
        PaymentResponse response = wechatPayChannelService.queryPayment(tradeNo);

        // Assert
        assertNotNull(response);
        assertEquals(tradeNo, response.getTradeNo());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
    }

    @Test
    @DisplayName("测试微信退款")
    void testRefund() {
        // Arrange
        RefundRequest request = new RefundRequest();
        request.setPaymentId(12345L);
        request.setRefundAmount(new BigDecimal("10.00"));
        request.setRefundReason("测试退款");

        // Act
        RefundResponse response = wechatPayChannelService.refund(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getRefundNo());
        assertEquals("SUCCESS", response.getRefundStatus());
        assertEquals(0, new BigDecimal("10.00").compareTo(response.getRefundAmount()));
    }

    @Test
    @DisplayName("测试微信签名验证")
    void testVerifySignature() {
        // Act
        boolean isValid = wechatPayChannelService.verifySignature("mock-data", "mock-sign");

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("测试获取渠道名称")
    void testGetChannelName() {
        assertEquals("wechat", wechatPayChannelService.getChannelName());
    }
}
