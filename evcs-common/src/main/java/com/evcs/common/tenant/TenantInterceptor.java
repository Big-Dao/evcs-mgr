package com.evcs.common.tenant;

import com.evcs.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器
 * 在每个请求中自动提取租户信息并设置到上下文中。
 *
 * <p>租户解析优先级（高→低）：
 * <ol>
 *   <li>Gateway 转发的可信 Header {@code X-Tenant-Id} / {@code X-User-Id}
 *       （Gateway 已从 JWT 派生并剥离客户端伪造的头）</li>
 *   <li>请求中的 JWT Token（直接访问服务的场景）</li>
 * </ol>
 *
 * <p>安全说明：
 * <ul>
 *   <li>不再接受 {@code ?tenantId=} 请求参数——客户端可控的参数不能作为租户身份源，
 *       否则构成租户注入旁路。</li>
 *   <li>登录等无需租户上下文的公开接口应在 {@code WebConfig} 的
 *       {@code excludePathPatterns} 中排除。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        // 注意：故意不再读取 ?tenantId= 请求参数。客户端可控的参数作为租户身份源
        // 会导致租户注入旁路（CVE 类风险）。公开接口应走 excludePathPatterns。
        ResolvedTenant resolved = resolveTenantId(request);

        if (resolved.tenantId != null) {
            TenantContext.setTenantId(resolved.tenantId);
            if (resolved.userId != null) {
                TenantContext.setUserId(resolved.userId);
            }
            log.debug("设置租户上下文成功 - {}", TenantContext.getContextInfo());
        } else {
            // 未解析到租户上下文。此处保持 fail-open 以兼容公开接口（登录等已被排除），
            // 但留下可审计的日志。若后续需要 fail-closed，可在此抛 TenantContextMissingException。
            log.debug("未解析到租户上下文，请求继续（公开接口或未认证）: {}", request.getRequestURI());
        }

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                @Nullable Exception ex) {
        // 请求完成后清除租户上下文，避免内存泄漏 / 线程池复用污染
        TenantContext.clear();
    }

    /**
     * 解析请求中的租户/用户 ID。抽成独立方法以便单元测试。
     *
     * @return 解析结果；若未解析到则 tenantId 为 null
     */
    ResolvedTenant resolveTenantId(HttpServletRequest request) {
        String tenantIdHeader = request.getHeader("X-Tenant-Id");
        String userIdHeader = request.getHeader("X-User-Id");

        if (tenantIdHeader != null && !tenantIdHeader.isEmpty()) {
            try {
                Long tenantId = Long.parseLong(tenantIdHeader);
                Long userId = (userIdHeader != null && !userIdHeader.isEmpty())
                        ? Long.parseLong(userIdHeader) : null;
                return new ResolvedTenant(tenantId, userId, ResolvedTenant.Source.HEADER);
            } catch (NumberFormatException e) {
                log.warn("无效的 Header 租户信息: X-Tenant-Id={}, X-User-Id={}", tenantIdHeader, userIdHeader);
            }
        }

        String token = extractToken(request);
        if (token != null && jwtUtil.verifyToken(token)) {
            Long tenantId = jwtUtil.getTenantId(token);
            Long userId = jwtUtil.getUserId(token);
            return new ResolvedTenant(tenantId, userId, ResolvedTenant.Source.JWT);
        }

        return new ResolvedTenant(null, null, ResolvedTenant.Source.NONE);
    }

    /**
     * 从请求中提取 JWT Token。
     * 优先 Authorization 头，其次 ?token= 请求参数（用于 WebSocket 等无法自定义头的场景）。
     */
    @Nullable
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // WebSocket 等场景无法自定义 Header，允许通过 token 参数传递。
        // 注意：该参数仅用于提取 token，租户身份仍由 JWT claim 决定，不会被客户端参数伪造。
        String tokenParam = request.getParameter("token");
        if (tokenParam != null) {
            return tokenParam;
        }

        return null;
    }

    /**
     * 租户解析结果的值对象。
     */
    static class ResolvedTenant {
        final Long tenantId;
        final Long userId;
        final Source source;

        ResolvedTenant(Long tenantId, Long userId, Source source) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.source = source;
        }

        enum Source { HEADER, JWT, NONE }
    }
}
