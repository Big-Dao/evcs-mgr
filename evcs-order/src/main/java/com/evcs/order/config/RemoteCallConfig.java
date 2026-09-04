package com.evcs.order.config;

import com.evcs.common.http.ContextPropagationClientHttpRequestInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 服务间远程调用共用的负载均衡 RestTemplate（传播租户/追踪上下文）。
 */
@Configuration("orderRemoteCallConfig")
public class RemoteCallConfig {

    @Bean("orderRemoteServiceRestTemplate")
    @LoadBalanced
    RestTemplate remoteServiceRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new ContextPropagationClientHttpRequestInterceptor());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }
}
