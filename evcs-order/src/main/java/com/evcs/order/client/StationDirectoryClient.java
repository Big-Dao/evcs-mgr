package com.evcs.order.client;

import com.evcs.common.http.ResultResponseEntityUtils;
import com.evcs.common.internalapi.InternalApiTokenProperties;
import com.evcs.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * station 服务站点目录客户端（订单表反范式化字段的写时解析）。
 *
 * <p>站点名称/省市与充电桩编码很少变化，内置 5 分钟 TTL 缓存以摊薄建单热路径开销；
 * 查询失败返回 null 由调用方放行（展示字段缺失不应阻断建单）。
 */
@Slf4j
@Component
public class StationDirectoryClient {

    /**
     * 站点简要信息（展示字段冗余来源）。
     */
    public record StationBrief(Long stationId, String stationName, String province, String city) {
    }

    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    private static final ParameterizedTypeReference<Result<StationBrief>> STATION_BRIEF =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Result<Map<String, Object>>> CHARGER_INFO =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final InternalApiTokenProperties internalApiTokenProperties;
    private final Map<Long, CacheEntry<StationBrief>> stationCache = new ConcurrentHashMap<>();
    private final Map<Long, CacheEntry<String>> chargerCodeCache = new ConcurrentHashMap<>();

    public StationDirectoryClient(RestTemplate restTemplate,
                                  InternalApiTokenProperties internalApiTokenProperties) {
        this.restTemplate = restTemplate;
        this.internalApiTokenProperties = internalApiTokenProperties;
    }

    /**
     * 解析站点简要信息（带 TTL 缓存）；失败返回 null。
     */
    public StationBrief getStationBrief(Long stationId) {
        if (stationId == null) {
            return null;
        }
        StationBrief cached = cachedValue(stationCache, stationId);
        if (cached != null) {
            return cached;
        }
        RequestEntity.HeadersBuilder<?> request = RequestEntity.get(
                UriComponentsBuilder.fromUriString("http://evcs-station/internal/api/v1/stats/stations/by-id/{stationId}")
                        .buildAndExpand(stationId).toUriString());
        attachToken(request);
        ResponseEntity<Result<StationBrief>> response = restTemplate.exchange(request.build(), STATION_BRIEF);
        StationBrief brief = ResultResponseEntityUtils.dataIfSuccess(response);
        if (brief != null) {
            stationCache.put(stationId, new CacheEntry<>(brief, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        }
        return brief;
    }

    /**
     * 解析充电桩编码（带 TTL 缓存）；失败返回 null。
     */
    public String getChargerCode(Long chargerId) {
        if (chargerId == null) {
            return null;
        }
        String cached = cachedValue(chargerCodeCache, chargerId);
        if (cached != null) {
            return cached;
        }
        RequestEntity.HeadersBuilder<?> request = RequestEntity.get(
                UriComponentsBuilder.fromUriString("http://evcs-station/internal/api/v1/chargers/by-id/{chargerId}")
                        .buildAndExpand(chargerId).toUriString());
        attachToken(request);
        ResponseEntity<Result<Map<String, Object>>> response =
                restTemplate.exchange(request.build(), CHARGER_INFO);
        Map<String, Object> data = ResultResponseEntityUtils.dataIfSuccess(response);
        if (data == null) {
            return null;
        }
        Object code = data.get("chargerCode");
        String chargerCode = code == null ? null : String.valueOf(code);
        if (chargerCode != null) {
            chargerCodeCache.put(chargerId, new CacheEntry<>(chargerCode, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        }
        return chargerCode;
    }

    private void attachToken(RequestEntity.HeadersBuilder<?> request) {
        if (internalApiTokenProperties.isEnabled() && StringUtils.hasText(internalApiTokenProperties.getToken())) {
            request.header(internalApiTokenProperties.getHeaderName(), internalApiTokenProperties.getToken());
        }
    }

    private <T> T cachedValue(Map<Long, CacheEntry<T>> cache, Long key) {
        CacheEntry<T> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAt()) {
            cache.remove(key);
            return null;
        }
        return entry.value();
    }

    private record CacheEntry<T>(T value, long expiresAt) {
    }
}
