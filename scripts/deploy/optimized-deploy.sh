#!/bin/bash

# EVCS Manager 优化部署脚本
# 根据系统资源自动选择最佳配置并部署

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 脚本配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MONITOR_SCRIPT="$PROJECT_ROOT/scripts/docker/resource-monitor.sh"

# 显示横幅
show_banner() {
    echo -e "${CYAN}"
    cat << "EOF"
    ____               ____    _____            _
   |  _ \ __ _ _   _  | __ )  | ____|_   ____ _| |__
   | |_) / _` | | | | |  _ \  |  _| \ \ / / _` | '_ \
   |  _ < (_| | |_| | | |_) | | |___ \ V / (_| | | | |
   |_| \_\__,_|\__, | |____/  |_____| \_/ \__,_|_| |_|
                __/ |
               |___/
                    Manager - 优化部署工具
EOF
    echo -e "${NC}"
}

# 显示帮助信息
show_help() {
    cat << EOF
EVCS Manager 优化部署工具

用法: $0 [选项] [环境]

环境:
    minimal     最小配置 (2-4GB内存, 2-4核CPU)
    optimized   优化配置 (6-8GB内存, 4-6核CPU) - 默认
    full        完整配置 (12-16GB内存, 8-12核CPU)
    auto        自动选择 (根据系统资源)

选项:
    -h, --help          显示帮助信息
    -v, --verbose       详细输出
    -q, --quiet         静默模式
    --dry-run           预览部署，不实际执行
    --force             强制重新部署
    --monitor           部署后启动监控
    --backup            部署前备份当前配置
    --no-health-check   跳过健康检查

示例:
    $0                  # 使用优化配置部署
    $0 minimal          # 使用最小配置部署
    $0 auto --monitor   # 自动选择配置并启动监控
    $0 --dry-run full   # 预览完整配置部署

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

log_step() {
    echo -e "${PURPLE}[STEP]${NC} $1"
}

# 检查系统资源
check_system_resources() {
    log_step "检查系统资源..."

    local total_memory_gb=$(free -g | awk '/^Mem:/ {print $2}')
    local available_memory_gb=$(free -g | awk '/^Mem:/ {print $7}')
    local cpu_cores=$(nproc)
    local disk_space_gb=$(df -BG / | awk 'NR==2 {print $4}' | sed 's/G//')

    echo "系统资源信息:"
    echo "  总内存: ${total_memory_gb}GB"
    echo "  可用内存: ${available_memory_gb}GB"
    echo "  CPU核心: ${cpu_cores}核"
    echo "  可用磁盘: ${disk_space_gb}GB"

    if [ "$available_memory_gb" -lt 2 ]; then
        log_error "可用内存不足，至少需要2GB内存"
        exit 1
    fi

    if [ "$cpu_cores" -lt 2 ]; then
        log_error "CPU核心不足，至少需要2核CPU"
        exit 1
    fi

    if [ "$disk_space_gb" -lt 5 ]; then
        log_error "磁盘空间不足，至少需要5GB可用空间"
        exit 1
    fi

    echo "$available_memory_gb,$cpu_cores,$disk_space_gb"
}

# 自动选择最佳配置
auto_select_config() {
    local resources=($(check_system_resources))
    local available_memory=${resources[0]}
    local cpu_cores=${resources[1]}

    log_step "自动选择最佳配置..."

    if [ "$available_memory" -lt 4 ] || [ "$cpu_cores" -lt 4 ]; then
        echo "minimal"
        log_info "选择最小配置 (内存${available_memory}GB, CPU${cpu_cores}核)"
    elif [ "$available_memory" -lt 8 ] || [ "$cpu_cores" -lt 6 ]; then
        echo "optimized"
        log_info "选择优化配置 (内存${available_memory}GB, CPU${cpu_cores}核)"
    else
        echo "full"
        log_info "选择完整配置 (内存${available_memory}GB, CPU${cpu_cores}核)"
    fi
}

# 获取配置文件路径
get_compose_file() {
    local env=$1
    case $env in
        "minimal")
            echo "$PROJECT_ROOT/docker-compose.minimal.yml"
            ;;
        "optimized")
            echo "$PROJECT_ROOT/docker-compose.optimized.yml"
            ;;
        "full")
            echo "$PROJECT_ROOT/docker-compose.yml"
            ;;
        *)
            log_error "未知的环境: $env"
            exit 1
            ;;
    esac
}

