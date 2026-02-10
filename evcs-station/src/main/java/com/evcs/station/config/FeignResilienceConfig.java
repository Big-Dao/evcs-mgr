package com.evcs.station.config;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign 客户端配置
 * <p>配置超时、重试等参数
 * <p>注意：熔断功能由 Spring Cloud CircuitBreaker 提供
 */
@Configuration
public class FeignResilienceConfig {

    /**
     * Feign 基础配置
     */
    @Bean
    public Feign.Builder feignBuilder() {
        return Feign.builder()
                // 日志级别：BASIC 记录请求方法、URL、响应状态码和执行时间
                .logLevel(Logger.Level.BASIC)
                // 连接超时：10 秒 (unit=SECONDS)
                // 读取超时：30 秒 (unit=SECONDS)
                .options(new Request.Options(10, TimeUnit.SECONDS, 30, TimeUnit.SECONDS, true))
                // 禁用 Feign 内置重试，由 Resilience4j 或 Spring Cloud CircuitBreaker 处理
                .retryer(Retryer.NEVER_RETRY);
    }
}
