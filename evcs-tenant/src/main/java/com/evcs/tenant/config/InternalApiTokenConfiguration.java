package com.evcs.tenant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InternalApiTokenProperties.class)
public class InternalApiTokenConfiguration {
}
