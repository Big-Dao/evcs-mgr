package com.evcs.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求ID过滤器
 * 为每个请求生成或传递唯一的请求ID，便于日志追踪
 */
@Slf4j
public class RequestIdFilter implements Filter {
    
    /**
     * 兼容策略：
     * - 优先使用 X-Trace-Id 作为链路追踪 ID
     * - 兼容旧的 X-Request-Id
     * - MDC 同时写入 traceId 和 requestId（后续逐步统一为 traceId）
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_REQUEST_ID_KEY = "requestId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 从请求头获取或生成 TraceId（优先 X-Trace-Id，兼容 X-Request-Id）
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = httpRequest.getHeader(REQUEST_ID_HEADER);
        }
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
            log.debug("生成新的TraceId: {}", traceId);
        } else {
            log.debug("从Header获取TraceId: {}", traceId);
        }
        
        // 设置到MDC中，便于日志记录
        MDC.put(MDC_TRACE_ID_KEY, traceId);
        MDC.put(MDC_REQUEST_ID_KEY, traceId);
        
        // 设置到响应头中，便于客户端追踪（同时写入新旧 header）
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);
        httpResponse.setHeader(REQUEST_ID_HEADER, traceId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            // 清理MDC，避免内存泄漏
            MDC.remove(MDC_TRACE_ID_KEY);
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("RequestIdFilter初始化完成");
    }
    
    @Override
    public void destroy() {
        log.info("RequestIdFilter销毁");
    }
}
