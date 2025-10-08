package com.evcs.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AuthApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}