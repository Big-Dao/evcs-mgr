#!/bin/bash
# 停止测试环境
# 用途：清理测试环境资源

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "=========================================="
echo "EVCS Manager - 停止测试环境"
echo "=========================================="
echo ""

# 询问是否删除数据卷
if [ -z "$CI" ]; then
    read -p "是否删除数据卷(包含数据库数据)? (y/N): " remove_volumes
else
    remove_volumes="n"
    echo "CI环境检测到，不删除数据卷"
fi

VOLUME_FLAG=""
if [ "$remove_volumes" = "y" ] || [ "$remove_volumes" = "Y" ]; then
    VOLUME_FLAG="-v"
    echo -e "${YELLOW}将删除所有数据卷...${NC}"
fi

echo ""
echo -e "${YELLOW}停止测试环境服务...${NC}"
docker compose -f docker-compose.test.yml down $VOLUME_FLAG --remove-orphans

echo ""
echo -e "${GREEN}测试环境已停止${NC}"

if [ -n "$VOLUME_FLAG" ]; then
    echo -e "${YELLOW}所有数据卷已删除${NC}"
fi

echo ""
