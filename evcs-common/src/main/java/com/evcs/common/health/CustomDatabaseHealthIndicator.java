package com.evcs.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库健康检查
 * 检查数据库连接和查询性能
 */
@Slf4j
@Component
@ConditionalOnBean(DataSource.class)
public class CustomDatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public CustomDatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                
                if (resultSet.next()) {
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    return Health.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("responseTime", responseTime + "ms")
                            .withDetail("status", "Connected")
                            .build();
                }
            }
            
            return Health.down()
                    .withDetail("error", "No response from database")
                    .build();
                    
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
