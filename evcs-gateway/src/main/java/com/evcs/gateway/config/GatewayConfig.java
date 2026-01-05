package com.evcs.gateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Objects;

/**
 * Gateway负载均衡配置
 * 优化WebClient连接设置，提供稳定的负载均衡支持
 */
@Configuration
public class GatewayConfig {

    /**
     * 配置负载均衡的WebClient，优化连接超时设置
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder(
                        @NonNull ExchangeFilterFunction contextPropagationExchangeFilterFunction
    ) {
        HttpClient httpClient = Objects.requireNonNull(
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                        .responseTimeout(Duration.ofSeconds(30))
                        .keepAlive(true)
        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(Objects.requireNonNull(contextPropagationExchangeFilterFunction));
    }

    /**
     * 配置普通WebClient用于外部调用
     */
    @Bean
    public WebClient.Builder webClientBuilder(
                        @NonNull ExchangeFilterFunction contextPropagationExchangeFilterFunction
    ) {
        HttpClient httpClient = Objects.requireNonNull(
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                        .responseTimeout(Duration.ofSeconds(30))
                        .keepAlive(true)
        );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(Objects.requireNonNull(contextPropagationExchangeFilterFunction));
    }
}