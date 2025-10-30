package com.evcs.protocol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(
    scanBasePackages = "com.evcs",
    exclude = {SecurityAutoConfiguration.class}
)
@EnableDiscoveryClient
public class ProtocolServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProtocolServiceApplication.class, args);
    }
}
