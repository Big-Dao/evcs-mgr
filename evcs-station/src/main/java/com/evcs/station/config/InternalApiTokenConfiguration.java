package com.evcs.station.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration("stationInternalApiTokenConfiguration")
@EnableConfigurationProperties(InternalApiTokenProperties.class)
public class InternalApiTokenConfiguration {
}
