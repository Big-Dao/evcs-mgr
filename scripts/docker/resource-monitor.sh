#!/bin/bash

# EVCS Manager - Docker资源监控脚本
# 用于监控和优化容器资源使用

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.optimized.yml"
MINIMAL_FILE="$PROJECT_ROOT/docker-compose.minimal.yml"

# 显示帮助信息
show_help() {
    cat << EOF
EVCS Manager Docker 资源监控工具

用法: $0 [选项] [命令]

命令:
    monitor         实时监控所有容器资源使用
    stats           显示容器资源统计
    analyze         分析资源使用并提出优化建议
    optimize        自动优化容器资源配置
    scale [up|down] 扩容/缩容服务
    clean           清理未使用的Docker资源
    switch [profile] 切换配置文件 (optimized|minimal|full)

选项:
    -h, --help      显示帮助信息
    -v, --verbose   详细输出
    -f, --file      指定docker-compose文件
    --no-color      禁用颜色输出

示例:
    $0 monitor                    # 实时监控
    $0 stats                      # 资源统计
    $0 analyze                    # 分析优化建议
    $0 optimize                   # 自动优化
    $0 scale down auth-service    # 缩容认证服务
    $0 switch minimal             # 切换到最小配置
    $0 clean                      # 清理资源

EOF
}

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查Docker和docker-compose
check_dependencies() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装或不在PATH中"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "docker-compose 未安装或不在PATH中"
        exit 1
    fi

    # 检查docker服务是否运行
    if ! docker info &> /dev/null; then
        log_error "Docker 服务未运行，请启动Docker"
        exit 1
    fi
}

# 获取容器资源使用情况
get_container_stats() {
    local container_name=$1
    local stats
    stats=$(docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}" | grep "$container_name" || echo "")
    echo "$stats"
}

# 实时监控所有容器
monitor_containers() {
    log_info "开始实时监控容器资源使用..."
    log_info "按 Ctrl+C 退出监控"

    echo
    echo "容器名称                    CPU%    内存使用/限制      内存%    网络 I/O         块 I/O"
    echo "=================================================================================================="

    while true; do
        clear
        echo "EVCS Manager - Docker 资源监控 (更新时间: $(date '+%Y-%m-%d %H:%M:%S'))"
        echo
        echo "容器名称                    CPU%    内存使用/限制      内存%    网络 I/O         块 I/O"
        echo "=================================================================================================="

        docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}" | \
        grep -E "(CONTAINER|evcs-)" | \
        while IFS=$'\t' read -r container cpu mem memperc netio blockio; do
            # 格式化输出
            printf "%-25s\t%-7s\t%-18s\t%-8s\t%-16s\t%s\n" "$container" "$cpu" "$mem" "$memperc" "$netio" "$blockio"
        done

        echo
        echo "系统资源:"
        echo "CPU 使用率: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)%"
        echo "内存使用: $(free -h | awk '/^Mem:/ {printf "%s/%s (%.1f%%)", $3, $2, $3*100/$2 }')"
        echo "磁盘使用: $(df -h / | awk 'NR==2 {printf "%s/%s (%s)", $3, $2, $5}')"

        sleep 5
    done
}

# 显示资源统计报告
show_resource_stats() {
    log_info "生成资源使用统计报告..."

    echo
    echo "=== 容器资源使用统计 ==="
    echo

    # 按内存使用排序
    echo "内存使用排行:"
    docker stats --no-stream --format "table {{.Container}}\t{{.MemUsage}}\t{{.MemPerc}}" | \
    grep -E "(CONTAINER|evcs-)" | \
    sort -k3 -hr | \
    head -10

    echo
    echo "CPU使用排行:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}" | \
    grep -E "(CONTAINER|evcs-)" | \
    sort -k2 -hr | \
    head -10

    echo
    echo "=== 总资源使用 ==="
    local total_memory_mb=$(docker stats --no-stream --format "{{.MemPerc}}" | grep -v "%-" | sed 's/%//' | awk '{sum+=$1} END {print sum}')
    local total_containers=$(docker ps --format "{{.Names}}" | grep evcs- | wc -l)

    echo "运行中的EVCS容器数量: $total_containers"
    echo "总内存使用百分比: ${total_memory_mb}%"

    # 显示镜像大小
    echo
    echo "=== 镜像大小统计 ==="
    docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | \
    grep -E "(REPOSITORY|evcs-)" | \
    sort -k3 -hr
}

# 分析资源使用并提供优化建议
analyze_resource_usage() {
    log_info "分析资源使用情况..."

    echo
    echo "=== 资源使用分析报告 ==="
    echo

    # 检查高内存使用的容器
    echo "1. 高内存使用容器 (>70%):"
    docker stats --no-stream --format "{{.Container}}\t{{.MemPerc}}" | \
    grep -E "evcs-.*[7-9][0-9]\.[0-9]+%" | \
    while IFS=$'\t' read -r container memperc; do
        log_warning "$container 使用内存 $memperc"
        echo "   建议: 检查内存泄漏或增加内存限制"
    done

    echo
    echo "2. 高CPU使用容器 (>50%):"
    docker stats --no-stream --format "{{.Container}}\t{{.CPUPerc}}" | \
    grep -E "evcs-.*[5-9][0-9]\.[0-9]+%|evcs-.*100\.[0-9]+%" | \
    while IFS=$'\t' read -r container cpuperc; do
        log_warning "$container 使用CPU $cpuperc"
        echo "   建议: 检查CPU密集型任务或考虑横向扩展"
    done

    echo
    echo "3. 优化建议:"

    # 检查未使用的镜像
    local unused_images=$(docker images -f "dangling=true" -q | wc -l)
    if [ $unused_images -gt 0 ]; then
        echo "   - 发现 $unused_images 个悬空镜像，建议清理"
    fi

    # 检查停止的容器
    local stopped_containers=$(docker ps -a --filter "status=exited" --format "{{.Names}}" | grep evcs- | wc -l)
    if [ $stopped_containers -gt 0 ]; then
        echo "   - 发现 $stopped_containers 个已停止的EVCS容器，建议清理"
    fi

    # 检查大的日志文件
    echo "   - 建议定期清理容器日志文件"
    echo "   - 考虑使用日志轮转策略"

    echo
    echo "4. 性能调优建议:"
    echo "   - JVM参数优化: 使用G1GC垃圾收集器"
    echo "   - 连接池配置: 根据负载调整数据库连接池大小"
    echo "   - 缓存策略: 优化Redis缓存配置"
    echo "   - 容器资源限制: 设置合理的CPU和内存限制"
}

