package com.evcs.tenant.client;

import com.evcs.common.http.ResultResponseEntityUtils;
import com.evcs.common.result.Result;
import com.evcs.tenant.config.InternalApiTokenProperties;
import com.evcs.tenant.dto.StationUsageCount;
import com.evcs.tenant.dto.stats.ChargerCodeRow;
import com.evcs.tenant.dto.stats.ChargerStatusStatsRow;
import com.evcs.tenant.dto.stats.StationNameRow;
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

    private static final ParameterizedTypeReference<Result<List<StationNameRow>>> STATION_NAMES =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Result<List<ChargerCodeRow>>> CHARGER_CODES =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Result<ChargerStatusStatsRow>> STATUS_STATS =
            new ParameterizedTypeReference<>() {};

    /**
     * 查询租户集合内全部站点的名称（仪表盘排名合并用）。
     */
    public List<StationNameRow> getStationNames(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return List.of();
        }
        return exchange(
                UriComponentsBuilder.fromUriString("http://evcs-station/internal/api/v1/stats/stations/names")
                        .queryParam("tenantIds", String.join(",", tenantIds.stream().map(String::valueOf).toList())),
                STATION_NAMES).getData();
    }

    /**
     * 查询租户集合内全部充电桩的编码（利用率合并用）。
     */
    public List<ChargerCodeRow> getChargerCodes(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return List.of();
        }
        return exchange(
                UriComponentsBuilder.fromUriString("http://evcs-station/internal/api/v1/stats/chargers/codes")
                        .queryParam("tenantIds", String.join(",", tenantIds.stream().map(String::valueOf).toList())),
                CHARGER_CODES).getData();
    }

    /**
     * 查询租户集合内充电桩状态分布（online/offline/charging/idle）。
     */
    public ChargerStatusStatsRow getChargerStatusStats(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return new ChargerStatusStatsRow(0L, 0L, 0L, 0L);
        }
        return exchange(
                UriComponentsBuilder.fromUriString("http://evcs-station/internal/api/v1/stats/chargers/status-stats")
                        .queryParam("tenantIds", String.join(",", tenantIds.stream().map(String::valueOf).toList())),
                STATUS_STATS).getData();
    }

    private <T> Result<T> exchange(UriComponentsBuilder builder,
                                   ParameterizedTypeReference<Result<T>> type) {
        RequestEntity.HeadersBuilder<?> request = RequestEntity.get(builder.build().toUri());
        if (internalApiTokenProperties.isEnabled() && StringUtils.hasText(internalApiTokenProperties.getToken())) {
            request.header(internalApiTokenProperties.getHeaderName(), internalApiTokenProperties.getToken());
        }
        ResponseEntity<Result<T>> response = restTemplate.exchange(request.build(), type);
        Result<T> body = ResultResponseEntityUtils.bodyIfSuccess(response);
        if (body == null) {
            throw new IllegalStateException("station 服务统计查询失败或返回失败结果");
        }
        return body;
    }
}
