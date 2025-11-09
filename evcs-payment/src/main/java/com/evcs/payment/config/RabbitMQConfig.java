package com.evcs.payment.config;

import lombok.Data;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQ配置
 */
@Configuration
@ConfigurationProperties(prefix = "evcs.payment.rabbitmq")
@Data
@Profile("!test")  // 在test profile下不加载此配置
public class RabbitMQConfig {

    /**
     * 是否启用RabbitMQ
     */
    private boolean enabled = false;

    /**
     * 交换机配置
     */
    private ExchangeConfig exchange = new ExchangeConfig();

    /**
     * 队列配置
     */
    private QueueConfig queue = new QueueConfig();

    /**
     * 路由键配置
     */
    private RoutingKeyConfig routingKey = new RoutingKeyConfig();

    /**
     * 交换机配置
     */
    @Data
    public static class ExchangeConfig {
        private String paymentDirect = "payment.direct.exchange";
        private String paymentTopic = "payment.topic.exchange";
        private String paymentFanout = "payment.fanout.exchange";
        private String paymentDlx = "payment.dlx.exchange"; // 死信交换机
    }

    /**
     * 队列配置
     */
    @Data
    public static class QueueConfig {
        private String paymentSuccess = "payment.success.queue";
        private String paymentFailure = "payment.failure.queue";
        private String refundSuccess = "refund.success.queue";
        private String paymentCallback = "payment.callback.queue";
        private String paymentDlq = "payment.dlq"; // 死信队列
        private int retryDelay = 60000; // 重试延迟时间（毫秒）
        private int maxRetries = 3; // 最大重试次数
    }

    /**
     * 路由键配置
     */
    @Data
    public static class RoutingKeyConfig {
        private String paymentSuccess = "payment.success";
        private String paymentFailure = "payment.failure";
        private String refundSuccess = "refund.success";
        private String paymentCallback = "payment.callback";
    }

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());

        // 开启发送确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                // 消息发送成功
            } else {
                // 消息发送失败，记录日志
                System.err.println("消息发送失败: " + cause);
            }
        });

        // 开启返回确认
        template.setReturnsCallback(returned -> {
            // 消息无法路由时回调
            System.err.println("消息无法路由: " + returned.getReplyText());
        });

        return template;
    }

    /**
     * 监听器容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());

        // 设置并发消费者数量
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        // 设置预取数量
        factory.setPrefetchCount(1);

        // 开启手动确认
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        return factory;
    }

    // ==================== 交换机配置 ====================

    @Bean
    public DirectExchange paymentDirectExchange() {
        return ExchangeBuilder.directExchange(exchange.getPaymentDirect())
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange paymentTopicExchange() {
        return ExchangeBuilder.topicExchange(exchange.getPaymentTopic())
                .durable(true)
                .build();
    }

    @Bean
    public FanoutExchange paymentFanoutExchange() {
        return ExchangeBuilder.fanoutExchange(exchange.getPaymentFanout())
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange paymentDlxExchange() {
        return ExchangeBuilder.directExchange(exchange.getPaymentDlx())
                .durable(true)
                .build();
    }

    // ==================== 队列配置 ====================

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(queue.getPaymentSuccess())
                .withArgument("x-dead-letter-exchange", exchange.getPaymentDlx())
                .withArgument("x-dead-letter-routing-key", queue.getPaymentDlq())
                .build();
    }

    @Bean
    public Queue paymentFailureQueue() {
        return QueueBuilder.durable(queue.getPaymentFailure())
                .withArgument("x-dead-letter-exchange", exchange.getPaymentDlx())
                .withArgument("x-dead-letter-routing-key", queue.getPaymentDlq())
                .build();
    }

    @Bean
    public Queue refundSuccessQueue() {
        return QueueBuilder.durable(queue.getRefundSuccess())
                .withArgument("x-dead-letter-exchange", exchange.getPaymentDlx())
                .withArgument("x-dead-letter-routing-key", queue.getPaymentDlq())
                .build();
    }

    @Bean
    public Queue paymentCallbackQueue() {
        return QueueBuilder.durable(queue.getPaymentCallback())
                .withArgument("x-dead-letter-exchange", exchange.getPaymentDlx())
                .withArgument("x-dead-letter-routing-key", queue.getPaymentDlq())
                .build();
    }

    @Bean
    public Queue paymentDeadLetterQueue() {
        return QueueBuilder.durable(queue.getPaymentDlq()).build();
    }

    // ==================== 绑定配置 ====================

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue())
                .to(paymentDirectExchange())
                .with(routingKey.getPaymentSuccess());
    }

    @Bean
    public Binding paymentFailureBinding() {
        return BindingBuilder.bind(paymentFailureQueue())
                .to(paymentDirectExchange())
                .with(routingKey.getPaymentFailure());
    }

    @Bean
    public Binding refundSuccessBinding() {
        return BindingBuilder.bind(refundSuccessQueue())
                .to(paymentDirectExchange())
                .with(routingKey.getRefundSuccess());
    }

    @Bean
    public Binding paymentCallbackBinding() {
        return BindingBuilder.bind(paymentCallbackQueue())
                .to(paymentDirectExchange())
                .with(routingKey.getPaymentCallback());
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(paymentDeadLetterQueue())
                .to(paymentDlxExchange())
                .with(queue.getPaymentDlq());
    }

    // ==================== 广播绑定 ====================

    @Bean
    public Binding paymentSuccessFanoutBinding() {
        return BindingBuilder.bind(paymentSuccessQueue())
                .to(paymentFanoutExchange());
    }

    @Bean
    public Binding paymentFailureFanoutBinding() {
        return BindingBuilder.bind(paymentFailureQueue())
                .to(paymentFanoutExchange());
    }
}