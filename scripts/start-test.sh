#!/bin/bash
# 启动测试环境
# 用途：CI/CD自动化测试和人工测试环境部署

set -e

echo "=========================================="
echo "EVCS Manager - 启动测试环境"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 检查Docker是否运行
echo -e "${YELLOW}检查Docker状态...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker未运行或未安装!${NC}"
    echo -e "${RED}请启动Docker后重试${NC}"
    exit 1
fi
echo -e "${GREEN}Docker运行正常 ✓${NC}"
echo ""

# 检查必要文件
echo -e "${YELLOW}检查必要文件...${NC}"
if [ ! -f "docker-compose.test.yml" ]; then
    echo -e "${RED}错误: docker-compose.test.yml 不存在${NC}"
    exit 1
fi
echo -e "${GREEN}配置文件检查通过 ✓${NC}"
echo ""

# 询问是否重新构建
if [ -z "$CI" ]; then
    read -p "是否重新构建应用? (y/N): " rebuild
else
    rebuild="y"
    echo "CI环境检测到，将自动重新构建应用"
fi

# 构建应用JAR文件
if [ "$rebuild" = "y" ] || [ "$rebuild" = "Y" ]; then
    echo ""
    echo -e "${YELLOW}构建应用JAR文件...${NC}"
    ./gradlew clean build -x test --no-daemon
    if [ $? -ne 0 ]; then
        echo -e "${RED}错误: 应用构建失败!${NC}"
        exit 1
    fi
    echo -e "${GREEN}应用构建成功 ✓${NC}"
    BUILD_FLAG="--build"
    echo -e "${YELLOW}将重新构建Docker镜像...${NC}"
else
    # 检查JAR文件是否存在
    if [ ! -f "evcs-tenant/build/libs/evcs-tenant-1.0.0.jar" ] || [ ! -f "evcs-station/build/libs/evcs-station-1.0.0-boot.jar" ]; then
        echo ""
        echo -e "${YELLOW}未找到预构建的JAR文件，开始构建...${NC}"
        ./gradlew clean build -x test --no-daemon
        if [ $? -ne 0 ]; then
            echo -e "${RED}错误: 应用构建失败!${NC}"
            exit 1
        fi
        echo -e "${GREEN}应用构建成功 ✓${NC}"
    fi
    BUILD_FLAG="--build"
fi

echo ""
echo -e "${YELLOW}停止并清理旧的测试环境...${NC}"
docker compose -f docker-compose.test.yml down --remove-orphans

echo ""
echo -e "${YELLOW}启动测试环境 (可能需要几分钟)...${NC}"
docker compose -f docker-compose.test.yml up $BUILD_FLAG -d

# 等待服务启动
echo ""
echo -e "${YELLOW}等待服务就绪...${NC}"
sleep 10

# 检查服务状态
echo ""
echo -e "${CYAN}检查服务状态...${NC}"
docker compose -f docker-compose.test.yml ps

echo ""
echo "=========================================="
echo -e "${GREEN}测试环境启动成功!${NC}"
echo "=========================================="
echo ""
echo -e "${CYAN}服务访问地址:${NC}"
echo "  PostgreSQL:      localhost:5432 (evcs_test/test_password_123)"
echo "  Redis:           localhost:6379 (密码: test_redis_123)"
echo "  RabbitMQ:        localhost:5672 (evcs_test/test_mq_123)"
echo "  RabbitMQ管理界面: http://localhost:15672"
echo "  Adminer数据库:   http://localhost:8090"
echo "  租户服务:        http://localhost:8081"
echo "  充电站服务:      http://localhost:8082"
echo ""
echo -e "${CYAN}健康检查:${NC}"
echo "  curl http://localhost:8081/actuator/health"
echo "  curl http://localhost:8082/actuator/health"
echo ""
echo -e "${YELLOW}查看日志:${NC}"
echo "  docker compose -f docker-compose.test.yml logs -f"
echo "  docker compose -f docker-compose.test.yml logs -f tenant-service"
echo "  docker compose -f docker-compose.test.yml logs -f station-service"
echo ""
echo -e "${YELLOW}停止服务:${NC}"
echo "  docker compose -f docker-compose.test.yml down"
echo ""
echo -e "${CYAN}运行健康检查:${NC}"
echo "  ./scripts/health-check.sh"
echo ""
echo -e "${CYAN}运行冒烟测试:${NC}"
echo "  ./scripts/smoke-test.sh"
echo ""
