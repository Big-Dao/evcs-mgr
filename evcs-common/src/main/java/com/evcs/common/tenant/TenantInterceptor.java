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
            // 从请求头中获取Token
            String token = extractToken(request);
            if (token != null && jwtUtil.verifyToken(token)) {
                // 从Token中提取租户信息
                Long tenantId = jwtUtil.getTenantId(token);
                Long userId = jwtUtil.getUserId(token);
                
                if (tenantId != null) {
                    // 设置租户上下文
                    TenantContext.setTenantId(tenantId);
                    TenantContext.setUserId(userId);
                    
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
            } else {
                // 对于不需要认证的请求（如登录接口），检查请求参数中是否有租户ID
                String tenantIdParam = request.getParameter("tenantId");
                if (tenantIdParam != null) {
                    try {
                        Long tenantId = Long.parseLong(tenantIdParam);
                        TenantContext.setTenantId(tenantId);
                        log.debug("从请求参数设置租户ID: {}", tenantId);
                    } catch (NumberFormatException e) {
                        log.warn("无效的租户ID参数: {}", tenantIdParam);
                    }
                }
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