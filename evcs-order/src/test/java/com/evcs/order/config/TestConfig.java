package com.evcs.order.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import com.evcs.protocol.api.ProtocolEventListener;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;

/**
 * 测试配置类
 * 提供测试环境所需的Bean Mock
 *
 * @author EVCS Team
 */
@TestConfiguration
@MapperScan("com.evcs.order.mapper")
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

    /**
     * 提供ProtocolEventListener mock实现用于测试环境
     */
    @Bean
    @Primary
    public ProtocolEventListener protocolEventListener() {
        return new ProtocolEventListener() {
            @Override
            public void onHeartbeat(Long chargerId, LocalDateTime time) {
                // Test mock - do nothing
            }

            @Override
            public void onStatusChange(Long chargerId, Integer status) {
                // Test mock - do nothing
            }

            @Override
            public void onStartAck(Long chargerId, String sessionId, boolean success, String message) {
                // Test mock - do nothing
            }

            @Override
            public void onStopAck(Long chargerId, boolean success, String message) {
                // Test mock - do nothing
            }

            @Override
            public void onError(Long chargerId, String code, String message) {
                // Test mock - do nothing
            }
        };
    }
}

