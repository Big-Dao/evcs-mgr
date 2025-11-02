package com.evcs.payment.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Mock RabbitMQ配置 - 用于测试环境
 */
@TestConfiguration
@Profile("test")
public class MockRabbitMQConfig {

    /**
     * 创建Mock队列Bean
     */
    @Bean
    @Primary
    public Queue mockPaymentSuccessQueue() {
        return new Queue("mock.payment.success.queue", true);
    }

    @Bean
    @Primary
    public Queue mockPaymentFailureQueue() {
        return new Queue("mock.payment.failure.queue", true);
    }

    @Bean
    @Primary
    public Queue mockRefundSuccessQueue() {
        return new Queue("mock.refund.success.queue", true);
    }

    @Bean
    @Primary
    public Queue mockPaymentDlqQueue() {
        return new Queue("mock.payment.dlq", true);
    }

    /**
     * Mock ConnectionFactory
     */
    @Bean
    @Primary
    public ConnectionFactory mockConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");

        return factory;
    }

    /**
     * Mock RabbitTemplate
     */
    @Bean
    @Primary
    public RabbitTemplate mockRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // Mock基本方法
        rabbitTemplate.setExchange("mock.exchange");
        rabbitTemplate.setRoutingKey("mock.routing.key");

        return rabbitTemplate;
    }
}