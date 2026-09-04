package com.evcs.tenant.client;

import com.evcs.common.http.ResultResponseEntityUtils;
import com.evcs.common.result.Result;
import com.evcs.tenant.config.InternalApiTokenProperties;
import com.evcs.tenant.dto.OrderUsageCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
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
 * order 服务资源用量客户端。
 *
 * <p>租户删除预检所需的订单计数归属 order 服务，通过其内部端点
 * （网关边缘封锁 + 共享内部令牌）获取。任何调用失败都向上抛出
 * （fail-closed）：用量不可用时阻断删除，避免误删有业务数据的租户。
 */
@Slf4j
@Component
public class OrderUsageClient {

    private static final ParameterizedTypeReference<Result<List<OrderUsageCount>>> USAGE_RESULT =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final InternalApiTokenProperties internalApiTokenProperties;

    public OrderUsageClient(
            @org.springframework.beans.factory.annotation.Qualifier("remoteServiceRestTemplate") RestTemplate restTemplate,
                            InternalApiTokenProperties internalApiTokenProperties) {
        this.restTemplate = restTemplate;
        this.internalApiTokenProperties = internalApiTokenProperties;
    }

    /**
     * 查询租户集合的订单用量，返回按租户ID索引的结果。
     */
    public Map<Long, OrderUsageCount> getUsageCounts(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String url = UriComponentsBuilder
                .fromUriString("http://evcs-order/internal/api/v1/order-usage-counts")
                .queryParam("tenantIds", String.join(",", tenantIds.stream().map(String::valueOf).toList()))
                .toUriString();

        RequestEntity.HeadersBuilder<?> builder = RequestEntity.get(url);
        if (internalApiTokenProperties.isEnabled() && StringUtils.hasText(internalApiTokenProperties.getToken())) {
            builder.header(internalApiTokenProperties.getHeaderName(), internalApiTokenProperties.getToken());
        }

        ResponseEntity<Result<List<OrderUsageCount>>> response =
                restTemplate.exchange(builder.build(), USAGE_RESULT);

        List<OrderUsageCount> data = ResultResponseEntityUtils.dataIfSuccess(response);
        if (data == null) {
            throw new IllegalStateException("order 服务用量查询失败或返回失败结果，删除预检 fail-closed");
        }
        return data.stream().collect(Collectors.toMap(OrderUsageCount::tenantId, Function.identity()));
    }
}
