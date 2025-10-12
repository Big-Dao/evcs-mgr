#!/bin/bash
# 健康检查脚本
# 用途：验证测试环境所有服务是否正常运行

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "=========================================="
echo "EVCS Manager - 健康检查"
echo "=========================================="
echo ""

# 检查函数
check_service() {
    local service_name=$1
    local url=$2
    local max_retries=${3:-30}
    local retry_interval=${4:-2}
    
    echo -e "${YELLOW}检查 ${service_name}...${NC}"
    
    for i in $(seq 1 $max_retries); do
        if curl -f -s -o /dev/null "$url"; then
            echo -e "${GREEN}✓ ${service_name} 健康检查通过${NC}"
            return 0
        fi
        
        if [ $i -lt $max_retries ]; then
            echo -n "."
            sleep $retry_interval
        fi
    done
    
    echo ""
    echo -e "${RED}✗ ${service_name} 健康检查失败 (${max_retries} 次重试后)${NC}"
    return 1
}

# 检查Docker容器状态
check_container_status() {
    local container_name=$1
    
    if docker ps --filter "name=${container_name}" --filter "status=running" --format "{{.Names}}" | grep -q "${container_name}"; then
        echo -e "${GREEN}✓ 容器 ${container_name} 正在运行${NC}"
        return 0
    else
        echo -e "${RED}✗ 容器 ${container_name} 未运行${NC}"
        return 1
    fi
}

# 检查数据库连接
check_database() {
    echo -e "${YELLOW}检查 PostgreSQL 数据库...${NC}"
    
    if docker exec evcs-postgres-test pg_isready -U evcs_test -d evcs_mgr_test > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PostgreSQL 数据库连接正常${NC}"
        return 0
    else
        echo -e "${RED}✗ PostgreSQL 数据库连接失败${NC}"
        return 1
    fi
}

# 检查Redis连接
check_redis() {
    echo -e "${YELLOW}检查 Redis 缓存...${NC}"
    
    if docker exec evcs-redis-test redis-cli -a test_redis_123 ping > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Redis 缓存连接正常${NC}"
        return 0
    else
        echo -e "${RED}✗ Redis 缓存连接失败${NC}"
        return 1
    fi
}

# 检查RabbitMQ连接
check_rabbitmq() {
    echo -e "${YELLOW}检查 RabbitMQ 消息队列...${NC}"
    
    if curl -f -s -u evcs_test:test_mq_123 http://localhost:15672/api/overview > /dev/null 2>&1; then
        echo -e "${GREEN}✓ RabbitMQ 消息队列连接正常${NC}"
        return 0
    else
        echo -e "${RED}✗ RabbitMQ 消息队列连接失败${NC}"
        return 1
    fi
}

# 执行健康检查
failed=0

echo -e "${CYAN}=== 容器状态检查 ===${NC}"
check_container_status "evcs-postgres-test" || failed=$((failed + 1))
check_container_status "evcs-redis-test" || failed=$((failed + 1))
check_container_status "evcs-rabbitmq-test" || failed=$((failed + 1))
check_container_status "evcs-tenant-test" || failed=$((failed + 1))
check_container_status "evcs-station-test" || failed=$((failed + 1))

echo ""
echo -e "${CYAN}=== 基础设施服务检查 ===${NC}"
check_database || failed=$((failed + 1))
check_redis || failed=$((failed + 1))
check_rabbitmq || failed=$((failed + 1))

echo ""
echo -e "${CYAN}=== 应用服务健康检查 ===${NC}"
check_service "租户服务" "http://localhost:8081/actuator/health" 30 2 || failed=$((failed + 1))
check_service "充电站服务" "http://localhost:8082/actuator/health" 30 2 || failed=$((failed + 1))

echo ""
echo "=========================================="
if [ $failed -eq 0 ]; then
    echo -e "${GREEN}所有健康检查通过! ✓${NC}"
    echo "=========================================="
    exit 0
else
    echo -e "${RED}发现 ${failed} 个健康检查失败 ✗${NC}"
    echo "=========================================="
    echo ""
    echo -e "${YELLOW}查看失败服务的日志:${NC}"
    echo "  docker compose -f docker-compose.test.yml logs tenant-service"
    echo "  docker compose -f docker-compose.test.yml logs station-service"
    exit 1
fi
