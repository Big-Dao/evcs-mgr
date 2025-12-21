#!/bin/bash
set -e

echo "=== Deploying EVCS to K8s (Internal Test Environment) ==="

# 镜像构建/推送建议（SSOT 见 docs/deployment/DEPLOYMENT-GUIDE.md）：
# - 内部测试环境常见“集群无外网”，不要在集群内做 git clone / npm install。
# - 推荐在开发机本地构建后，通过 port-forward 推送到集群内置 registry：
#     kubectl -n evcs port-forward deploy/registry 5000:5000
#     ./gradlew pushK8sImages -Devcs.k8s.registry=127.0.0.1:5000 -Devcs.k8s.tag=$EVCS_IMAGE_TAG
# - 前端镜像：使用 k8s/push-images-from-local.sh。
#
# 本脚本只负责：渲染占位符并 apply manifests。
export EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"
export EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG:-dev}"

echo "Using images: ${EVCS_K8S_REGISTRY}/evcs/*:${EVCS_IMAGE_TAG}"

# 1. Apply Base (Namespace, Common Config, Secrets)
echo "Applying Base Configuration..."
envsubst '${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG}' < k8s/deployments/00-base.yaml | kubectl apply -f -

# 2. Create ConfigMap for Config Repo
echo "Creating Config Repo ConfigMap..."
# Check if config-repo exists
if [ -d "config-repo" ]; then
    kubectl create configmap evcs-config-repo --from-file=config-repo/ -n evcs --dry-run=client -o yaml | kubectl apply -f -
else
    echo "Error: config-repo directory not found!"
    exit 1
fi

# 3. Apply Infrastructure
echo "Applying Infrastructure (DB, Redis, RabbitMQ)..."
envsubst '${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG}' < k8s/deployments/01-infrastructure.yaml | kubectl apply -f -

# 4. Apply Discovery & Config
echo "Applying Discovery & Config Server..."
envsubst '${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG}' < k8s/deployments/02-discovery-config.yaml | kubectl apply -f -

# 5. Patch Common Config with Service ClusterIPs (workaround for broken Service DNS)
echo "Resolving Service ClusterIPs (DNS workaround)..."
get_cluster_ip() {
    kubectl get svc -n evcs "$1" -o jsonpath='{.spec.clusterIP}'
}

EVCS_POSTGRES_IP="$(get_cluster_ip evcs-postgres)"
EVCS_REDIS_IP="$(get_cluster_ip evcs-redis)"
EVCS_RABBIT_IP="$(get_cluster_ip evcs-rabbitmq)"
EVCS_EUREKA_IP="$(get_cluster_ip evcs-eureka)"
EVCS_CONFIG_IP="$(get_cluster_ip evcs-config)"
export EVCS_CONFIG_IP

# Microservice ClusterIPs (for gateway route URIs; DNS workaround)
EVCS_AUTH_IP="$(get_cluster_ip evcs-auth)"
EVCS_TENANT_IP="$(get_cluster_ip evcs-tenant)"
EVCS_STATION_IP="$(get_cluster_ip evcs-station)"
EVCS_PAYMENT_IP="$(get_cluster_ip evcs-payment)"

echo "  evcs-postgres:  ${EVCS_POSTGRES_IP}"
echo "  evcs-redis:     ${EVCS_REDIS_IP}"
echo "  evcs-rabbitmq:  ${EVCS_RABBIT_IP}"
echo "  evcs-eureka:    ${EVCS_EUREKA_IP}"
echo "  evcs-config:    ${EVCS_CONFIG_IP}"
echo "  evcs-auth:      ${EVCS_AUTH_IP}"
echo "  evcs-tenant:    ${EVCS_TENANT_IP}"
echo "  evcs-station:   ${EVCS_STATION_IP}"
echo "  evcs-payment:   ${EVCS_PAYMENT_IP}"

kubectl patch configmap -n evcs evcs-common-config --type merge --patch "$(cat <<EOF
{"data":{
    "CONFIG_SERVER_URL":"http://${EVCS_CONFIG_IP}:8888",
    "SPRING_CONFIG_IMPORT":"optional:configserver:http://${EVCS_CONFIG_IP}:8888",
    "EUREKA_SERVER_URL":"http://${EVCS_EUREKA_IP}:8761/eureka/",
    "EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE":"http://${EVCS_EUREKA_IP}:8761/eureka/",
    "SPRING_DATASOURCE_URL":"jdbc:postgresql://${EVCS_POSTGRES_IP}:5432/evcs_mgr?sslmode=disable",
    "SPRING_DATA_REDIS_HOST":"${EVCS_REDIS_IP}",
    "SPRING_RABBITMQ_HOST":"${EVCS_RABBIT_IP}",
    "EVCS_AUTH_IP":"${EVCS_AUTH_IP}",
    "EVCS_TENANT_IP":"${EVCS_TENANT_IP}",
    "EVCS_STATION_IP":"${EVCS_STATION_IP}",
    "EVCS_PAYMENT_IP":"${EVCS_PAYMENT_IP}"
}}
EOF
)"

# 6. Restart Config Server to pick up patched Eureka URL
kubectl rollout restart deployment -n evcs config-server || true

# 7. Apply Gateway
echo "Applying Gateway..."
envsubst '${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG} ${EVCS_CONFIG_IP}' < k8s/deployments/03-gateway.yaml | kubectl apply -f -

# 7b. Patch Common Config with Gateway ClusterIP (for frontend nginx proxy; DNS workaround)
EVCS_GATEWAY_IP="$(get_cluster_ip evcs-gateway)"
echo "  evcs-gateway:   ${EVCS_GATEWAY_IP}"
kubectl patch configmap -n evcs evcs-common-config --type merge --patch "$(cat <<EOF
{"data":{
    "EVCS_GATEWAY_IP":"${EVCS_GATEWAY_IP}"
}}
EOF
)"

# 8. Apply Microservices
echo "Applying Microservices..."
envsubst '${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG} ${EVCS_CONFIG_IP}' < k8s/deployments/04-services.yaml | kubectl apply -f -

# 9. Apply Frontend
echo "Applying Frontend (evcs-admin)..."
envsubst '${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG}' < k8s/deployments/05-frontend.yaml | kubectl apply -f -

echo "=== Deployment Submitted ==="
echo "Check status with: kubectl get pods -n evcs"
