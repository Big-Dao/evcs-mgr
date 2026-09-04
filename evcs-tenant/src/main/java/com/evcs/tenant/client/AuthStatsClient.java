package com.evcs.tenant.client;

import com.evcs.common.http.ResultResponseEntityUtils;
import com.evcs.common.result.Result;
import com.evcs.tenant.config.InternalApiTokenProperties;
import com.evcs.tenant.dto.stats.UsernameRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * auth 服务用户统计客户端（tenant 仪表盘的用户侧数据来源）。
 */
@Slf4j
@Component
public class AuthStatsClient {

    private static final ParameterizedTypeReference<Result<Map<String, Long>>> COUNT =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Result<List<UsernameRow>>> USERNAMES =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final InternalApiTokenProperties internalApiTokenProperties;

    public AuthStatsClient(
            @org.springframework.beans.factory.annotation.Qualifier("remoteServiceRestTemplate") RestTemplate restTemplate,
                           InternalApiTokenProperties internalApiTokenProperties) {
        this.restTemplate = restTemplate;
        this.internalApiTokenProperties = internalApiTokenProperties;
    }

    /**
     * 统计租户集合内启用中的用户数（对齐原 countUsers：deleted=0 AND status=1）。
     */
    public long countActiveUsers(List<Long> tenantIds) {
        RequestEntity.HeadersBuilder<?> request = RequestEntity.get(
                UriComponentsBuilder.fromUriString("http://evcs-auth/internal/api/v1/stats/users/active-count")
                        .queryParam("tenantIds", String.join(",", tenantIds.stream().map(String::valueOf).toList()))
                        .build().toUri());
        attachToken(request);
        ResponseEntity<Result<Map<String, Long>>> response =
                restTemplate.exchange(request.build(), COUNT);
        Result<Map<String, Long>> body = ResultResponseEntityUtils.bodyIfSuccess(response);
        if (body == null || body.getData() == null || body.getData().get("count") == null) {
            throw new IllegalStateException("auth 服务用户统计查询失败，仪表盘数据不可用");
        }
        return body.getData().get("count");
    }

    /**
     * 按用户ID批量查询用户名。
     */
    public List<UsernameRow> getUsernames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        RequestEntity.HeadersBuilder<?> request = RequestEntity.get(
                UriComponentsBuilder.fromUriString("http://evcs-auth/internal/api/v1/stats/users/usernames")
                        .queryParam("ids", String.join(",", userIds.stream().map(String::valueOf).toList()))
                        .build().toUri());
        attachToken(request);
        ResponseEntity<Result<List<UsernameRow>>> response =
                restTemplate.exchange(request.build(), USERNAMES);
        Result<List<UsernameRow>> body = ResultResponseEntityUtils.bodyIfSuccess(response);
        if (body == null) {
            throw new IllegalStateException("auth 服务用户名查询失败，仪表盘数据不可用");
        }
        return body.getData();
    }

    private void attachToken(RequestEntity.HeadersBuilder<?> request) {
        if (internalApiTokenProperties.isEnabled() && StringUtils.hasText(internalApiTokenProperties.getToken())) {
            request.header(internalApiTokenProperties.getHeaderName(), internalApiTokenProperties.getToken());
        }
    }
}
