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
 *
 * <p>安全说明：只绑定网关剥离并重写过的可信头（X-Tenant-Id / X-User-Id，均由 JWT claim 派生）。
 * X-Tenant-Type / X-Tenant-Ancestors 不属于可信头（网关不重写、JWT 也不含对应 claim），
 * 绑定它们等于允许客户端伪造平台管理员身份或租户祖先链，因此一律不得从请求头读取。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String USER_HEADER = "X-User-Id";

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
    }
}

