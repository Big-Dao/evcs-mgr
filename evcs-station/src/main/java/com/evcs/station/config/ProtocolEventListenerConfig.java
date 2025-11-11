package com.evcs.station.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.protocol.api.impl.ProtocolEventListenerLoggingImpl;

/**
 * Provides a lightweight ProtocolEventListener so that protocol
 * components in evcs-protocol can start even when the full
 * protocol stack is disabled in the station service.
 */
@Configuration
public class ProtocolEventListenerConfig {

    @Bean
    @ConditionalOnMissingBean(ProtocolEventListener.class)
    public ProtocolEventListener protocolEventListener() {
        // Use the shared logging-only implementation to satisfy the dependency.
        return new ProtocolEventListenerLoggingImpl();
    }
}
