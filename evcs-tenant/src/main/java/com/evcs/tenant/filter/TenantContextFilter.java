package com.evcs.tenant.filter;

import com.evcs.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文过滤器
 * 从Gateway传递的Header中提取租户ID和用户ID，设置到TenantContext
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenantIdHeader = request.getHeader("X-Tenant-Id");
            String userIdHeader = request.getHeader("X-User-Id");
            
            if (tenantIdHeader != null) {
                Long tenantId = Long.parseLong(tenantIdHeader);
                TenantContext.setCurrentTenantId(tenantId);
                log.info("从Header设置租户ID: {}", tenantId);
            } else {
                log.warn("未收到X-Tenant-Id header，请求路径: {}", request.getRequestURI());
            }
            
            if (userIdHeader != null) {
                Long userId = Long.parseLong(userIdHeader);
                TenantContext.setCurrentUserId(userId);
                log.info("从Header设置用户ID: {}", userId);
            }
            
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理上下文
            TenantContext.clear();
        }
    }
}
