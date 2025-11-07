#!/bin/bash

echo "🔍 EVCS充电站管理平台健康检查"
echo "================================="

# 服务端点配置
declare -A services=(
    ["Eureka注册中心"]="http://localhost:8761/actuator/health"
    ["配置中心"]="http://localhost:8888/actuator/health"
    ["API网关"]="http://localhost:8080/actuator/health"
    ["认证服务"]="http://localhost:8081/actuator/health"
    ["充电站服务"]="http://localhost:8082/actuator/health"
    ["订单服务"]="http://localhost:8083/actuator/health"
    ["支付服务"]="http://localhost:8084/actuator/health"
    ["协议服务"]="http://localhost:8085/actuator/health"
    ["租户服务"]="http://localhost:8086/actuator/health"
    ["监控服务"]="http://localhost:8087/actuator/health"
)

echo ""
echo "📊 服务健康状态："
echo ""

all_healthy=true

for service in "${!services[@]}"; do
    url="${services[$service]}"

    # 使用curl检查健康状态
    if command -v curl &> /dev/null; then
        health_response=$(curl -s --max-time 5 "$url" 2>/dev/null)

        if [ $? -eq 0 ] && echo "$health_response" | grep -q '"status":"UP"'; then
            status="✅ 健康"
            response_time=$(curl -o /dev/null -s -w "%{time_total}" --max-time 5 "$url" 2>/dev/null)
            echo "$service: $status (${response_time}s)"
        else
            status="❌ 不可用"
            echo "$service: $status"
            all_healthy=false
        fi
    else
        echo "$service: ⚠️ curl命令不可用，无法检查"
        all_healthy=false
    fi
done

echo ""
echo "📈 Eureka服务注册情况："
echo ""

if command -v curl &> /dev/null; then
    eureka_apps=$(curl -s "http://localhost:8761/eureka/apps" 2>/dev/null)

    if [ $? -eq 0 ]; then
        # 提取注册的服务数量
        registered_count=$(echo "$eureka_apps" | grep -o '<name>[^<]*' | wc -l)
        echo "已注册服务数量: $registered_count"

        # 列出所有注册的服务
        echo ""
        echo "注册的服务列表："
        echo "$eureka_apps" | grep -o '<name>[^<]*' | sed 's/<name>//' | while read -r service; do
            if [ -n "$service" ]; then
                echo "  - $service"
            fi
        done
    else
        echo "❌ 无法连接到Eureka服务器"
        all_healthy=false
    fi
else
    echo "⚠️ curl命令不可用，无法检查Eureka注册情况"
    all_healthy=false
fi

echo ""
echo "📊 系统资源使用情况："
echo ""

# 检查Java进程
java_processes=$(pgrep -f "evcs-.*-1.0.0.jar" | wc -l)
echo "运行中的Java服务进程数: $java_processes"

# 检查内存使用
if command -v free &> /dev/null; then
    memory_info=$(free -h | grep Mem)
    total_memory=$(echo $memory_info | awk '{print $2}')
    used_memory=$(echo $memory_info | awk '{print $3}')
    echo "内存使用: $used_memory / $total_memory"
fi

# 检查磁盘使用
if command -v df &> /dev/null; then
    disk_usage=$(df -h . | tail -1 | awk '{print $5}')
    echo "磁盘使用: $disk_usage"
fi

echo ""
echo "📝 最近的服务日志："
echo ""

if [ -d "logs" ]; then
    # 显示每个服务的最后几行日志
    for log_file in logs/*.log; do
        if [ -f "$log_file" ]; then
            service_name=$(basename "$log_file" .log)
            echo "=== $service_name 服务日志 (最后5行) ==="
            tail -5 "$log_file" 2>/dev/null | sed 's/^/  /'
            echo ""
        fi
    done
else
    echo "⚠️ 日志目录不存在"
fi

echo ""
if [ "$all_healthy" = true ]; then
    echo "🎉 所有服务运行正常！"
    exit 0
else
    echo "⚠️ 部分服务存在问题，请检查相关日志"
    exit 1
fi