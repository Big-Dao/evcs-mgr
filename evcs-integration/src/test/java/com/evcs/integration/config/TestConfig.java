package com.evcs.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration;
import com.evcs.protocol.api.ProtocolEventListener;

import java.time.LocalDateTime;

/**
 * Integration测试配置类
 * 排除Knife4j自动配置并提供Protocol mock beans
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {Knife4jAutoConfiguration.class})
public class TestConfig {
    
    /**
     * 提供ProtocolEventListener mock实现用于测试环境
     * 使用@Primary确保优先注入此mock实例
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
