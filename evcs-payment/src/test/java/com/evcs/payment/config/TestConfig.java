package com.evcs.payment.config;

import com.evcs.payment.metrics.PaymentMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisServer;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;

/**
 * Payment统一测试配置类
 * 整合所有测试环境需要的Mock配置：
 * 1. RabbitMQ Mock
 * 2. Redis Mock (嵌入式Redis)
 * 3. Metrics Mock
 *
 * @author EVCS
 * @since 2025-01-07
 */
@TestConfiguration
public class TestConfig {

    private RedisServer redisServer;

    /**
     * 启动嵌入式Redis服务器
     */
    @PostConstruct
    public void setUp() throws IOException {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }

    /**
     * 关闭嵌入式Redis服务器
     */
    @PreDestroy
    public void tearDown() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    /**
     * RabbitTemplate Mock - 唯一Primary避免冲突
     */
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return mock(RabbitTemplate.class);
    }

    /**
     * RabbitMQConfig Mock - 提供配置对象用于PaymentMessageServiceImpl
     */
    @Bean
    public com.evcs.payment.config.RabbitMQConfig rabbitMQConfig() {
        com.evcs.payment.config.RabbitMQConfig config = new com.evcs.payment.config.RabbitMQConfig();
        com.evcs.payment.config.RabbitMQConfig.ExchangeConfig exchange = new com.evcs.payment.config.RabbitMQConfig.ExchangeConfig();
        exchange.setPaymentDirect("test.payment.direct.exchange");
        config.setExchange(exchange);
        
        com.evcs.payment.config.RabbitMQConfig.RoutingKeyConfig routingKey = new com.evcs.payment.config.RabbitMQConfig.RoutingKeyConfig();
        routingKey.setPaymentSuccess("test.payment.success");
        routingKey.setPaymentFailure("test.payment.failure");
        routingKey.setRefundSuccess("test.refund.success");
        config.setRoutingKey(routingKey);
        
        return config;
    }

    /**
     * AlipayClient Mock - 避免真实SDK调用和RSA签名错误
     */
    @Bean
    @Primary
    public AlipayClient alipayClient() throws Exception {
        AlipayClient mockClient = mock(AlipayClient.class);
        
        // Mock APP支付响应
        AlipayTradeAppPayResponse appPayResponse = new AlipayTradeAppPayResponse();
        appPayResponse.setCode("10000");
        appPayResponse.setMsg("Success");
        appPayResponse.setBody("app_id=test&method=alipay.trade.app.pay");
        org.mockito.Mockito.when(mockClient.sdkExecute(any(com.alipay.api.request.AlipayTradeAppPayRequest.class)))
            .thenReturn(appPayResponse);
        
        // Mock 扫码支付响应
        AlipayTradePrecreateResponse precreateResponse = new AlipayTradePrecreateResponse();
        precreateResponse.setCode("10000");
        precreateResponse.setMsg("Success");
        precreateResponse.setQrCode("https://qr.alipay.com/test123456");
        precreateResponse.setOutTradeNo("ALI1234567890");
        org.mockito.Mockito.when(mockClient.execute(any(com.alipay.api.request.AlipayTradePrecreateRequest.class)))
            .thenReturn(precreateResponse);
        
        // Mock 查询支付响应
        AlipayTradeQueryResponse queryResponse = new AlipayTradeQueryResponse();
        queryResponse.setCode("10000");
        queryResponse.setMsg("Success");
        queryResponse.setTradeStatus("TRADE_SUCCESS");
        queryResponse.setTotalAmount("100.00");
        queryResponse.setOutTradeNo("ALI1234567890");
        org.mockito.Mockito.when(mockClient.execute(any(com.alipay.api.request.AlipayTradeQueryRequest.class)))
            .thenReturn(queryResponse);
        
        // Mock 退款响应
        AlipayTradeRefundResponse refundResponse = new AlipayTradeRefundResponse();
        refundResponse.setCode("10000");
        refundResponse.setMsg("Success");
        refundResponse.setFundChange("Y");
        org.mockito.Mockito.when(mockClient.execute(any(com.alipay.api.request.AlipayTradeRefundRequest.class)))
            .thenReturn(refundResponse);
        
        return mockClient;
    }
    
    /**
     * AlipayClientFactory Mock - 返回mock的AlipayClient
     */
    @Bean
    @Primary
    public com.evcs.payment.service.channel.AlipayClientFactory alipayClientFactory(AlipayClient alipayClient) {
        com.evcs.payment.service.channel.AlipayClientFactory factory = 
            mock(com.evcs.payment.service.channel.AlipayClientFactory.class);
        org.mockito.Mockito.when(factory.getAlipayClient()).thenReturn(alipayClient);
        return factory;
    }
    
    /**
     * PaymentChannelMap Mock - 用于RefundCallbackService等需要channel map的服务
     */
    @Bean
    @Primary
    public java.util.Map<String, com.evcs.payment.service.channel.IPaymentChannel> paymentChannelMap() {
        java.util.Map<String, com.evcs.payment.service.channel.IPaymentChannel> channelMap = new java.util.HashMap<>();
        
        // Mock Alipay Channel
        com.evcs.payment.service.channel.IPaymentChannel alipayChannel = 
            mock(com.evcs.payment.service.channel.IPaymentChannel.class);
        org.mockito.Mockito.when(alipayChannel.getChannelName()).thenReturn("alipay");
        org.mockito.Mockito.when(alipayChannel.verifySignature(any(), any())).thenReturn(true);
        channelMap.put("alipay", alipayChannel);
        
        // Mock Wechat Channel
        com.evcs.payment.service.channel.IPaymentChannel wechatChannel = 
            mock(com.evcs.payment.service.channel.IPaymentChannel.class);
        org.mockito.Mockito.when(wechatChannel.getChannelName()).thenReturn("wechat");
        org.mockito.Mockito.when(wechatChannel.verifySignature(any(), any())).thenReturn(true);
        channelMap.put("wechat", wechatChannel);
        
        return channelMap;
    }

    /**
     * Redis连接工厂 - 连接嵌入式Redis
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        org.springframework.data.redis.connection.RedisStandaloneConfiguration config =
            new org.springframework.data.redis.connection.RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6370);
        config.setDatabase(0);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * RedisTemplate配置 - 支持LocalDateTime序列化
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * MeterRegistry Mock - 简单实现用于测试
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * PaymentMetrics Mock - 空操作实现
     */
    @Bean
    @Primary
    public PaymentMetrics paymentMetrics(MeterRegistry meterRegistry) {
        return new NoOpPaymentMetrics(meterRegistry);
    }

    /**
     * PaymentMetrics空操作实现类 - 用于测试环境
     */
    public static class NoOpPaymentMetrics extends PaymentMetrics {
        public NoOpPaymentMetrics(MeterRegistry meterRegistry) {
            super(meterRegistry);
        }

        @Override
        public void recordPaymentRequest() {
            // No-op for testing
        }

        @Override
        public void recordPaymentSuccess(String channel, Long amount) {
            // No-op for testing
        }

        @Override
        public void recordPaymentFailure(String channel) {
            // No-op for testing
        }

        @Override
        public void recordCallbackReceived() {
            // No-op for testing
        }

        @Override
        public void recordCallbackSuccess() {
            // No-op for testing
        }

        @Override
        public void recordCallbackFailure() {
            // No-op for testing
        }

        @Override
        public void recordRefundRequest() {
            // No-op for testing
        }

        @Override
        public void recordRefundSuccess(Long amount) {
            // No-op for testing
        }

        @Override
        public void recordRefundFailure() {
            // No-op for testing
        }
    }
}
