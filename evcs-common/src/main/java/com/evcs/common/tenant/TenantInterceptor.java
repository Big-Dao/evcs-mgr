package com.evcs.common.tenant;

import com.evcs.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器
 * 在每个请求中自动从JWT Token中提取租户信息并设置到上下文中
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
        
        try {
            Long tenantId = null;
            Long userId = null;
            
            // 优先从 Gateway 转发的 Header 中获取租户信息（已通过 Gateway JWT 验证）
            String tenantIdHeader = request.getHeader("X-Tenant-Id");
            String userIdHeader = request.getHeader("X-User-Id");
            
            if (tenantIdHeader != null && !tenantIdHeader.isEmpty()) {
                try {
                    tenantId = Long.parseLong(tenantIdHeader);
                    if (userIdHeader != null && !userIdHeader.isEmpty()) {
                        userId = Long.parseLong(userIdHeader);
                    }
                    log.debug("从 Gateway Header 获取租户信息 - tenantId: {}, userId: {}", tenantId, userId);
                } catch (NumberFormatException e) {
                    log.warn("无效的 Header 租户信息: X-Tenant-Id={}, X-User-Id={}", tenantIdHeader, userIdHeader);
                }
            }
            
            // 如果 Header 中没有，尝试从 JWT Token 中提取（直接访问服务的场景）
            if (tenantId == null) {
                String token = extractToken(request);
                if (token != null && jwtUtil.verifyToken(token)) {
                    tenantId = jwtUtil.getTenantId(token);
                    userId = jwtUtil.getUserId(token);
                    log.debug("从 JWT Token 提取租户信息 - tenantId: {}, userId: {}", tenantId, userId);
                }
            }
            
            // 如果还是没有，检查请求参数（用于登录等公开接口）
            if (tenantId == null) {
                String tenantIdParam = request.getParameter("tenantId");
                if (tenantIdParam != null) {
                    try {
                        tenantId = Long.parseLong(tenantIdParam);
                        log.debug("从请求参数获取租户ID: {}", tenantId);
                    } catch (NumberFormatException e) {
                        log.warn("无效的租户ID参数: {}", tenantIdParam);
                    }
                }
            }
            
            // 设置租户上下文
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
                if (userId != null) {
                    TenantContext.setUserId(userId);
                }
                
                // 从数据库获取租户类型和祖级路径信息
                // 实际应用中，建议使用Redis缓存租户信息以提高性能
                // 示例：
                // String cacheKey = "tenant:info:" + tenantId;
                // TenantInfo tenantInfo = redisTemplate.opsForValue().get(cacheKey);
                // if (tenantInfo == null) {
                //     tenantInfo = tenantService.getTenantInfo(tenantId);
                //     redisTemplate.opsForValue().set(cacheKey, tenantInfo, 1, TimeUnit.HOURS);
                // }
                // TenantContext.setTenantType(tenantInfo.getTenantType());
                // TenantContext.setTenantAncestors(tenantInfo.getAncestors());
                
                log.debug("设置租户上下文成功 - {}", TenantContext.getContextInfo());
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("租户拦截器处理失败", e);
            // 即使失败也继续处理请求，避免影响系统正常运行
            return true;
        }
    }
    
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, 
                               @NonNull HttpServletResponse response, 
                               @NonNull Object handler, 
                               Exception ex) {
        // 请求完成后清除租户上下文，避免内存泄漏
        TenantContext.clear();
    }
    
    /**
     * 从请求中提取JWT Token
     */
    private String extractToken(HttpServletRequest request) {
        // 从Authorization头中提取
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 从请求参数中提取（用于WebSocket等场景）
        String tokenParam = request.getParameter("token");
        if (tokenParam != null) {
            return tokenParam;
        }
        
        return null;
    }
}