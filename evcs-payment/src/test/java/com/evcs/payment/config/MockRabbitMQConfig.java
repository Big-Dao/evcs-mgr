package com.evcs.payment.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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
     * Mock RabbitTemplate
     */
    @Bean
    @Primary
    public RabbitTemplate mockRabbitTemplate() {
        return new MockRabbitTemplate();
    }

    /**
     * Mock RabbitTemplate实现
     */
    public static class MockRabbitTemplate extends RabbitTemplate {
        @Override
        public void convertAndSend(String exchange, String routingKey, Object message) {
            // 在测试环境中，只是打印日志而不真正发送消息
            System.out.println("Mock RabbitMQ - 发送消息: exchange=" + exchange + ", routingKey=" + routingKey + ", message=" + message);
        }
    }
}