#!/usr/bin/env bash
set -euo pipefail

echo "=== Deploying EVCS to K8s (Internal Test Environment) ==="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

NAMESPACE="${EVCS_NAMESPACE:-evcs}"
EVCS_DEPLOY_WAIT="${EVCS_DEPLOY_WAIT:-true}"
ROLLOUT_TIMEOUT_SECONDS="${EVCS_ROLLOUT_TIMEOUT_SECONDS:-180}"
DRY_RUN=false

# Workload scheduling (mixed-arch clusters): pin to a specific node if needed.
# Default keeps current behavior (serve-app is amd64 in internal env).
EVCS_NODE_NAME="${EVCS_NODE_NAME:-serve-app}"

# Version identifiers for backend components (exposed via /actuator/info -> info.evcs.*)
EVCS_GIT_COMMIT="${EVCS_GIT_COMMIT:-}"
EVCS_GIT_BRANCH="${EVCS_GIT_BRANCH:-}"

usage() {
    cat <<'EOF'
Usage: k8s/deploy.sh [options]

Options:
    -n, --namespace <ns>      Kubernetes namespace (default: evcs)
    --dry-run                 Render/apply manifests with kubectl --dry-run=client; skip ClusterIP patch/restarts
    --no-wait                 Do not wait for deployments rollout (overrides EVCS_DEPLOY_WAIT)
    -h, --help                Show help

Environment:
    EVCS_K8S_REGISTRY          Image registry host:port (default: 192.168.20.235:5000)
    EVCS_IMAGE_TAG             Image tag (default: dev)
    EVCS_NODE_NAME             Pin workloads to a specific node name (default: serve-app)
    EVCS_DEPLOY_WAIT           Whether to wait for rollouts (default: true)
    EVCS_ROLLOUT_TIMEOUT_SECONDS  Rollout timeout seconds (default: 180)
    EVCS_NAMESPACE             Same as --namespace
EOF
}

need_cmd() {
    command -v "$1" >/dev/null 2>&1 || { echo "Error: missing command: $1" >&2; exit 1; }
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -n|--namespace)
            NAMESPACE="$2"; shift 2;;
        --dry-run)
            DRY_RUN=true; shift;;
        --no-wait)
            EVCS_DEPLOY_WAIT=false; shift;;
        -h|--help)
            usage; exit 0;;
        *)
            echo "Error: unknown argument: $1" >&2
            usage
            exit 1;;
    esac
done

need_cmd kubectl
need_cmd envsubst

cd "${REPO_ROOT}"

if [ -z "${EVCS_GIT_COMMIT}" ]; then
    EVCS_GIT_COMMIT="$(cd "${REPO_ROOT}" && git rev-parse --short HEAD 2>/dev/null || true)"
fi
if [ -z "${EVCS_GIT_BRANCH}" ]; then
    EVCS_GIT_BRANCH="$(cd "${REPO_ROOT}" && git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
fi

export EVCS_GIT_COMMIT
export EVCS_GIT_BRANCH

# 镜像构建/推送建议（SSOT 见 docs/deployment/DEPLOYMENT-GUIDE.md）：
# - 内部测试环境常见“集群无外网”，不要在集群内做 git clone / npm install。
# - 推荐在开发机本地构建后，通过 port-forward 推送到集群内置 registry：
#     kubectl -n evcs port-forward deploy/registry 5000:5000
#     ./gradlew pushK8sImages -Devcs.k8s.registry=127.0.0.1:5000 -Devcs.k8s.tag=$EVCS_IMAGE_TAG
# - 前端镜像：使用 k8s/push-images-from-local.sh。
# - 若本机 Docker 不可用（常见于 WSL），前端可使用 Kaniko + 预构建 dist：k8s/build-admin-frontend-prebuilt-from-local.sh。
#
# 本脚本只负责：渲染占位符并 apply manifests。
export EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"
export EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG:-dev}"
export EVCS_NAMESPACE="${NAMESPACE}"
export EVCS_NODE_NAME

echo "Namespace:   ${NAMESPACE}"
echo "Using images: ${EVCS_K8S_REGISTRY}/evcs/*:${EVCS_IMAGE_TAG}"
echo "Node pin:    kubernetes.io/hostname=${EVCS_NODE_NAME}"
if [ -n "${EVCS_GIT_COMMIT}" ] && [ -n "${EVCS_GIT_BRANCH}" ]; then
    echo "Git:         ${EVCS_GIT_BRANCH}@${EVCS_GIT_COMMIT}"
