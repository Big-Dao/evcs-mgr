package com.evcs.auth.security;

import com.evcs.auth.entity.SysUser;
import com.evcs.auth.service.ISysUserService;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.common.tenant.TenantContext;
import com.evcs.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器（auth 服务侧）。
 *
 * <p>职责：
 * <ul>
 *   <li>从 {@code Authorization: Bearer <token>} 头提取 JWT。</li>
 *   <li>校验 token 有效性，加载用户并验证状态。</li>
 *   <li>构造含角色权限的 {@link UsernamePasswordAuthenticationToken} 写入 SecurityContext。</li>
 * </ul>
 *
 * <p>注意：网关已做入口鉴权；此过滤器是下游服务的防御纵深，并为本服务的 @PreAuthorize 提供角色信息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ISysUserService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtUtil.verifyToken(token)) {
            log.debug("JWT 校验失败，路径: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwtUtil.getUserId(token);
        Long tenantId = jwtUtil.getTenantId(token);
        if (userId == null || tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 加载用户（跨租户查询，因为 JWT 自包含 tenantId）
            SysUser user;
            try {
                CustomTenantLineHandler.disableTenantFilter();
                user = userService.getUserByIdWithTenant(userId, tenantId);
            } finally {
                CustomTenantLineHandler.enableTenantFilter();
            }

            if (user == null || user.getStatus() != null && user.getStatus() == 0) {
                log.debug("JWT 对应用户不存在或已禁用: userId={}, tenantId={}", userId, tenantId);
                filterChain.doFilter(request, response);
                return;
            }

            List<String> roleCodes = userService.listRoleCodes(userId);
            List<SimpleGrantedAuthority> authorities = roleCodes.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 同步设置租户上下文，供下游数据隔离使用
            TenantContext.setTenantId(tenantId);
            TenantContext.setUserId(userId);

        } catch (Exception e) {
            log.warn("JWT 认证处理异常: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(jakarta.servlet.http.HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
