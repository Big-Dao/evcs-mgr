package com.evcs.tenant.client;

import com.evcs.common.http.ResultResponseEntityUtils;
import com.evcs.common.result.Result;
import com.evcs.tenant.config.InternalApiTokenProperties;
import com.evcs.tenant.dto.stats.ChargerActiveDaysRow;
import com.evcs.tenant.dto.stats.OrderDailySummary;
import com.evcs.tenant.dto.stats.OrderHourlyCount;
import com.evcs.tenant.dto.stats.OrderTrendPoint;
import com.evcs.tenant.dto.stats.RecentOrderRow;
import com.evcs.tenant.dto.stats.StationOrderCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

/**
 * order 服务统计客户端（tenant 仪表盘的订单侧数据来源）。
 *
 * <p>任何调用失败都向上抛出，由调用方决定降级（fail-closed 原则下的显式降级）。
 */
@Slf4j
@Component
public class OrderStatsClient {

    private static final ParameterizedTypeReference<Result<OrderDailySummary>> SUMMARY =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Result<List<OrderTrendPoint>>> TRENDS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Result<List<OrderHourlyCount>>> HISTOGRAM =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Result<List<StationOrderCount>>> STATION_COUNTS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Result<List<ChargerActiveDaysRow>>> ACTIVE_DAYS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Result<List<RecentOrderRow>>> RECENT =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final InternalApiTokenProperties internalApiTokenProperties;

    public OrderStatsClient(RestTemplate restTemplate,
                            InternalApiTokenProperties internalApiTokenProperties) {
        this.restTemplate = restTemplate;
        this.internalApiTokenProperties = internalApiTokenProperties;
    }

    public OrderDailySummary getDailySummary(LocalDate date, List<Long> tenantIds) {
        return exchange(
                UriComponentsBuilder.fromUriString("http://evcs-order/internal/api/v1/stats/orders/daily-summary")
                        .queryParam("date", date.toString())
                        .queryParam("tenantIds", join(tenantIds)),
                SUMMARY).getData();
    }

    public List<OrderTrendPoint> getDailyTrends(LocalDate startDate, List<Long> tenantIds) {
        return exchange(
                UriComponentsBuilder.fromUriString("http://evcs-order/internal/api/v1/stats/orders/trends")
                        .queryParam("startDate", startDate.toString())
                        .queryParam("tenantIds", join(tenantIds)),
                TRENDS).getData();
    }

    public List<OrderHourlyCount> getHourlyHistogram(LocalDate date, List<Long> tenantIds, Long stationId) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString("http://evcs-order/internal/api/v1/stats/orders/hourly-histogram")
                        .queryParam("date", date.toString())
                        .queryParam("tenantIds", join(tenantIds));
        if (stationId != null) {
            builder.queryParam("stationId", stationId);
        }
        return exchange(builder, HISTOGRAM).getData();
    }

    public List<StationOrderCount> getStationOrderCounts(List<Long> tenantIds) {
        return exchange(
                UriComponentsBuilder.fromUriString("http://evcs-order/internal/api/v1/stats/orders/station-order-counts")
                        .queryParam("tenantIds", join(tenantIds)),
                STATION_COUNTS).getData();
    }

    public List<ChargerActiveDaysRow> getChargerActiveDays(LocalDate since, List<Long> tenantIds) {
        return exchange(
                UriComponentsBuilder.fromUriString("http://evcs-order/internal/api/v1/stats/orders/charger-active-days")
                        .queryParam("since", since.toString())
                        .queryParam("tenantIds", join(tenantIds)),
                ACTIVE_DAYS).getData();
    }

    public List<RecentOrderRow> getRecentOrders(List<Long> tenantIds, int limit) {
        return exchange(
                UriComponentsBuilder.fromUriString("http://evcs-order/internal/api/v1/stats/orders/recent")
                        .queryParam("tenantIds", join(tenantIds))
                        .queryParam("limit", limit),
                RECENT).getData();
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
            throw new IllegalStateException("order 服务统计查询失败，仪表盘数据不可用");
        }
        return body;
    }

    private String join(List<Long> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }
}
