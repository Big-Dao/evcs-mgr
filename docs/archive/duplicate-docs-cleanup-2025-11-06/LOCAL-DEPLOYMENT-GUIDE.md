# 本地部署指南 (无Docker版本)

由于Docker Hub连接问题，这里提供本地Java直接运行的部署方案。

## 📋 前置条件

- Java 21+
- PostgreSQL 15+
- Redis 7+
- RabbitMQ 3.12+ (可选)

## 🚀 快速启动

### 1. 启动基础服务 (如果本地有安装)

如果本地已安装PostgreSQL、Redis、RabbitMQ，可以直接启动：

```bash
# PostgreSQL
sudo systemctl start postgresql

# Redis
sudo systemctl start redis

# RabbitMQ (可选)
sudo systemctl start rabbitmq-server
```

### 2. 创建数据库

```bash
# 连接PostgreSQL
psql -U postgres

# 创建数据库
CREATE DATABASE evcs_mgr;

# 退出
\q
```

### 3. 初始化数据库表

```bash
# 执行初始化SQL脚本
psql -U postgres -d evcs_mgr -f sql/init.sql
psql -U postgres -d evcs_mgr -f sql/charging_station_tables.sql
psql -U postgres -d evcs_mgr -f sql/evcs_order_tables.sql
```

## 🔧 启动应用服务

### 方式一：使用启动脚本

```bash
# 启动所有核心服务
./scripts/start-services.sh
```

### 方式二：手动启动

```bash
# 1. 启动Eureka注册中心
java -Xms256m -Xmx512m -jar evcs-eureka/build/libs/evcs-eureka-1.0.0.jar &
sleep 30

# 2. 启动配置中心
java -Xms256m -Xmx512m -jar evcs-config/build/libs/evcs-config-1.0.0.jar &
sleep 30

# 3. 启动认证服务
java -Xms512m -Xmx512m -jar evcs-auth/build/libs/evcs-auth-1.0.0.jar &
sleep 20

# 4. 启动网关服务
java -Xms512m -Xmx512m -jar evcs-gateway/build/libs/evcs-gateway-1.0.0.jar &
sleep 20

# 5. 启动业务服务 (并行启动)
java -Xms512m -Xmx512m -jar evcs-tenant/build/libs/evcs-tenant-1.0.0.jar &
java -Xms512m -Xmx512m -jar evcs-station/build/libs/evcs-station-1.0.0.jar &
java -Xms512m -Xmx512m -jar evcs-order/build/libs/evcs-order-1.0.0.jar &
java -Xms512m -Xmx512m -jar evcs-payment/build/libs/evcs-payment-1.0.0.jar &
java -Xms512m -Xmx512m -jar evcs-protocol/build/libs/evcs-protocol-1.0.0.jar &
java -Xms512m -Xmx512m -jar evcs-monitoring/build/libs/evcs-monitoring-1.0.0.jar &
```

## ⚙️ 环境配置

创建环境变量文件 `.env`：

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=5432
DB_NAME=evcs_mgr
DB_USER=postgres
DB_PASSWORD=postgres

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379

# RabbitMQ配置
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# 服务端口配置
EUREKA_PORT=8761
CONFIG_PORT=8888
GATEWAY_PORT=8080
AUTH_PORT=8081
STATION_PORT=8082
ORDER_PORT=8083
PAYMENT_PORT=8084
PROTOCOL_PORT=8085
TENANT_PORT=8086
MONITORING_PORT=8087
```

## 🌐 访问地址

启动成功后，可以通过以下地址访问：

- **Eureka注册中心**: http://localhost:8761
- **配置中心**: http://localhost:8888
- **API网关**: http://localhost:8080
- **认证服务**: http://localhost:8081
- **充电站服务**: http://localhost:8082
- **订单服务**: http://localhost:8083
- **支付服务**: http://localhost:8084
- **协议服务**: http://localhost:8085
- **租户服务**: http://localhost:8086
- **监控服务**: http://localhost:8087

## 📝 启动脚本示例

创建 `start-services.sh`：

```bash
#!/bin/bash

echo "🚀 启动EVCS充电站管理平台..."

# 检查Java版本
java_version=$(java -version 2>&1 | grep -oP 'version "?(1\.)?\K\d+' | head -1)
if [ "$java_version" -lt 21 ]; then
    echo "❌ 需要Java 21或更高版本"
    exit 1
