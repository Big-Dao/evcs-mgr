package com.evcs.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = MonitoringServiceApplication.class,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.cloud.config.client.ConfigClientAutoConfiguration,org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=test-jwt-secret-key-for-unit-tests-0123456789"
    }
)
@DisplayName("监控服务启动测试")
class MonitoringServiceApplicationTest {

    @Test
    @DisplayName("应用上下文 - 应成功加载")
    void testContextLoads() {
    }
}
