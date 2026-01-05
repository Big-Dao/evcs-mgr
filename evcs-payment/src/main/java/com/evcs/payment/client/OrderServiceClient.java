package com.evcs.payment.client;

import com.evcs.common.http.EvcsHeaderNames;
import com.evcs.common.http.OutgoingRequestContextHeaders;
import com.evcs.common.http.ResultResponseEntityUtils;
import com.evcs.common.result.Result;
import com.evcs.payment.config.OrderSyncConfig;
import com.evcs.payment.entity.PaymentOrder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceClient {

    private final RestTemplate restTemplate;
    private final OrderSyncConfig orderSyncConfig;
    private final CircuitBreaker orderServiceCircuitBreaker;
    private final Retry orderServiceRetry;

    public boolean notifyPaymentCallback(PaymentOrder paymentOrder, boolean success) {
        Supplier<Boolean> supplier = () -> doNotifyPaymentCallback(paymentOrder, success);
        supplier = CircuitBreaker.decorateSupplier(orderServiceCircuitBreaker, supplier);
        supplier = Retry.decorateSupplier(orderServiceRetry, supplier);
        return supplier.get();
    }

    public Map<String, Object> getOrderDetail(PaymentOrder paymentOrder) {
        Supplier<Map<String, Object>> supplier = () -> doGetOrderDetail(paymentOrder);
        supplier = CircuitBreaker.decorateSupplier(orderServiceCircuitBreaker, supplier);
        supplier = Retry.decorateSupplier(orderServiceRetry, supplier);
        return supplier.get();
    }

    private boolean doNotifyPaymentCallback(PaymentOrder paymentOrder, boolean success) {
        try {
            String orderServiceUrl = orderSyncConfig.getOrderServiceUrl() + "/order/payment/callback";

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("tradeId", paymentOrder.getTradeNo());
            params.add("success", String.valueOf(success));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            OutgoingRequestContextHeaders.applyTo(headers);

            if (paymentOrder.getTenantId() != null) {
                headers.set(EvcsHeaderNames.TENANT_ID, String.valueOf(paymentOrder.getTenantId()));
            }
            if (paymentOrder.getCreateBy() != null) {
                headers.set(EvcsHeaderNames.USER_ID, String.valueOf(paymentOrder.getCreateBy()));
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            var response = restTemplate.exchange(
                orderServiceUrl,
                Objects.requireNonNull(HttpMethod.POST),
                request,
                new ParameterizedTypeReference<Result<Boolean>>() {}
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("Order callback API returned non-OK: paymentOrderId={}, status={}",
                    paymentOrder.getId(), response.getStatusCode());
                return false;
            }

            Result<Boolean> result = Objects.requireNonNull(response.getBody());
            if (!ResultResponseEntityUtils.isSuccess(response, HttpStatus.OK)) {
                log.warn("Order callback API returned failure: paymentOrderId={}, result={}", paymentOrder.getId(), result);
                return false;
            }

            if (Boolean.TRUE.equals(result.getData())) {
                return true;
            }

            log.warn("Order callback API returned failure: paymentOrderId={}, result={}", paymentOrder.getId(), result);
            return false;

        } catch (HttpClientErrorException ex) {
            // Business / request errors should not be retried
            log.warn("Order callback API client error (no retry): paymentOrderId={}, status={}, body={}",
                paymentOrder.getId(), ex.getStatusCode(), safeBody(ex));
            return false;
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            // Transient errors: bubble up for retry/circuit breaker
            throw ex;
        } catch (RestClientException ex) {
            throw ex;
        }
    }

    private Map<String, Object> doGetOrderDetail(PaymentOrder paymentOrder) {
        try {
            String orderServiceUrl = orderSyncConfig.getOrderServiceUrl() + "/order/" + paymentOrder.getOrderId();

            HttpHeaders headers = new HttpHeaders();
            OutgoingRequestContextHeaders.applyTo(headers);
            if (paymentOrder.getTenantId() != null) {
                headers.set(EvcsHeaderNames.TENANT_ID, String.valueOf(paymentOrder.getTenantId()));
            }

            HttpEntity<?> request = new HttpEntity<>(headers);

            var response = restTemplate.exchange(
                orderServiceUrl,
                Objects.requireNonNull(HttpMethod.GET),
                request,
                new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
            );

            return ResultResponseEntityUtils.dataIfSuccess(response, HttpStatus.OK);

        } catch (HttpClientErrorException ex) {
            log.warn("Order query API client error (no retry): orderId={}, status={}, body={}",
                paymentOrder.getOrderId(), ex.getStatusCode(), safeBody(ex));
            return null;
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw ex;
        }
    }

    private String safeBody(HttpClientErrorException ex) {
        try {
            return ex.getResponseBodyAsString();
        } catch (Exception ignore) {
            return "";
        }
    }
}
