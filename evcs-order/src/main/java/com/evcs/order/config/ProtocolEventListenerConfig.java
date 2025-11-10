package com.evcs.order.config;

import com.evcs.protocol.api.ProtocolEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

@Configuration
public class ProtocolEventListenerConfig {

    private static final Logger log = LoggerFactory.getLogger(ProtocolEventListenerConfig.class);

    @Bean
    @Primary
    public ProtocolEventListener protocolEventListener() {
        return new ProtocolEventListener() {
            @Override
            public void onHeartbeat(Long chargerId, LocalDateTime time) {
                log.debug("[OrderService] Heartbeat chargerId={} time={}", chargerId, time);
            }

            @Override
            public void onStatusChange(Long chargerId, Integer status) {
                log.debug("[OrderService] Status change chargerId={} status={}", chargerId, status);
            }

            @Override
            public void onError(Long chargerId, String code, String message) {
                log.warn("[OrderService] Protocol error chargerId={} code={} message={}", chargerId, code, message);
            }
        };
    }
}
