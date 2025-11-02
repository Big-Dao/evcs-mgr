package com.evcs.payment.config;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;

/**
 * RabbitMQ测试配置
 * 提供测试环境的RabbitMQ配置和Mock对象
 */
@TestConfiguration
@EnableRabbit
@Profile("test")
public class RabbitMQTestConfig {

    /**
     * 测试环境使用的RabbitTemplate
     * 使用标准RabbitTemplate，在测试环境中配置更短的超时时间
     */
    @Bean
    @Primary
    public RabbitTemplate testRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        // 设置基础配置
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReplyTimeout(2000);
        rabbitTemplate.setReceiveTimeout(2000);

        return rabbitTemplate;
    }

    /**
     * 测试环境的监听器容器工厂
     * 配置更短的超时时间以加速测试
     */
    @Bean
    @Primary
    public SimpleRabbitListenerContainerFactory testRabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        factory.setReceiveTimeout(2000L);

        // 测试环境不需要自动启动
        factory.setAutoStartup(false);

        return factory;
    }
}