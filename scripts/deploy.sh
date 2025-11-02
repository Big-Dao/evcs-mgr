#!/bin/bash

# EVCS充电站管理系统 - 部署脚本
# 用于部署整个系统到Docker环境

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# 检查Docker和Docker Compose
check_dependencies() {
    log_info "检查依赖..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi

    log_success "依赖检查完成"
}

# 创建必要的目录
create_directories() {
    log_info "创建必要的目录..."

    mkdir -p logs
    mkdir -p monitoring/{prometheus,grafana,elasticsearch,logstash,redis}
    mkdir -p monitoring/grafana/{provisioning,dashboards}
    mkdir -p monitoring/grafana/provisioning/{datasources,dashboards}

    log_success "目录创建完成"
}

# 构建应用
build_applications() {
    log_info "构建应用程序..."

    # 构建所有模块
    ./gradlew clean build -x test

    if [ $? -ne 0 ]; then
        log_error "应用程序构建失败"
        exit 1
    fi

    log_success "应用程序构建完成"
}

# 启动基础设施服务
start_infrastructure() {
    log_info "启动基础设施服务..."

    # 启动Redis
    docker-compose -f docker-compose.monitoring.yml up -d redis

    # 等待Redis启动
    log_info "等待Redis启动..."
    sleep 10

    # 检查Redis健康状态
    for i in {1..30}; do
        if docker-compose -f docker-compose.monitoring.yml exec -T redis redis-cli ping | grep -q PONG; then
            log_success "Redis启动完成"
            break
        fi
        if [ $i -eq 30 ]; then
            log_error "Redis启动超时"
            exit 1
        fi
        sleep 2
    done
}

# 启动应用服务
start_applications() {
    log_info "启动应用服务..."

    # 启动EVCS网关
    docker-compose -f docker-compose.monitoring.yml up -d evcs-gateway

    # 等待应用启动
    log_info "等待应用启动..."
    sleep 30

    # 检查应用健康状态
    for i in {1..60}; do
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log_success "EVCS网关启动完成"
            break
        fi
        if [ $i -eq 60 ]; then
            log_error "应用启动超时"
            exit 1
        fi
        sleep 5
    done
}

# 启动监控服务
start_monitoring() {
    log_info "启动监控服务..."

    # 启动监控组件
    docker-compose -f docker-compose.monitoring.yml --profile monitoring up -d

    log_success "监控服务启动完成"
}

# 启动日志服务
start_logging() {
    log_info "启动日志服务..."

    # 启动日志组件
    docker-compose -f docker-compose.monitoring.yml --profile logging up -d

    log_success "日志服务启动完成"
}

# 启动链路追踪
start_tracing() {
    log_info "启动链路追踪服务..."

    # 启动Jaeger
    docker-compose -f docker-compose.monitoring.yml --profile tracing up -d

    log_success "链路追踪服务启动完成"
}

# 验证部署
verify_deployment() {
    log_info "验证部署..."

    # 检查所有服务状态
    echo "=== 服务状态 ==="
    docker-compose -f docker-compose.monitoring.yml ps

    # 检查关键端点
    echo ""
    echo "=== 健康检查 ==="

    # EVCS网关健康检查
    if curl -f http://localhost:8080/actuator/health &> /dev/null; then
        log_success "EVCS网关: 健康"
    else
        log_error "EVCS网关: 不健康"
    fi

    # Prometheus健康检查
    if curl -f http://localhost:9090/-/healthy &> /dev/null; then
        log_success "Prometheus: 健康"
    else
        log_warning "Prometheus: 未启动或未启用监控"
    fi

    # Grafana健康检查
    if curl -f http://localhost:3000/api/health &> /dev/null; then
        log_success "Grafana: 健康"
    else
        log_warning "Grafana: 未启动或未启用监控"
    fi

    log_success "部署验证完成"
}

# 显示访问信息
show_access_info() {
    log_info "服务访问信息:"
    echo ""
    echo "🚀 EVCS网关:"
    echo "   URL: http://localhost:8080"
    echo "   健康检查: http://localhost:8080/actuator/health"
    echo "   指标: http://localhost:8080/actuator/prometheus"
    echo ""
    echo "📊 监控服务 (如果启用):"
    echo "   Prometheus: http://localhost:9090"
    echo "   Grafana: http://localhost:3000 (admin/admin123)"
    echo "   Node Exporter: http://localhost:9100/metrics"
    echo ""
    echo "📝 日志服务 (如果启用):"
    echo "   Elasticsearch: http://localhost:9200"
    echo "   Kibana: http://localhost:5601"
    echo ""
    echo "🔍 链路追踪 (如果启用):"
    echo "   Jaeger: http://localhost:16686"
    echo ""
    log_success "部署完成! 🎉"
}

# 清理函数
cleanup() {
    log_info "清理旧的容器和镜像..."
    docker-compose -f docker-compose.monitoring.yml down -v
    docker system prune -f
    log_success "清理完成"
}

# 完整部署
full_deploy() {
    log_info "开始完整部署..."

    check_dependencies
    create_directories
    build_applications
    start_infrastructure
    start_applications
    start_monitoring
    start_logging
    start_tracing
    verify_deployment
    show_access_info
}

# 基础部署（不含监控）
basic_deploy() {
    log_info "开始基础部署..."

    check_dependencies
    create_directories
    build_applications
    start_infrastructure
    start_applications
    verify_deployment
    show_access_info
}

# 显示帮助信息
show_help() {
    echo "EVCS充电站管理系统 - 部署脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  full      完整部署（包含所有监控组件）"
    echo "  basic     基础部署（仅应用和基础设施）"
    echo "  monitor   仅启动监控组件"
    echo "  logs      仅启动日志组件"
    echo "  tracing   仅启动链路追踪组件"
    echo "  cleanup   清理所有容器和镜像"
    echo "  verify    验证当前部署"
    echo "  help      显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 full     # 完整部署"
    echo "  $0 basic    # 基础部署"
    echo "  $0 cleanup  # 清理环境"
}

# 主函数
main() {
    case "${1:-full}" in
        "full")
            full_deploy
            ;;
        "basic")
            basic_deploy
            ;;
        "monitor")
            start_monitoring
            ;;
        "logs")
            start_logging
            ;;
        "tracing")
            start_tracing
            ;;
        "cleanup")
            cleanup
            ;;
        "verify")
            verify_deployment
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            log_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
}

# 脚本入口
main "$@"