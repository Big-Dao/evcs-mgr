package com.evcs.protocol.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 * 配置协议事件的交换机、队列和绑定关系
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // 交换机名称
    public static final String PROTOCOL_EXCHANGE = "evcs.protocol.events";
    
    // 队列名称
    public static final String HEARTBEAT_QUEUE = "evcs.protocol.heartbeat";
    public static final String STATUS_QUEUE = "evcs.protocol.status";
    public static final String CHARGING_QUEUE = "evcs.protocol.charging";
    
    // 死信交换机和队列
    public static final String DLX_EXCHANGE = "evcs.protocol.dlx";
    public static final String DLX_QUEUE = "evcs.protocol.dlx.queue";
    
    // 路由键
    public static final String HEARTBEAT_ROUTING_KEY = "protocol.heartbeat.*";
    public static final String STATUS_ROUTING_KEY = "protocol.status.*";
    public static final String CHARGING_START_ROUTING_KEY = "protocol.charging.start";
    public static final String CHARGING_STOP_ROUTING_KEY = "protocol.charging.stop";

    /**
     * 声明Topic类型交换机
     */
    @Bean
    public TopicExchange protocolExchange() {
        return ExchangeBuilder
                .topicExchange(PROTOCOL_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明死信交换机
     */
    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder
                .directExchange(DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明心跳队列
     */
    @Bean
    public Queue heartbeatQueue() {
        return QueueBuilder
                .durable(HEARTBEAT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlx")
                .build();
    }

    /**
     * 声明状态队列
     */
    @Bean
    public Queue statusQueue() {
        return QueueBuilder
                .durable(STATUS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlx")
                .build();
    }

    /**
     * 声明充电事件队列
     */
    @Bean
    public Queue chargingQueue() {
        return QueueBuilder
                .durable(CHARGING_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlx")
                .build();
    }

    /**
     * 声明死信队列
     */
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    /**
     * 绑定心跳队列到交换机
     */
    @Bean
    public Binding heartbeatBinding(Queue heartbeatQueue, TopicExchange protocolExchange) {
        return BindingBuilder
                .bind(heartbeatQueue)
                .to(protocolExchange)
                .with(HEARTBEAT_ROUTING_KEY);
    }

    /**
     * 绑定状态队列到交换机
     */
    @Bean
    public Binding statusBinding(Queue statusQueue, TopicExchange protocolExchange) {
        return BindingBuilder
                .bind(statusQueue)
                .to(protocolExchange)
                .with(STATUS_ROUTING_KEY);
    }

    /**
     * 绑定充电队列到交换机（开始充电）
     */
    @Bean
    public Binding chargingStartBinding(Queue chargingQueue, TopicExchange protocolExchange) {
        return BindingBuilder
                .bind(chargingQueue)
                .to(protocolExchange)
                .with(CHARGING_START_ROUTING_KEY);
    }

    /**
     * 绑定充电队列到交换机（停止充电）
     */
    @Bean
    public Binding chargingStopBinding(Queue chargingQueue, TopicExchange protocolExchange) {
        return BindingBuilder
                .bind(chargingQueue)
                .to(protocolExchange)
                .with(CHARGING_STOP_ROUTING_KEY);
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder
                .bind(dlxQueue)
                .to(dlxExchange)
                .with("dlx");
    }

    /**
     * 配置消息转换器（使用JSON）
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate
     * 启用发布确认和消息持久化
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        
        // 启用发布确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Message sent successfully, correlationData: {}", correlationData);
            } else {
                log.error("Message send failed, correlationData: {}, cause: {}", correlationData, cause);
            }
        });
        
        // 启用返回回调（消息无法路由时触发）
        template.setReturnsCallback(returned -> {
            log.error("Message returned: {}, replyCode: {}, replyText: {}, exchange: {}, routingKey: {}",
                    returned.getMessage(), returned.getReplyCode(), returned.getReplyText(),
                    returned.getExchange(), returned.getRoutingKey());
        });
        
        template.setMandatory(true);
        
        return template;
    }

    /**
     * 配置消息监听容器工厂
     * 启用手动确认模式
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        
        // 设置手动确认模式
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        
        // 设置并发消费者数量
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        
        // 设置预取数量
        factory.setPrefetchCount(10);
        
        return factory;
    }
}
