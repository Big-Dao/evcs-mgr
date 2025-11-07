package com.evcs.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 安全的CORS配置
 * 修复了原有的安全风险，使用白名单模式
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:Content-Type,Authorization,X-Request-Id,X-Tenant-Id}")
    private String allowedHeaders;

    @Value("${cors.exposed-headers:X-Request-Id,X-Total-Count}")
    private String exposedHeaders;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public CorsWebFilter corsWebFilter(GlobalCorsProperties globalCorsProperties) {
        log.info("Initializing secure CORS configuration with allowed origins: {}", allowedOrigins);

        CorsConfiguration config = new CorsConfiguration();

        // 安全的Origin配置 - 只允许配置的域名
        allowedOrigins.forEach(origin -> {
            if (isValidOrigin(origin)) {
                config.addAllowedOrigin(origin);
                log.debug("Added allowed origin: {}", origin);
            } else {
                log.warn("Invalid origin format, skipping: {}", origin);
            }
        });

        // 明确允许的HTTP方法
        Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .filter(method -> !method.isEmpty())
                .forEach(method -> {
                    config.addAllowedMethod(method.toUpperCase());
                    log.debug("Added allowed method: {}", method);
                });

        // 明确允许的请求头
        Arrays.stream(allowedHeaders.split(","))
                .map(String::trim)
                .filter(header -> !header.isEmpty())
                .forEach(header -> {
                    config.addAllowedHeader(header);
                    log.debug("Added allowed header: {}", header);
                });

        // 暴露的响应头
        Arrays.stream(exposedHeaders.split(","))
                .map(String::trim)
                .filter(header -> !header.isEmpty())
                .forEach(header -> {
                    config.addExposedHeader(header);
                    log.debug("Added exposed header: {}", header);
                });

        // 预检请求缓存时间
        config.setMaxAge(java.time.Duration.ofSeconds(maxAge));

        // 是否允许携带凭证
        config.setAllowCredentials(allowCredentials);

        // 验证配置有效性
        validateCorsConfiguration(config);

        // 创建配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 为不同路径配置不同的CORS策略
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/actuator/**", createActuatorCorsConfig());
        source.registerCorsConfiguration("/swagger-ui/**", createSwaggerCorsConfig());
        source.registerCorsConfiguration("/v3/api-docs/**", createSwaggerCorsConfig());

        log.info("CORS configuration initialized successfully");
        return new CorsWebFilter(source);
    }

    /**
     * 为Actuator端点创建严格的CORS配置
     */
    private CorsConfiguration createActuatorCorsConfig() {
        CorsConfiguration config = new CorsConfiguration();

        // Actuator端点通常只允许内网访问
        config.addAllowedOrigin("http://localhost:8080");
        config.addAllowedOrigin("http://localhost:3000");

        config.addAllowedMethod("GET");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("X-Request-Id");

        config.setAllowCredentials(false);
        config.setMaxAge(java.time.Duration.ofSeconds(300)); // 5分钟缓存

        return config;
    }

    /**
     * 为Swagger文档创建宽松的CORS配置
     */
    private CorsConfiguration createSwaggerCorsConfig() {
        CorsConfiguration config = new CorsConfiguration();

        // Swagger文档可以允许更多源访问
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");

        config.addAllowedMethod("GET");
        config.addAllowedMethod("OPTIONS");

        config.addAllowedHeader("*");

        config.setAllowCredentials(false);
        config.setMaxAge(java.time.Duration.ofSeconds(3600));

        return config;
    }

    /**
     * 验证Origin格式是否有效
     */
    private boolean isValidOrigin(String origin) {
        if (origin == null || origin.trim().isEmpty()) {
            return false;
        }

        // 检查通配符模式
        if (origin.equals("*")) {
            log.warn("Wildcard origin '*' is not allowed for security reasons");
            return false;
        }

        // 检查URL格式
        try {
            if (origin.startsWith("http://") || origin.startsWith("https://")) {
                return true;
            }

            // 检查通配符域名 (如 *.example.com)
            if (origin.startsWith("*.")) {
                return origin.length() > 2 && origin.contains(".");
            }

            return false;
        } catch (Exception e) {
            log.warn("Invalid origin format: {}", origin, e);
            return false;
        }
    }

    /**
     * 验证CORS配置的完整性
     */
    private void validateCorsConfiguration(CorsConfiguration config) {
        if (config.getAllowedOrigins().isEmpty()) {
            throw new IllegalStateException("No allowed origins configured. This is a security risk.");
        }

        if (config.getAllowedMethods().isEmpty()) {
            throw new IllegalStateException("No allowed methods configured.");
        }

        if (config.getAllowCredentials() && config.getAllowedOrigins().size() > 1) {
            log.warn("WARNING: AllowCredentials is true with multiple origins. This may cause security issues.");
        }

        log.info("CORS configuration validation passed. Origins: {}, Methods: {}, Credentials: {}",
                config.getAllowedOrigins(),
                config.getAllowedMethods(),
                config.getAllowCredentials());
    }
}
