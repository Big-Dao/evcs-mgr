#!/bin/bash

echo "🚀 启动EVCS充电站管理平台..."

# 检查Java版本
if ! command -v java &> /dev/null; then
    echo "❌ 未找到Java，请安装Java 21或更高版本"
    exit 1
fi

java_version=$(java -version 2>&1 | grep -oP 'version "?(1\.)?\K\d+' | head -1)
if [ "$java_version" -lt 21 ]; then
    echo "❌ 需要Java 21或更高版本，当前版本：$java_version"
    exit 1
fi

echo "✅ Java版本检查通过：$java_version"

# 检查JAR文件是否存在
echo "📦 检查JAR文件..."
required_jars=(
    "evcs-eureka/build/libs/evcs-eureka-1.0.0.jar"
    "evcs-config/build/libs/evcs-config-1.0.0.jar"
    "evcs-auth/build/libs/evcs-auth-1.0.0.jar"
    "evcs-gateway/build/libs/evcs-gateway-1.0.0.jar"
    "evcs-tenant/build/libs/evcs-tenant-1.0.0.jar"
    "evcs-station/build/libs/evcs-station-1.0.0.jar"
    "evcs-order/build/libs/evcs-order-1.0.0.jar"
    "evcs-payment/build/libs/evcs-payment-1.0.0.jar"
    "evcs-protocol/build/libs/evcs-protocol-1.0.0.jar"
    "evcs-monitoring/build/libs/evcs-monitoring-1.0.0.jar"
)

for jar in "${required_jars[@]}"; do
    if [ ! -f "$jar" ]; then
        echo "❌ JAR文件不存在：$jar"
        echo "请先运行：./gradlew build -x test"
        exit 1
    fi
done

echo "✅ 所有JAR文件检查完成"

# 创建日志目录
mkdir -p logs
echo "📁 日志目录已创建：logs/"

# 设置环境变量
export SPRING_PROFILES_ACTIVE=local
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=evcs_mgr
export DB_USER=postgres
export DB_PASSWORD=postgres
export REDIS_HOST=localhost
export REDIS_PORT=6379

echo "🏗️ 启动基础设施服务..."

# 启动Eureka
echo "启动Eureka注册中心..."
java -Xms256m -Xmx512m \
    -Dspring.profiles.active=local \
    -jar evcs-eureka/build/libs/evcs-eureka-1.0.0.jar > logs/eureka.log 2>&1 &
EUREKA_PID=$!
echo "Eureka PID: $EUREKA_PID"

# 等待Eureka启动
echo "等待Eureka启动..."
for i in {1..30}; do
    if curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; then
        echo "✅ Eureka启动成功"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Eureka启动超时，请检查日志：logs/eureka.log"
        exit 1
    fi
    sleep 2
done

# 启动配置中心
echo "启动配置中心..."
java -Xms256m -Xmx512m \
    -Dspring.profiles.active=local \
    -Deureka.client.service-url.defaultZone=http://localhost:8761/eureka \
    -jar evcs-config/build/libs/evcs-config-1.0.0.jar > logs/config.log 2>&1 &
CONFIG_PID=$!
echo "Config Server PID: $CONFIG_PID"

# 等待配置中心启动
echo "等待配置中心启动..."
for i in {1..30}; do
    if curl -s http://localhost:8888/actuator/health > /dev/null 2>&1; then
        echo "✅ 配置中心启动成功"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ 配置中心启动超时，请检查日志：logs/config.log"
        exit 1
    fi
    sleep 2
done

echo "🔧 启动核心服务..."

# 启动认证服务
echo "启动认证服务..."
java -Xms512m -Xmx512m \
    -Dspring.profiles.active=local \
    -Deureka.client.service-url.defaultZone=http://localhost:8761/eureka \
    -Dspring.cloud.config.uri=http://localhost:8888 \
    -jar evcs-auth/build/libs/evcs-auth-1.0.0.jar > logs/auth.log 2>&1 &
AUTH_PID=$!
echo "Auth Service PID: $AUTH_PID"

# 启动网关服务
echo "启动API网关..."
java -Xms512m -Xmx512m \
    -Dspring.profiles.active=local \
    -Deureka.client.service-url.defaultZone=http://localhost:8761/eureka \
    -Dspring.cloud.config.uri=http://localhost:8888 \
    -jar evcs-gateway/build/libs/evcs-gateway-1.0.0.jar > logs/gateway.log 2>&1 &
GATEWAY_PID=$!
echo "Gateway PID: $GATEWAY_PID"

# 等待核心服务启动
sleep 30

echo "🏢 启动业务服务..."

# 启动所有业务服务
declare -A services=(
    ["tenant"]="evcs-tenant/build/libs/evcs-tenant-1.0.0.jar"
    ["station"]="evcs-station/build/libs/evcs-station-1.0.0.jar"
    ["order"]="evcs-order/build/libs/evcs-order-1.0.0.jar"
    ["payment"]="evcs-payment/build/libs/evcs-payment-1.0.0.jar"
    ["protocol"]="evcs-protocol/build/libs/evcs-protocol-1.0.0.jar"
    ["monitoring"]="evcs-monitoring/build/libs/evcs-monitoring-1.0.0.jar"
)

declare -A pids=()

for service in "${!services[@]}"; do
    jar_path="${services[$service]}"
    echo "启动${service}服务..."
    java -Xms512m -Xmx512m \
        -Dspring.profiles.active=local \
        -Deureka.client.service-url.defaultZone=http://localhost:8761/eureka \
        -Dspring.cloud.config.uri=http://localhost:8888 \
        -jar "$jar_path" > logs/${service}.log 2>&1 &
    pids["$service"]=$!
    echo "${service} PID: ${pids[$service]}"
done

# 保存PID到文件
echo "EUREKA_PID=$EUREKA_PID" > .service-pids
echo "CONFIG_PID=$CONFIG_PID" >> .service-pids
echo "AUTH_PID=$AUTH_PID" >> .service-pids
echo "GATEWAY_PID=$GATEWAY_PID" >> .service-pids
for service in "${!pids[@]}"; do
    echo "${service^^}_PID=${pids[$service]}" >> .service-pids
done

echo ""
echo "🎉 所有服务启动完成！"
echo ""
echo "📊 服务访问地址："
echo "Eureka注册中心:   http://localhost:8761"
echo "配置中心:        http://localhost:8888"
echo "API网关:         http://localhost:8080"
echo "认证服务:        http://localhost:8081"
echo "充电站服务:      http://localhost:8082"
echo "订单服务:        http://localhost:8083"
echo "支付服务:        http://localhost:8084"
echo "协议服务:        http://localhost:8085"
echo "租户服务:        http://localhost:8086"
echo "监控服务:        http://localhost:8087"
echo ""
echo "📝 查看日志："
echo "tail -f logs/eureka.log      # Eureka注册中心"
echo "tail -f logs/config.log       # 配置中心"
echo "tail -f logs/gateway.log      # API网关"
echo "tail -f logs/auth.log         # 认证服务"
echo "tail -f logs/station.log      # 充电站服务"
echo "tail -f logs/order.log        # 订单服务"
echo "tail -f logs/payment.log      # 支付服务"
echo ""
echo "🛑 停止所有服务："
echo "./stop-services.sh"
echo ""
echo "🔍 健康检查："
echo "./health-check.sh"