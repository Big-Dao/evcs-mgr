package com.evcs.common.config;

import com.evcs.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行配置
 *
 * - 启用 Spring 的 {@code @Async} 支持
 * - 注册名为 {@code chargingExecutor} 的线程池（同时作为默认的 {@code taskExecutor}）
 * - 为线程池设置 {@link TenantContextTaskDecorator}，在调度异步任务时传递租户上下文
 *
 * 使用示例：
 * - 在需要异步执行并且需要租户上下文传递的地方使用：
 *   {@code @Async("chargingExecutor")}
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 注入用于在异步任务中传播 TenantContext 的 TaskDecorator
     */
    private final TenantContextTaskDecorator tenantContextTaskDecorator;

    // 可按需调整这些常量（也可以改为读取配置）
    private static final int CHARGING_CORE_POOL_SIZE = 10;
    private static final int CHARGING_MAX_POOL_SIZE = 50;
    private static final int CHARGING_QUEUE_CAPACITY = 500;
    private static final int CHARGING_KEEP_ALIVE_SECONDS = 60;
    private static final int CHARGING_AWAIT_TERMINATION_SECONDS = 30;

    /**
     * 主线程池：chargingExecutor
     *
     * 同时提供别名 {@code taskExecutor}，以便将其作为默认的 async executor 使用。
     */
    @Bean(name = {"chargingExecutor", "taskExecutor"})
    public ThreadPoolTaskExecutor chargingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(CHARGING_CORE_POOL_SIZE);
        executor.setMaxPoolSize(CHARGING_MAX_POOL_SIZE);
        executor.setQueueCapacity(CHARGING_QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(CHARGING_KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("charging-exec-");

        // 关键：在任务执行前将提交线程的 TenantContext 传播到工作线程
        executor.setTaskDecorator(tenantContextTaskDecorator);

        // 回退策略与优雅停机配置
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(CHARGING_AWAIT_TERMINATION_SECONDS);

        // 初始化线程池
        executor.initialize();
        return executor;
    }

    /**
     * 将 chargingExecutor 作为默认的异步执行器返回（用于不显式指定 executor 的 @Async）。
     */
    @Override
    public Executor getAsyncExecutor() {
        return chargingExecutor();
    }

    /**
     * 异步方法未捕获异常的处理器：记录异常并同时打印当前租户上下文（有助于排查并发环境下的问题）。
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                try {
                    log.error(
                        "Uncaught async exception in method [{}], params={}, tenantContext={}",
                        method,
                        Arrays.toString(params),
                        TenantContext.getContextInfo(),
                        ex
                    );
                } catch (Exception loggingEx) {
                    // 避免在异常处理期间再次抛出异常
                    log.error("Error while logging async uncaught exception", loggingEx);
                }
            }
        };
    }
}
