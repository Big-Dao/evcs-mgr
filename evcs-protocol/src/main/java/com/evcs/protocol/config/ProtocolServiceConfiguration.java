package com.evcs.protocol.config;

import com.evcs.protocol.api.ProtocolManager;
import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.protocol.enums.ProtocolType;
import com.evcs.protocol.service.impl.ProtocolManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 协议服务配置类
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ProtocolProperties.class)
public class ProtocolServiceConfiguration {

    private final ProtocolProperties protocolProperties;

    /**
     * 协议管理器Bean
     */
    @Bean
    public ProtocolManager protocolManager(List<ProtocolEventListener> eventListeners) {
        log.info("Initializing protocol manager with default protocol: {}", protocolProperties.getDefaultProtocol());

        ProtocolManagerImpl protocolManager = new ProtocolManagerImpl(protocolProperties);

        // 设置事件监听器
        if (!eventListeners.isEmpty()) {
            // 如果有多个监听器，使用第一个作为全局监听器
            // 实际应用中可能需要更复杂的事件分发机制
            protocolManager.setEventListener(eventListeners.get(0));
            log.info("Set protocol event listener: {}", eventListeners.get(0).getClass().getSimpleName());
        }

        return protocolManager;
    }

    /**
     * REST模板Bean（用于云快充协议的HTTP调用）
     */
    @Bean
    @ConditionalOnProperty(name = "evcs.protocol.cloud-charge.enabled", havingValue = "true", matchIfMissing = true)
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 可以在这里添加拦截器、消息转换器等配置
        // restTemplate.setInterceptors(List.of(new CloudChargeInterceptor()));
        // restTemplate.setMessageConverters(...);

        log.info("RestTemplate configured for CloudCharge protocol");
        return restTemplate;
    }

    /**
     * 协议服务初始化
     */
    @Bean
    @ConditionalOnProperty(name = "evcs.protocol.enabled", havingValue = "true", matchIfMissing = true)
    public ProtocolServiceInitializer protocolServiceInitializer(
            ProtocolManager protocolManager,
            List<com.evcs.protocol.api.IProtocolService> protocolServices) {

        log.info("Initializing protocol services...");

        // 注册所有协议服务
        for (com.evcs.protocol.api.IProtocolService protocolService : protocolServices) {
            ProtocolType protocolType = protocolService.getSupportedProtocolType();
            protocolManager.registerProtocolService(protocolType, protocolService);
            log.info("Registered protocol service: {} -> {}", protocolType, protocolService.getClass().getSimpleName());
        }

        log.info("Protocol services initialization completed. Registered protocols: {}",
                protocolManager.getRegisteredProtocolTypes());

        return new ProtocolServiceInitializer(protocolManager);
    }

    /**
     * 协议服务初始化器
     * 负责在服务启动后执行初始化操作
     */
    @RequiredArgsConstructor
    public static class ProtocolServiceInitializer {

        private final ProtocolManager protocolManager;

        /**
         * 执行初始化操作
         */
        public void initialize() {
            log.info("Protocol service initializer started");

            // 这里可以添加启动时的初始化逻辑
            // 例如：加载设备配置、检查连接状态、启动定时任务等
            if (protocolManager != null) {
                log.debug("Protocol manager initialized: {}", protocolManager.getClass().getSimpleName());
            }

            log.info("Protocol service initializer completed");
        }
    }
}