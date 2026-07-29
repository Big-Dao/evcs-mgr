package com.evcs.protocol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = "com.evcs"
)
@EnableDiscoveryClient
@EnableScheduling
public class ProtocolServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProtocolServiceApplication.class, args);
    }
}
