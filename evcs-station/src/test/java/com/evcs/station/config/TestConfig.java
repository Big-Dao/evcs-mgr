package com.evcs.station.config;

import com.evcs.protocol.api.ProtocolEventListener;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

/**
 * 测试配置类
 * 提供测试所需的Mock Bean
 */
@TestConfiguration
public class TestConfig {

    /**
     * 提供一个空的ProtocolEventListener实现用于测试
     * 避免测试时因缺少该Bean导致ApplicationContext启动失败
     */
    @Bean
    @Primary
    public ProtocolEventListener protocolEventListener() {
        return new ProtocolEventListener() {
            @Override
            public void onHeartbeat(Long chargerId, LocalDateTime time) {
                // 测试环境空实现
            }

            @Override
            public void onStatusChange(Long chargerId, Integer status) {
                // 测试环境空实现
            }

            @Override
            public void onStartAck(Long chargerId, String sessionId, boolean success, String message) {
                // 测试环境空实现
            }

            @Override
            public void onStopAck(Long chargerId, boolean success, String message) {
                // 测试环境空实现
            }

            @Override
            public void onError(Long chargerId, String code, String message) {
                // 测试环境空实现
            }
        };
    }
}
