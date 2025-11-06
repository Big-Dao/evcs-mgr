# 服务名称映射表

## Docker Compose 服务名（官方标准）
基于 `docker-compose.local-images.yml` 的定义：

| 服务名 | 描述 | 内部端口 | 备注 |
|--------|------|----------|------|
| `postgres` | PostgreSQL数据库 | 5432 | 基础设施 |
| `redis` | Redis缓存 | 6379 | 基础设施 |
| `rabbitmq` | RabbitMQ消息队列 | 5672/15672 | 基础设施 |
| `eureka` | 服务注册中心 | 8761 | 微服务核心 |
| `config` | 配置服务器 | 8888 | 微服务核心 |
| `gateway` | API网关 | 8080 | **核心路由** |
| `auth` | 认证服务 | 8081 | 用户认证 |
| `tenant` | 租户管理服务 | 8083 | 业务服务 |
| `station` | 站点管理服务 | 8082 | 业务服务 |
| `order` | 订单服务 | 8084 | 业务服务 |
| `payment` | 支付服务 | 8085 | 业务服务 |
| `evcs-admin` | 前端管理界面 | 80 | Web界面 |

## 服务名使用规则

### ✅ 正确用法
- Nginx配置: `proxy_pass http://gateway:8080/`
- Spring配置: `uri: lb://auth`
- 服务调用: `http://tenant:8083/api`

### ❌ 错误用法
- `gateway-service` → 应该是 `gateway`
- `evcs-auth` → 应该是 `auth`
- `tenant-service` → 应该是 `tenant`

## 配置文件检查要点

1. **docker-compose.yml** - 服务名定义的权威来源
2. **nginx.conf** - proxy_pass中的服务名必须与docker-compose一致
3. **Config Server配置** - lb://后面的服务名
4. **application.yml** - Eureka注册和发现的服务名

## 端口映射表

| 服务 | 内部端口 | 外部端口 | 说明 |
|------|----------|----------|------|
| gateway | 8080 | 8080 | API网关 |
| auth | 8081 | 8081 | 认证服务 |
| station | 8082 | 8082 | 站点服务 |
| tenant | 8083 | 8083 | 租户服务 |
| order | 8084 | 8084 | 订单服务 |
| payment | 8085 | 8085 | 支付服务 |
| evcs-admin | 80 | 8086 | 前端界面 |

**重要**: 内部服务间通信使用内部端口，外部访问使用映射端口。