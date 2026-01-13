# EVCS 日志规范

> **版本**: v1.0  
> **创建日期**: 2026-01-13  
> **维护者**: 架构团队  
> **状态**: 已发布

---

## 1. 概述

本文档定义 EVCS 充电站管理系统的日志规范，包括日志级别、格式、脱敏规则和最佳实践。

### 1.1 日志目标

- **可观测性**: 支持问题定位和性能分析
- **可追溯性**: 支持全链路追踪
- **合规性**: 满足安全审计要求
- **性能**: 最小化日志对系统性能的影响

---

## 2. 日志级别

### 2.1 级别定义

| 级别 | 用途 | 生产环境 | 示例 |
|------|------|----------|------|
| **ERROR** | 系统错误，需要立即关注 | ✅ 开启 | 数据库连接失败、支付异常 |
| **WARN** | 潜在问题，需要关注 | ✅ 开启 | 重试成功、配置缺失使用默认值 |
| **INFO** | 业务关键节点 | ✅ 开启 | 订单创建、支付成功、充电开始 |
| **DEBUG** | 调试信息 | ❌ 关闭 | 方法入参、SQL 语句 |
| **TRACE** | 详细追踪 | ❌ 关闭 | 循环内部状态 |

### 2.2 环境配置

```yaml
# application.yml
logging:
  level:
    root: INFO
    com.evcs: INFO
    org.springframework: WARN
    org.hibernate.SQL: WARN
    com.zaxxer.hikari: WARN

# 开发环境
---
spring:
  config:
    activate:
      on-profile: dev
logging:
  level:
    com.evcs: DEBUG
    org.hibernate.SQL: DEBUG

# 生产环境
---
spring:
  config:
    activate:
      on-profile: prod
logging:
  level:
    root: WARN
    com.evcs: INFO
```

---

## 3. 日志格式

### 3.1 标准格式

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] [%X{tenantId}] %logger{36} - %msg%n
```

### 3.2 字段说明

| 字段 | 说明 | 示例 |
|------|------|------|
| 时间戳 | ISO 8601 格式 | 2026-01-13 10:30:00.123 |
| 线程名 | 执行线程 | [http-nio-8080-exec-1] |
| 级别 | 日志级别 | INFO |
| TraceId | 链路追踪ID | [abc123] |
| TenantId | 租户ID | [100001] |
| Logger | 类名 | OrderService |
| Message | 日志消息 | 订单创建成功 |

### 3.3 Logback 配置

```xml
<!-- logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-}] [%X{tenantId:-}] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- JSON 格式（生产环境推荐） -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>tenantId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>
    
    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/evcs/${spring.application.name}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/evcs/${spring.application.name}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-}] [%X{tenantId:-}] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- 开发环境 -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
    
    <!-- 生产环境 -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="JSON"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

---

## 4. MDC 上下文

### 4.1 标准 MDC 字段

| 字段 | 说明 | 来源 |
|------|------|------|
| `traceId` | 链路追踪ID | Gateway 生成或 Sleuth |
| `tenantId` | 租户ID | TenantContext |
| `userId` | 用户ID | SecurityContext |
| `requestId` | 请求ID | Gateway |
| `clientIp` | 客户端IP | Request Header |

