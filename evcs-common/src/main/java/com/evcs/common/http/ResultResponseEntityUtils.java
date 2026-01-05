package com.evcs.common.http;

import com.evcs.common.result.Result;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

/**
 * Utilities for evaluating {@link ResponseEntity} responses that wrap {@link Result}.
 */
public final class ResultResponseEntityUtils {

    private ResultResponseEntityUtils() {
    }

    public static <T> boolean isSuccess(@Nullable ResponseEntity<Result<T>> responseEntity) {
        return isSuccess(responseEntity, null);
    }

    public static <T> boolean isSuccess(
        @Nullable ResponseEntity<Result<T>> responseEntity,
        @Nullable HttpStatusCode expectedStatus
    ) {
        if (responseEntity == null) {
            return false;
        }

        if (expectedStatus != null) {
            if (!expectedStatus.equals(responseEntity.getStatusCode())) {
                return false;
            }
        } else if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            return false;
        }

        Result<T> body = responseEntity.getBody();
        return body != null && body.isSuccess();
    }

    @Nullable
    public static <T> Result<T> bodyIfSuccess(@Nullable ResponseEntity<Result<T>> responseEntity) {
        return bodyIfSuccess(responseEntity, null);
    }

    @Nullable
    public static <T> Result<T> bodyIfSuccess(
        @Nullable ResponseEntity<Result<T>> responseEntity,
        @Nullable HttpStatusCode expectedStatus
    ) {
        if (!isSuccess(responseEntity, expectedStatus)) {
            return null;
        }
        return responseEntity.getBody();
    }

    @Nullable
    public static <T> T dataIfSuccess(@Nullable ResponseEntity<Result<T>> responseEntity) {
        return dataIfSuccess(responseEntity, null);
    }

    @Nullable
    public static <T> T dataIfSuccess(
        @Nullable ResponseEntity<Result<T>> responseEntity,
        @Nullable HttpStatusCode expectedStatus
    ) {
        Result<T> body = bodyIfSuccess(responseEntity, expectedStatus);
        return body == null ? null : body.getData();
    }
}
