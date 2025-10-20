package com.evcs.station.config;

import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.IOCPPProtocolService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 协议配置类
 *
 * 注意：Protocol服务相关的配置已临时禁用
 * - evcs-protocol模块的依赖在Day 1被改为Optional以解决编译错误
 * - 相关的MQ消费者（ChargerEventConsumer等）已被暂时禁用
 * - 计划在Week 9（协议完善阶段）重新启用并重构
 *
 * 当前状态：
 * - 协议事件流暂时断开
 * - 系统在standalone模式下运行
 * - 不影响基本的CRUD测试
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
        log.info(
            "ProtocolConfig initialized - Protocol services temporarily disabled"
        );
        log.info("Running in standalone mode (no protocol event processing)");
        log.info("Scheduled for re-enablement in Week 9");
    }
}
