package com.evcs.payment.service;

import com.evcs.payment.config.MockPaymentMetricsConfig;
import com.evcs.payment.config.TestRedisConfig;
import com.evcs.payment.dto.RefundCallbackRequest;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.service.IRefundCallbackService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.evcs.payment.service.impl.RefundCallbackServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 退款回调服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestRedisConfig.class, MockPaymentMetricsConfig.class})
@DisplayName("退款回调服务测试")
class RefundCallbackServiceTest {

    @MockBean
    private PaymentOrderMapper paymentOrderMapper;

    @MockBean
    private Map<String, IPaymentChannel> paymentChannelMap;

    @MockBean
    private IPaymentChannel alipayChannel;

    private RefundCallbackServiceImpl refundCallbackService;

    @Test
    @DisplayName("测试解析支付宝退款回调")
    void testParseAlipayRefundCallback() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ALI123456789");
        params.put("out_request_no", "REFUND123");
        params.put("trade_no", "202411022200123456789");
        params.put("refund_fee", "50.00");
        params.put("refund_status", "REFUND_SUCCESS");
        params.put("refund_reason", "用户申请退款");
        params.put("gmt_refund_pay", "2024-11-02 18:00:00");
        params.put("sign", "test_signature");
        params.put("sign_type", "RSA2");

        // Act
        RefundCallbackRequest request = refundCallbackService.parseAlipayRefundCallback(params);

        // Assert
        assertNotNull(request);
        assertEquals("alipay", request.getChannel());
        assertEquals("ALI123456789", request.getOutTradeNo());
        assertEquals("REFUND123", request.getOutRequestNo());
        assertEquals("202411022200123456789", request.getTradeNo());
        assertEquals(new BigDecimal("50.00"), request.getRefundFee());
        assertEquals("REFUND_SUCCESS", request.getRefundStatus());
        assertEquals("用户申请退款", request.getReason());
        assertEquals("2024-11-02 18:00:00", request.getGmtRefundPay());
        assertEquals("test_signature", request.getSign());
        assertEquals("RSA2", request.getSignType());
        assertNotNull(request.getRawParams());
    }

    @Test
    @DisplayName("测试处理退款成功回调")
    void testHandleRefundSuccessCallback() {
        // Arrange
        RefundCallbackRequest callbackRequest = createRefundCallbackRequest("REFUND_SUCCESS");

        PaymentOrder order = new PaymentOrder();
        order.setId(1L);
        order.setTradeNo("ALI123456789");
        order.setStatus(PaymentStatus.SUCCESS.getCode());
        order.setAmount(new BigDecimal("100.00"));
        order.setRefundAmount(BigDecimal.ZERO);

        when(paymentOrderMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(order);
        when(paymentOrderMapper.updateById(any(PaymentOrder.class))).thenReturn(1);
        when(paymentChannelMap.get("alipay")).thenReturn(alipayChannel);
        when(alipayChannel.verifySignature(any(), any())).thenReturn(true);

        // Act
        boolean result = refundCallbackService.handleRefundCallback(callbackRequest);

        // Assert
        assertTrue(result);
        verify(paymentOrderMapper, times(1)).updateById(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("测试处理退款失败回调")
    void testHandleRefundFailedCallback() {
        // Arrange
        RefundCallbackRequest callbackRequest = createRefundCallbackRequest("REFUND_FAILED");

        PaymentOrder order = new PaymentOrder();
        order.setId(1L);
        order.setTradeNo("ALI123456789");
        order.setStatus(PaymentStatus.SUCCESS.getCode());
        order.setAmount(new BigDecimal("100.00"));

        when(paymentOrderMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(order);
        when(paymentChannelMap.get("alipay")).thenReturn(alipayChannel);
        when(alipayChannel.verifySignature(any(), any())).thenReturn(true);

        // Act
        boolean result = refundCallbackService.handleRefundCallback(callbackRequest);

        // Assert
        assertTrue(result); // 回调处理成功，虽然退款失败
        verify(paymentOrderMapper, never()).updateById(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("测试签名验证失败")
    void testHandleRefundCallbackWithInvalidSignature() {
        // Arrange
        RefundCallbackRequest callbackRequest = createRefundCallbackRequest("REFUND_SUCCESS");

        when(paymentChannelMap.get("alipay")).thenReturn(alipayChannel);
        when(alipayChannel.verifySignature(any(), any())).thenReturn(false);

        // Act
        boolean result = refundCallbackService.handleRefundCallback(callbackRequest);

        // Assert
        assertFalse(result);
        verify(paymentOrderMapper, never()).selectOne(any());
        verify(paymentOrderMapper, never()).updateById(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("测试订单不存在")
    void testHandleRefundCallbackWithNonExistentOrder() {
        // Arrange
        RefundCallbackRequest callbackRequest = createRefundCallbackRequest("REFUND_SUCCESS");

        when(paymentOrderMapper.selectOne(any())).thenReturn(null);
        when(paymentChannelMap.get("alipay")).thenReturn(alipayChannel);
        when(alipayChannel.verifySignature(any(), any())).thenReturn(true);

        // Act
        boolean result = refundCallbackService.handleRefundCallback(callbackRequest);

        // Assert
        assertFalse(result);
        verify(paymentOrderMapper, never()).updateById(any(PaymentOrder.class));
    }

    /**
     * 创建退款回调请求
     */
    private RefundCallbackRequest createRefundCallbackRequest(String refundStatus) {
        RefundCallbackRequest request = new RefundCallbackRequest();
        request.setChannel("alipay");
        request.setOutTradeNo("ALI123456789");
        request.setOutRequestNo("REFUND123");
        request.setTradeNo("202411022200123456789");
        request.setRefundFee(new BigDecimal("50.00"));
        request.setRefundStatus(refundStatus);
        request.setReason("用户申请退款");
        request.setGmtRefundPay("2024-11-02 18:00:00");
        request.setSign("test_signature");
        request.setSignType("RSA2");

        Map<String, String> rawParams = new HashMap<>();
        rawParams.put("out_trade_no", "ALI123456789");
        rawParams.put("refund_status", refundStatus);
        request.setRawParams(rawParams);

        return request;
    }
}