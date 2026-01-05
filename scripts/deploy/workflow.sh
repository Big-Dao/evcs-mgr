#!/bin/bash
set -euo pipefail

# EVCS Manager - Deployment Workflow Orchestrator
# 用于统一调度本地测试、Compose、K8s 以及私有 Registry 等部署动作。

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()   { echo -e "${BLUE}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }
success(){ echo -e "${GREEN}[OK]${NC} $*"; }

usage() {
  cat <<'EOF'
EVCS 部署工作流脚本

用法:
  workflow.sh <action> [options]

可用 action:
  plan                打印部署阶段/脚本映射
  prepare-dirs        创建 .local/registry-data 及监控/日志目录
  local-registry      启动本地私有 Registry（封装 registry/start-registry.sh）
  test-env            启动 docker-compose.test.yml（封装 scripts/start-test.sh）
  compose-up          部署 docker-compose.yml（封装 scripts/deploy.sh，附加 --with-monitoring 可叠加监控 profile）
  compose-down        停止 docker-compose.yml 环境
  k8s-push            依赖 EVCS_K8S_REGISTRY/EVCS_IMAGE_TAG，执行 Gradle 推送镜像 + 前端镜像
  k8s-deploy          调用 k8s/deploy.sh 渲染并 apply manifests
  health              顺序执行 health-check.sh 与 smoke-test.sh

全局环境变量:
  EVCS_K8S_REGISTRY   推送/部署时使用的 registry（默认 192.168.20.235:5000）
  EVCS_IMAGE_TAG      镜像 tag（默认 dev）
  WITH_MONITORING     当 action=compose-up 时传入 'true' 表示叠加 monitoring/tracing/logging profile

示例:
  ./scripts/deploy/workflow.sh plan
  ./scripts/deploy/workflow.sh test-env
  EVCS_K8S_REGISTRY=127.0.0.1:5000 EVCS_IMAGE_TAG=dev ./scripts/deploy/workflow.sh k8s-push
  EVCS_IMAGE_TAG=dev ./scripts/deploy/workflow.sh k8s-deploy
EOF
}

ensure_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    error "未找到命令: $1"
    exit 1
  fi
}

ensure_docker_compose() {
  if docker compose version >/dev/null 2>&1; then
    echo "docker compose"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    echo "docker-compose"
    return
  fi
  error "未检测到 docker compose / docker-compose，请安装后重试"
  exit 1
}

action_plan() {
  cat <<'EOF'
部署阶段概览：
  1. 准备目录          -> workflow.sh prepare-dirs
  2. 启动私有 Registry -> workflow.sh local-registry
  3. 本地测试环境      -> workflow.sh test-env
  4. Compose 全量环境  -> workflow.sh compose-up [--with-monitoring]
  5. 推送 K8s 镜像      -> workflow.sh k8s-push (需 EVCS_K8S_REGISTRY / EVCS_IMAGE_TAG)
  6. K8s 部署          -> workflow.sh k8s-deploy
  7. 健康/冒烟检查     -> workflow.sh health

更多背景：docs/deployment/DEPLOYMENT-WORKFLOW.md
EOF
}

action_prepare_dirs() {
  log "创建本地运行所需目录..."
  mkdir -p "$PROJECT_ROOT/.local/registry-data"
  mkdir -p "$PROJECT_ROOT/logs"
  mkdir -p "$PROJECT_ROOT/monitoring"/{prometheus,grafana,elasticsearch,logstash,redis}
  mkdir -p "$PROJECT_ROOT/monitoring/grafana/provisioning"/{datasources,dashboards}
  success "目录准备完成（.local/registry-data, logs, monitoring/*）"
}

action_local_registry() {
  log "启动本地 Docker Registry..."
  (cd "$PROJECT_ROOT" && REGISTRY_DATA_DIR="${REGISTRY_DATA_DIR:-$PROJECT_ROOT/.local/registry-data}" \
    "$PROJECT_ROOT/scripts/registry/start-registry.sh")
}

action_test_env() {
  log "启动测试环境（docker-compose.test.yml）..."
  (cd "$PROJECT_ROOT" && "$PROJECT_ROOT/scripts/start-test.sh")
}

action_compose_up() {
  local with_monitoring=${WITH_MONITORING:-false}
  if [[ "${1:-}" == "--with-monitoring" ]]; then
    with_monitoring=true
  fi

  log "调用 scripts/deploy.sh 启动 Compose 环境..."
  (cd "$PROJECT_ROOT" && "$PROJECT_ROOT/scripts/deploy.sh")

  if [[ "$with_monitoring" == "true" ]]; then
    log "叠加 monitoring/logging/tracing profile..."
    local compose_cmd
    compose_cmd=$(ensure_docker_compose)
    (cd "$PROJECT_ROOT" && $compose_cmd -f docker-compose.yml -f docker-compose.monitoring.yml \
      --profile monitoring --profile logging --profile tracing up -d)
  fi
}

action_compose_down() {
  local compose_cmd
  compose_cmd=$(ensure_docker_compose)
  log "停止 docker-compose 环境..."
  (cd "$PROJECT_ROOT" && $compose_cmd -f docker-compose.yml -f docker-compose.monitoring.yml down)
}

action_k8s_push() {
  ensure_cmd kubectl
  ensure_cmd ./gradlew
  export EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"
  export EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG:-dev}"

  log "推送 Java 服务镜像（Gradle pushK8sImages）..."
  (cd "$PROJECT_ROOT" && ./gradlew pushK8sImages \
    -Devcs.k8s.registry="$EVCS_K8S_REGISTRY" \
    -Devcs.k8s.tag="$EVCS_IMAGE_TAG" \
    --no-daemon)

  log "推送前端镜像（k8s/push-images-from-local.sh）..."
  (cd "$PROJECT_ROOT" && EVCS_PUSH_JAVA_IMAGES=false \
    EVCS_IMAGE_TAG="$EVCS_IMAGE_TAG" EVCS_K8S_REGISTRY="$EVCS_K8S_REGISTRY" \
    bash k8s/push-images-from-local.sh)

  success "K8s 镜像推送完成：${EVCS_K8S_REGISTRY}/*:${EVCS_IMAGE_TAG}"
}

action_k8s_deploy() {
  ensure_cmd kubectl
  export EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"
  export EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG:-dev}"
  log "部署到 K8s（k8s/deploy.sh）..."
  (cd "$PROJECT_ROOT" && EVCS_K8S_REGISTRY="$EVCS_K8S_REGISTRY" EVCS_IMAGE_TAG="$EVCS_IMAGE_TAG" \
    bash k8s/deploy.sh)
}

action_health() {
  log "执行健康检查脚本..."
  (cd "$PROJECT_ROOT" && bash scripts/health-check.sh)
  log "执行冒烟测试脚本..."
  (cd "$PROJECT_ROOT" && bash scripts/smoke-test.sh)
}

ACTION="${1:-}"
shift || true

case "$ACTION" in
  plan) action_plan ;;
  prepare-dirs) action_prepare_dirs ;;
  local-registry) action_local_registry ;;
  test-env) action_test_env ;;
  compose-up) action_compose_up "$@" ;;
  compose-down) action_compose_down ;;
  k8s-push) action_k8s_push ;;
  k8s-deploy) action_k8s_deploy ;;
  health) action_health ;;
  ""|-h|--help) usage ;;
  *)
    usage
    error "未知 action: $ACTION"
    exit 1
    ;;
esac
