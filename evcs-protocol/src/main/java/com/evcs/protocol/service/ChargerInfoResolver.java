package com.evcs.protocol.service;

import com.evcs.common.result.Result;
import com.evcs.protocol.dto.ChargerBasicInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Resolve charger basic info (id/tenant/station) by chargerCode.
 * Used by websocket-originated OCPP messages where tenantId is not available in the payload.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargerInfoResolver {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RestTemplate restTemplate;

    public ChargerBasicInfo resolveByChargerCode(String chargerCode) {
        if (chargerCode == null || chargerCode.isBlank()) {
            return null;
        }

        CacheEntry cached = cache.get(chargerCode);
        if (cached != null && !cached.isExpired()) {
            return cached.info;
        }

        ChargerBasicInfo fresh = fetchFromStationService(chargerCode);
        if (fresh != null) {
            cache.put(chargerCode, new CacheEntry(fresh, Instant.now().plus(CACHE_TTL)));
        }
        return fresh;
    }

    private ChargerBasicInfo fetchFromStationService(String chargerCode) {
        if (restTemplate == null) {
            return null;
        }
        try {
            String url = "http://evcs-station/charger/code/" + chargerCode;
            ParameterizedTypeReference<Result<ChargerBasicInfo>> typeRef = new ParameterizedTypeReference<>() {};
            RequestEntity<Void> requestEntity = RequestEntity.get(requiredUri(url)).build();
            ResponseEntity<Result<ChargerBasicInfo>> response = restTemplate.exchange(requestEntity, typeRef);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }
            Result<ChargerBasicInfo> result = response.getBody();
            if (result == null) {
                return null;
            }
            Integer code = result.getCode();
            if (code != null && code == 200) {
                return result.getData();
            }
        } catch (Exception e) {
            log.debug("Failed to resolve charger info from station service, chargerCode={}", chargerCode, e);
        }
        return null;
    }

    @NonNull
    private static URI requiredUri(String url) {
        URI uri = URI.create(url);
        if (uri == null) {
            throw new IllegalStateException("URI.create returned null");
        }
        return uri;
    }

    private static final class CacheEntry {
        private final ChargerBasicInfo info;
        private final Instant expiresAt;

        private CacheEntry(ChargerBasicInfo info, Instant expiresAt) {
            this.info = info;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
