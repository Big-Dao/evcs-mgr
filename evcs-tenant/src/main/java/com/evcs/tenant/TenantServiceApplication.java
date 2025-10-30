package com.evcs.tenant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 租户服务启动类
 */
@SpringBootApplication(
    scanBasePackages = "com.evcs",
    exclude = {SecurityAutoConfiguration.class}
)
@EnableDiscoveryClient
@MapperScan("com.evcs.tenant.mapper")
public class TenantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantServiceApplication.class, args);
    }
}