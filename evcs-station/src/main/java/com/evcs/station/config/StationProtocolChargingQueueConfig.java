package com.evcs.station.config;

import com.evcs.protocol.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class StationProtocolChargingQueueConfig {

    /**
     * Station-dedicated charging queue.
     *
     * IMPORTANT: Order service consumes {@link RabbitMQConfig#CHARGING_QUEUE}. Station must use a different queue
     * to avoid competing consumption and losing events.
     */
    public static final String STATION_CHARGING_QUEUE = "evcs.station.protocol.charging";

    @Bean
    public TopicExchange stationProtocolExchange() {
        return ExchangeBuilder
            .topicExchange(RabbitMQConfig.PROTOCOL_EXCHANGE)
            .durable(true)
            .build();
    }

    @Bean
    public DirectExchange stationProtocolDlxExchange() {
        return ExchangeBuilder
            .directExchange(RabbitMQConfig.DLX_EXCHANGE)
            .durable(true)
            .build();
    }

    @Bean
    public Queue stationChargingQueue() {
        return QueueBuilder
            .durable(STATION_CHARGING_QUEUE)
            .withArgument("x-dead-letter-exchange", RabbitMQConfig.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "dlx")
            .build();
    }

    @Bean
    public Binding stationChargingStartBinding(Queue stationChargingQueue, TopicExchange stationProtocolExchange) {
        log.info("Binding station charging queue to routing key: {}", RabbitMQConfig.CHARGING_START_ROUTING_KEY);
        return BindingBuilder
            .bind(stationChargingQueue)
            .to(stationProtocolExchange)
            .with(RabbitMQConfig.CHARGING_START_ROUTING_KEY);
    }

    @Bean
    public Binding stationChargingStopBinding(Queue stationChargingQueue, TopicExchange stationProtocolExchange) {
        log.info("Binding station charging queue to routing key: {}", RabbitMQConfig.CHARGING_STOP_ROUTING_KEY);
        return BindingBuilder
            .bind(stationChargingQueue)
            .to(stationProtocolExchange)
            .with(RabbitMQConfig.CHARGING_STOP_ROUTING_KEY);
    }
}
