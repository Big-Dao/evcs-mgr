package com.evcs.station.config;

import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.IOCPPProtocolService;
import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.station.protocol.ProtocolEventListenerImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ProtocolConfig {
    private final IOCPPProtocolService ocppService;
    private final ICloudChargeProtocolService cloudService;
    private final ProtocolEventListenerImpl listenerImpl;

    @Bean
    public ProtocolEventListener protocolEventListener() {
        return listenerImpl;
    }

    @PostConstruct
    public void init() {
        ProtocolEventListener listener = listenerImpl;
        ocppService.setEventListener(listener);
        cloudService.setEventListener(listener);
    }
}