fi
echo "kubectl ctx: $(kubectl config current-context 2>/dev/null || echo '<unknown>')"

KUBECTL_APPLY_FLAGS=()
if $DRY_RUN; then
    KUBECTL_APPLY_FLAGS+=(--dry-run=client)
    EVCS_DEPLOY_WAIT=false
    echo "[INFO] --dry-run enabled: skipping ClusterIP patch/restarts/rollout waits"
fi

kubectl_apply_stdin() {
    kubectl apply -n "${NAMESPACE}" "${KUBECTL_APPLY_FLAGS[@]}" -f -
}

# 1. Apply Base (Namespace, Common Config, Secrets)
echo "Applying Base Configuration..."
envsubst '${EVCS_NAMESPACE} ${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG} ${EVCS_GIT_COMMIT} ${EVCS_GIT_BRANCH} ${EVCS_NODE_NAME}' < k8s/deployments/00-base.yaml | kubectl apply "${KUBECTL_APPLY_FLAGS[@]}" -f -

# 2. Create ConfigMap for Config Repo
echo "Creating Config Repo ConfigMap..."
# Check if config-repo exists
if [ -d "config-repo" ]; then
    kubectl create configmap evcs-config-repo \
        --from-file=config-repo/ \
        -n "${NAMESPACE}" \
        --dry-run=client \
        -o yaml | kubectl_apply_stdin
else
    echo "Error: config-repo directory not found!"
    exit 1
fi

# 3. Apply Infrastructure
echo "Applying Infrastructure (DB, Redis, RabbitMQ)..."
envsubst '${EVCS_NAMESPACE} ${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG} ${EVCS_GIT_COMMIT} ${EVCS_GIT_BRANCH} ${EVCS_NODE_NAME}' < k8s/deployments/01-infrastructure.yaml | kubectl_apply_stdin

# 4. Apply Discovery & Config
echo "Applying Discovery & Config Server..."
envsubst '${EVCS_NAMESPACE} ${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG} ${EVCS_GIT_COMMIT} ${EVCS_GIT_BRANCH} ${EVCS_NODE_NAME}' < k8s/deployments/02-discovery-config.yaml | kubectl_apply_stdin

restart_if_exists() {
    local name="$1"
    if kubectl -n "${NAMESPACE}" get deployment "$name" >/dev/null 2>&1; then
        kubectl -n "${NAMESPACE}" rollout restart deployment "$name" || true
    fi
}

rollout_status_if_exists() {
    local name="$1"
    if kubectl -n "${NAMESPACE}" get deployment "$name" >/dev/null 2>&1; then
        kubectl -n "${NAMESPACE}" rollout status "deployment/${name}" --timeout="${ROLLOUT_TIMEOUT_SECONDS}s" || true
    fi
}

# 5. Patch Common Config with Service ClusterIPs (workaround for broken Service DNS)
echo "Resolving Service ClusterIPs (DNS workaround)..."
if $DRY_RUN; then
    echo "[INFO] dry-run: skipping ClusterIP resolution/patch"
    EVCS_CONFIG_IP=""
    EVCS_GATEWAY_IP=""
    EVCS_POSTGRES_IP=""
    EVCS_REDIS_IP=""
    EVCS_RABBIT_IP=""
    EVCS_EUREKA_IP=""
    EVCS_AUTH_IP=""
    EVCS_TENANT_IP=""
    EVCS_STATION_IP=""
    EVCS_ORDER_IP=""
    EVCS_PAYMENT_IP=""
    EVCS_MONITORING_IP=""
else
    get_cluster_ip() {
        local svc="$1"
        local ip=""
        # Service creation can be slightly delayed; poll up to ~30s.
        for _ in {1..30}; do
            ip="$(kubectl get svc -n "${NAMESPACE}" "${svc}" -o jsonpath='{.spec.clusterIP}' 2>/dev/null || true)"
            if [ -n "${ip}" ] && [ "${ip}" != "None" ]; then
                echo "${ip}"
                return 0
            fi
            sleep 1
        done
        return 1
    }

    require_ip() {
        local svc="$1"
        local ip="$2"
        if [ -z "${ip}" ] || [ "${ip}" = "None" ]; then
            echo "Error: failed to resolve ClusterIP for service: ${svc} (ns=${NAMESPACE})" >&2
            exit 1
        fi
    }