# 自动优化配置
auto_optimize() {
    log_info "开始自动优化资源配置..."

    # 检查当前使用的配置文件
    local current_profile="optimized"
    if [ -f "$MINIMAL_FILE" ] && docker-compose -f "$MINIMAL_FILE" ps | grep -q "Up"; then
        current_profile="minimal"
    fi

    echo "当前配置模式: $current_profile"

    # 根据系统可用资源建议配置
    local available_memory_gb=$(free -g | awk '/^Mem:/ {print $7}')
    local available_cpu_cores=$(nproc)

    echo
    echo "系统可用资源:"
    echo "  可用内存: ${available_memory_gb}GB"
    echo "  CPU核心数: ${available_cpu_cores}"

    echo
    echo "优化建议:"

    if [ $available_memory_gb -lt 4 ]; then
        log_warning "系统内存不足4GB，建议使用minimal配置"
        echo "执行: $0 switch minimal"
    elif [ $available_memory_gb -lt 8 ]; then
        log_warning "系统内存4-8GB，建议使用optimized配置"
        echo "当前配置已合适"
    else
        log_success "系统资源充足，可以使用full配置"
        echo "考虑切换到完整配置以获得更好性能"
    fi

    # 清理建议
    echo
    log_info "执行资源清理..."
    docker system prune -f
    docker volume prune -f

    log_success "自动优化完成"
}

# 扩容/缩容服务
scale_service() {
    local direction=$1
    local service=$2

    if [ -z "$service" ]; then
        log_error "请指定服务名称"
        echo "可用服务: auth-service, gateway, station-service, order-service"
        return 1
    fi

    local current_replicas=$(docker-compose -f "$COMPOSE_FILE" ps -q "$service" | wc -l)

    case $direction in
        "up")
            local new_replicas=$((current_replicas + 1))
            log_info "扩容 $service 到 $new_replicas 个实例"
            docker-compose -f "$COMPOSE_FILE" up -d --scale "$service=$new_replicas" "$service"
            ;;
        "down")
            if [ $current_replicas -gt 1 ]; then
                local new_replicas=$((current_replicas - 1))
                log_info "缩容 $service 到 $new_replicas 个实例"
                docker-compose -f "$COMPOSE_FILE" up -d --scale "$service=$new_replicas" "$service"
            else
                log_warning "$service 只有1个实例，无法缩容"
            fi
            ;;
        *)
            log_error "未知的扩容方向: $direction"
            echo "使用 'up' 或 'down'"
            return 1
            ;;
    esac
}

# 切换配置文件
switch_profile() {
    local profile=${1:-"optimized"}

    log_info "停止当前服务..."
    docker-compose -f "$COMPOSE_FILE" down 2>/dev/null || true
    docker-compose -f "$MINIMAL_FILE" down 2>/dev/null || true
    docker-compose down 2>/dev/null || true

    case $profile in
        "minimal")
            log_info "切换到最小配置模式..."
            if [ -f "$MINIMAL_FILE" ]; then
                docker-compose -f "$MINIMAL_FILE" up -d
                log_success "已切换到minimal配置"
            else
                log_error "minimal配置文件不存在: $MINIMAL_FILE"
            fi
            ;;
        "optimized")
            log_info "切换到优化配置模式..."
            if [ -f "$COMPOSE_FILE" ]; then
                docker-compose -f "$COMPOSE_FILE" up -d
                log_success "已切换到optimized配置"
            else
                log_error "optimized配置文件不存在: $COMPOSE_FILE"
            fi
            ;;
        "full")
            log_info "切换到完整配置模式..."
            docker-compose up -d
            log_success "已切换到full配置"
            ;;
        *)
            log_error "未知的配置模式: $profile"
            echo "可用模式: minimal, optimized, full"
            return 1
            ;;
    esac
}

# 清理Docker资源
clean_docker_resources() {
    log_info "清理Docker资源..."

    echo "清理前:"
    docker system df

    echo
    log_info "清理悬空镜像..."
    docker image prune -f

    log_info "清理未使用的容器..."
    docker container prune -f

    log_info "清理未使用的网络..."
    docker network prune -f

    log_info "清理未使用的卷..."
    docker volume prune -f

    echo
    log_info "清理后:"
    docker system df

    log_success "Docker资源清理完成"
}

# 主函数
main() {
    check_dependencies

    case "${1:-}" in
        "monitor")
            monitor_containers
            ;;
        "stats")
            show_resource_stats
            ;;
        "analyze")
            analyze_resource_usage
            ;;
        "optimize")
            auto_optimize
            ;;
        "scale")
            scale_service "$2" "$3"
            ;;
        "switch")
            switch_profile "$2"
            ;;
        "clean")
            clean_docker_resources
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            log_error "未知命令: ${1:-}"
            echo
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"