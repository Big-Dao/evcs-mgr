package com.evcs.common.exception;

import com.evcs.common.result.Result;
import com.evcs.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>统一处理系统中的各类异常，返回标准的 Result 格式
 * <p>异常处理优先级（@Order）：数字越小优先级越高
 *
 * @see com.evcs.common.result.Result
 * @see com.evcs.common.result.ResultCode
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 租户上下文缺失异常处理
     */
    @ExceptionHandler(TenantContextMissingException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @Order(1)
    public Result<Void> handleTenantContextMissingException(TenantContextMissingException e) {
        log.error("租户上下文缺失: {}", e.getMessage());
        return Result.failure(ResultCode.UNAUTHORIZED.getCode(), e.getMessage());
    }

    /**
     * 资源不存在异常处理
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @Order(2)
    public Result<Void> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    /**
     * 资源冲突异常处理
     */
    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @Order(3)
    public Result<Void> handleResourceConflictException(ResourceConflictException e) {
        log.warn("资源冲突: {}", e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    /**
     * 服务不可用异常处理
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @Order(4)
    public Result<Void> handleServiceUnavailableException(ServiceUnavailableException e) {
        log.error("服务不可用: {}", e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    /**
     * 第三方服务异常处理
     */
    @ExceptionHandler(ThirdPartyServiceException.class)
    @Order(5)
    public Result<Void> handleThirdPartyServiceException(ThirdPartyServiceException e) {
        log.error("第三方服务异常: thirdParty={}, message={}",
                e.getThirdParty(), e.getMessage(), e);
        return Result.failure(e.getCode(), e.getMessage());
    }

    /**
     * 基础异常处理
     */
    @ExceptionHandler(BaseException.class)
    @Order(6)
    public Result<Void> handleBaseException(BaseException e, HttpServletResponse response) {
        log.warn("基础异常: code={}, message={}", e.getCode(), e.getMessage());
        response.setStatus(resolveHttpStatus(e.getCode()));
        return Result.failure(e.getCode(), e.getMessage());
    }

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    @Order(7)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常: {}", e.getMessage());
        response.setStatus(resolveHttpStatus(e.getCode()));
        return Result.failure(e.getCode(), e.getMessage());
    }

    private int resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST.value();
        }

        // 标准 HTTP 4xx/5xx 直接映射
        if (code >= 400 && code < 600) {
            return code;
        }

        // 非标准业务码（如 4xxx/5xxx）：统一返回 400，由 body.code 传递业务码
        return HttpStatus.BAD_REQUEST.value();
    }

    /**
     * 参数校验异常处理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        return Result.failure(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("绑定异常: {}", message);
        return Result.failure(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 约束校验异常处理
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束校验异常: {}", message);
        return Result.failure(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @Order(8)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.failure(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 非法状态异常处理
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @Order(9)
    public Result<Void> handleIllegalStateException(IllegalStateException e) {
        log.error("非法状态: {}", e.getMessage(), e);
        return Result.failure(ResultCode.INTERNAL_SERVER_ERROR.getCode(),
                "系统状态异常，请稍后重试");
    }

    /**
     * 中断异常处理
     */
    @ExceptionHandler(InterruptedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @Order(10)
    public Result<Void> handleInterruptedException(InterruptedException e) {
        log.warn("任务被中断: {}", e.getMessage());
        Thread.currentThread().interrupt();
        return Result.failure(ResultCode.SERVICE_UNAVAILABLE.getCode(),
                "任务处理被中断");
    }

    /**
     * 通用异常处理
     * 注意: 此处理器会捕获所有异常,包括RuntimeException
     * 上述特定异常会被更具体的处理器优先捕获
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @Order(Integer.MAX_VALUE)
    public Result<Void> handleException(Exception e) {
        log.error("未处理的异常", e);
        return Result.failure(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
