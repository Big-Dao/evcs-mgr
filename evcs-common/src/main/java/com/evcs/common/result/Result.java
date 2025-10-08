package com.evcs.common.result;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用API响应结果
 */
@Data
public class Result<T> implements Serializable {
    
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 响应时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 请求追踪ID
     */
    private String traceId;
    
    public Result() {
        this.timestamp = LocalDateTime.now();
    }
    
    public Result(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }
    
    public Result(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }
    
    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }
    
    public static Result<Void> success(String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, null);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> failure() {
        return new Result<>(ResultCode.FAILURE.getCode(), ResultCode.FAILURE.getMessage());
    }
    
    public static <T> Result<T> failure(String message) {
        return new Result<>(ResultCode.FAILURE.getCode(), message);
    }

    public static <T> Result<T> fail() {
        return failure();
    }
    
    public static <T> Result<T> fail(String message) {
        return failure(message);
    }
    
    public static <T> Result<T> failure(Integer code, String message) {
        return new Result<>(code, message);
    }
    
    public static <T> Result<T> failure(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage());
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
    
    public Result<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}