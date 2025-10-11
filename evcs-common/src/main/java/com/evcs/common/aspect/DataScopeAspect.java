package com.evcs.common.aspect;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.exception.BusinessException;
import com.evcs.common.result.ResultCode;
import com.evcs.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 数据权限切面
 * 在方法执行前检查数据访问权限
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {
    
    /**
     * 定义切点：所有标注了@DataScope注解的方法
     */
    @Pointcut("@annotation(com.evcs.common.annotation.DataScope)")
    public void dataScopePointcut() {}
    
    /**
     * 在方法执行前检查数据权限
     */
    @Before("dataScopePointcut()")
    public void checkDataScope(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            DataScope dataScope = method.getAnnotation(DataScope.class);
            
            if (dataScope == null) {
                return;
            }
            
            log.debug("开始数据权限检查 - 方法: {}, 权限类型: {}", 
                    method.getName(), dataScope.value());
            
            // 获取当前用户的租户信息
            Long currentTenantId = TenantContext.getTenantId();
            Long currentUserId = TenantContext.getUserId();
            Integer tenantType = TenantContext.getTenantType();
            
            if (currentTenantId == null) {
                log.warn("数据权限检查失败：缺少租户上下文");
                throw new BusinessException(ResultCode.UNAUTHORIZED, "缺少租户信息");
            }
            
            // 根据数据权限类型进行检查
            switch (dataScope.value()) {
                case ALL:
                    // 无限制访问，通常只有系统管理员
                    if (!TenantContext.isSystemAdmin()) {
                        throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，需要系统管理员权限");
                    }
                    break;
                    
                case TENANT:
                    // 租户级别权限检查
                    checkTenantPermission(joinPoint, dataScope, currentTenantId);
                    break;
                    
                case TENANT_HIERARCHY:
                    // 层级租户权限检查
                    checkTenantHierarchyPermission(joinPoint, dataScope, currentTenantId);
                    break;
                    
                case USER:
                    // 用户级别权限检查
                    checkUserPermission(joinPoint, dataScope, currentUserId);
                    break;
                    
                case ROLE:
                    // 角色权限检查
                    checkRolePermission(joinPoint, dataScope);
                    break;
                    
                case CUSTOM:
                    // 自定义权限检查
                    checkCustomPermission(joinPoint, dataScope);
                    break;
                    
                default:
                    log.warn("未知的数据权限类型: {}", dataScope.value());
            }
            
            log.debug("数据权限检查通过 - 方法: {}", method.getName());
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("数据权限检查异常", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "数据权限检查失败");
        }
    }
    
    /**
     * 检查租户级别权限
     */
    private void checkTenantPermission(JoinPoint joinPoint, DataScope dataScope, Long currentTenantId) {
        // 从方法参数中提取目标租户ID
        Long targetTenantId = extractTenantIdFromArgs(joinPoint);
        
        if (targetTenantId != null && !currentTenantId.equals(targetTenantId)) {
            if (!dataScope.allowCrossTenant() || !TenantContext.isSystemAdmin()) {
                throw new BusinessException(ResultCode.FORBIDDEN, 
                        String.format("无权访问租户[%d]的数据", targetTenantId));
            }
        }
    }
    
    /**
     * 检查层级租户权限
     */
    private void checkTenantHierarchyPermission(JoinPoint joinPoint, DataScope dataScope, Long currentTenantId) {
        Long targetTenantId = extractTenantIdFromArgs(joinPoint);
        
        if (targetTenantId != null && !TenantContext.hasAccessToTenant(targetTenantId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, 
                    String.format("无权访问租户[%d]的数据", targetTenantId));
        }
    }
    
    /**
     * 检查用户级别权限
     */
    private void checkUserPermission(JoinPoint joinPoint, DataScope dataScope, Long currentUserId) {
        Long targetUserId = extractUserIdFromArgs(joinPoint);
        
        if (targetUserId != null && !currentUserId.equals(targetUserId)) {
            if (!TenantContext.isSystemAdmin()) {
                throw new BusinessException(ResultCode.FORBIDDEN, 
                        String.format("无权访问用户[%d]的数据", targetUserId));
            }
        }
    }
    
    /**
     * 检查角色权限
     * 基于用户角色的数据权限检查
     */
    private void checkRolePermission(JoinPoint joinPoint, DataScope dataScope) {
        Long currentUserId = TenantContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "缺少用户信息");
        }
        
        // 系统管理员拥有所有权限
        if (TenantContext.isSystemAdmin()) {
            log.debug("系统管理员，跳过角色权限检查");
            return;
        }
        
        // 获取用户角色信息并检查数据权限范围
        // 实际应用中，应该从用户服务或缓存中获取用户的角色信息
        // 然后根据角色的数据权限范围（如：本部门、本部门及下级部门等）进行权限判断
        // 
        // 示例实现：
        // UserRoleInfo roleInfo = userRoleService.getUserRoleInfo(currentUserId);
        // if (roleInfo == null || !roleInfo.hasDataPermission()) {
        //     throw new BusinessException(ResultCode.FORBIDDEN, "缺少必要的数据权限");
        // }
        // 
        // // 根据角色的数据权限范围过滤数据
        // String dataScope = roleInfo.getDataScope(); // 例如：ALL, DEPT, DEPT_AND_CHILD, SELF
        // if ("SELF".equals(dataScope)) {
        //     // 只能访问自己的数据
        //     checkUserPermission(joinPoint, dataScope, currentUserId);
        // }
        
        log.debug("角色权限检查完成 - 用户ID: {}", currentUserId);
    }
    
    /**
     * 检查自定义权限
     */
    private void checkCustomPermission(JoinPoint joinPoint, DataScope dataScope) {
        try {
            DataScope.DataScopeHandler handler = dataScope.handler().getDeclaredConstructor().newInstance();
            Long targetTenantId = extractTenantIdFromArgs(joinPoint);
            Long targetUserId = extractUserIdFromArgs(joinPoint);
            String operation = joinPoint.getSignature().getName();
            
            if (!handler.hasPermission(targetTenantId, targetUserId, operation)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "自定义数据权限检查失败");
            }
        } catch (Exception e) {
            log.error("自定义权限检查异常", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "自定义权限检查失败");
        }
    }
    
    /**
     * 从方法参数中提取租户ID
     */
    private Long extractTenantIdFromArgs(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];
            
            // 检查参数名是否包含tenantId
            if (parameter.getName().toLowerCase().contains("tenantid") && arg instanceof Long) {
                return (Long) arg;
            }
            
            // 检查参数是否是实体对象，且包含tenantId字段
            if (arg != null) {
                try {
                    java.lang.reflect.Field tenantIdField = arg.getClass().getDeclaredField("tenantId");
                    tenantIdField.setAccessible(true);
                    Object tenantId = tenantIdField.get(arg);
                    if (tenantId instanceof Long) {
                        return (Long) tenantId;
                    }
                } catch (Exception e) {
                    // 忽略异常，继续下一个参数
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从方法参数中提取用户ID
     */
    private Long extractUserIdFromArgs(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];
            
            // 检查参数名是否包含userId
            if (parameter.getName().toLowerCase().contains("userid") && arg instanceof Long) {
                return (Long) arg;
            }
            
            // 检查参数是否是实体对象，且包含userId字段
            if (arg != null) {
                try {
                    java.lang.reflect.Field userIdField = arg.getClass().getDeclaredField("userId");
                    userIdField.setAccessible(true);
                    Object userId = userIdField.get(arg);
                    if (userId instanceof Long) {
                        return (Long) userId;
                    }
                } catch (Exception e) {
                    // 忽略异常，继续下一个参数
                }
            }
        }
        
        return null;
    }
}