# 检查配置文件
check_compose_file() {
    local compose_file=$1
    if [ ! -f "$compose_file" ]; then
        log_error "配置文件不存在: $compose_file"
        exit 1
    fi

    # 检查配置文件语法
    if command -v docker-compose &> /dev/null; then
        if ! docker-compose -f "$compose_file" config &> /dev/null; then
            log_error "配置文件语法错误: $compose_file"
            exit 1
        fi
    else
        log_warning "docker-compose 未安装，跳过语法检查"
    fi
}

# 备份当前配置
backup_current_config() {
    if [ "$BACKUP" = "true" ]; then
        log_step "备份当前配置..."

        local backup_dir="$PROJECT_ROOT/backups/$(date +%Y%m%d_%H%M%S)"
        mkdir -p "$backup_dir"

        # 备份当前运行的配置
        if docker ps --format "{{.Names}}" | grep -q "evcs-"; then
            docker-compose ps > "$backup_dir/current-services.txt"
            log_info "当前服务状态已备份到: $backup_dir/current-services.txt"
        fi

        # 备份配置文件
        cp "$PROJECT_ROOT/docker-compose.yml" "$backup_dir/" 2>/dev/null || true
        cp "$PROJECT_ROOT/docker-compose.optimized.yml" "$backup_dir/" 2>/dev/null || true
        cp "$PROJECT_ROOT/docker-compose.minimal.yml" "$backup_dir/" 2>/dev/null || true

        log_success "配置已备份到: $backup_dir"
    fi
}

# 停止当前服务
stop_current_services() {
    log_step "停止当前EVCS服务..."

    # 停止所有可能的配置
    local compose_files=(
        "$PROJECT_ROOT/docker-compose.yml"
        "$PROJECT_ROOT/docker-compose.optimized.yml"
        "$PROJECT_ROOT/docker-compose.minimal.yml"
    )

    for file in "${compose_files[@]}"; do
        if [ -f "$file" ]; then
            docker-compose -f "$file" down --remove-orphans 2>/dev/null || true
        fi
    done

    log_success "当前服务已停止"
}

# 清理系统资源
cleanup_system() {
    log_step "清理系统资源..."

    # 清理悬空镜像
    docker image prune -f &> /dev/null || true

    # 清理停止的容器
    docker container prune -f &> /dev/null || true

    # 清理未使用的网络
    docker network prune -f &> /dev/null || true

    log_success "系统资源清理完成"
}

# 部署服务
deploy_services() {
    local compose_file=$1
    local env=$2

    log_step "部署服务 (环境: $env)..."

    if [ "$DRY_RUN" = "true" ]; then
        log_info "预览模式 - 将使用的配置文件: $compose_file"
        docker-compose -f "$compose_file" config
        return 0
    fi

    # 构建镜像（如果需要）
    if [ "$FORCE" = "true" ]; then
        log_info "强制重新构建镜像..."
        docker-compose -f "$compose_file" build --no-cache
    else
        docker-compose -f "$compose_file" build
    fi

    # 启动服务
    log_info "启动服务..."
    docker-compose -f "$compose_file" up -d

    log_success "服务部署完成"
}

