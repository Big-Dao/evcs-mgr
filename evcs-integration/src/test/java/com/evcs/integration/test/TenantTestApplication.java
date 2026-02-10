package com.evcs.integration.test;

import com.evcs.tenant.TenantServiceApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {"com.evcs.tenant", "com.evcs.common"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TenantServiceApplication.class
        )
)
@MapperScan("com.evcs.tenant.mapper")
public class TenantTestApplication {
}
