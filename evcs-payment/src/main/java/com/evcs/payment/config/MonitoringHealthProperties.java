package com.evcs.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 支付监控健康检查配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "evcs.payment.monitoring.health")
public class MonitoringHealthProperties {

    /**
     * 数据库校验查询
     */
    private String databaseValidationQuery = "SELECT 1";

    /**
     * 外部API健康检查配置
     */
    private Api api = new Api();

    @Data
    public static class Api {
        /**
         * 健康检查端点
         */
        private List<String> endpoints = new ArrayList<>();

        /**
         * 请求超时时间（毫秒）
         */
        private long timeoutMs = 3000;

        /**
         * 响应期望关键字（可选）
         */
        private String expectedResponseKeyword = "";
    }
}
