package com.evcs.protocol.client;

import com.evcs.common.http.ResultResponseEntityUtils;
import com.evcs.common.result.Result;
import com.evcs.protocol.dto.ChargerBasicInfo;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.function.Supplier;

@Slf4j
@Component
public class StationServiceClient {

    private static final ParameterizedTypeReference<Result<ChargerBasicInfo>> CHARGER_BASIC_INFO_RESULT =
        new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Autowired
    public StationServiceClient(
        @Qualifier("stationServiceCircuitBreaker") CircuitBreaker circuitBreaker,
        @Qualifier("stationServiceRetry") Retry retry,
        RestTemplate restTemplate
    ) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.restTemplate = restTemplate;
    }

    @Nullable
    public ChargerBasicInfo getChargerById(Long chargerId) {
        if (chargerId == null) {
            return null;
        }

        String url = UriComponentsBuilder.fromUriString("http://evcs-station/charger/{chargerId}")
            .buildAndExpand(chargerId)
            .toUriString();

        return executeChargerInfoRequest(url, "chargerId=" + chargerId);
    }

    @Nullable
    public ChargerBasicInfo getChargerByCode(String chargerCode) {
        if (chargerCode == null || chargerCode.isBlank()) {
            return null;
        }

        String url = UriComponentsBuilder.fromUriString("http://evcs-station/charger/code/{chargerCode}")
            .buildAndExpand(chargerCode)
            .toUriString();

        return executeChargerInfoRequest(url, "chargerCode=" + chargerCode);
    }

    @Nullable
    private ChargerBasicInfo executeChargerInfoRequest(String url, String logKey) {
        RequestEntity<Void> requestEntity = RequestEntity.get(requiredUri(url)).build();

        Supplier<ResponseEntity<Result<ChargerBasicInfo>>> supplier =
            () -> restTemplate.exchange(requestEntity, CHARGER_BASIC_INFO_RESULT);

        Supplier<ResponseEntity<Result<ChargerBasicInfo>>> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        decorated = Retry.decorateSupplier(retry, decorated);

        try {
            ResponseEntity<Result<ChargerBasicInfo>> response = decorated.get();
            return ResultResponseEntityUtils.dataIfSuccess(response);
        } catch (HttpClientErrorException ex) {
            // 4xx is a business/error response, do not retry
            log.debug("Station service charger lookup returned client error (not retrying): {} status={}", logKey, ex.getStatusCode());
            return null;
        } catch (Exception ex) {
            log.warn("Station service charger lookup failed after retries: {}", logKey, ex);
            return null;
        }
    }

    private static URI requiredUri(String url) {
        URI uri = URI.create(url);
        if (uri == null) {
            throw new IllegalStateException("URI.create returned null");
        }
        return uri;
    }
}
