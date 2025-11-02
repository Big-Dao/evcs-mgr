package com.evcs.protocol.config;

import com.evcs.protocol.enums.ProtocolType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 协议配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "evcs.protocol")
public class ProtocolProperties {

    /**
     * 是否启用协议服务
     */
    private boolean enabled = true;

    /**
     * 默认协议类型
     */
    private ProtocolType defaultProtocol = ProtocolType.OCPP;

    /**
     * OCPP协议配置
     */
    private OCPPConfig ocpp = new OCPPConfig();

    /**
     * 云快充协议配置
     */
    private CloudChargeConfig cloudCharge = new CloudChargeConfig();

    /**
     * 连接池配置
     */
    private ConnectionPoolConfig connectionPool = new ConnectionPoolConfig();

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * OCPP协议配置
     */
    @Data
    public static class OCPPConfig {
        /**
         * 是否启用OCPP协议
         */
        private boolean enabled = true;

        /**
         * WebSocket端口
         */
        private int port = 8088;

        /**
         * 心跳间隔（秒）
         */
        private int heartbeatInterval = 60;

        /**
         * 连接超时时间（秒）
         */
        private int connectionTimeout = 30;

        /**
         * 消息超时时间（秒）
         */
        private int messageTimeout = 10;

        /**
         * 最大连接数
         */
        private int maxConnections = 1000;

        /**
         * OCPP版本
         */
        private String version = "1.6";
    }

    /**
     * 云快充协议配置
     */
    @Data
    public static class CloudChargeConfig {
        /**
         * 是否启用云快充协议
         */
        private boolean enabled = true;

        /**
         * API基础URL
         */
        private String baseUrl = "https://api.cloudcharge.com";

        /**
         * API版本
         */
        private String apiVersion = "3.0";

        /**
         * 应用ID
         */
        private String appId;

        /**
         * 应用密钥
         */
        private String appSecret;

        /**
         * 签名算法
         */
        private String signAlgorithm = "HMAC-SHA256";

        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 10000;

        /**
         * 读取超时时间（毫秒）
         */
        private int readTimeout = 30000;

        /**
         * 厂商配置
         */
        private Map<String, VendorConfig> vendors;
    }

    /**
     * 厂商配置
     */
    @Data
    public static class VendorConfig {
        /**
         * 厂商名称
         */
        private String name;

        /**
         * 厂商API地址
         */
        private String apiUrl;

        /**
         * 厂商密钥
         */
        private String secret;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 扩展配置
         */
        private Map<String, Object> extra;
    }

    /**
     * 连接池配置
     */
    @Data
    public static class ConnectionPoolConfig {
        /**
         * 最大连接数
         */
        private int maxTotal = 200;

        /**
         * 每个路由的最大连接数
         */
        private int maxPerRoute = 50;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 5000;

        /**
         * 连接请求超时时间（毫秒）
         */
        private int connectionRequestTimeout = 10000;

        /**
         * Socket超时时间（毫秒）
         */
        private int socketTimeout = 30000;

        /**
         * 连接在池中保持空闲而不被回收的最小时间（毫秒）
         */
        private int validateAfterInactivity = 2000;
    }

    /**
     * 重试配置
     */
    @Data
    public static class RetryConfig {
        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 重试间隔（毫秒）
         */
        private int delay = 1000;

        /**
         * 重试间隔倍数
         */
        private double multiplier = 2.0;

        /**
         * 最大重试间隔（毫秒）
         */
        private int maxDelay = 10000;
    }
}