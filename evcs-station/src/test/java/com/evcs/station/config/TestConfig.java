package com.evcs.station.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * 测试配置类
 *
 * 注意：由于主代码使用了Optional依赖（@Autowired(required = false)），
 * 不再需要为Protocol服务提供Mock Bean。
 * 服务层会在协议服务不可用时优雅降级。
 *
 * 如果特定测试需要测试协议交互，可以在测试类中单独创建Mock。
 */
@TestConfiguration
public class TestConfig {
    // Protocol服务的Mock已移除
    // 主代码使用Optional依赖，测试时会自动处理null情况
    // 如果需要添加其他测试专用的Bean，可以在这里定义
}
