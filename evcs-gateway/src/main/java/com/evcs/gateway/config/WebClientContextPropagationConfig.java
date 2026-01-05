package com.evcs.gateway.config;

import com.evcs.common.http.OutgoingRequestContextHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

@Configuration
public class WebClientContextPropagationConfig {

    @Bean
    public ExchangeFilterFunction contextPropagationExchangeFilterFunction() {
        return (request, next) -> {
            var mutated = ClientRequest.from(request)
                .headers(OutgoingRequestContextHeaders::applyTo)
                .build();
            return next.exchange(mutated);
        };
    }
}
