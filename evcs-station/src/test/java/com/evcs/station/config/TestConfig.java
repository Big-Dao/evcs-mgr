package com.evcs.station.config;

import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.IOCPPProtocolService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 测试配置类
 * 提供测试所需的Mock Bean
 */
@TestConfiguration
public class TestConfig {

    /**
     * Mock OCPP协议服务
     */
    @Bean
    @Primary
    public IOCPPProtocolService ocppProtocolService() {
        return mock(IOCPPProtocolService.class);
    }

    /**
     * Mock CloudCharge协议服务
     */
    @Bean
    @Primary
    public ICloudChargeProtocolService cloudChargeProtocolService() {
        return mock(ICloudChargeProtocolService.class);
    }
}
