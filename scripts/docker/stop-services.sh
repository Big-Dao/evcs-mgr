#!/bin/bash

echo "🛑 停止EVCS充电站管理平台..."

# 从PID文件读取进程ID
if [ -f .service-pids ]; then
    source .service-pids

    # 停止所有服务
    services=(
        "EUREKA_PID:eureka"
        "CONFIG_PID:config"
        "AUTH_PID:auth"
        "GATEWAY_PID:gateway"
        "TENANT_PID:tenant"
        "STATION_PID:station"
        "ORDER_PID:order"
        "PAYMENT_PID:payment"
        "PROTOCOL_PID:protocol"
        "MONITORING_PID:monitoring"
    )

    for service_info in "${services[@]}"; do
        pid_var="${service_info%%:*}"
        service_name="${service_info##*:}"
        pid="${!pid_var}"

        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            echo "停止${service_name}服务 (PID: $pid)..."
            kill "$pid"
            # 等待进程结束
            for i in {1..10}; do
                if ! kill -0 "$pid" 2>/dev/null; then
                    echo "✅ ${service_name}服务已停止"
                    break
                fi
                sleep 1
            done

            # 如果进程仍然存在，强制杀死
            if kill -0 "$pid" 2>/dev/null; then
                echo "强制停止${service_name}服务..."
                kill -9 "$pid"
            fi
        else
            echo "⚠️ ${service_name}服务未运行或PID无效"
        fi
    done

    # 删除PID文件
    rm -f .service-pids
else
    echo "⚠️ 未找到PID文件，尝试通过进程名停止服务..."

    # 通过进程名停止所有Java服务
    pids=$(pgrep -f "evcs-.*-1.0.0.jar")
    if [ -n "$pids" ]; then
        echo "找到运行中的Java服务进程：$pids"
        echo "停止所有Java服务..."
        echo "$pids" | xargs kill

        # 等待进程结束
        sleep 5

        # 检查是否还有进程在运行
        remaining_pids=$(pgrep -f "evcs-.*-1.0.0.jar")
        if [ -n "$remaining_pids" ]; then
            echo "强制停止剩余进程..."
            echo "$remaining_pids" | xargs kill -9
        fi
    else
        echo "⚠️ 未找到运行中的Java服务"
    fi
fi

echo "✅ 所有服务已停止"