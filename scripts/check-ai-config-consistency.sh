#!/bin/bash

# EVCS AI助手配置一致性检查脚本
# 检查所有AI助手配置文件中的微服务模块描述一致性

echo "🔍 开始检查AI助手配置一致性..."

# 定义期望的微服务列表
declare -A expected_services=(
    ["evcs-gateway"]="API网关，路由和安全防护"
    ["evcs-auth"]="认证授权服务，JWT + RBAC"
    ["evcs-station"]="充电站管理，设备控制"
    ["evcs-order"]="订单管理，计费方案"
    ["evcs-payment"]="支付服务，支付宝/微信"
    ["evcs-protocol"]="协议处理，OCPP/云快充"
    ["evcs-tenant"]="租户管理，多租户隔离"
    ["evcs-monitoring"]="监控服务，健康检查"
    ["evcs-config"]="配置中心，Git配置"
    ["evcs-eureka"]="服务注册中心"
    ["evcs-common"]="公共组件，共享工具类"
)

# 配置文件列表
config_files=(
    "PROJECT-CODING-STANDARDS.md"
    ".claude/project-instructions.md"
    ".github/copilot-instructions.md"
    ".codex/project-context.md"
)

# 检查结果
issues=0

echo "📋 检查微服务模块描述一致性..."

for service in "${!expected_services[@]}"; do
    expected_desc="${expected_services[$service]}"
    echo ""
    echo "🔍 检查服务: $service"
    echo "   期望描述: $expected_desc"

    for config_file in "${config_files[@]}"; do
        if [[ -f "$config_file" ]]; then
            # 检查配置文件中是否包含该服务
            if grep -q "$service" "$config_file"; then
                # 提取实际描述
                actual_desc=$(grep "$service" "$config_file" | sed "s/.*$service[^-]*[- ]*\(.*\)/\1/" | head -1)

                # 简化比较，只检查关键词
                if [[ "$actual_desc" == *"API网关"* && "$expected_desc" == *"API网关"* ]] || \
                   [[ "$actual_desc" == *"认证授权"* && "$expected_desc" == *"认证授权"* ]] || \
                   [[ "$actual_desc" == *"充电站管理"* && "$expected_desc" == *"充电站管理"* ]] || \
                   [[ "$actual_desc" == *"订单管理"* && "$expected_desc" == *"订单管理"* ]] || \
                   [[ "$actual_desc" == *"支付服务"* && "$expected_desc" == *"支付服务"* ]] || \
                   [[ "$actual_desc" == *"协议处理"* && "$expected_desc" == *"协议处理"* ]] || \
                   [[ "$actual_desc" == *"租户管理"* && "$expected_desc" == *"租户管理"* ]] || \
                   [[ "$actual_desc" == *"监控服务"* && "$expected_desc" == *"监控服务"* ]] || \
                   [[ "$actual_desc" == *"配置中心"* && "$expected_desc" == *"配置中心"* ]] || \
                   [[ "$actual_desc" == *"服务注册中心"* && "$expected_desc" == *"服务注册中心"* ]] || \
                   [[ "$actual_desc" == *"公共组件"* && "$expected_desc" == *"公共组件"* ]]; then
                    echo "   ✅ $config_file: 描述一致"
                else
                    echo "   ❌ $config_file: 描述不一致 - $actual_desc"
                    ((issues++))
                fi
            else
                echo "   ⚠️  $config_file: 未找到该服务"
                ((issues++))
            fi
        else
            echo "   ❌ $config_file: 文件不存在"
            ((issues++))
        fi
    done
done

echo ""
echo "📊 检查完成"
echo "  📁 检查配置文件数: ${#config_files[@]}"
echo "  🔍 检查服务数: ${#expected_services[@]}"
echo "  ❌ 发现问题数: $issues"

if [ $issues -eq 0 ]; then
    echo "  ✅ 所有AI助手配置的微服务描述一致"
    exit 0
else
    echo "  🚨 发现配置不一致问题，请检查上述报告"
    exit 1
fi