package com.evcs.station.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 协议服务注入已移除，避免Object类型导致的bean冲突
     *
     * 原代码问题：
     * @Autowired(required = false) private Object ocppService;
     *
     * 这会导致Spring尝试注入任何Object类型的bean，
     * 引发NoUniqueBeanDefinitionException错误
     *
     * Week 9 TODO:
     * - 重新设计protocol模块依赖方式
     * - 考虑使用接口隔离或API调用
     * - 恢复协议事件监听功能
     */

    @PostConstruct
    public void init() {
        log.info(
            "ProtocolConfig initialized - Protocol services temporarily disabled"
        );
        log.info("Running in standalone mode (no protocol event processing)");
        log.info("Scheduled for re-enablement in Week 9");
    }
}
