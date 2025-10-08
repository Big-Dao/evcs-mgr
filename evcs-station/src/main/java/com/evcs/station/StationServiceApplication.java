package com.evcs.station;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 充电站服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.evcs")
@EnableDiscoveryClient
@EnableTransactionManagement
@MapperScan("com.evcs.station.mapper")
public class StationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StationServiceApplication.class, args);
    }
}