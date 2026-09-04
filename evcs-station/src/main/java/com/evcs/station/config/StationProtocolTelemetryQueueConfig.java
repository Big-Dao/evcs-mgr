package com.evcs.station.config;

import com.evcs.protocol.mq.ProtocolMqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class StationProtocolTelemetryQueueConfig {

    /**
     * Station-dedicated telemetry queue.
     *
     * Telemetry is high frequency; station must use a different queue to avoid competing consumption.
     */
    public static final String STATION_TELEMETRY_QUEUE = "evcs.station.protocol.telemetry";

    @Bean
    public Queue stationTelemetryQueue() {
        return QueueBuilder
            .durable(STATION_TELEMETRY_QUEUE)
            .withArgument("x-dead-letter-exchange", ProtocolMqConstants.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "dlx")
            .build();
    }

    @Bean
    public Binding stationTelemetryBinding(Queue stationTelemetryQueue, TopicExchange stationProtocolExchange) {
        log.info("Binding station telemetry queue to routing key: {}", ProtocolMqConstants.TELEMETRY_ROUTING_KEY);
        return BindingBuilder
            .bind(stationTelemetryQueue)
            .to(stationProtocolExchange)
            .with(ProtocolMqConstants.TELEMETRY_ROUTING_KEY);
    }
}
