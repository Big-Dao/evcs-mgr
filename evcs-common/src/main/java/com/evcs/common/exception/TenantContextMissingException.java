package com.evcs.common.exception;

/**
 * 租户上下文缺失异常
 * 当需要租户ID但TenantContext中未设置时抛出此异常
 */
public class TenantContextMissingException extends RuntimeException {
    
    public TenantContextMissingException() {
        super("租户上下文缺失，无法执行数据操作");
    }
    
    public TenantContextMissingException(String message) {
        super(message);
    }
    
    public TenantContextMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}
