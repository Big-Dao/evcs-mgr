package com.evcs.payment.integration;

import com.evcs.common.tenant.TenantContext;
import com.evcs.payment.PaymentServiceApplication;
import com.evcs.payment.client.OrderServiceClient;
import com.evcs.payment.config.OrderSyncConfig;
import com.evcs.payment.config.TestConfig;
import com.evcs.payment.entity.PaymentOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(
    classes = {PaymentServiceApplication.class, TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:payment_order_client_testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.autoconfigure.exclude=com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",

        "evcs.payment.order-sync.order-service-url=http://order-service/api",
        "evcs.payment.order-sync.api-timeout-ms=2000",
        "evcs.payment.order-sync.max-retries=3",
        "evcs.payment.order-sync.retry-interval-ms=1"
    }
)
@ActiveProfiles("test")
class OrderServiceClientResilienceTest {

    private static final String ORDER_CALLBACK_URL =
        "http://order-service/api/order/payment/callback";

    @Autowired
    private RestTemplate orderSyncRestTemplate;

    @Autowired
    private OrderSyncConfig orderSyncConfig;

    @Autowired
    private OrderServiceClient orderServiceClient;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("订单同步RestTemplate - 应显式配置连接/读取超时")
    void testOrderSyncRestTemplate_shouldConfigureTimeouts() {
        // Arrange
        RestTemplate rt = Objects.requireNonNull(
            orderSyncRestTemplate,
            "orderSyncRestTemplate must be injected"
        );
        OrderSyncConfig cfg = Objects.requireNonNull(
            orderSyncConfig,
            "orderSyncConfig must be injected"
        );

        // Act
        var requestFactory = unwrapRequestFactory(rt.getRequestFactory());

        // Assert
        assertNotNull(requestFactory, "RestTemplate requestFactory must not be null");

        Integer connectTimeout = readIntFieldIfPresent(requestFactory, "connectTimeout");
        Integer readTimeout = readIntFieldIfPresent(requestFactory, "readTimeout");
        assertNotNull(
            connectTimeout,
            "connectTimeout should be configured on request factory: " +
                requestFactory.getClass().getName()
        );
        assertNotNull(
            readTimeout,
            "readTimeout should be configured on request factory: " +
                requestFactory.getClass().getName()
        );
        assertEquals(
            cfg.getApiTimeoutMs(),
            connectTimeout,
            "Connect timeout should be configured from properties"
        );
        assertEquals(
            cfg.getApiTimeoutMs(),
            readTimeout,
            "Read timeout should be configured from properties"
        );
    }

    private static Integer readIntFieldIfPresent(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        Object value;
        try {
            value = ReflectionTestUtils.getField(target, fieldName);
        } catch (IllegalArgumentException ignore) {
            return null;
        }
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Duration) {
            return Math.toIntExact(((Duration) value).toMillis());
        }
        return null;
    }

    private static org.springframework.http.client.ClientHttpRequestFactory unwrapRequestFactory(
        org.springframework.http.client.ClientHttpRequestFactory requestFactory
    ) {
        org.springframework.http.client.ClientHttpRequestFactory current = requestFactory;
        // RestTemplate wraps the factory when interceptors/buffering are enabled.
        for (int i = 0; i < 5 && current != null; i++) {
            if (current instanceof org.springframework.http.client.InterceptingClientHttpRequestFactory) {
                Object delegate = ReflectionTestUtils.getField(current, "requestFactory");
                if (delegate == null) {
                    delegate = ReflectionTestUtils.getField(current, "delegate");
                }
                current = (org.springframework.http.client.ClientHttpRequestFactory) delegate;
                continue;
            }
            if (current instanceof org.springframework.http.client.BufferingClientHttpRequestFactory) {
                Object delegate = ReflectionTestUtils.getField(current, "requestFactory");
                if (delegate == null) {
                    delegate = ReflectionTestUtils.getField(current, "delegate");
                }
                current = (org.springframework.http.client.ClientHttpRequestFactory) delegate;
                continue;
            }
            return current;
        }
        return current;
    }

    @Test
    @DisplayName("订单回调 - 重试过程中traceId/requestId应稳定且请求头应保持透传")
    void testNotifyPaymentCallback_shouldRetryOn5xx_andKeepTraceStable() {
        // Arrange
        TenantContext.setTenantId(123L);
        TenantContext.setUserId(456L);
        MDC.put("traceId", "trace-xyz");
        MDC.put("requestId", "req-xyz");

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(1L);
        paymentOrder.setOrderId(1001L);
        paymentOrder.setTradeNo("TRADE-001");
        paymentOrder.setTenantId(123L);
        paymentOrder.setCreateBy(456L);

        RestTemplate requiredRestTemplate = Objects.requireNonNull(
            orderSyncRestTemplate,
            "orderSyncRestTemplate must be injected"
        );
        org.springframework.http.client.ClientHttpRequestFactory originalFactory =
            requiredRestTemplate.getRequestFactory();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(requiredRestTemplate);

        // Expect 3 attempts: 2x 5xx -> retry, then success.
        for (int attempt = 0; attempt < 2; attempt++) {
            mockServer
                .expect(requestTo(ORDER_CALLBACK_URL))
                .andExpect(method(Objects.requireNonNull(HttpMethod.POST)))
                .andExpect(header("X-Tenant-Id", "123"))
                .andExpect(header("X-User-Id", "456"))
                .andExpect(header("X-Trace-Id", "trace-xyz"))
                .andExpect(header("X-Request-Id", "req-xyz"))
                .andRespond(withServerError());
        }

        mockServer
            .expect(requestTo(ORDER_CALLBACK_URL))
            .andExpect(method(Objects.requireNonNull(HttpMethod.POST)))
            .andExpect(header("X-Tenant-Id", "123"))
            .andExpect(header("X-User-Id", "456"))
            .andExpect(header("X-Trace-Id", "trace-xyz"))
            .andExpect(header("X-Request-Id", "req-xyz"))
            .andRespond(
                withSuccess(
                    "{\"code\":200,\"message\":\"操作成功\",\"data\":true}",
                    MediaType.APPLICATION_JSON
                )
            );

        try {
            // Act
            boolean ok = orderServiceClient.notifyPaymentCallback(paymentOrder, true);

            // Assert
            assertTrue(ok, "Order callback should eventually succeed after retries");
            assertEquals(
                "trace-xyz",
                MDC.get("traceId"),
                "MDC.traceId should remain unchanged"
            );
            assertEquals(
                "req-xyz",
                MDC.get("requestId"),
                "MDC.requestId should remain unchanged"
            );

            mockServer.verify();
        } finally {
            // MockRestServiceServer swaps the RestTemplate requestFactory; restore it to avoid
            // cross-test pollution since the RestTemplate bean is shared.
            requiredRestTemplate.setRequestFactory(originalFactory);
        }
    }
}
