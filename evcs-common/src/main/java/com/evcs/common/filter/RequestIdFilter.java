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
    
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 从请求头获取或生成RequestId
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
            log.debug("生成新的RequestId: {}", requestId);
        } else {
            log.debug("从Header获取RequestId: {}", requestId);
        }
        
        // 设置到MDC中，便于日志记录
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        
        // 设置到响应头中，便于客户端追踪
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            // 清理MDC，避免内存泄漏
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
