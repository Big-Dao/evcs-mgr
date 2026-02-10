package com.evcs.station.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 弹性调用注解
 * <p>用于标记需要熔断、重试、超时保护的方法
 * <p>使用方法：
 * <pre>
 * {@code
 * @Resilient(
 *     name = "protocolService",
 *     fallbackMethod = "doFallback"
 * )
 * public boolean callProtocol(String deviceId) {
 *     // 业务逻辑
 * }
 *
 * private boolean doFallback(String deviceId, Exception e) {
 *     log.error("Protocol call failed for device: {}", deviceId, e);
 *     return false;
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Resilient {

    /**
     * 弹性实例名称
     * <p>对应 resilience4j 配置中的实例名
     */
    String name() default "default";

    /**
     * 熔断器类型
     */
    CircuitBreaker circuitBreaker() default CircuitBreaker.ENABLED;

    /**
     * 重试类型
     */
    Retry retry() default Retry.ENABLED;

    /**
     * 超时类型
     */
    TimeLimiter timeLimiter() default TimeLimiter.ENABLED;

    /**
     * 降级方法名称
     * <p>当熔断器打开、超时或所有重试失败时调用
     * <p>方法签名必须与原方法兼容，可以添加一个 Exception 参数
     */
    String fallbackMethod() default "";

    /**
     * 熔断器开关
     */
    enum CircuitBreaker {
        ENABLED,
        DISABLED
    }

    /**
     * 重试开关
     */
    enum Retry {
        ENABLED,
        DISABLED
    }

    /**
     * 超时开关
     */
    enum TimeLimiter {
        ENABLED,
        DISABLED
    }
}
