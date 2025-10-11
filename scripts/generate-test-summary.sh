#!/bin/bash

# M8 测试摘要生成脚本
# 用于生成测试执行摘要和覆盖率报告

set -e

echo "=========================================="
echo "EVCS Manager M8 测试摘要生成"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 运行测试
echo "📋 运行测试套件..."
./gradlew test jacocoTestReport --continue > /tmp/test-output.log 2>&1 || true

# 提取测试结果
echo ""
echo "📊 测试结果摘要:"
echo "=========================================="

# 统计各模块测试结果
for module in evcs-auth evcs-common evcs-integration evcs-order evcs-payment evcs-station evcs-tenant; do
    if [ -f "$module/build/test-results/test/TEST-*.xml" ]; then
        total=$(grep -r "tests=" "$module/build/test-results/test/" 2>/dev/null | grep -oP 'tests="\K[0-9]+' | awk '{s+=$1} END {print s}' || echo "0")
        failures=$(grep -r "failures=" "$module/build/test-results/test/" 2>/dev/null | grep -oP 'failures="\K[0-9]+' | awk '{s+=$1} END {print s}' || echo "0")
        skipped=$(grep -r "skipped=" "$module/build/test-results/test/" 2>/dev/null | grep -oP 'skipped="\K[0-9]+' | awk '{s+=$1} END {print s}' || echo "0")
        passed=$((total - failures - skipped))
        
        if [ "$total" -gt 0 ]; then
            if [ "$failures" -eq 0 ]; then
                echo -e "${GREEN}✓${NC} $module: $passed/$total tests passed"
            else
                echo -e "${RED}✗${NC} $module: $passed/$total tests passed, $failures failed"
            fi
        fi
    fi
done

echo ""
echo "📈 代码覆盖率报告:"
echo "=========================================="

# 检查覆盖率报告
for module in evcs-auth evcs-common evcs-integration evcs-order evcs-payment evcs-station evcs-tenant; do
    csv_file="$module/build/reports/jacoco/jacocoTestReport.csv"
    if [ -f "$csv_file" ]; then
        # 提取覆盖率数据 (instruction coverage)
        coverage=$(tail -n 1 "$csv_file" | awk -F',' '{
            missed=$4; covered=$5; 
            if (missed + covered > 0) 
                printf "%.1f%%", (covered / (missed + covered)) * 100; 
            else 
                print "N/A"
        }')
        echo "  $module: $coverage"
    fi
done

echo ""
echo "📄 详细报告位置:"
echo "=========================================="
echo "  测试报告: {模块}/build/reports/tests/test/index.html"
echo "  覆盖率报告: {模块}/build/reports/jacoco/index.html"
echo ""

echo "✅ 测试摘要生成完成"
echo ""
echo "使用以下命令查看详细报告:"
echo "  firefox evcs-station/build/reports/tests/test/index.html"
echo "  firefox evcs-station/build/reports/jacoco/index.html"
