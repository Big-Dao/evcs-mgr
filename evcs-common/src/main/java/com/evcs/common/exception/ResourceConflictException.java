package com.evcs.common.exception;

import com.evcs.common.result.ResultCode;

/**
 * 资源冲突异常
 * <p>用于表示资源冲突（409），如重复创建、状态冲突等
 */
public class ResourceConflictException extends BaseException {

    public ResourceConflictException(String message) {
        super(ResultCode.CONFLICT, message);
    }

    public ResourceConflictException(String resource, String conflict) {
        super(ResultCode.CONFLICT, String.format("%s冲突: %s", resource, conflict));
    }

    public ResourceConflictException(String message, Throwable cause) {
        super(ResultCode.CONFLICT, message, cause);
    }
}