EVCS_POSTGRES_IP="$(get_cluster_ip evcs-postgres)"; require_ip evcs-postgres "${EVCS_POSTGRES_IP}"
EVCS_REDIS_IP="$(get_cluster_ip evcs-redis)"; require_ip evcs-redis "${EVCS_REDIS_IP}"
EVCS_RABBIT_IP="$(get_cluster_ip evcs-rabbitmq)"; require_ip evcs-rabbitmq "${EVCS_RABBIT_IP}"
EVCS_EUREKA_IP="$(get_cluster_ip evcs-eureka)"; require_ip evcs-eureka "${EVCS_EUREKA_IP}"
EVCS_CONFIG_IP="$(get_cluster_ip evcs-config)"; require_ip evcs-config "${EVCS_CONFIG_IP}"
export EVCS_CONFIG_IP

# Microservice ClusterIPs (for gateway route URIs; DNS workaround)
EVCS_AUTH_IP="$(get_cluster_ip evcs-auth)"; require_ip evcs-auth "${EVCS_AUTH_IP}"
EVCS_TENANT_IP="$(get_cluster_ip evcs-tenant)"; require_ip evcs-tenant "${EVCS_TENANT_IP}"
EVCS_STATION_IP="$(get_cluster_ip evcs-station)"; require_ip evcs-station "${EVCS_STATION_IP}"
EVCS_ORDER_IP="$(get_cluster_ip evcs-order)"; require_ip evcs-order "${EVCS_ORDER_IP}"
EVCS_PAYMENT_IP="$(get_cluster_ip evcs-payment)"; require_ip evcs-payment "${EVCS_PAYMENT_IP}"
EVCS_MONITORING_IP="$(get_cluster_ip evcs-monitoring)"; require_ip evcs-monitoring "${EVCS_MONITORING_IP}"

echo "  evcs-postgres:  ${EVCS_POSTGRES_IP}"
echo "  evcs-redis:     ${EVCS_REDIS_IP}"
echo "  evcs-rabbitmq:  ${EVCS_RABBIT_IP}"
echo "  evcs-eureka:    ${EVCS_EUREKA_IP}"
echo "  evcs-config:    ${EVCS_CONFIG_IP}"
echo "  evcs-auth:      ${EVCS_AUTH_IP}"
echo "  evcs-tenant:    ${EVCS_TENANT_IP}"
echo "  evcs-station:   ${EVCS_STATION_IP}"
echo "  evcs-order:     ${EVCS_ORDER_IP}"
echo "  evcs-payment:   ${EVCS_PAYMENT_IP}"
echo "  evcs-monitoring:${EVCS_MONITORING_IP}"

kubectl patch configmap -n "${NAMESPACE}" evcs-common-config --type merge --patch "$(cat <<EOF
{"data":{
    "CONFIG_SERVER_URL":"http://${EVCS_CONFIG_IP}:8888",
    "SPRING_CONFIG_IMPORT":"configserver:http://${EVCS_CONFIG_IP}:8888",
    "SPRING_CLOUD_CONFIG_FAIL_FAST":"true",
    "SPRING_CLOUD_CONFIG_RETRY_MAX_ATTEMPTS":"20",
    "SPRING_CLOUD_CONFIG_RETRY_INITIAL_INTERVAL":"2000",
    "SPRING_CLOUD_CONFIG_RETRY_MULTIPLIER":"1.5",
    "SPRING_CLOUD_CONFIG_RETRY_MAX_INTERVAL":"10000",
    "EUREKA_SERVER_URL":"http://${EVCS_EUREKA_IP}:8761/eureka/",
    "EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE":"http://${EVCS_EUREKA_IP}:8761/eureka/",
    "SPRING_DATASOURCE_URL":"jdbc:postgresql://${EVCS_POSTGRES_IP}:5432/evcs_mgr?sslmode=disable",
    "SPRING_DATA_REDIS_HOST":"${EVCS_REDIS_IP}",
    "SPRING_RABBITMQ_HOST":"${EVCS_RABBIT_IP}",
    "EVCS_AUTH_IP":"${EVCS_AUTH_IP}",
    "EVCS_TENANT_IP":"${EVCS_TENANT_IP}",
    "EVCS_STATION_IP":"${EVCS_STATION_IP}",
    "EVCS_ORDER_IP":"${EVCS_ORDER_IP}",
    "EVCS_PAYMENT_IP":"${EVCS_PAYMENT_IP}",
    "EVCS_MONITORING_IP":"${EVCS_MONITORING_IP}"
}}
EOF
)"
fi

