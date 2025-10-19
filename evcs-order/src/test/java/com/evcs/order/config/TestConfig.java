package com.evcs.order.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * 测试配置类
 * 提供测试环境所需的Bean Mock
 * 
 * @author EVCS Team
 */
@TestConfiguration
public class TestConfig {

    /**
     * Mock MeterRegistry（避免依赖真实的Metrics组件）
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * Mock RedisTemplate（避免依赖真实的Redis连接）
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        return (RedisTemplate<String, Object>) mock(RedisTemplate.class);
    }
}
