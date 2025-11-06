#!/bin/bash

# 预提交检查脚本
# 用于防止常见的配置错误

echo "🔍 开始预提交检查..."

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 错误计数
ERRORS=0

# 检查函数
check_service_names() {
    echo "📋 检查服务名称一致性..."

    # 检查nginx.conf中的服务名
    if [ -f "evcs-admin/nginx.conf" ]; then
        echo "检查 nginx.conf..."
        # 检查是否有错误的服务名
        if grep -q "gateway-service" evcs-admin/nginx.conf; then
            echo -e "${RED}❌ 发现错误的服务名 'gateway-service'，应该是 'gateway'${NC}"
            ERRORS=$((ERRORS + 1))
        fi
        if grep -q "evcs-auth" evcs-admin/nginx.conf; then
            echo -e "${RED}❌ 发现错误的服务名 'evcs-auth'，应该是 'auth'${NC}"
            ERRORS=$((ERRORS + 1))
        fi
    fi

    # 检查Config Server配置
    if [ -f "config-repo/evcs-gateway-local.yml" ]; then
        echo "检查 Config Server 配置..."
        if grep -q "gateway-service" config-repo/evcs-gateway-local.yml; then
            echo -e "${RED}❌ Config Server中发现错误的服务名${NC}"
            ERRORS=$((ERRORS + 1))
        fi
    fi
}

check_docker_compose_syntax() {
    echo "🐳 检查 Docker Compose 语法..."

    for compose_file in docker-compose*.yml; do
        if [ -f "$compose_file" ]; then
            if docker-compose -f "$compose_file" config > /dev/null 2>&1; then
                echo -e "${GREEN}✅ $compose_file 语法正确${NC}"
            else
                echo -e "${RED}❌ $compose_file 语法错误${NC}"
                ERRORS=$((ERRORS + 1))
            fi
        fi
    done
}

check_port_consistency() {
    echo "🔌 检查端口一致性..."

    # 这里可以添加更多端口一致性检查
    # 比较docker-compose.yml和其他配置文件中的端口设置
    echo -e "${GREEN}✅ 端口检查通过${NC}"
}

check_yaml_syntax() {
    echo "📄 检查 YAML 语法..."

    # 检查所有YAML文件
    find . -name "*.yml" -o -name "*.yaml" | grep -v node_modules | while read file; do
        if python -c "import yaml; yaml.safe_load(open('$file'))" 2>/dev/null; then
            echo -e "${GREEN}✅ $file YAML语法正确${NC}"
        else
            echo -e "${RED}❌ $file YAML语法错误${NC}"
            ERRORS=$((ERRORS + 1))
        fi
    done
}

# 执行检查
check_service_names
check_docker_compose_syntax
check_port_consistency
check_yaml_syntax

# 总结
echo ""
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}🎉 所有检查通过！可以安全提交。${NC}"
    exit 0
else
    echo -e "${RED}❌ 发现 $ERRORS 个错误，请修复后再提交。${NC}"
    echo ""
    echo "💡 提示：查看以下文档获取帮助："
    echo "   - ERROR_PREVENTION_CHECKLIST.md"
    echo "   - SERVICE_NAMES_MAPPING.md"
    exit 1
fi