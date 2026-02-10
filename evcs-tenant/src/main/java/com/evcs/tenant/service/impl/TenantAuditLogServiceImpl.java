package com.evcs.tenant.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.tenant.TenantContext;
import com.evcs.tenant.entity.TenantAuditLog;
import com.evcs.tenant.mapper.TenantAuditLogMapper;
import com.evcs.tenant.service.ITenantAuditLogService;
import com.evcs.tenant.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户审计日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantAuditLogServiceImpl extends ServiceImpl<TenantAuditLogMapper, TenantAuditLog>
        implements ITenantAuditLogService {

    private final ObjectProvider<ISysTenantService> tenantServiceProvider;

    @Override
    public void logAudit(Long operatorTenantId, Long operatorUserId, Long targetTenantId,
                         String action, String result, String details) {
        try {
            TenantAuditLog auditLog = new TenantAuditLog();
            auditLog.setOperatorTenantId(operatorTenantId);
            auditLog.setOperatorUserId(operatorUserId);
            auditLog.setTargetTenantId(targetTenantId);
            auditLog.setAction(action);
            auditLog.setResult(result);
            auditLog.setDetails(details);
            auditLog.setOperateTime(LocalDateTime.now());

            // 获取目标租户名称
            if (targetTenantId != null) {
                ISysTenantService tenantService = tenantServiceProvider.getIfAvailable();
                if (tenantService != null) {
                    var targetTenant = tenantService.getTenantById(targetTenantId);
                    if (targetTenant != null) {
                        auditLog.setTargetTenantName(targetTenant.getTenantName());
                    }
                }
            }

            // 获取请求上下文信息
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setClientIp(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));

                // 尝试从请求头获取 traceId
                String traceId = request.getHeader("X-Trace-Id");
                if (StrUtil.isBlank(traceId)) {
                    traceId = request.getHeader("X-Request-Id");
                }
                auditLog.setTraceId(traceId);
            }

            this.save(auditLog);

            // 同时输出到日志，便于实时监控
            log.info("AUDIT: operator_tenant={}, operator_user={}, target_tenant={}, action={}, result={}, details={}",
                    operatorTenantId, operatorUserId, targetTenantId, action, result, details);
        } catch (Exception e) {
            // 审计日志记录失败不应影响主业务流程
            log.error("Failed to save audit log: action={}, target_tenant={}", action, targetTenantId, e);
        }
    }

    @Override
    public void logSuccess(Long targetTenantId, String action, String details) {
        logAudit(
                TenantContext.getCurrentTenantId(),
                TenantContext.getCurrentUserId(),
                targetTenantId,
                action,
                RESULT_SUCCESS,
                details
        );
    }

    @Override
    public void logFailure(Long targetTenantId, String action, String errorCode, String errorMessage) {
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", errorCode);
        details.put("errorMessage", errorMessage);

        logAudit(
                TenantContext.getCurrentTenantId(),
                TenantContext.getCurrentUserId(),
                targetTenantId,
                action,
                RESULT_FAILURE,
                JSONUtil.toJsonStr(details)
        );
    }

    @Override
    public List<TenantAuditLog> queryAuditLogPage(Long targetTenantId, String action,
                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                   Integer offset, Integer limit) {
        return baseMapper.queryAuditLogPage(targetTenantId, action, startTime, endTime, offset, limit);
    }

    @Override
    public long countAuditLog(Long targetTenantId, String action,
                              LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.countAuditLog(targetTenantId, action, startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> statisticsCrossLayer(LocalDateTime startTime) {
        return baseMapper.statisticsCrossLayer(startTime);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况，取第一个IP
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
