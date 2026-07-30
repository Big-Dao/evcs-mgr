package com.evcs.station.config;

import com.evcs.protocol.api.ProtocolEventListener;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 测试配置类
 * 提供测试所需的Mock Bean
 */
@TestConfiguration
@MapperScan("com.evcs.station.mapper")
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

    /**
     * RedissonClient Mock - 避免测试环境依赖Redis
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() throws Exception {
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(lock).unlock();
        when(client.getLock(anyString())).thenReturn(lock);
        return client;
    }
}
