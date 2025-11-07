# EVCS API网关服务

> **端口**: 8080 | **状态**: 活跃
>
> **功能**: 统一API入口、路由转发和安全防护

## 📋 服务概述

EVCS API网关服务是系统的统一入口，负责：
- API请求路由和转发
- 跨域处理(CORS)
- 请求限流和熔断
- 统一认证和授权
- 请求日志和监控

## 🔧 技术栈
- Spring Boot 3.2.10
- Spring Cloud Gateway
- Spring Security 6.x
- Redis (缓存和限流)
- Micrometer (监控指标)

## 🚀 快速启动

### 本地开发
```bash
# 构建服务
./gradlew :evcs-gateway:build

# 运行服务
./gradlew :evcs-gateway:bootRun

# 访问健康检查
curl http://localhost:8080/actuator/health
```

### Docker部署
```bash
# 构建镜像
docker build -t evcs-gateway:latest ./evcs-gateway

# 运行容器
docker run -p 8080:8080 evcs-gateway:latest
```

## 📡 路由配置

### 服务路由映射
| 路径 | 目标服务 | 端口 |
|------|---------|------|
| `/api/v1/auth/**` | evcs-auth | 8081 |
| `/api/v1/stations/**` | evcs-station | 8082 |
| `/api/v1/orders/**` | evcs-order | 8083 |
| `/api/v1/payments/**` | evcs-payment | 8084 |

### 路由规则示例
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=2
```

## 🔒 安全特性

- **JWT验证**: 自动验证JWT令牌
- **请求限流**: 基于IP和用户的限流控制
- **CORS支持**: 跨域请求处理
- **安全头**: 添加安全相关的HTTP头

## 📊 监控配置

### 健康检查端点
- `/actuator/health` - 服务健康状态
- `/actuator/gateway/routes` - 路由信息
- `/actuator/metrics` - 性能指标

### 监控指标
- 请求响应时间
- 错误率统计
- 活跃连接数
- 限流触发次数

## ⚙️ 配置说明

### 基础配置
```yaml
server:
  port: 8080

spring:
  application:
    name: evcs-gateway
```

### 限流配置
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rate-limited-route
          uri: http://localhost:8081
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

## 🔗 相关链接

- [API文档](../../docs/references/API-DOCUMENTATION.md#evcs-gateway)
- [项目文档](../../docs/README.md)
- [开发指南](../../docs/development/DEVELOPER-GUIDE.md)
- [架构设计](../../docs/architecture/architecture.md)

## 🧪 测试

```bash
# 运行单元测试
./gradlew :evcs-gateway:test

# 运行集成测试
./gradlew :evcs-gateway:integrationTest
```

## 🚨 故障排查

### 常见问题
1. **503 Service Unavailable**: 检查下游服务是否启动
2. **401 Unauthorized**: 检查JWT令牌是否有效
3. **429 Too Many Requests**: 检查限流配置

### 日志查看
```bash
# 查看网关日志
docker logs evcs-gateway

# 查看特定路由日志
grep "auth-service" /var/log/evcs/gateway.log
```

---

**服务负责人**: 网关团队
**代码审查**: 架构团队
**部署负责人**: DevOps团队