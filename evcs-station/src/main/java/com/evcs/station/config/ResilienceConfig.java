package com.evcs.station.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j 配置
 * 提供熔断器、重试、超时等弹性能力
 */
@Slf4j
@Configuration
public class ResilienceConfig {

    /**
     * 熔断器注册表配置
     * <p>熔断器用于防止级联故障，当服务失败率达到阈值时自动打开熔断
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // 失败率阈值：超过 50% 失败率时打开熔断
                .failureRateThreshold(50)
                // 熔断打开持续时间：30 秒
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // 半开状态允许的调用数：3 次
                .permittedNumberOfCallsInHalfOpenState(3)
                // 滑动窗口大小：100 次调用
                .slidingWindowSize(100)
                // 最小调用数：10 次后才计算失败率
                .minimumNumberOfCalls(10)
                // 异常记录：所有异常都计入失败率
                .recordExceptions(Throwable.class)
                // 忽略的异常：业务异常不计入失败率
                .ignoreExceptions(IllegalArgumentException.class, IllegalStateException.class)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * 重试注册表配置
     * <p>用于处理瞬态故障，如网络抖动
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                // 最大重试次数：3 次
                .maxAttempts(3)
                // 重试等待间隔：指数退避，初始 100ms，倍数 2，最大间隔 2 秒
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialRandomBackoff(100, 2, 2000))
                // 重试异常：所有异常都重试
                .retryExceptions(Throwable.class)
                // 忽略重试的异常：业务异常不重试
                .ignoreExceptions(IllegalArgumentException.class, IllegalStateException.class)
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * 超时限制器注册表配置
     * <p>防止长时间阻塞
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                // 超时时间：30 秒
                .timeoutDuration(Duration.ofSeconds(30))
                // 取消运行中的 future：true
                .cancelRunningFuture(true)
                .build();

        return TimeLimiterRegistry.of(config);
    }
}