### 4.2 MDC 过滤器

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 设置 TraceId
            String traceId = request.getHeader("X-Trace-Id");
            if (StringUtils.isBlank(traceId)) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            MDC.put("traceId", traceId);
            
            // 设置租户ID
            Long tenantId = TenantContext.getCurrentTenantId();
            if (tenantId != null) {
                MDC.put("tenantId", tenantId.toString());
            }
            
            // 设置用户ID
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetails) {
                MDC.put("userId", ((UserDetails) auth.getPrincipal()).getUsername());
            }
            
            // 设置客户端IP
            MDC.put("clientIp", getClientIp(request));
            
            // 响应头返回 TraceId
            response.setHeader("X-Trace-Id", traceId);
            
            filterChain.doFilter(request, response);
            
        } finally {
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
```

---

## 5. 敏感数据脱敏

### 5.1 必须脱敏的字段

| 字段类型 | 脱敏规则 | 示例 |
|----------|----------|------|
| 手机号 | 保留前3后4 | 138****1234 |
| 身份证号 | 保留前6后4 | 110101****1234 |
| 银行卡号 | 保留前4后4 | 6222****1234 |
| 密码 | 完全隐藏 | ****** |
| Token/密钥 | 保留前8 | abcd1234**** |
| 邮箱 | 保留首字母和域名 | a****@example.com |

### 5.2 脱敏工具类

```java
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {}

    /**
     * 手机号脱敏
     */
    public static String maskPhone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 身份证号脱敏
     */
    public static String maskIdCard(String idCard) {
        if (StringUtils.isBlank(idCard) || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "****" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 银行卡号脱敏
     */
    public static String maskBankCard(String cardNo) {
        if (StringUtils.isBlank(cardNo) || cardNo.length() < 8) {
            return cardNo;
        }
        return cardNo.substring(0, 4) + "****" + cardNo.substring(cardNo.length() - 4);
    }

    /**
     * 密码脱敏
     */
    public static String maskPassword(String password) {
        return "******";
    }

    /**
     * Token/密钥脱敏
     */
    public static String maskToken(String token) {
        if (StringUtils.isBlank(token) || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 8) + "****";
    }

    /**
     * 邮箱脱敏
     */
    public static String maskEmail(String email) {
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String prefix = parts[0];
        if (prefix.length() <= 1) {
            return "*@" + parts[1];
        }
        return prefix.charAt(0) + "****@" + parts[1];
    }
}
```

### 5.3 日志脱敏注解

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    SensitiveType type();
}

public enum SensitiveType {
    PHONE,
    ID_CARD,
    BANK_CARD,
    PASSWORD,
    TOKEN,
    EMAIL
}

// 使用示例
@Data
public class UserDTO {
    private Long id;
    private String name;
    
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
    
    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;
    
    @Sensitive(type = SensitiveType.PASSWORD)
    private String password;
}
```

### 5.4 Jackson 脱敏序列化

```java
public class SensitiveSerializer extends JsonSerializer<String> {

    private final SensitiveType type;

    public SensitiveSerializer(SensitiveType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        
        String masked = switch (type) {
            case PHONE -> SensitiveDataMasker.maskPhone(value);
            case ID_CARD -> SensitiveDataMasker.maskIdCard(value);
            case BANK_CARD -> SensitiveDataMasker.maskBankCard(value);
            case PASSWORD -> SensitiveDataMasker.maskPassword(value);
            case TOKEN -> SensitiveDataMasker.maskToken(value);
            case EMAIL -> SensitiveDataMasker.maskEmail(value);
        };
        
        gen.writeString(masked);
    }
}
```

---

## 6. 日志最佳实践

### 6.1 正确示例

```java
// ✅ 正确：使用占位符
log.info("订单创建成功, orderId={}, userId={}", orderId, userId);

// ✅ 正确：异常带堆栈
log.error("支付失败, orderId={}", orderId, exception);

// ✅ 正确：业务关键节点
log.info("开始充电, chargerId={}, userId={}, connectorId={}", chargerId, userId, connectorId);

// ✅ 正确：脱敏输出
log.info("用户登录, phone={}", SensitiveDataMasker.maskPhone(phone));

// ✅ 正确：条件判断
if (log.isDebugEnabled()) {
    log.debug("详细参数: {}", expensiveToString(params));
}
```

### 6.2 错误示例

```java
// ❌ 错误：字符串拼接
log.info("订单创建成功, orderId=" + orderId);

// ❌ 错误：敏感信息明文
log.info("用户登录, phone={}, password={}", phone, password);

// ❌ 错误：过于简略
log.error("失败");

// ❌ 错误：异常丢失堆栈
log.error("支付失败: " + e.getMessage());

// ❌ 错误：无意义的日志
log.info("进入方法");
log.info("退出方法");
```

### 6.3 业务关键点日志

```java
@Service
public class OrderService {

    public Order createOrder(CreateOrderRequest request) {
        log.info("[订单创建] 开始, userId={}, stationId={}, chargerId={}", 
            request.getUserId(), request.getStationId(), request.getChargerId());
        
        try {
            // 业务逻辑
            Order order = doCreateOrder(request);
            
            log.info("[订单创建] 成功, orderId={}, orderNo={}", 
                order.getId(), order.getOrderNo());
            
            return order;
            
        } catch (BusinessException e) {
            log.warn("[订单创建] 业务异常, userId={}, reason={}", 
                request.getUserId(), e.getMessage());
            throw e;
            
        } catch (Exception e) {
            log.error("[订单创建] 系统异常, userId={}", request.getUserId(), e);
            throw new SystemException("订单创建失败", e);
        }
    }
}
```

---

## 7. 审计日志

### 7.1 审计日志表结构

```sql
CREATE TABLE sys_audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT,
    username VARCHAR(64),
    operation VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64),
    resource_id VARCHAR(64),
    request_method VARCHAR(16),
    request_url VARCHAR(512),
    request_params TEXT,
    response_code INTEGER,
    client_ip VARCHAR(64),
    user_agent VARCHAR(256),
    trace_id VARCHAR(64),
    duration_ms INTEGER,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_tenant_time ON sys_audit_log(tenant_id, create_time);
CREATE INDEX idx_audit_user ON sys_audit_log(user_id);
CREATE INDEX idx_audit_trace ON sys_audit_log(trace_id);
```

### 7.2 审计注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    String operation();
    String resourceType() default "";
}

// 使用示例
@AuditLog(operation = "创建订单", resourceType = "ORDER")
@PostMapping
public Result<Long> createOrder(@RequestBody CreateOrderRequest request) {
    // ...
}
```

---

## 8. 日志采集

### 8.1 Kubernetes 环境

```yaml
# Filebeat DaemonSet
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: filebeat
spec:
  template:
    spec:
      containers:
      - name: filebeat
        image: elastic/filebeat:8.11.0
        volumeMounts:
        - name: varlog
          mountPath: /var/log
        - name: config
          mountPath: /usr/share/filebeat/filebeat.yml
          subPath: filebeat.yml
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
```

### 8.2 Filebeat 配置

```yaml
filebeat.inputs:
- type: container
  paths:
    - '/var/log/containers/evcs-*.log'
  processors:
    - add_kubernetes_metadata:
        host: ${NODE_NAME}
        matchers:
        - logs_path:
            logs_path: "/var/log/containers/"

output.elasticsearch:
  hosts: ['elasticsearch:9200']
  indices:
    - index: "evcs-logs-%{+yyyy.MM.dd}"
```

---

## 9. 相关文档

- [监控告警配置指南](../operations/MONITORING-ALERTING-GUIDE.md)
- [故障排查手册](../operations/TROUBLESHOOTING-GUIDE.md)
- [项目编码规范](PROJECT-CODING-STANDARDS.md)

---

## 10. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本 |