fi

# 创建日志目录
mkdir -p logs

echo "📦 启动基础设施服务..."

# 启动Eureka
echo "启动Eureka注册中心..."
java -Xms256m -Xmx512m -jar evcs-eureka/build/libs/evcs-eureka-1.0.0.jar > logs/eureka.log 2>&1 &
EUREKA_PID=$!
echo "Eureka PID: $EUREKA_PID"

# 等待Eureka启动
sleep 30

# 启动配置中心
echo "启动配置中心..."
java -Xms256m -Xmx512m -jar evcs-config/build/libs/evcs-config-1.0.0.jar > logs/config.log 2>&1 &
CONFIG_PID=$!
echo "Config Server PID: $CONFIG_PID"

# 等待配置中心启动
sleep 30

echo "🔧 启动核心服务..."

# 启动认证服务
echo "启动认证服务..."
java -Xms512m -Xmx512m -jar evcs-auth/build/libs/evcs-auth-1.0.0.jar > logs/auth.log 2>&1 &
AUTH_PID=$!

# 启动网关服务
echo "启动API网关..."
java -Xms512m -Xmx512m -jar evcs-gateway/build/libs/evcs-gateway-1.0.0.jar > logs/gateway.log 2>&1 &
GATEWAY_PID=$!

# 等待核心服务启动
sleep 20

echo "🏢 启动业务服务..."

# 启动所有业务服务
services=("tenant" "station" "order" "payment" "protocol" "monitoring")
pids=()

for service in "${services[@]}"; do
    echo "启动${service}服务..."
    java -Xms512m -Xmx512m -jar evcs-${service}/build/libs/evcs-${service}-1.0.0.jar > logs/${service}.log 2>&1 &
    pids+=($!)
    echo "${service} PID: ${pids[-1]}"
done

echo "✅ 所有服务启动完成！"
echo ""
echo "📊 服务状态："
echo "Eureka: http://localhost:8761"
echo "API网关: http://localhost:8080"
echo ""
echo "📝 查看日志："
echo "tail -f logs/eureka.log"
echo "tail -f logs/gateway.log"
echo ""
echo "🛑 停止所有服务："
echo "./stop-services.sh"
```

创建 `stop-services.sh`：

```bash
#!/bin/bash

echo "🛑 停止EVCS充电站管理平台..."

# 停止所有Java进程
pkill -f "evcs-.*-1.0.0.jar"

echo "✅ 所有服务已停止"
```

## 🔍 健康检查

检查服务是否正常运行：

```bash
# 检查服务状态
curl -s http://localhost:8761/actuator/health
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8081/actuator/health

# 查看服务注册情况
curl -s http://localhost:8761/eureka/apps
```

## 🐛 故障排查

### 常见问题

1. **端口冲突**
   - 检查端口是否被占用：`netstat -tlnp | grep :8080`
   - 修改配置文件中的端口设置

2. **数据库连接失败**
   - 检查PostgreSQL是否运行：`systemctl status postgresql`
   - 检查数据库是否存在：`psql -U postgres -l`

3. **Redis连接失败**
   - 检查Redis是否运行：`systemctl status redis`
   - 测试连接：`redis-cli ping`

4. **内存不足**
   - 调整JVM参数：减少 `-Xms` 和 `-Xmx` 值
   - 增加系统内存或关闭其他应用

### 查看日志

```bash
# 查看所有服务日志
tail -f logs/*.log

# 查看特定服务日志
tail -f logs/gateway.log
tail -f logs/auth.log
```

## 📈 性能调优

### JVM参数优化

```bash
# 生产环境推荐参数
java -Xms1g -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -jar your-service.jar
```

### 数据库连接池优化

在 `application.yml` 中配置：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```

## 🔄 后续步骤

1. **配置数据**: 添加初始租户、用户数据
2. **前端部署**: 部署Vue.js管理界面
3. **监控配置**: 配置Prometheus + Grafana
4. **负载均衡**: 配置Nginx反向代理

## 📞 技术支持

如遇问题，请查看：
1. 日志文件：`logs/` 目录
2. 健康检查端点：`/actuator/health`
3. 服务注册中心：`http://localhost:8761`