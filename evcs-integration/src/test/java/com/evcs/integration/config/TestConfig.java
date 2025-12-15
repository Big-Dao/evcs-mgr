package com.evcs.integration.config;

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

import java.time.LocalDateTime;

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
public class TestConfig {

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
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> mock = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(mock.opsForValue()).thenReturn(ops);
        // Mock setIfAbsent for lock
        Mockito.when(ops.setIfAbsent(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.any())).thenReturn(true);
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

}
