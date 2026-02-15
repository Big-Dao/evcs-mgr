package com.evcs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = ConfigServiceApplication.class,
    properties = {
        "spring.profiles.active=native",
        "spring.cloud.config.server.native.search-locations=classpath:/config",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
    }
)
@DisplayName("配置中心服务启动测试")
class ConfigServiceApplicationTest {

    @Test
    @DisplayName("应用上下文 - 应成功加载")
    void testContextLoads() {
    }
}
