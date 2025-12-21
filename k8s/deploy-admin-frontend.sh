#!/bin/bash
set -euo pipefail

# One-command deploy for admin frontend:
# 1) Build/push image inside cluster via Kaniko (git context)
# 2) Apply k8s frontend manifests
# 3) Wait for rollout and print access URL

EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"
EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG:-dev}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

export EVCS_K8S_REGISTRY EVCS_IMAGE_TAG

echo "=== Deploying Admin Frontend ==="
echo "Image: ${EVCS_K8S_REGISTRY}/evcs/admin-frontend:${EVCS_IMAGE_TAG}"

BUILD_MODE="${EVCS_ADMIN_BUILD_MODE:-git}"
echo "Build mode: ${BUILD_MODE}"

echo "Ensuring EVCS_GATEWAY_IP in configmap..."
GATEWAY_CLUSTER_IP="$(kubectl get svc -n evcs evcs-gateway -o jsonpath='{.spec.clusterIP}')"
if [ -z "${GATEWAY_CLUSTER_IP}" ]; then
	echo "Error: failed to resolve evcs-gateway ClusterIP" >&2
	exit 1
fi
kubectl patch configmap -n evcs evcs-common-config --type merge -p "{\"data\":{\"EVCS_GATEWAY_IP\":\"${GATEWAY_CLUSTER_IP}\"}}" >/dev/null
echo "EVCS_GATEWAY_IP=${GATEWAY_CLUSTER_IP}"

# Build image
# - local: build dist + docker build/push on dev machine, push via port-forwarded registry
# - git:   build/push inside cluster via Kaniko (requires cluster network access to Git)
if [ "${BUILD_MODE}" = "local" ]; then
	EVCS_PUSH_JAVA_IMAGES=false \
	EVCS_PUSH_ADMIN_FRONTEND=true \
	EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG}" \
	bash "${REPO_ROOT}/k8s/push-images-from-local.sh"
else
	bash "${REPO_ROOT}/k8s/build-admin-frontend.sh"
fi

# Apply manifests
kubectl apply -f <(envsubst '${EVCS_K8S_REGISTRY} ${EVCS_IMAGE_TAG}' < "${REPO_ROOT}/k8s/deployments/05-frontend.yaml")

# Wait ready
kubectl rollout status -n evcs deploy/admin-frontend --timeout=180s

NODE_IP="${EVCS_NODE_IP:-192.168.20.235}"
NODE_PORT="${EVCS_ADMIN_NODE_PORT:-30090}"

echo "=== Admin Frontend Ready ==="
echo "URL: http://${NODE_IP}:${NODE_PORT}/"
