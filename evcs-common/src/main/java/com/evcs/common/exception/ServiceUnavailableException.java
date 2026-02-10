package com.evcs.common.exception;

import com.evcs.common.result.ResultCode;

/**
 * 服务不可用异常
 * <p>用于表示服务暂时不可用（503）
 */
public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(String message) {
        super(ResultCode.SERVICE_UNAVAILABLE, message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(ResultCode.SERVICE_UNAVAILABLE, message, cause);
    }

    /**
     * 为指定服务名称创建异常
     */
    public static ServiceUnavailableException forService(String serviceName) {
        return new ServiceUnavailableException(String.format("%s服务暂时不可用", serviceName));
    }
}
