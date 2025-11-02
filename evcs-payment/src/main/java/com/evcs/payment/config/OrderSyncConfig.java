package com.evcs.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 订单同步配置
 *
 * 控制支付成功后订单状态同步的行为
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "evcs.payment.order-sync")
public class OrderSyncConfig {

    /**
     * 是否启用直接API调用
     */
    private boolean directApiEnabled = true;

    /**
     * 订单服务URL
     */
    private String orderServiceUrl = "http://localhost:8083/api";

    /**
     * API调用超时时间（毫秒）
     */
    private int apiTimeoutMs = 5000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 重试间隔（毫秒）
     */
    private long retryIntervalMs = 1000;

    /**
     * 是否启用幂等性检查
     */
    private boolean idempotencyCheckEnabled = true;

    /**
     * 是否启用降级处理
     */
    private boolean fallbackEnabled = true;

    /**
     * 是否记录同步日志
     */
    private boolean syncLoggingEnabled = true;

    @Bean
    public RestTemplate orderSyncRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 配置超时时间
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) apiTimeoutMs);
        factory.setReadTimeout((int) apiTimeoutMs);
        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }
}