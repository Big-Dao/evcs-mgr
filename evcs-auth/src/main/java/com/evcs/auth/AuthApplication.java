package com.evcs.auth;

import com.evcs.auth.config.FeignContextPropagationConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证服务启动类 - 极简版本
 */
@SpringBootApplication(scanBasePackages = {"com.evcs.auth", "com.evcs.common"})
@MapperScan("com.evcs.auth.mapper")
@EnableDiscoveryClient
@EnableFeignClients(
    basePackages = "com.evcs.auth.client",
    defaultConfiguration = FeignContextPropagationConfiguration.class
)
public class AuthApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
