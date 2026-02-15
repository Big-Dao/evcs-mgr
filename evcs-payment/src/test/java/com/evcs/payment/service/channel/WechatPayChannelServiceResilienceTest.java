package com.evcs.payment.service.channel;

import com.evcs.common.tenant.TenantContext;
import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.config.PaymentResilienceConfig;
import com.evcs.payment.dto.PaymentResponse;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.http.HttpMethod;
import com.wechat.pay.java.core.http.HttpRequest;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.mock.env.MockEnvironment;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WechatPayChannelServiceResilienceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("微信支付查询 - 5xx应触发重试且traceId/requestId应稳定")
    void testQueryPayment_shouldRetryOn5xx_andKeepTraceStable() throws Exception {
        // Arrange
        TenantContext.setTenantId(123L);
        TenantContext.setUserId(456L);
        MDC.put("traceId", "trace-xyz");
        MDC.put("requestId", "req-xyz");

        PaymentConfig paymentConfig = new PaymentConfig();
        paymentConfig.setEnabled(true);
        PaymentConfig.WechatConfig wechatConfig = paymentConfig.getWechat();
        wechatConfig.setEnabled(true);
        wechatConfig.setAppId("wx-app");
        wechatConfig.setMchid("1900000000");
        wechatConfig.setMerchantSerialNumber("serial-001");
        wechatConfig.setApiV3Key("apiV3Key");
        wechatConfig.setPrivateKey("dummy-private-key");
        wechatConfig.setMaxRetries(3);
        wechatConfig.setRetryIntervalMs(1);

        WechatPayClientFactory clientFactory = Mockito.mock(WechatPayClientFactory.class);
        when(clientFactory.isActive()).thenReturn(true);

        JsapiServiceExtension jsapiService = Mockito.mock(JsapiServiceExtension.class);
        when(clientFactory.getJsapiService()).thenReturn(Optional.of(jsapiService));

        ServiceException transient5xx = new ServiceException(
            new HttpRequest.Builder()
                .httpMethod(HttpMethod.GET)
                .url(URI.create("https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/TRADE-001").toURL())
                .build(),
            500,
            "{\"code\":\"ERROR\",\"message\":\"oops\"}"
        );

        when(jsapiService.queryOrderByOutTradeNo(any()))
            .thenThrow(transient5xx)
            .thenThrow(transient5xx)
            .thenReturn(null);

        PaymentResilienceConfig resilienceConfig = new PaymentResilienceConfig();
        Retry wechatPayRetry = resilienceConfig.wechatPayRetry(paymentConfig);
        CircuitBreaker wechatPayCircuitBreaker = resilienceConfig.wechatPayCircuitBreaker();

        WechatPayChannelService service = new WechatPayChannelService(
            paymentConfig,
            clientFactory,
            new com.fasterxml.jackson.databind.ObjectMapper(),
            new MockEnvironment(),
            wechatPayRetry,
            wechatPayCircuitBreaker
        );

        // Act
        PaymentResponse response = service.queryPayment("TRADE-001");

        // Assert
        assertNotNull(response, "response must not be null");
        verify(jsapiService, times(3)).queryOrderByOutTradeNo(any());

        assertEquals("trace-xyz", MDC.get("traceId"), "MDC.traceId should remain unchanged");
        assertEquals("req-xyz", MDC.get("requestId"), "MDC.requestId should remain unchanged");
    }

    @Test
    @DisplayName("微信支付查询 - 4xx不应重试")
    void testQueryPayment_shouldNotRetryOn4xx() throws Exception {
        // Arrange
        MDC.put("traceId", "trace-abc");
        MDC.put("requestId", "req-abc");

        PaymentConfig paymentConfig = new PaymentConfig();
        paymentConfig.setEnabled(true);
        PaymentConfig.WechatConfig wechatConfig = paymentConfig.getWechat();
        wechatConfig.setEnabled(true);
        wechatConfig.setAppId("wx-app");
        wechatConfig.setMchid("1900000000");
        wechatConfig.setMerchantSerialNumber("serial-001");
        wechatConfig.setApiV3Key("apiV3Key");
        wechatConfig.setPrivateKey("dummy-private-key");
        wechatConfig.setMaxRetries(3);
        wechatConfig.setRetryIntervalMs(1);

        WechatPayClientFactory clientFactory = Mockito.mock(WechatPayClientFactory.class);
        when(clientFactory.isActive()).thenReturn(true);

        JsapiServiceExtension jsapiService = Mockito.mock(JsapiServiceExtension.class);
        when(clientFactory.getJsapiService()).thenReturn(Optional.of(jsapiService));

        ServiceException business4xx = new ServiceException(
            new HttpRequest.Builder()
                .httpMethod(HttpMethod.GET)
                .url(URI.create("https://api.mch.weixin.qq.com/v3/pay/transactions/out-trade-no/TRADE-001").toURL())
                .build(),
            400,
            "{\"code\":\"PARAM_ERROR\",\"message\":\"bad request\"}"
        );

        when(jsapiService.queryOrderByOutTradeNo(any())).thenThrow(business4xx);

        PaymentResilienceConfig resilienceConfig = new PaymentResilienceConfig();
        Retry wechatPayRetry = resilienceConfig.wechatPayRetry(paymentConfig);
        CircuitBreaker wechatPayCircuitBreaker = resilienceConfig.wechatPayCircuitBreaker();

        WechatPayChannelService service = new WechatPayChannelService(
            paymentConfig,
            clientFactory,
            new com.fasterxml.jackson.databind.ObjectMapper(),
            new MockEnvironment(),
            wechatPayRetry,
            wechatPayCircuitBreaker
        );

        // Act
        PaymentResponse response = service.queryPayment("TRADE-001");

        // Assert
        assertNotNull(response, "response must not be null");
        verify(jsapiService, times(1)).queryOrderByOutTradeNo(any());
        assertEquals("trace-abc", MDC.get("traceId"), "MDC.traceId should remain unchanged");
        assertEquals("req-abc", MDC.get("requestId"), "MDC.requestId should remain unchanged");
    }
}