# 健康检查
health_check() {
    if [ "$NO_HEALTH_CHECK" = "true" ]; then
        log_info "跳过健康检查"
        return 0
    fi

    log_step "执行健康检查..."

    local compose_file=$1
    local services=$(docker-compose -f "$compose_file" config --services)
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        log_info "健康检查第 $attempt/$max_attempts 次尝试..."

        local healthy_count=0
        local total_count=0

        for service in $services; do
            local health=$(docker inspect --format='{{.State.Health.Status}}' "evcs-$service" 2>/dev/null || echo "none")
            if [ "$health" = "healthy" ]; then
                ((healthy_count++))
            elif [ "$health" = "none" ]; then
                # 服务没有健康检查，认为容器运行即健康
                if docker ps --format "{{.Names}}" | grep -q "evcs-$service"; then
                    ((healthy_count++))
                fi
            fi
            ((total_count++))
        done

        if [ $healthy_count -eq $total_count ] && [ $total_count -gt 0 ]; then
            log_success "所有服务健康检查通过 ($healthy_count/$total_count)"
            return 0
        fi

        log_info "当前健康状态: $healthy_count/$total_count 服务正常"
        sleep 10
        ((attempt++))
    done

    log_warning "健康检查超时，但服务可能仍在启动中"
    return 1
}

# 显示部署摘要
show_deployment_summary() {
    local compose_file=$1
    local env=$2

    echo
    echo -e "${GREEN}=== 部署摘要 ===${NC}"
    echo "环境: $env"
    echo "配置文件: $compose_file"
    echo

    echo "运行中的服务:"
    docker-compose -f "$compose_file" ps

    echo
    echo "服务访问地址:"

    # 根据配置显示端口
    if [ "$env" = "minimal" ]; then
        echo "  Eureka:     http://localhost:8761"
        echo "  Config:     http://localhost:8888"
        echo "  Gateway:    http://localhost:8080"
        echo "  Auth:       http://localhost:8081"
        echo "  Station:    http://localhost:8082"
    elif [ "$env" = "optimized" ]; then
        echo "  Eureka:     http://localhost:8761"
        echo "  Config:     http://localhost:8888"
        echo "  Gateway:    http://localhost:8080"
        echo "  Auth:       http://localhost:8081"
        echo "  Station:    http://localhost:8082"
        echo "  Order:      http://localhost:8083"
        echo "  Payment:    http://localhost:8084"
        echo "  Protocol:   http://localhost:8085"
        echo "  Tenant:     http://localhost:8086"
        echo "  Monitoring: http://localhost:8087"
        echo "  Admin:      http://localhost:3000"
    else
        echo "  所有服务已启动，请查看端口映射"
    fi

    echo
    echo "数据库连接:"
    echo "  PostgreSQL: localhost:5432"
    echo "  Redis:      localhost:6379"
    echo
}

# 启动监控
start_monitoring() {
    if [ "$MONITOR" = "true" ] && [ -x "$MONITOR_SCRIPT" ]; then
        log_step "启动资源监控..."
        log_info "使用 Ctrl+C 停止监控"
        sleep 3
        "$MONITOR_SCRIPT" monitor
    else
        log_info "手动启动监控: $MONITOR_SCRIPT monitor"
    fi
}

# 主函数
main() {
    local env="optimized"

    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            minimal|optimized|full|auto)
                env="$1"
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--verbose)
                set -x
                shift
                ;;
            -q|--quiet)
                exec 1>/dev/null
                shift
                ;;
            --dry-run)
                DRY_RUN="true"
                shift
                ;;
            --force)
                FORCE="true"
                shift
                ;;
            --monitor)
                MONITOR="true"
                shift
                ;;
            --backup)
                BACKUP="true"
                shift
                ;;
            --no-health-check)
                NO_HEALTH_CHECK="true"
                shift
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # 显示横幅
    show_banner

    # 自动选择配置
    if [ "$env" = "auto" ]; then
        env=$(auto_select_config)
    fi

    # 获取配置文件路径
    local compose_file=$(get_compose_file "$env")

    log_info "部署环境: $env"
    log_info "配置文件: $compose_file"

    # 检查依赖
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    # 检查配置文件
    check_compose_file "$compose_file"

    # 备份当前配置
    backup_current_config

    # 停止当前服务
    stop_current_services

    # 清理系统资源
    cleanup_system

    # 部署服务
    deploy_services "$compose_file" "$env"

    # 健康检查
    health_check "$compose_file"

    # 显示部署摘要
    show_deployment_summary "$compose_file" "$env"

    # 启动监控
    start_monitoring

    log_success "部署完成！"
}

# 执行主函数
main "$@"