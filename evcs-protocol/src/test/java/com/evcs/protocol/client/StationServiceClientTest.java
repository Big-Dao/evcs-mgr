package com.evcs.protocol.client;

import com.evcs.common.result.Result;
import com.evcs.protocol.config.InternalApiTokenProperties;
import com.evcs.protocol.dto.ChargerBasicInfo;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StationServiceClient 内部端点与内部令牌测试。
 *
 * <p>充电器查询属于服务间内部调用：必须走 /internal/api/** 内部端点
 * （网关边缘封锁该前缀，服务内由内部令牌过滤器保护），
 * 并在启用内部令牌时携带 X-Internal-Token 请求头。
 */
class StationServiceClientTest {

    private static final String INTERNAL_TOKEN = "protocol-internal-test-token-0123456789";

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
    }

    @AfterEach
    void tearDown() {
        // 占位：无状态
    }

    private StationServiceClient clientWith(InternalApiTokenProperties properties) {
        return new StationServiceClient(
                CircuitBreaker.ofDefaults("test-station"),
                Retry.ofDefaults("test-station"),
                restTemplate,
                properties);
    }

    private InternalApiTokenProperties tokenProperties(boolean enabled) {
        InternalApiTokenProperties properties = new InternalApiTokenProperties();
        properties.setEnabled(enabled);
        properties.setToken(INTERNAL_TOKEN);
        return properties;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private RequestEntity<?> capturedRequest() {
        ArgumentCaptor<RequestEntity<?>> captor = ArgumentCaptor.forClass((Class) RequestEntity.class);
        verify(restTemplate).exchange(captor.capture(), any(ParameterizedTypeReference.class));
        return captor.getValue();
    }

    private void stubSuccess() {
        when(restTemplate.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(Result.success(new ChargerBasicInfo())));
    }

    @Test
    @DisplayName("启用内部令牌 - 按ID查询应走内部端点并携带 X-Internal-Token")
    void shouldUseInternalPathAndTokenById() {
        stubSuccess();
        StationServiceClient client = clientWith(tokenProperties(true));

        client.getChargerById(7L);

        RequestEntity<?> request = capturedRequest();
        assertEquals("/internal/api/v1/chargers/by-id/7", request.getUrl().getPath(),
                "充电器查询应调用 station 的内部端点");
        assertEquals(INTERNAL_TOKEN, request.getHeaders().getFirst("X-Internal-Token"));
    }

    @Test
    @DisplayName("启用内部令牌 - 按编码查询应走内部端点并携带 X-Internal-Token")
    void shouldUseInternalPathAndTokenByCode() {
        stubSuccess();
        StationServiceClient client = clientWith(tokenProperties(true));

        client.getChargerByCode("CHARGER-CODE-1");

        RequestEntity<?> request = capturedRequest();
        assertEquals("/internal/api/v1/chargers/by-code/CHARGER-CODE-1", request.getUrl().getPath(),
                "充电器查询应调用 station 的内部端点");
        assertEquals(INTERNAL_TOKEN, request.getHeaders().getFirst("X-Internal-Token"));
    }

    @Test
    @DisplayName("未启用内部令牌 - 仍走内部端点但不携带令牌头")
    void shouldUseInternalPathWithoutTokenWhenDisabled() {
        stubSuccess();
        StationServiceClient client = clientWith(tokenProperties(false));

        client.getChargerByCode("CHARGER-CODE-2");

        RequestEntity<?> request = capturedRequest();
        assertEquals("/internal/api/v1/chargers/by-code/CHARGER-CODE-2", request.getUrl().getPath());
        assertNull(request.getHeaders().getFirst("X-Internal-Token"));
    }
}
