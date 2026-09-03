package com.evcs.tenant.client;

import com.evcs.common.http.ContextPropagationClientHttpRequestInterceptor;
import com.evcs.common.http.ResultResponseEntityUtils;
import com.evcs.common.result.Result;
import com.evcs.tenant.config.InternalApiTokenProperties;
import com.evcs.tenant.dto.StationUsageCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * station 服务资源用量客户端。
 *
 * <p>租户配额校验所需的站点/充电桩计数归属于 station 服务，
 * 通过其内部端点（网关边缘封锁 + 共享内部令牌）获取，
 * 取代跨服务直查数据库。任何调用失败都向上抛出（fail-closed）：
 * 配额数据不可用时宁可阻断资源创建，也不放行。
 */
@Slf4j
@Component
public class StationUsageClient {

    private static final ParameterizedTypeReference<Result<List<StationUsageCount>>> USAGE_RESULT =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final InternalApiTokenProperties internalApiTokenProperties;

    public StationUsageClient(RestTemplate restTemplate,
                              InternalApiTokenProperties internalApiTokenProperties) {
        this.restTemplate = restTemplate;
        this.internalApiTokenProperties = internalApiTokenProperties;
    }

    /**
     * 查询租户集合的站点/充电桩用量，返回按租户ID索引的结果。
     */
    public Map<Long, StationUsageCount> getUsageCounts(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String url = UriComponentsBuilder
                .fromUriString("http://evcs-station/internal/api/v1/usage-counts")
                .queryParam("tenantIds", String.join(",", tenantIds.stream().map(String::valueOf).toList()))
                .toUriString();

        RequestEntity.HeadersBuilder<?> builder = RequestEntity.get(url);
        if (internalApiTokenProperties.isEnabled() && StringUtils.hasText(internalApiTokenProperties.getToken())) {
            builder.header(internalApiTokenProperties.getHeaderName(), internalApiTokenProperties.getToken());
        }

        ResponseEntity<Result<List<StationUsageCount>>> response =
                restTemplate.exchange(builder.build(), USAGE_RESULT);

        List<StationUsageCount> data = ResultResponseEntityUtils.dataIfSuccess(response);
        if (data == null) {
            throw new IllegalStateException("station 服务用量查询失败或返回失败结果，配额校验 fail-closed");
        }
        return data.stream().collect(Collectors.toMap(StationUsageCount::tenantId, Function.identity()));
    }

    /**
     * tenant 服务内部调用用的负载均衡 RestTemplate。
     */
    @Configuration
    static class StationUsageClientConfig {

        @Bean
        @LoadBalanced
        RestTemplate stationUsageRestTemplate() {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getInterceptors().add(new ContextPropagationClientHttpRequestInterceptor());
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(5000);
            restTemplate.setRequestFactory(factory);
            return restTemplate;
        }
    }
}
