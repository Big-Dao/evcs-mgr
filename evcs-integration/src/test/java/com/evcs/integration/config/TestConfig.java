package com.evcs.integration.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration;
import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.payment.service.channel.AlipayClientFactory;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.embedded.RedisServer;

import java.time.LocalDateTime;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import com.alipay.api.AlipayClient;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

/**
 * Integration测试配置类
 * 排除Knife4j自动配置并提供Protocol mock beans
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    Knife4jAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@MapperScan({
    "com.evcs.station.mapper",
    "com.evcs.order.mapper",
    "com.evcs.payment.mapper",
    "com.evcs.tenant.mapper",
    "com.evcs.protocol.mapper"
})
public class TestConfig {

    private static final Object REDIS_LOCK = new Object();
    private static volatile boolean redisStarted = false;
    private static RedisServer redisServer;
    private static int redisPort;

    @PostConstruct
    public void setUpEmbeddedRedis() throws IOException {
        synchronized (REDIS_LOCK) {
            if (!redisStarted) {
                redisPort = findAvailablePort();
                redisServer = new RedisServer(redisPort);
                redisServer.start();
                redisStarted = true;

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    synchronized (REDIS_LOCK) {
                        if (redisServer != null && redisServer.isActive()) {
                            redisServer.stop();
                        }
                    }
                }));
            }
        }
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        if (!redisStarted) {
            throw new IllegalStateException("Embedded Redis is not started");
        }
        return new LettuceConnectionFactory("localhost", redisPort);
    }

    @Bean
    @Primary
    public AlipayClient integrationTestAlipayClient() {
        AlipayClient client = Mockito.mock(AlipayClient.class);
        AlipayTradeAppPayResponse response = new AlipayTradeAppPayResponse();
        response.setBody("mock_pay_params");
        try {
            Mockito.when(client.sdkExecute(Mockito.any(AlipayTradeAppPayRequest.class)))
                .thenReturn(response);
        } catch (Exception e) {
            throw new IllegalStateException("Stub AlipayClient.sdkExecute failed", e);
        }
        return client;
    }

    @Bean
    @Primary
    public AlipayClientFactory integrationTestAlipayClientFactory(AlipayClient integrationTestAlipayClient) {
        AlipayClientFactory factory = Mockito.mock(AlipayClientFactory.class);
        Mockito.when(factory.getAlipayClient()).thenReturn(integrationTestAlipayClient);
        return factory;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(RabbitTemplate.class)
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> mock = (RedisTemplate<String, Object>) Mockito.mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> ops = (ValueOperations<String, Object>) Mockito.mock(ValueOperations.class);
        Mockito.when(mock.opsForValue()).thenReturn(ops);
        // Mock setIfAbsent for lock
        Mockito.when(ops.setIfAbsent(
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyLong(),
            Mockito.any(java.util.concurrent.TimeUnit.class)
        )).thenReturn(true);
        return mock;
    }

    /**
     * 提供ProtocolEventListener mock实现用于测试环境
     * 使用@Primary确保优先注入此mock实例
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ProtocolEventListener.class)
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

    /**
     * RedissonClient Mock - 避免测试环境依赖Redis
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() throws Exception {
        RedissonClient client = Mockito.mock(RedissonClient.class);
        RLock lock = Mockito.mock(RLock.class);
        Mockito.when(lock.tryLock(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(TimeUnit.class))).thenReturn(true);
        Mockito.when(lock.isHeldByCurrentThread()).thenReturn(true);
        Mockito.doNothing().when(lock).unlock();
        Mockito.when(client.getLock(Mockito.anyString())).thenReturn(lock);
        return client;
    }

}
