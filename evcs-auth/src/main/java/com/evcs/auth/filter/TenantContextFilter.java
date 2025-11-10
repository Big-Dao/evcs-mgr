package com.evcs.auth.filter;

import com.evcs.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 将请求头中的多租户上下文信息注入到线程上下文，配合 MyBatis-Plus 多租户过滤。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String USER_HEADER = "X-User-Id";
    private static final String TENANT_TYPE_HEADER = "X-Tenant-Type";
    private static final String TENANT_ANCESTORS_HEADER = "X-Tenant-Ancestors";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            bindTenantContext(request);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void bindTenantContext(HttpServletRequest request) {
        String tenantIdHeader = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(tenantIdHeader)) {
            try {
                TenantContext.setTenantId(Long.parseLong(tenantIdHeader));
            } catch (NumberFormatException ex) {
                log.warn("非法的租户ID请求头: {}", tenantIdHeader);
            }
        }

        String userIdHeader = request.getHeader(USER_HEADER);
        if (StringUtils.hasText(userIdHeader)) {
            try {
                TenantContext.setUserId(Long.parseLong(userIdHeader));
            } catch (NumberFormatException ex) {
                log.warn("非法的用户ID请求头: {}", userIdHeader);
            }
        }

        String tenantTypeHeader = request.getHeader(TENANT_TYPE_HEADER);
        if (StringUtils.hasText(tenantTypeHeader)) {
            try {
                TenantContext.setTenantType(Integer.parseInt(tenantTypeHeader));
            } catch (NumberFormatException ex) {
                log.warn("非法的租户类型请求头: {}", tenantTypeHeader);
            }
        }

        String tenantAncestors = request.getHeader(TENANT_ANCESTORS_HEADER);
        if (StringUtils.hasText(tenantAncestors)) {
            TenantContext.setTenantAncestors(tenantAncestors);
        }
    }
}

