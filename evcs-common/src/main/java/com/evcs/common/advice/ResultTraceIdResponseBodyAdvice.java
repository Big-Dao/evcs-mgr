package com.evcs.common.advice;

import com.evcs.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 为统一响应 {@link Result} 自动补齐 traceId。
 *
 * 兼容策略：
 * - 优先读取 MDC[traceId]
 * - 兼容 MDC[requestId]
 */
@Slf4j
@RestControllerAdvice
public class ResultTraceIdResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            @Nullable Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response
    ) {
        if (!(body instanceof Result<?> result)) {
            return body;
        }

        if (result.getTraceId() != null && !result.getTraceId().isBlank()) {
            return body;
        }

        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get(MDC_REQUEST_ID_KEY);
        }

        if (traceId != null && !traceId.isBlank()) {
            result.traceId(traceId);
        } else {
            log.debug("Missing traceId in MDC for Result response");
        }

        return body;
    }
}
