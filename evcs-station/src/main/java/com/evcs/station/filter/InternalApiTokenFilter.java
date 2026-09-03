package com.evcs.station.filter;

import com.evcs.station.config.InternalApiTokenProperties;
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

/**
 * 内部 API 令牌过滤器：/internal/api/** 仅允许携带共享内部令牌的服务间调用。
 *
 * <p>外部入口由网关边缘过滤（InternalApiBlockWebFilter 返回 404），
 * 本过滤器提供服务内的防御纵深。与 evcs-tenant 的同名机制保持一致。
 */
@Slf4j
@Component("stationInternalApiTokenFilter")
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
            response.getWriter().write("{\"success\":false,\"code\":\"401\",\"message\":\"内部接口鉴权失败\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
