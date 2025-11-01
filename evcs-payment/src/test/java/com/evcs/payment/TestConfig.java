package com.evcs.payment;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 支付服务测试配置
 * 提供测试环境所需的Bean
 */
@TestConfiguration
public class TestConfig {

    /**
     * 提供简单的MeterRegistry用于测试
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}