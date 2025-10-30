# Spring Cloud 配置管理规范文档

> **文档版本**: v1.0  
> **最后更新**: 2025-10-30  
> **适用技术栈**: Spring Cloud Config + Spring Boot 2.x/3.x  
> **维护者**: @Big-Dao

---

## 目录

1. [核心原则](#核心原则)
2. [配置分类标准](#配置分类标准)
3. [文件组织结构](#文件组织结构)
4. [配置优先级](#配置优先级)
5. [命名规范](#命名规范)
6. [敏感信息处理](#敏感信息处理)
7. [动态刷新机制](#动态刷新机制)
8. [环境管理](#环境管理)
9. [代码示例](#代码示例)
10. [常见错误](#常见错误)
11. [检查清单](#检查清单)

---

## 核心原则

### 🎯 配置分离三原则

1. **静态不变的配置** → 本地 YAML
2. **环境相关的配置** → Config Server
3. **敏感信息** → Config Server + 加密

### 📏 配置管理黄金法则

```
如果配置满足以下任一条件，必须放入 Config Server：
✅ 在不同环境（dev/test/prod）有不同值
✅ 需要在运行时动态修改
✅ 包含敏感信息（密码、密钥、token）
✅ 需要在多个微服务间共享
✅ 需要版本控制和审计追踪
```

---

## 配置分类标准

### 📦 Config Server 配置清单

#### 1. 环境相关配置 (MUST)

```yaml
# ✅ 正确：放在 Config Server
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://prod-db.company.com:3306/orders
    username: prod_user
    password: '{cipher}AQATBvLSf3hNkg...'
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  redis:
    host: prod-redis.company.com
    port: 6379
    password: '{cipher}BQBCvMTg4iOlm...'
```

#### 2. 动态刷新配置 (MUST)

```yaml
# ✅ 正确：需要动态调整的配置
# order-service.yml
feature:
  express-checkout: true
  international-shipping: false
  recommendation-engine: true

business:
  order:
    max-items: 100
    timeout-minutes: 30
    
ratelimit:
  enabled: true
  requests-per-second: 100
  burst-capacity: 200
```

#### 3. 敏感信息配置 (MUST + 加密)

```yaml
# ✅ 正确：加密存储
# payment-service-prod.yml
api:
  stripe:
    secret-key: '{cipher}AQA9vJK2nM...'
    publishable-key: '{cipher}BQB8wLN3oP...'
    
  aws:
    access-key: '{cipher}CQC7xMP4qR...'
    secret-key: '{cipher}DQD6yNQ5rS...'

security:
  oauth2:
    client:
      client-id: payment-service
      client-secret: '{cipher}EQE5zOR6tT...'
```

#### 4. 跨服务共享配置 (MUST)

```yaml
# ✅ 正确：所有服务共享
# application.yml (Config Server)
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka2:8762/eureka/
    registry-fetch-interval-seconds: 5

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
      
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 📁 本地 YAML 配置清单

#### 1. 启动必需配置 (MUST)

```yaml
# ✅ 正确：bootstrap.yml (本地)
spring:
  application:
    name: order-service  # 服务标识，不可变
  cloud:
    config:
      uri: ${CONFIG_SERVER_URI:http://localhost:8888}
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 6
        multiplier: 1.1
      profile: ${SPRING_PROFILES_ACTIVE:dev}
```

#### 2. 服务元数据配置 (MUST)

```yaml
# ✅ 正确：application.yml (本地)
server:
  port: ${SERVER_PORT:8081}
  servlet:
    context-path: /api
  shutdown: graceful

spring:
  application:
    name: order-service
```

#### 3. 框架默认配置 (SHOULD)

```yaml
# ✅ 正确：application.yml (本地)
spring:
  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false
      
  mvc:
    throw-exception-if-no-handler-found: true
    pathmatch:
      matching-strategy: ant_path_matcher
```

#### 4. 本地开发配置 (MAY)

```yaml
# ✅ 正确：application-local.yml (本地，不提交 Git)
spring:
  cloud:
    config:
      enabled: false  # 本地开发禁用 Config Server
      
  datasource:
    url: jdbc:mysql://localhost:3306/test_db
    username: root
    password: root
    
logging:
  level:
    root: DEBUG
    com.company: TRACE
```

---

## 文件组织结构

### 🗂️ Config Server Git 仓库结构（标准）

```
config-repo/
├── README.md                          # 配置仓库说明文档
├── .gitignore
│
├── application.yml                    # 全局默认配置（所有服务所有环境）
├── application-dev.yml                # 全局开发环境配置
├── application-test.yml               # 全局测试环境配置
├── application-staging.yml            # 全局预发布环境配置
├── application-prod.yml               # 全局生产环境配置
│
├── services/                          # 服务特定配置目录
│   ├── user-service.yml              # user-service 默认配置
│   ├── user-service-dev.yml          # user-service 开发环境
│   ├── user-service-test.yml
│   ├── user-service-prod.yml
│   │
│   ├── order-service.yml
│   ├── order-service-dev.yml
│   ├── order-service-prod.yml
│   │
│   ├── payment-service.yml
│   ├── payment-service-dev.yml
│   ├── payment-service-prod.yml
│   │
│   └── gateway-service.yml
│       └── gateway-service-prod.yml
│
├── shared/                            # 共享配置模块（可选）
│   ├── datasource-mysql.yml          # MySQL 数据源配置
│   ├── datasource-postgresql.yml     # PostgreSQL 数据源配置
│   ├── redis.yml                      # Redis 配置
│   ├── rabbitmq.yml                   # RabbitMQ 配置
│   ├── security.yml                   # 安全配置
│   └── monitoring.yml                 # 监控配置
│
└── scripts/                           # 工具脚本
    ├── encrypt.sh                     # 加密工具
    └── validate-config.sh             # 配置验证脚本
```

### 🗂️ 微服务本地配置结构（标准）

```
order-service/
├── src/main/resources/
│   ├── bootstrap.yml                  # 启动引导配置（连接 Config Server）
│   ├── application.yml                # 本地默认配置
│   ├── application-local.yml          # 本地开发配置（Git ignore）
│   ├── application-test.yml           # 单元测试配置
│   │
│   ├── logback-spring.xml            # 日志配置
│   │
│   └── META-INF/
│       └── spring.factories          # SPI 配置
│
└── config/
    └── CONVENTIONS.md                 # 本文档引用
```

---

## 配置优先级

### 📊 加载顺序（从低到高优先级）

```
优先级 1 (最低)  ← Config Server: application.yml
优先级 2         ← Config Server: application-{profile}.yml
优先级 3         ← Config Server: {service-name}.yml
优先级 4         ← Config Server: {service-name}-{profile}.yml
优先级 5         ← 本地: bootstrap.yml
优先级 6         ← 本地: application.yml
优先级 7         ← 本地: application-{profile}.yml
优先级 8         ← 环境变量 (Environment Variables)
优先级 9 (最高)  ← 命令行参数 (Command Line Args)
```

### 💡 实际应用示例

```yaml
# Config Server: application.yml (优先级 1)
logging:
  level:
    root: INFO

# Config Server: order-service-prod.yml (优先级 4)
logging:
  level:
    root: WARN
    com.company.order: INFO

# 环境变量 (优先级 8)
LOGGING_LEVEL_ROOT=ERROR

# 最终生效: ERROR (环境变量覆盖所有配置文件)
```

### ⚠️ 配置覆盖策略

```yaml
# ❌ 错误：期望合并但实际会整体覆盖
# Config Server: application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000

# 本地: application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      # ⚠️ minimum-idle 和 connection-timeout 会丢失！

# ✅ 正确：只覆盖需要修改的配置
# 使用环境变量精确覆盖
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
```

---

## 命名规范

### 📝 配置文件命名规则

#### 通用命名格式

```
{服务名称}-{环境}.yml
```

#### 标准示例

```
✅ 正确命名
application.yml                 # 全局默认
application-dev.yml             # 全局开发环境
user-service.yml                # 用户服务默认
user-service-prod.yml           # 用户服务生产环境
order-service-staging.yml       # 订单服务预发布环境

❌ 错误命名
user_service.yml               # 不使用下划线
UserService.yml                # 不使用大写
user-service-production.yml    # 不使用完整单词，用 prod
userservice.yml                # 必须使用连字符
```

### 🏷️ 配置属性命名规则

#### 使用 kebab-case（推荐）

```yaml
# ✅ 正确：使用 kebab-case
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    
feature:
  express-checkout: true
  international-shipping: false
  
business:
  order:
    max-items-per-cart: 100
    checkout-timeout-minutes: 30
```

#### 避免的命名方式

```yaml
# ❌ 错误：不要混用不同风格
feature:
  expressCheckout: true          # camelCase
  international_shipping: false  # snake_case
  MaxItemsPerCart: 100          # PascalCase
```

### 🎨 环境标识命名规范

```yaml
# 标准环境标识（仅使用这些）
dev        # 开发环境
test       # 测试环境
staging    # 预发布环境
prod       # 生产环境

# 特殊场景（可选）
local      # 本地开发
uat        # 用户验收测试
perf       # 性能测试
```

---

## 敏感信息处理

### 🔐 加密配置（强制要求）

#### Config Server 加密配置

```yaml
# Config Server: application.yml
encrypt:
  key: ${ENCRYPT_KEY}  # 从环境变量读取

# 或使用 Key Store（生产环境推荐）
encrypt:
  key-store:
    location: classpath:config-server.jks
    password: ${KEYSTORE_PASSWORD}
    alias: config-server-key
    secret: ${KEY_SECRET}
```

#### 加密敏感信息流程

```bash
# 1. 启动 Config Server

# 2. 加密明文密码
curl http://localhost:8888/encrypt -d "my-secret-password"
# 返回: AQATBvLSf3hNkgP8xLmN2qRsT4uVwXyZ...

# 3. 在配置文件中使用加密值
# user-service-prod.yml
spring:
  datasource:
    password: '{cipher}AQATBvLSf3hNkgP8xLmN2qRsT4uVwXyZ...'

# 4. 验证解密（测试用）
curl http://localhost:8888/decrypt -d "AQATBvLSf3hNkgP8xLmN2qRsT4uVwXyZ..."
```

### 🛡️ 敏感信息清单

#### 必须加密的配置类型

```yaml
# ✅ 必须加密
spring:
  datasource:
    password: '{cipher}...'           # 数据库密码
    
  redis:
    password: '{cipher}...'           # Redis 密码
    
  rabbitmq:
    password: '{cipher}...'           # 消息队列密码

security:
  oauth2:
    client:
      client-secret: '{cipher}...'   # OAuth2 密钥
      
api:
  keys:
    stripe: '{cipher}...'            # 第三方 API 密钥
    aws-secret: '{cipher}...'        # AWS Secret Key
    jwt-secret: '{cipher}...'        # JWT 签名密钥

encrypt:
  key: ${ENCRYPT_KEY}                # 从环境变量读取，不写在文件中
```

#### 不需要加密的配置

```yaml
# ⚠️ 这些配置通常不需要加密（非敏感）
spring:
  datasource:
    url: jdbc:mysql://db.example.com:3306/orders  # 数据库地址
    username: app_user                             # 用户名（非敏感）
    
eureka:
  client:
    service-url:
      defaultZone: http://eureka:8761/eureka/     # 服务注册地址
```

### 🔑 密钥管理最佳实践

```bash
# ❌ 错误：加密密钥写在配置文件中
encrypt:
  key: "my-hardcoded-key"  # 绝对不要这样做！

# ✅ 正确：使用环境变量
export ENCRYPT_KEY="your-encryption-key-from-vault"

# ✅ 更好：使用专业密钥管理服务
# - AWS Secrets Manager
# - Azure Key Vault
# - HashiCorp Vault
# - Spring Cloud Vault
```

---

## 动态刷新机制

### 🔄 @RefreshScope 使用规范

#### 配置类（推荐方式）

```java
// ✅ 正确：使用 @ConfigurationProperties
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "feature")
public class FeatureProperties {
    
    private boolean expressCheckout;
    private boolean internationalShipping;
    private boolean recommendationEngine;
    
    // 配置变更后自动刷新
}
```

#### 服务类

```java
// ✅ 正确：需要动态刷新的服务类
@Service
@RefreshScope
public class OrderService {
    
    @Value("${business.order.max-items:100}")
    private int maxItems;
    
    @Value("${business.order.timeout-minutes:30}")
    private int timeoutMinutes;
    
    public Order createOrder(OrderRequest request) {
        if (request.getItems().size() > maxItems) {
            throw new IllegalArgumentException("Too many items");
        }
        // 使用最新的配置值
    }
}
```

#### Controller 类（谨慎使用）

```java
// ⚠️ 注意：Controller 使用 @RefreshScope 可能有副作用
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    // ✅ 推荐：注入 @RefreshScope 的配置类
    private final FeatureProperties featureProperties;
    
    public OrderController(FeatureProperties featureProperties) {
        this.featureProperties = featureProperties;
    }
    
    @GetMapping("/checkout")
    public ResponseEntity<?> checkout() {
        if (featureProperties.isExpressCheckout()) {
            // 使用新功能
        }
    }
}
```

### 🚀 刷新配置的方式

#### 方式一：手动刷新单个服务

```bash
# 刷新特定服务实例
curl -X POST http://order-service-instance1:8080/actuator/refresh \
  -H "Content-Type: application/json"
```

#### 方式二：Spring Cloud Bus 全局刷新（推荐）

```yaml
# Config Server: application.yml
spring:
  cloud:
    bus:
      enabled: true
    stream:
      bindings:
        springCloudBusInput:
          destination: springCloudBus
          
management:
  endpoints:
    web:
      exposure:
        include: busrefresh
```

```bash
# 刷新所有服务实例
curl -X POST http://any-service:8080/actuator/bus-refresh \
  -H "Content-Type: application/json"

# 刷新指定服务的所有实例
curl -X POST http://any-service:8080/actuator/bus-refresh/order-service:**
```

#### 方式三：Git Webhook 自动刷新（推荐生产环境）

```yaml
# Config Server: application.yml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/company/config-repo
          default-label: main
          clone-on-start: true
        webhook:
          enabled: true
          
management:
  endpoints:
    web:
      exposure:
        include: busrefresh
```

**GitHub Webhook 配置：**
```
Payload URL: http://config-server.company.com/monitor
Content type: application/json
Secret: your-webhook-secret
Events: Just the push event
```

### ⚠️ 刷新限制和注意事项

```java
// ❌ 错误：这些配置无法动态刷新
@Configuration
public class DataSourceConfig {
    
    @Value("${spring.datasource.url}")
    private String url;  // 数据源配置刷新无效
    
    @Bean
    public DataSource dataSource() {
        // Bean 创建后无法刷新
    }
}

// ❌ 错误：@Scheduled 注解的参数无法刷新
@Component
public class ScheduledTask {
    
    @Scheduled(fixedDelayString = "${task.fixed-delay}")
    public void execute() {
        // 定时任务间隔无法动态修改
    }
}

// ✅ 正确：可以刷新的配置
@Service
@RefreshScope
public class BusinessService {
    
    @Value("${business.max-retry:3}")
    private int maxRetry;  // 可以动态刷新
    
    public void process() {
        for (int i = 0; i < maxRetry; i++) {
            // 使用最新的配置
        }
    }
}
```

---

## 环境管理

### 🌍 环境划分标准

```yaml
# 开发环境 (dev)
用途: 开发人员日常开发
数据: 测试数据
配置特点: 详细日志、DEBUG 级别、快速失败

# 测试环境 (test)
用途: 自动化测试、集成测试
数据: 标准测试数据集
配置特点: 模拟生产、完整监控

# 预发布环境 (staging)
用途: 生产前验证
数据: 生产数据副本（脱敏）
配置特点: 与生产完全一致

# 生产环境 (prod)
用途: 正式运行
数据: 真实业务数据
配置特点: 高性能、安全加固、完整监控
```

### 🎯 环境切换方式

#### 方式一：Spring Profiles（推荐）

```bash
# 启动时指定环境
java -jar order-service.jar --spring.profiles.active=prod

# 使用环境变量
export SPRING_PROFILES_ACTIVE=prod
java -jar order-service.jar

# Docker 环境
docker run -e SPRING_PROFILES_ACTIVE=prod order-service:latest

# Kubernetes ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-service-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
```

#### 方式二：多 Profile 激活

```bash
# 同时激活多个 profile
java -jar app.jar --spring.profiles.active=prod,monitoring,security

# 对应配置文件
application-prod.yml        # 生产环境基础配置
application-monitoring.yml  # 监控配置
application-security.yml    # 安全配置
```

### 📊 环境配置差异化示例

```yaml
# application-dev.yml (开发环境)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dev_db
    hikari:
      maximum-pool-size: 5
      
logging:
  level:
    root: DEBUG
    com.company: TRACE
    
feature:
  circuit-breaker: false
  rate-limit: false

# application-prod.yml (生产环境)
spring:
  datasource:
    url: jdbc:mysql://prod-db-cluster:3306/prod_db
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
logging:
  level:
    root: WARN
    com.company: INFO
    
feature:
  circuit-breaker: true
  rate-limit: true
  
resilience4j:
  circuitbreaker:
    instances:
      orderService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60000
```

---

## 代码示例

### 📚 完整配置示例

#### 1. Bootstrap 配置（本地）

```yaml
# src/main/resources/bootstrap.yml
spring:
  application:
    name: order-service
    
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
    
  cloud:
    config:
      # Config Server 地址
      uri: ${CONFIG_SERVER_URI:http://localhost:8888}
      # 配置文件名（默认使用 spring.application.name）
      name: ${spring.application.name}
      # 环境标识
      profile: ${spring.profiles.active}
      # Git 分支
      label: main
      # 快速失败
      fail-fast: true
      # 重试策略
      retry:
        initial-interval: 1000
        max-attempts: 6
        multiplier: 1.1
        max-interval: 2000
      # 启用自动刷新
      auto-refresh: true
```

#### 2. Application 配置（本地）

```yaml
# src/main/resources/application.yml
server:
  port: ${SERVER_PORT:8081}
  shutdown: graceful
  
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
    
  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
      
management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh
  endpoint:
    health:
      show-details: when-authorized
```

#### 3. Config Server 配置

```yaml
# config-repo/order-service-prod.yml
spring:
  datasource:
    url: jdbc:mysql://prod-mysql-cluster:3306/orders?useSSL=true&serverTimezone=UTC
    username: order_service_user
    password: '{cipher}AQATBvLSf3hNkgP8xLmN2qRsT4uVwXyZ1a2B3c4D5e6F7g8H9i0J...'
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: OrderServiceHikariPool
      
  redis:
    host: prod-redis-cluster.company.com
    port: 6379
    password: '{cipher}BQBCvMTg4iOlmQ9yMnO3rStU5vWxYz2A3b4C5d6E7f8G9h0I1j2K...'
    database: 0
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        
feature:
  express-checkout: true
  international-shipping: true
  recommendation-engine: true
  fraud-detection: true
  
business:
  order:
    max-items: 100
    max-amount: 1000000
    timeout-minutes: 30
    auto-cancel-hours: 24
    
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        register-health-indicator: true
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60000
        
  ratelimiter:
    instances:
      orderCreation:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 0
        
logging:
  level:
    root: WARN
    com.company.order: INFO
    com.company.order.repository: DEBUG
  file:
    name: /var/log/order-service/application.log
    max-size: 100MB
    max-history: 30
```

### 💻 Java 代码示例

#### 配置属性类

```java
package com.company.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 业务配置属性
 * 
 * 使用说明：
 * 1. 所有需要动态刷新的配置必须添加 @RefreshScope
 * 2. 使用 JSR-303 验证确保配置合法性
 * 3. 提供默认值防止配置缺失
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "business.order")
@Validated
public class OrderBusinessProperties {
    
    /**
     * 订单最大商品数量
     */
    @Min(1)
    @Max(500)
    @NotNull
    private Integer maxItems = 100;
    
    /**
     * 订单最大金额（分）
     */
    @Min(1)
    private Long maxAmount = 1000000L;
    
    /**
     * 订单超时时间（分钟）
     */
    @Min(1)
    @Max(1440)
    private Integer timeoutMinutes = 30;
    
    /**
     * 自动取消未支付订单时间（小时）
     */
    @Min(1)
    @Max(168)
    private Integer autoCancelHours = 24;
}
```

#### 功能开关类

```java
package com.company.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 功能开关配置
 * 
 * 使用场景：
 * - 灰度发布新功能
 * - 紧急关闭问题功能
 * - A/B 测试控制
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "feature")
public class FeatureToggleProperties {
    
    /**
     * 快速结账功能
     */
    private boolean expressCheckout = false;
    
    /**
     * 国际物流功能
     */
    private boolean internationalShipping = false;
    
    /**
     * 推荐引擎
     */
    private boolean recommendationEngine = false;
    
    /**
     * 欺诈检测
     */
    private boolean fraudDetection = true;
}
```

#### 服务类使用配置

```java
package com.company.order.service;

import com.company.order.config.FeatureToggleProperties;
import com.company.order.config.OrderBusinessProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

/**
 * 订单服务
 * 
 * ✅ 正确实践：
 * 1. 使用构造器注入配置属性（而非 @Value）
 * 2. 服务类添加 @RefreshScope 支持配置热更新
 * 3. 配置变更后自动使用新值
 */
@Slf4j
@Service
@RefreshScope
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderBusinessProperties businessProperties;
    private final FeatureToggleProperties featureToggle;
    
    /**
     * 创建订单
     */
    public Order createOrder(OrderRequest request) {
        // 使用最新的配置值
        if (request.getItems().size() > businessProperties.getMaxItems()) {
            log.warn("订单商品数量超限: {} > {}", 
                request.getItems().size(), 
                businessProperties.getMaxItems());
            throw new BusinessException("商品数量超过限制");
        }
        
        if (request.getTotalAmount() > businessProperties.getMaxAmount()) {
            log.warn("订单金额超限: {} > {}", 
                request.getTotalAmount(), 
                businessProperties.getMaxAmount());
            throw new BusinessException("订单金额超过限制");
        }
        
        // 根据功能开关执行不同逻辑
        if (featureToggle.isFraudDetection()) {
            fraudDetectionService.check(request);
        }
        
        if (featureToggle.isRecommendationEngine()) {
            recommendationService.addRecommendations(request);
        }
        
        // 创建订单逻辑
        Order order = buildOrder(request);
        
        log.info("订单创建成功: orderId={}, items={}, amount={}", 
            order.getId(), 
            order.getItems().size(), 
            order.getTotalAmount());
            
        return order;
    }
}
```

#### 配置变更监听器

```java
package com.company.order.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 配置变更监听器
 * 
 * 用于监控和记录配置变更事件
 */
@Slf4j
@Component
public class ConfigChangeListener {
    
    @EventListener
    public void onConfigChange(EnvironmentChangeEvent event) {
        Set<String> changedKeys = event.getKeys();
        
        log.info("检测到配置变更, 变更项数量: {}", changedKeys.size());
        
        changedKeys.forEach(key -> {
            log.info("配置项变更: key={}", key);
        });
        
        // 可以在这里添加业务逻辑
        // 例如：清除缓存、重新初始化某些组件等
    }
}
```

---

## 常见错误

### ❌ 错误一：所有配置都放 Config Server

```yaml
# ❌ 错误：bootstrap.yml 也放在 Config Server
# 这会导致服务无法启动，因为连接不到 Config Server

# ✅ 正确：bootstrap.yml 必须在本地
# src/main/resources/bootstrap.yml
spring:
  application:
    name: order-service
  cloud:
    config:
      uri: http://config-server:8888
```

**原因**: Bootstrap 阶段需要知道如何连接 Config Server，这是"鸡生蛋"问题。

---

### ❌ 错误二：敏感信息明文存储

```yaml
# ❌ 错误：明文密码提交到 Git
spring:
  datasource:
    password: MyP@ssw0rd123!
    
api:
  stripe:
    secret-key: sk_live_51Hx...

# ✅ 正确：使用加密
spring:
  datasource:
    password: '{cipher}AQATBvLSf3hNkg...'
    
api:
  stripe:
    secret-key: '{cipher}BQBCvMTg4iOlm...'
```

**后果**: 密码泄露、安全审计不通过、合规风险。

---

### ❌ 错误三：忘记添加 @RefreshScope

```java
// ❌ 错误：配置无法动态刷新
@Service
public class OrderService {
    
    @Value("${business.order.max-items}")
    private int maxItems;  // 启动后永远不会更新
}

// ✅ 正确：添加 @RefreshScope
@Service
@RefreshScope
public class OrderService {
    
    @Value("${business.order.max-items}")
    private int maxItems;  // 配置刷新后会更新
}
```

**现象**: 修改 Config Server 配置后调用 `/actuator/refresh`，但配置值不生效。

---

### ❌ 错误四：配置文件命名不规范

```yaml
# ❌ 错误命名
order_service.yml          # 使用了下划线
OrderService-prod.yml      # 使用了大写
order-service-production.yml  # 环境名太长

# ✅ 正确命名
order-service.yml
order-service-prod.yml
```

**后果**: Config Server 无法正确匹配配置文件，导致配置加载失败。

---

### ❌ 错误五：循环依赖配置

```yaml
# ❌ 错误：配置相互引用
# application.yml
service:
  url: ${other.service.url}/api

# other-service.yml
other:
  service:
    url: ${service.url}/callback

# ✅ 正确：使用独立的配置值
# application.yml
service:
  url: http://service:8080/api

# other-service.yml
other:
  service:
    url: http://other-service:8081/api
```

**后果**: 启动时抛出 `Could not resolve placeholder` 异常。

---

### ❌ 错误六：不区分环境的配置

```yaml
# ❌ 错误：所有环境使用相同配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db
    hikari:
      maximum-pool-size: 10  # 开发和生产用同样的连接池大小

# ✅ 正确：根据环境调整
# application-dev.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5

# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
```

**后果**: 生产环境性能不足或资源浪费。

---

### ❌ 错误七：配置过度集中

```yaml
# ❌ 错误：把所有服务配置都写在 application.yml
# application.yml (Config Server)
user-service:
  port: 8081
  db:
    url: jdbc:mysql://...

order-service:
  port: 8082
  db:
    url: jdbc:mysql://...
    
payment-service:
  port: 8083
  # ... 100 多行配置

# ✅ 正确：按服务拆分配置文件
# user-service.yml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://...
```

**后果**: 配置文件难以维护，团队协作冲突频繁。

---

### ❌ 错误八：配置缺少默认值

```java
// ❌ 错误：没有默认值
@Value("${business.order.max-items}")
private int maxItems;  // 配置缺失时启动失败

// ✅ 正确：提供默认值
@Value("${business.order.max-items:100}")
private int maxItems;  // 配置缺失时使用默认值 100
```

**后果**: 配置文件缺少某个属性时，服务启动失败。

---

### ❌ 错误九：使用 @Value 注入复杂对象

```java
// ❌ 错误：用 @Value 注入多个相关配置
@Value("${datasource.url}")
private String url;

@Value("${datasource.username}")
private String username;

@Value("${datasource.password}")
private String password;

// ✅ 正确：使用 @ConfigurationProperties
@ConfigurationProperties(prefix = "datasource")
@Data
public class DataSourceProperties {
    private String url;
    private String username;
    private String password;
}
```

**原因**: `@ConfigurationProperties` 支持类型安全、验证、宽松绑定等特性。

---

### ❌ 错误十：Config Server 没有健康检查

```yaml
# ❌ 错误：服务启动时 Config Server 挂了，没有降级策略
spring:
  cloud:
    config:
      fail-fast: true  # 启动失败

# ✅ 正确：提供本地降级配置
spring:
  cloud:
    config:
      fail-fast: false  # 允许使用本地配置启动
      
# 并在本地提供完整的降级配置
# application.yml (本地)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fallback_db
```

**建议**: 生产环境配置 Config Server 集群和本地降级配置。

---

## 检查清单

### 📋 配置前检查

开始配置之前，确认以下事项：

- [ ] 已明确当前环境（dev/test/staging/prod）
- [ ] 已了解配置的作用域（单服务/多服务共享）
- [ ] 已识别敏感信息并准备加密
- [ ] 已确定配置是否需要动态刷新
- [ ] Config Server Git 仓库已创建并配置权限

### 📋 本地配置检查清单

- [ ] **bootstrap.yml 存在且包含必需配置**
  - [ ] `spring.application.name` 已配置
  - [ ] `spring.cloud.config.uri` 已配置
  - [ ] `spring.profiles.active` 通过环境变量或参数指定
  
- [ ] **application.yml 仅包含静态配置**
  - [ ] `server.port` 已配置
  - [ ] 框架默认配置（Jackson、MVC 等）
  - [ ] 不包含环境相关配置
  - [ ] 不包含敏感信息

- [ ] **application-local.yml 已添加到 .gitignore**
  ```gitignore
  # .gitignore
  application-local.yml
  application-local.yaml
  ```

### 📋 Config Server 配置检查清单

- [ ] **配置文件命名规范**
  - [ ] 使用 kebab-case 命名
  - [ ] 环境后缀正确（dev/test/staging/prod）
  - [ ] 服务名与 `spring.application.name` 一致

- [ ] **敏感信息已加密**
  - [ ] 数据库密码使用 `{cipher}` 前缀
  - [ ] API 密钥已加密
  - [ ] OAuth2 密钥已加密
  - [ ] 加密密钥通过环境变量注入

- [ ] **配置文件结构合理**
  - [ ] 全局配置在 `application.yml`
  - [ ] 环境配置在 `application-{profile}.yml`
  - [ ] 服务特定配置在 `{service}-{profile}.yml`

- [ ] **配置内容完整**
  - [ ] 所有环境都有对应配置文件
  - [ ] 生产环境配置已仔细审核
  - [ ] 数据库连接池参数已根据环境调整

### 📋 动态刷新检查清单

- [ ] **需要动态刷新的类添加 @RefreshScope**
  - [ ] `@ConfigurationProperties` 类
  - [ ] 使用 `@Value` 的 `@Component` 或 `@Service`

- [ ] **Actuator 端点已启用**
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: refresh,busrefresh
  ```

- [ ] **Spring Cloud Bus 已配置（可选）**
  - [ ] RabbitMQ 或 Kafka 连接配置
  - [ ] `/actuator/bus-refresh` 端点可访问

### 📋 安全检查清单

- [ ] **敏感信息保护**
  - [ ] 没有明文密码提交到 Git
  - [ ] 加密密钥通过环境变量注入
  - [ ] Config Server Git 仓库设置了访问权限

- [ ] **Config Server 安全配置**
  - [ ] 启用了 Spring Security 认证
  - [ ] `/encrypt` 和 `/decrypt` 端点受保护
  - [ ] 生产环境使用 HTTPS

- [ ] **审计和监控**
  - [ ] 配置变更有 Git 提交记录
  - [ ] 配置刷新有日志记录
  - [ ] 配置变更有告警通知

### 📋 部署前检查清单

- [ ] **环境变量已设置**
  ```bash
  SPRING_PROFILES_ACTIVE=prod
  CONFIG_SERVER_URI=https://config.company.com
  ENCRYPT_KEY=***
  ```

- [ ] **Config Server 可达性测试**
  ```bash
  curl http://config-server:8888/actuator/health
  curl http://config-server:8888/order-service/prod
  ```

- [ ] **配置加载测试**
  - [ ] 启动服务并检查日志
  - [ ] 验证敏感配置已正确解密
  - [ ] 测试配置热刷新功能

- [ ] **降级方案就绪**
  - [ ] 本地有完整的降级配置
  - [ ] Config Server 故障时服务可启动

### 📋 运维检查清单

- [ ] **监控配置**
  - [ ] Config Server 健康检查
  - [ ] 配置刷新成功率监控
  - [ ] 配置加载失败告警

- [ ] **备份和恢复**
  - [ ] Config Git 仓库定期备份
  - [ ] 有配置回滚流程
  - [ ] 加密密钥有安全备份

---

## 附录

### 🔗 相关资源

**官方文档**
- [Spring Cloud Config 官方文档](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Spring Boot 配置属性](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)

**最佳实践参考**
- [12-Factor App - 配置](https://12factor.net/config)
- [Spring Cloud Config 加密解密](https://cloud.spring.io/spring-cloud-config/reference/html/#_encryption_and_decryption)

### 📞 问题反馈

如发现本规范有不合理之处或需要补充的内容，请：
1. 提交 Issue 到配置仓库
2. 联系配置管理团队
3. 在团队会议上讨论

### 📜 变更日志

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2025-10-30 | 初始版本 | @Big-Dao |

---

## 快速参考卡

```
┌─────────────────────────────────────────────────────────────┐
│  配置决策树                                                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  配置是否在不同环境有不同值？                                 │
│       ├─ YES → Config Server                                │
│       └─ NO  → 继续判断                                      │
│                                                              │
│  配置是否需要运行时动态修改？                                 │
│       ├─ YES → Config Server + @RefreshScope               │
│       └─ NO  → 继续判断                                      │
│                                                              │
│  配置是否包含敏感信息？                                       │
│       ├─ YES → Config Server + 加密                         │
│       └─ NO  → 继续判断                                      │
│                                                              │
│  配置是否需要多服务共享？                                     │
│       ├─ YES → Config Server (application.yml)             │
│       └─ NO  → 本地 YAML                                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**记住这三个规则，配置管理不出错：**

1. **服务名和端口** → 本地配置
2. **数据库和中间件** → Config Server
3. **密码和密钥** → Config Server + 加密

---

**本文档持续更新中，请关注最新版本。**

**在使用 LLM 辅助开发时，请将此文档作为系统提示词的一部分提供给 LLM。**