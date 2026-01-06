package com.evcs.payment.integration;

import com.evcs.payment.PaymentServiceApplication;
import com.evcs.payment.config.TestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = {PaymentServiceApplication.class, TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:payment_outbound_testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.autoconfigure.exclude=com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",

        "evcs.payment.order-sync.order-service-url=http://order-service/api",
        "evcs.payment.order-sync.api-timeout-ms=2000",
        "evcs.payment.order-sync.max-retries=1",
        "evcs.payment.order-sync.retry-interval-ms=10"
    }
)
@ActiveProfiles("test")
class OutboundHttpContextPropagationTest {

    private static final String ORDER_CALLBACK_URL = "http://order-service/api/order/payment/callback";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RestTemplate orderSyncRestTemplate;

    @BeforeAll
    static void setUp() {
    }

    @AfterAll
    static void tearDown() {
    }

    @Test
    @DisplayName("出站HTTP链路 - 应将入站租户与Trace上下文透传到下游服务")
    void testOutboundHttp_shouldPropagateTenantAndTraceHeaders_whenInboundRequestHasHeaders() throws Exception {
        // Arrange
        RestTemplate requiredRestTemplate = Objects.requireNonNull(orderSyncRestTemplate, "orderSyncRestTemplate must be injected");
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(requiredRestTemplate);
        mockServer.expect(requestTo(ORDER_CALLBACK_URL))
            .andExpect(method(Objects.requireNonNull(HttpMethod.POST, "HttpMethod.POST")))
            .andExpect(header("X-Tenant-Id", "123"))
            .andExpect(header("X-User-Id", "456"))
            .andExpect(header("X-Trace-Id", "req-abc"))
            .andExpect(header("X-Request-Id", "req-abc"))
            .andRespond(withSuccess("{\"code\":200,\"message\":\"操作成功\",\"data\":true}", org.springframework.http.MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "123");
        headers.set("X-User-Id", "456");

        // Only provide legacy header; RequestIdFilter should fallback and unify trace/request ids.
        headers.set("X-Request-Id", "req-abc");

        String url = "http://localhost:" + port + "/__test/outbound/order-callback?success=true";

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            new HttpEntity<>(null, headers),
            String.class
        );

        // Assert (local response)
        assertEquals(200, response.getStatusCode().value(), "Test trigger endpoint should return 200");
        assertEquals("OK", response.getBody(), "Test trigger endpoint should return OK");

        mockServer.verify();
    }
}
