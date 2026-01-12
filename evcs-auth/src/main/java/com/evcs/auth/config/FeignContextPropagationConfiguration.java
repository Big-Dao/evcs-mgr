package com.evcs.auth.config;

import com.evcs.common.http.EvcsHeaderNames;
import com.evcs.common.tenant.TenantContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignContextPropagationConfiguration {

    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Bean
    public RequestInterceptor contextPropagationRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                applyIfAbsent(template, EvcsHeaderNames.TRACE_ID, resolveTraceId());
                applyIfAbsent(template, EvcsHeaderNames.REQUEST_ID, resolveRequestId());

                Long tenantId = TenantContext.getTenantId();
                if (tenantId != null) {
                    applyIfAbsent(template, EvcsHeaderNames.TENANT_ID, String.valueOf(tenantId));
                }

                Long userId = TenantContext.getUserId();
                if (userId != null) {
                    applyIfAbsent(template, EvcsHeaderNames.USER_ID, String.valueOf(userId));
                }

                Integer tenantType = TenantContext.getTenantType();
                if (tenantType != null) {
                    applyIfAbsent(template, EvcsHeaderNames.TENANT_TYPE, String.valueOf(tenantType));
                }

                String ancestors = TenantContext.getTenantAncestors();
                if (ancestors != null && !ancestors.isBlank()) {
                    applyIfAbsent(template, EvcsHeaderNames.TENANT_ANCESTORS, ancestors);
                }
            }

            private void applyIfAbsent(RequestTemplate template, String name, String value) {
                if (value == null || value.isBlank()) {
                    return;
                }
                if (!template.headers().containsKey(name)) {
                    template.header(name, value);
                }
            }

            private String resolveTraceId() {
                String traceId = MDC.get(MDC_TRACE_ID_KEY);
                if (traceId != null && !traceId.isBlank()) {
                    return traceId;
                }
                return MDC.get(MDC_REQUEST_ID_KEY);
            }

            private String resolveRequestId() {
                String requestId = MDC.get(MDC_REQUEST_ID_KEY);
                if (requestId != null && !requestId.isBlank()) {
                    return requestId;
                }
                return MDC.get(MDC_TRACE_ID_KEY);
            }
        };
    }
}
