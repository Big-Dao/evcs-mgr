package com.evcs.payment.service.channel;

import com.evcs.payment.config.PaymentConfig;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.http.DefaultHttpClientBuilder;
import com.wechat.pay.java.core.http.HttpClient;

import java.util.Objects;

final class WechatPayHttpClientFactory {

    private WechatPayHttpClientFactory() {
    }

    static HttpClient buildHttpClient(Config config, PaymentConfig.WechatConfig wechatConfig) {
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(wechatConfig, "wechatConfig must not be null");

        return new DefaultHttpClientBuilder()
            .config(config)
            .connectTimeoutMs(Math.max(1, wechatConfig.getConnectTimeoutMs()))
            .readTimeoutMs(Math.max(1, wechatConfig.getReadTimeoutMs()))
            .writeTimeoutMs(Math.max(1, wechatConfig.getWriteTimeoutMs()))
            // Avoid OkHttp hidden retries; retries should be governed by Resilience4j.
            .disableRetryOnConnectionFailure()
            .build();
    }
}
