package com.evcs.common.exception;

import com.evcs.common.result.ResultCode;

/**
 * 资源不存在异常
 * <p>用于表示请求的资源未找到（404）
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resource, Long id) {
        super(ResultCode.NOT_FOUND, String.format("%s[id=%d]不存在", resource, id));
    }

    public ResourceNotFoundException(String resource, String identifier) {
        super(ResultCode.NOT_FOUND, String.format("%s[%s]不存在", resource, identifier));
    }

    public ResourceNotFoundException(String message) {
        super(ResultCode.NOT_FOUND, message);
    }

    public ResourceNotFoundException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }
}
