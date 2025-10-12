#!/bin/bash
# 冒烟测试脚本
# 用途：验证测试环境基本功能是否正常

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "=========================================="
echo "EVCS Manager - 冒烟测试"
echo "=========================================="
echo ""

# 测试计数器
total_tests=0
passed_tests=0
failed_tests=0

# 测试函数
run_test() {
    local test_name=$1
    local test_command=$2
    
    total_tests=$((total_tests + 1))
    echo -e "${YELLOW}测试 #${total_tests}: ${test_name}${NC}"
    
    if eval "$test_command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 通过${NC}"
        passed_tests=$((passed_tests + 1))
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        failed_tests=$((failed_tests + 1))
        return 1
    fi
}

# API测试函数
test_api() {
    local test_name=$1
    local method=$2
    local url=$3
    local expected_status=${4:-200}
    
    total_tests=$((total_tests + 1))
    echo -e "${YELLOW}测试 #${total_tests}: ${test_name}${NC}"
    
    response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" || echo "000")
    status_code=$(echo "$response" | tail -n 1)
    
    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✓ 通过 (状态码: ${status_code})${NC}"
        passed_tests=$((passed_tests + 1))
        return 0
    else
        echo -e "${RED}✗ 失败 (期望: ${expected_status}, 实际: ${status_code})${NC}"
        failed_tests=$((failed_tests + 1))
        return 1
    fi
}

echo -e "${CYAN}=== 基础设施服务测试 ===${NC}"
echo ""

# PostgreSQL测试
run_test "PostgreSQL连接测试" \
    "docker exec evcs-postgres-test psql -U evcs_test -d evcs_mgr_test -c 'SELECT 1'"

# Redis测试
run_test "Redis连接测试" \
    "docker exec evcs-redis-test redis-cli -a test_redis_123 PING | grep -q PONG"

# RabbitMQ测试
run_test "RabbitMQ连接测试" \
    "curl -s -u evcs_test:test_mq_123 http://localhost:15672/api/overview | grep -q version"

echo ""
echo -e "${CYAN}=== 应用服务健康检查测试 ===${NC}"
echo ""

# 租户服务健康检查
test_api "租户服务健康检查" "GET" "http://localhost:8081/actuator/health" 200

# 充电站服务健康检查
test_api "充电站服务健康检查" "GET" "http://localhost:8082/actuator/health" 200

echo ""
echo -e "${CYAN}=== Spring Boot Actuator端点测试 ===${NC}"
echo ""

# 租户服务信息端点
test_api "租户服务Info端点" "GET" "http://localhost:8081/actuator/info" 200

# 充电站服务信息端点
test_api "充电站服务Info端点" "GET" "http://localhost:8082/actuator/info" 200

echo ""
echo -e "${CYAN}=== 数据库表结构测试 ===${NC}"
echo ""

# 检查核心表是否存在
run_test "检查sys_tenant表" \
    "docker exec evcs-postgres-test psql -U evcs_test -d evcs_mgr_test -c '\dt sys_tenant' | grep -q sys_tenant"

run_test "检查sys_user表" \
    "docker exec evcs-postgres-test psql -U evcs_test -d evcs_mgr_test -c '\dt sys_user' | grep -q sys_user"

run_test "检查charging_station表" \
    "docker exec evcs-postgres-test psql -U evcs_test -d evcs_mgr_test -c '\dt charging_station' | grep -q charging_station"

run_test "检查charger表" \
    "docker exec evcs-postgres-test psql -U evcs_test -d evcs_mgr_test -c '\dt charger' | grep -q charger"

echo ""
echo -e "${CYAN}=== 服务依赖测试 ===${NC}"
echo ""

# 测试租户服务能访问数据库
run_test "租户服务数据库连接" \
    "docker logs evcs-tenant-test 2>&1 | grep -q 'HikariPool-1 - Start completed'"

# 测试充电站服务能访问数据库
run_test "充电站服务数据库连接" \
    "docker logs evcs-station-test 2>&1 | grep -q 'HikariPool-1 - Start completed'"

echo ""
echo "=========================================="
echo -e "${CYAN}测试结果统计${NC}"
echo "=========================================="
echo "总测试数: $total_tests"
echo -e "${GREEN}通过: $passed_tests${NC}"
echo -e "${RED}失败: $failed_tests${NC}"
echo ""

if [ $failed_tests -eq 0 ]; then
    success_rate=100
else
    success_rate=$((passed_tests * 100 / total_tests))
fi

echo "成功率: ${success_rate}%"
echo ""

if [ $failed_tests -eq 0 ]; then
    echo "=========================================="
    echo -e "${GREEN}冒烟测试全部通过! ✓${NC}"
    echo "=========================================="
    echo ""
    echo -e "${CYAN}测试环境可以开始人工测试！${NC}"
    echo ""
    echo "访问地址："
    echo "  租户服务API: http://localhost:8081"
    echo "  充电站服务API: http://localhost:8082"
    echo "  数据库管理: http://localhost:8090"
    echo "  RabbitMQ管理: http://localhost:15672"
    echo ""
    exit 0
else
    echo "=========================================="
    echo -e "${RED}部分冒烟测试失败 ✗${NC}"
    echo "=========================================="
    echo ""
    echo -e "${YELLOW}排查建议:${NC}"
    echo "  1. 检查服务日志: docker-compose -f docker-compose.test.yml logs"
    echo "  2. 检查容器状态: docker-compose -f docker-compose.test.yml ps"
    echo "  3. 重新部署环境: ./scripts/start-test.sh"
    echo ""
    exit 1
fi
