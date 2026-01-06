package com.evcs.payment.service.callback;

import com.evcs.payment.dto.CallbackRequest;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.IPaymentService;
import com.evcs.payment.service.OrderSyncService;
import com.evcs.payment.service.callback.impl.PaymentCallbackServiceImpl;
import com.evcs.payment.service.channel.WechatPayClientFactory;
import com.evcs.payment.service.message.PaymentMessageService;
import com.wechat.pay.java.core.notification.Notification;
import com.wechat.pay.java.core.notification.NotificationParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentCallbackServiceWechatSignatureTest {

    @Test
    @DisplayName("微信支付回调验签 - SDK解析成功且timestamp在窗口内时应通过")
    void testVerifySignature_shouldReturnTrue_whenParserPassesAndTimestampValid() {
        // Arrange
        IPaymentService paymentService = mock(IPaymentService.class);
        PaymentMetrics paymentMetrics = mock(PaymentMetrics.class);
        PaymentMessageService paymentMessageService = mock(PaymentMessageService.class);
        OrderSyncService orderSyncService = mock(OrderSyncService.class);

        WechatPayClientFactory factory = mock(WechatPayClientFactory.class);
        when(factory.isActive()).thenReturn(true);

        NotificationParser parser = mock(NotificationParser.class);
        when(factory.getNotificationParser()).thenReturn(Optional.of(parser));

        PaymentCallbackServiceImpl service = new PaymentCallbackServiceImpl(
            paymentService,
            paymentMetrics,
            paymentMessageService,
            orderSyncService,
            Optional.of(factory)
        );

        CallbackRequest request = new CallbackRequest();
        request.setRawData("{\"id\":\"evt\",\"resource\":{\"ciphertext\":\"x\"}}");
        request.setHeaders(buildWechatHeaders(String.valueOf(System.currentTimeMillis() / 1000L)));

        // Act
        boolean ok = service.verifySignature("wechat", request);

        // Assert
        assertTrue(ok, "Signature verification should pass when NotificationParser.parse succeeds");
        verify(parser, times(1)).parse(any(), eq(Notification.class));
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("微信支付回调验签 - timestamp过期时应拒绝且不调用SDK解析")
    void testVerifySignature_shouldReturnFalse_whenTimestampTooOld() {
        // Arrange
        IPaymentService paymentService = mock(IPaymentService.class);
        PaymentMetrics paymentMetrics = mock(PaymentMetrics.class);
        PaymentMessageService paymentMessageService = mock(PaymentMessageService.class);
        OrderSyncService orderSyncService = mock(OrderSyncService.class);

        WechatPayClientFactory factory = mock(WechatPayClientFactory.class);
        when(factory.isActive()).thenReturn(true);

        NotificationParser parser = mock(NotificationParser.class);
        when(factory.getNotificationParser()).thenReturn(Optional.of(parser));

        PaymentCallbackServiceImpl service = new PaymentCallbackServiceImpl(
            paymentService,
            paymentMetrics,
            paymentMessageService,
            orderSyncService,
            Optional.of(factory)
        );

        long tooOldTimestamp = (System.currentTimeMillis() / 1000L) - 3600L;
        CallbackRequest request = new CallbackRequest();
        request.setRawData("{\"id\":\"evt\",\"resource\":{\"ciphertext\":\"x\"}}");
        request.setHeaders(buildWechatHeaders(String.valueOf(tooOldTimestamp)));

        // Act
        boolean ok = service.verifySignature("wechat", request);

        // Assert
        assertFalse(ok, "Signature verification should reject callbacks outside the allowed timestamp skew window");
        verify(parser, never()).parse(any(), any());
    }

    @Test
    @DisplayName("微信支付回调验签 - 真实接入启用但NotificationParser不可用时应拒绝")
    void testVerifySignature_shouldReturnFalse_whenParserMissingAndWechatActive() {
        // Arrange
        IPaymentService paymentService = mock(IPaymentService.class);
        PaymentMetrics paymentMetrics = mock(PaymentMetrics.class);
        PaymentMessageService paymentMessageService = mock(PaymentMessageService.class);
        OrderSyncService orderSyncService = mock(OrderSyncService.class);

        WechatPayClientFactory factory = mock(WechatPayClientFactory.class);
        when(factory.isActive()).thenReturn(true);
        when(factory.getNotificationParser()).thenReturn(Optional.empty());

        PaymentCallbackServiceImpl service = new PaymentCallbackServiceImpl(
            paymentService,
            paymentMetrics,
            paymentMessageService,
            orderSyncService,
            Optional.of(factory)
        );

        CallbackRequest request = new CallbackRequest();
        request.setRawData("{\"id\":\"evt\",\"resource\":{\"ciphertext\":\"x\"}}");
        request.setHeaders(buildWechatHeaders(String.valueOf(System.currentTimeMillis() / 1000L)));

        // Act
        boolean ok = service.verifySignature("wechat", request);

        // Assert
        assertFalse(ok, "When WeChat real integration is active, signature verification must not be skipped");
        verifyNoInteractions(paymentService);
    }

    private static Map<String, String> buildWechatHeaders(String timestamp) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Wechatpay-Serial", "serial-001");
        headers.put("Wechatpay-Signature", "sig");
        headers.put("Wechatpay-Timestamp", timestamp);
        headers.put("Wechatpay-Nonce", "nonce");
        headers.put("Wechatpay-Signature-Type", "WECHATPAY2-SHA256-RSA2048");
        return headers;
    }
}
