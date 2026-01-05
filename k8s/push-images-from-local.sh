#!/bin/bash
set -euo pipefail

# Build on dev machine and push to the in-cluster registry via kubectl port-forward.
# Motivation:
# - K3S/test environment may have no outbound network (GitHub/npm registries).
# - Local Docker may reject HTTP registries (HTTPS default) when pushing to 192.168.x.x:5000.
# - Port-forward to localhost avoids Docker insecure-registry config.
#
# What it does:
# 1) port-forward deploy/registry -> localhost:${REGISTRY_LOCAL_PORT}
# 2) (optional) push Java service images via Jib: ./gradlew pushK8sImages
# 3) (optional) build evcs-admin dist locally and docker build/push admin-frontend

NAMESPACE="${EVCS_NAMESPACE:-evcs}"
EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG:-dev}"
REGISTRY_LOCAL_PORT="${REGISTRY_LOCAL_PORT:-5000}"

EVCS_PUSH_JAVA_IMAGES="${EVCS_PUSH_JAVA_IMAGES:-true}"
EVCS_PUSH_ADMIN_FRONTEND="${EVCS_PUSH_ADMIN_FRONTEND:-true}"

# Admin build options
EVCS_ADMIN_DIR="${EVCS_ADMIN_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../evcs-admin" && pwd)}"
EVCS_ADMIN_NPM_REGISTRY="${EVCS_ADMIN_NPM_REGISTRY:-https://registry.npmmirror.com/}"
EVCS_ADMIN_DOCKERFILE="${EVCS_ADMIN_DOCKERFILE:-Dockerfile.prebuilt}"
# For air-gapped builds, you can override NGINX_IMAGE to an internal mirror.
EVCS_ADMIN_NGINX_IMAGE="${EVCS_ADMIN_NGINX_IMAGE:-nginx:alpine}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

PORT_FWD_PID=""
cleanup() {
  if [ -n "${PORT_FWD_PID}" ]; then
    kill "${PORT_FWD_PID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: missing required command: $1" >&2
    exit 1
  fi
}

has_cmd() {
  command -v "$1" >/dev/null 2>&1
}

require_cmd kubectl
require_cmd curl

REGISTRY_ENDPOINT="127.0.0.1:${REGISTRY_LOCAL_PORT}"

echo "=== EVCS local build -> push to K3S registry ==="
echo "Namespace:          ${NAMESPACE}"
echo "Tag:                ${EVCS_IMAGE_TAG}"
echo "Local registry:     ${REGISTRY_ENDPOINT} (via port-forward)"
echo "Push Java images:   ${EVCS_PUSH_JAVA_IMAGES}"
echo "Push admin frontend:${EVCS_PUSH_ADMIN_FRONTEND}"

# Start port-forward to registry
echo "Starting port-forward: deploy/registry 5000 -> localhost:${REGISTRY_LOCAL_PORT} ..."
set +e
kubectl -n "${NAMESPACE}" port-forward deploy/registry "${REGISTRY_LOCAL_PORT}:5000" >/tmp/evcs-registry-portforward.log 2>&1 &
PORT_FWD_PID=$!
set -e

# Wait until registry is reachable
for i in {1..40}; do
  if curl -fsS "http://${REGISTRY_ENDPOINT}/v2/" >/dev/null 2>&1; then
    break
  fi
  sleep 0.25
  if ! kill -0 "${PORT_FWD_PID}" >/dev/null 2>&1; then
    echo "ERROR: port-forward process exited early. See /tmp/evcs-registry-portforward.log" >&2
    exit 1
  fi
  if [ "$i" = "40" ]; then
    echo "ERROR: registry not reachable at http://${REGISTRY_ENDPOINT}/v2/" >&2
    echo "Log: /tmp/evcs-registry-portforward.log" >&2
    exit 1
  fi
done

echo "Registry is reachable: http://${REGISTRY_ENDPOINT}/v2/"

# Push Java images via Jib (no Docker daemon needed)
if [ "${EVCS_PUSH_JAVA_IMAGES}" = "true" ]; then
  GRADLE_TASK="${GRADLE_TASK:-pushK8sImages}"
  echo "=== Pushing Java service images via Jib ==="
  echo "Command: ./gradlew ${GRADLE_TASK} -Devcs.k8s.registry=${REGISTRY_ENDPOINT} -Devcs.k8s.tag=${EVCS_IMAGE_TAG}"
  (cd "${REPO_ROOT}" && ./gradlew ${GRADLE_TASK} -Devcs.k8s.registry="${REGISTRY_ENDPOINT}" -Devcs.k8s.tag="${EVCS_IMAGE_TAG}")
fi

# Push admin frontend image
if [ "${EVCS_PUSH_ADMIN_FRONTEND}" = "true" ]; then
  require_cmd docker

  # Node/npm are often installed via shell-initialized version managers.
  # When this script runs under bash, npm may be missing from PATH.
  NPM_RUNNER=""
  if has_cmd npm; then
    NPM_RUNNER="npm"
  elif has_cmd zsh && zsh -lic 'command -v npm >/dev/null 2>&1'; then
    echo "[INFO] npm not found in current shell; falling back to 'zsh -lic npm'"
    NPM_RUNNER="zsh -lic npm"
  else
    echo "ERROR: npm not found in PATH (current shell and zsh)." >&2
    echo "Hint: ensure Node.js/npm is installed system-wide, or run this script from a shell where npm is available." >&2
    exit 1
  fi

  if [ ! -d "${EVCS_ADMIN_DIR}" ]; then
    echo "ERROR: EVCS_ADMIN_DIR not found: ${EVCS_ADMIN_DIR}" >&2
    exit 1
  fi

  echo "=== Building evcs-admin dist locally ==="
  echo "Dir:          ${EVCS_ADMIN_DIR}"
  echo "NPM registry: ${EVCS_ADMIN_NPM_REGISTRY}"

  pushd "${EVCS_ADMIN_DIR}" >/dev/null
  ${NPM_RUNNER} config set registry "${EVCS_ADMIN_NPM_REGISTRY}" >/dev/null 2>&1 || true
  ${NPM_RUNNER} install
  ${NPM_RUNNER} run build
  if [ ! -d "${EVCS_ADMIN_DIR}/dist" ]; then
    echo "ERROR: dist/ not found after npm run build" >&2
    exit 1
  fi

  echo "=== Building & pushing admin-frontend image ==="
  IMAGE="${REGISTRY_ENDPOINT}/evcs/admin-frontend:${EVCS_IMAGE_TAG}"
  echo "Image: ${IMAGE}"

  docker build \
    -f "${EVCS_ADMIN_DOCKERFILE}" \
    --build-arg "NGINX_IMAGE=${EVCS_ADMIN_NGINX_IMAGE}" \
    -t "${IMAGE}" \
    .

  docker push "${IMAGE}"

  popd >/dev/null
fi

echo "=== Done pushing images ==="
echo "Next steps:"
echo "  1) Deploy/refresh manifests:  EVCS_IMAGE_TAG=${EVCS_IMAGE_TAG} bash k8s/deploy.sh"
echo "  2) Or restart only workloads: kubectl -n ${NAMESPACE} rollout restart deploy/admin-frontend deploy/station-service"
