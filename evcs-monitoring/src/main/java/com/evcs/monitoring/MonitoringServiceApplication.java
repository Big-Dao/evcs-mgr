package com.evcs.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Entry point for the monitoring and alert service.
 */
@SpringBootApplication(
    scanBasePackages = "com.evcs",
    exclude = {SecurityAutoConfiguration.class}
)
@EnableDiscoveryClient
public class MonitoringServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringServiceApplication.class, args);
    }
}
