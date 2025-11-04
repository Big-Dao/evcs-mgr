package com.evcs.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 安全配置类
 * 简化版的安全配置，专注于基本功能
 * 主要安全配置已移至GatewaySecurityConfig类
 */
@Slf4j
@Configuration
public class SecurityConfig {
    // CORS和路由配置已移至其他配置类，避免Bean名称冲突
    // 主要安全功能在GatewaySecurityConfig中实现
}