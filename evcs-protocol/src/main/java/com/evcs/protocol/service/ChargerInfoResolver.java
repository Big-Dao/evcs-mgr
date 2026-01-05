package com.evcs.protocol.service;

import com.evcs.protocol.client.StationServiceClient;
import com.evcs.protocol.dto.ChargerBasicInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private final StationServiceClient stationServiceClient;

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
        return stationServiceClient.getChargerByCode(chargerCode);
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
