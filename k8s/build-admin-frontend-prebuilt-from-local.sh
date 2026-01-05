#!/bin/bash
set -euo pipefail

# Build evcs-admin dist locally (npm), then build & push a runtime-only image in-cluster with Kaniko.
# This avoids outbound network access from the cluster to npm registries.

EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"
EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG:-dev}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
EVCS_ADMIN_DIR="${EVCS_ADMIN_CONTEXT_DIR:-${REPO_ROOT}/evcs-admin}"

NPM_REGISTRY="${NPM_REGISTRY:-https://registry.npmmirror.com/}"

if [ ! -d "${EVCS_ADMIN_DIR}" ]; then
  echo "Error: evcs-admin dir not found: ${EVCS_ADMIN_DIR}" >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  if command -v zsh >/dev/null 2>&1 && zsh -lic 'command -v npm >/dev/null 2>&1'; then
    echo "[INFO] npm not found in current shell; using 'zsh -lic npm'" >&2
    NPM_RUNNER="zsh -lic npm"
  else
    echo "ERROR: 本机未找到 npm（需要 Node.js/npm 先本地构建 dist/）。" >&2
    echo "请在装有 Node.js 的机器上执行：" >&2
    echo "  cd evcs-admin && npm install && npm run build" >&2
    echo "然后再执行：" >&2
    echo "  EVCS_ADMIN_DOCKERFILE=Dockerfile.prebuilt bash k8s/build-admin-frontend-from-local.sh" >&2
    exit 1
  fi
else
  NPM_RUNNER="npm"
fi

if [ -z "${NPM_RUNNER:-}" ]; then
  echo "Error: NPM_RUNNER not set" >&2
  echo "然后再执行：" >&2
  echo "  EVCS_ADMIN_DOCKERFILE=Dockerfile.prebuilt bash k8s/build-admin-frontend-from-local.sh" >&2
  exit 1
fi

echo "=== Prebuilding evcs-admin locally ==="
echo "Dir:        ${EVCS_ADMIN_DIR}"
echo "NPM mirror: ${NPM_REGISTRY}"

pushd "${EVCS_ADMIN_DIR}" >/dev/null

${NPM_RUNNER} config set registry "${NPM_REGISTRY}" >/dev/null 2>&1 || true

# No lockfile in repo; use npm install.
${NPM_RUNNER} install
${NPM_RUNNER} run build

if [ ! -d "${EVCS_ADMIN_DIR}/dist" ]; then
  echo "Error: dist/ not found after build" >&2
  exit 1
fi

popd >/dev/null

echo "=== Building runtime image in cluster (no npm) ==="
export EVCS_ADMIN_DOCKERFILE="Dockerfile.prebuilt"
export EVCS_K8S_REGISTRY
export EVCS_IMAGE_TAG

bash "${SCRIPT_DIR}/build-admin-frontend-from-local.sh"
