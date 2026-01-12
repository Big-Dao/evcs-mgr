package com.evcs.tenant.filter;

import com.alibaba.fastjson2.JSON;
import com.evcs.common.result.Result;
import com.evcs.tenant.config.InternalApiTokenProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class InternalApiTokenFilter extends OncePerRequestFilter {

    private final InternalApiTokenProperties properties;

    @PostConstruct
    void validateConfig() {
        if (properties.isEnabled() && !StringUtils.hasText(properties.getToken())) {
            throw new IllegalStateException("Internal API token enforcement is enabled but token is empty. Configure 'evcs.internal.api.token'.");
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !StringUtils.hasText(uri) || !uri.startsWith(properties.getPathPrefix());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader(properties.getHeaderName());
        if (!StringUtils.hasText(token) || !token.equals(properties.getToken())) {
            log.warn("Internal API auth failed: uri={}, remoteAddr={}", request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(JSON.toJSONString(Result.fail("内部接口鉴权失败")));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
