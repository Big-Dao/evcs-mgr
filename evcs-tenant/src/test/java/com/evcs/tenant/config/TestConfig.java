package com.evcs.tenant.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * 测试配置类
 * 排除Knife4j自动配置以避免javax.servlet依赖问题
 *
 * @author EVCS
 * @since 2025-01-07
 */
@TestConfiguration
@EnableAutoConfiguration(
    excludeName = "com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration"
)
public class TestConfig {
    // 测试环境不需要Knife4j API文档功能
}
