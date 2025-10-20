package com.evcs.station.config;

import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.IOCPPProtocolService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 协议配置类
 * 使用Optional处理evcs-protocol模块的依赖，避免在模块不可用时出现编译错误
 */
@Slf4j
@Configuration
public class ProtocolConfig {

    @Autowired(required = false)
    private IOCPPProtocolService ocppService;

    @Autowired(required = false)
    private ICloudChargeProtocolService cloudService;

    @PostConstruct
    public void init() {
        // Protocol event listener functionality disabled temporarily
        // Will be re-enabled in Week 9 (Protocol Enhancement phase)
        if (ocppService != null || cloudService != null) {
            log.info(
                "Protocol services available, but event listener disabled for now"
            );
        } else {
            log.info(
                "Protocol services not available, running in standalone mode"
            );
        }
    }
}