if ! $DRY_RUN; then
    # IMPORTANT: ConfigMap env vars are only read at Pod start.
    # When re-deploying into an existing cluster, the patched ClusterIP values won't
    # take effect until the affected deployments are restarted.
    restart_if_exists "gateway"
    restart_if_exists "auth-service"
    restart_if_exists "tenant-service"
    restart_if_exists "station-service"
    restart_if_exists "order-service"
    restart_if_exists "payment-service"
    restart_if_exists "protocol-service"
    restart_if_exists "monitoring-service"

    # 6. Restart Config Server to pick up patched Eureka URL
    kubectl rollout restart deployment -n "${NAMESPACE}" config-server || true
fi

# 7. Apply Gateway
echo "Applying Gateway..."
envsubst '${EVCS_NAMESPACE} ${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG} ${EVCS_GIT_COMMIT} ${EVCS_GIT_BRANCH} ${EVCS_NODE_NAME} ${EVCS_CONFIG_IP}' < k8s/deployments/03-gateway.yaml | kubectl_apply_stdin

if ! $DRY_RUN; then
    # 7b. Patch Common Config with Gateway ClusterIP (for frontend nginx proxy; DNS workaround)
    EVCS_GATEWAY_IP="$(get_cluster_ip evcs-gateway)"; require_ip evcs-gateway "${EVCS_GATEWAY_IP}"
    echo "  evcs-gateway:   ${EVCS_GATEWAY_IP}"
    kubectl patch configmap -n "${NAMESPACE}" evcs-common-config --type merge --patch "$(cat <<EOF
{"data":{
    "EVCS_GATEWAY_IP":"${EVCS_GATEWAY_IP}"
}}
EOF
)"

    # Frontend nginx entrypoint requires EVCS_GATEWAY_IP; restart to pick it up on re-deploy.
    restart_if_exists "admin-frontend"
fi

# 8. Apply Microservices
echo "Applying Microservices..."
envsubst '${EVCS_NAMESPACE} ${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG} ${EVCS_GIT_COMMIT} ${EVCS_GIT_BRANCH} ${EVCS_NODE_NAME} ${EVCS_CONFIG_IP}' < k8s/deployments/04-services.yaml | kubectl_apply_stdin

# 9. Apply Frontend
echo "Applying Frontend (evcs-admin)..."
envsubst '${EVCS_NAMESPACE} ${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG} ${EVCS_GIT_COMMIT} ${EVCS_GIT_BRANCH} ${EVCS_NODE_NAME}' < k8s/deployments/05-frontend.yaml | kubectl_apply_stdin

if [[ "${EVCS_DEPLOY_WAIT}" = "true" ]]; then
    echo "Waiting for rollouts (timeout: ${ROLLOUT_TIMEOUT_SECONDS}s)..."
    rollout_status_if_exists "postgres"
    rollout_status_if_exists "redis"
    rollout_status_if_exists "rabbitmq"
    rollout_status_if_exists "registry"
    rollout_status_if_exists "eureka"
    rollout_status_if_exists "config-server"
    rollout_status_if_exists "gateway"
    rollout_status_if_exists "auth-service"
    rollout_status_if_exists "tenant-service"
    rollout_status_if_exists "station-service"
    rollout_status_if_exists "order-service"
    rollout_status_if_exists "payment-service"
    rollout_status_if_exists "protocol-service"
    rollout_status_if_exists "monitoring-service"
    rollout_status_if_exists "admin-frontend"
fi

echo "=== Deployment Submitted ==="
echo "Check status with: kubectl get pods -n ${NAMESPACE}"
echo "Verify with:       k8s/verify-after-reboot.sh -n ${NAMESPACE}"
