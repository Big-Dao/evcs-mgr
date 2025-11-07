# EVCS 系统部署指南

## 针对小规模业务的优化配置

### 🎯 配置文件结构

经过优化，项目现在提供3个核心配置文件：

1. **docker-compose.yml** - 生产环境（完整功能）
2. **docker-compose.core-dev.yml** - 核心开发环境（推荐）
3. **docker-compose.monitoring.yml** - 监控服务（可选）

### 🚀 快速启动

#### 开发环境（推荐）
```bash
# 启动核心服务：基础设施 + 认证 + 网关
docker-compose -f docker-compose.core-dev.yml up -d

# 查看服务状态
docker-compose -f docker-compose.core-dev.yml ps

# 查看日志
docker-compose -f docker-compose.core-dev.yml logs -f
```

#### 生产环境
```bash
# 启动完整服务
docker-compose up -d

# 添加监控服务
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

### 📊 资源优化对比

| 配置类型 | 内存使用 | 服务数量 | 启动时间 | 适用场景 |
|---------|---------|---------|---------|---------|
| 核心开发 | ~2GB | 6个核心服务 | ~2分钟 | 日常开发、测试 |
| 完整生产 | ~4GB | 11个服务 | ~4分钟 | 生产环境、完整功能 |
| 监控扩展 | +1GB | +3个监控服务 | +1分钟 | 运维监控 |

### 🔧 服务端口分配

| 服务 | 开发环境 | 生产环境 | 说明 |
|------|---------|---------|------|
| Gateway | 8080 | 8080 | API网关 |
| Auth | 8081 | 8081 | 认证服务 |
| Station | 8082 | 8082 | 充电站服务 |
| Order | 8083 | 8083 | 订单服务 |
| Payment | 8084 | 8084 | 支付服务 |
| Protocol | 8085 | 8085 | 协议服务 |
| Tenant | 8086 | 8086 | 租户服务 |
| Monitoring | 8087 | 8087 | 监控服务 |
| Eureka | 8761 | 8761 | 服务注册 |
| Config | 8888 | 8888 | 配置中心 |
| PostgreSQL | 5432 | 5432 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |

### 🛠️ 开发工作流

#### 1. 本地开发
```bash
# 启动核心环境
docker-compose -f docker-compose.core-dev.yml up -d

# 等待服务启动完成（约2分钟）
curl http://localhost:8761/actuator/health  # Eureka
curl http://localhost:8080/api/auth/test     # Gateway + Auth

# 开发和测试
# API测试: http://localhost:8080/api/auth/login
```

#### 2. 添加业务服务
```bash
# 当需要其他业务服务时，启动完整环境
docker-compose up -d station-service order-service

# 或者根据需要选择性启动
docker-compose up -d station-service
```

#### 3. 监控调试
```bash
# 添加监控服务（可选）
docker-compose -f docker-compose.core-dev.yml -f docker-compose.monitoring.yml up -d

# 访问监控面板
# Grafana: http://localhost:3001
# Prometheus: http://localhost:9090
```

### 💡 小规模业务优化要点

#### JVM参数优化
- **内存配置**: `-Xms256m -Xmx512m`（轻量级）
- **容器支持**: `-XX:+UseContainerSupport`
- **内存百分比**: `-XX:MaxRAMPercentage=60.0`
- **移除复杂配置**: 去除了FlightRecording等重型监控

#### 服务精简
- **核心服务**: 只保留必要的6个服务
- **按需扩展**: 根据业务需要逐步添加其他服务
- **资源节省**: 相比完整环境节省50%内存

#### 构建优化
- **统一Gradle版本**: 8.11-jdk21-alpine
- **统一Dockerfile**: 移除重复的Dockerfile.simple
- **环境变量标准化**: 使用Spring Boot标准格式

### 🔍 故障排查

#### 常见问题
1. **端口冲突**: 确保端口未被占用
2. **内存不足**: 检查Docker Desktop内存设置
3. **服务启动慢**: 数据库初始化需要1-2分钟

#### 健康检查命令
```bash
# 检查所有服务状态
docker-compose -f docker-compose.core-dev.yml ps

# 检查服务健康状态
curl http://localhost:8761/actuator/health  # Eureka
curl http://localhost:8888/actuator/health   # Config
curl http://localhost:8080/actuator/health   # Gateway
curl http://localhost:8081/actuator/health   # Auth
```

### 📝 配置说明

#### 核心开发环境包含：
- ✅ PostgreSQL (数据库)
- ✅ Redis (缓存)
- ✅ Eureka (服务注册)
- ✅ Config Server (配置中心)
- ✅ Gateway (API网关)
- ✅ Auth Service (认证服务)

#### 完整生产环境额外包含：
- 🏢 Station Service (充电站管理)
- 📦 Order Service (订单管理)
- 💳 Payment Service (支付服务)
- 📡 Protocol Service (协议服务)
- 👥 Tenant Service (租户管理)
- 📊 Monitoring Service (监控服务)

这个优化方案特别适合小规模业务，既保证了核心功能，又大大降低了资源消耗和复杂度。