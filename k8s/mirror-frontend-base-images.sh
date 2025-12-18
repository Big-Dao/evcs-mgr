#!/bin/bash
set -euo pipefail

# Mirror frontend base images into the in-cluster registry, so Kaniko doesn't need outbound access.
#
# It pulls base images via containerd on the node, then pushes them to the local insecure registry.
#
# Usage:
#   bash k8s/mirror-frontend-base-images.sh
#   EVCS_K8S_REGISTRY=192.168.20.235:5000 bash k8s/mirror-frontend-base-images.sh

EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"

# Pull/push a single platform to avoid multi-arch index + attestations causing push failures
# against a plain Docker Registry v2.
EVCS_MIRROR_PLATFORM="${EVCS_MIRROR_PLATFORM:-linux/amd64}"

SRC_NODE_IMAGE="${SRC_NODE_IMAGE:-docker.m.daocloud.io/library/node:20-alpine}"
SRC_NGINX_IMAGE="${SRC_NGINX_IMAGE:-docker.m.daocloud.io/library/nginx:alpine}"

DST_NODE_IMAGE="${EVCS_K8S_REGISTRY}/base/node:20-alpine"
DST_NGINX_IMAGE="${EVCS_K8S_REGISTRY}/base/nginx:alpine"

# k3s uses containerd; prefer `k3s ctr` when available
if command -v k3s >/dev/null 2>&1; then
  CTR=(k3s ctr)
else
  CTR=(ctr)
fi

# Accessing containerd via k3s typically requires root. Prefer non-interactive sudo; if not available,
# instruct the user to run the script with sudo.
SUDO=()
if [[ "$(id -u)" != "0" ]]; then
  if command -v sudo >/dev/null 2>&1; then
    if sudo -n true >/dev/null 2>&1; then
      SUDO=(sudo -n)
    else
      echo "ERROR: 需要 root 权限访问 containerd socket。"
      echo "请在可交互终端执行：sudo bash k8s/mirror-frontend-base-images.sh"
      exit 1
    fi
  else
    echo "ERROR: 需要 root 权限访问 containerd socket，但系统未安装 sudo。"
    echo "请使用 root 账户运行：bash k8s/mirror-frontend-base-images.sh"
    exit 1
  fi
fi

echo "=== Mirroring frontend base images ==="
echo "Registry:  ${EVCS_K8S_REGISTRY}"
echo "Platform:  ${EVCS_MIRROR_PLATFORM}"
echo "Source:    ${SRC_NODE_IMAGE} -> ${DST_NODE_IMAGE}"
echo "Source:    ${SRC_NGINX_IMAGE} -> ${DST_NGINX_IMAGE}"

mirror_one() {
  local src="$1"
  local dst="$2"
  local tmp_ref
  tmp_ref="evcs-mirror-tmp/$(echo "${dst}" | tr '/:' '__'):${EVCS_MIRROR_PLATFORM}"

  echo "--- Pull: ${src}"
  "${SUDO[@]}" "${CTR[@]}" images pull --platform "${EVCS_MIRROR_PLATFORM}" "${src}"

  echo "--- Convert: ${src} -> ${tmp_ref} (${EVCS_MIRROR_PLATFORM})"
  "${SUDO[@]}" "${CTR[@]}" images convert --platform "${EVCS_MIRROR_PLATFORM}" "${src}" "${tmp_ref}"

  echo "--- Push: ${dst} (plain http)"

  # Push the converted single-platform image under the desired remote tag.
  "${SUDO[@]}" "${CTR[@]}" images push --plain-http "${dst}" "${tmp_ref}"

  echo "--- Cleanup local tmp ref: ${tmp_ref}"
  "${SUDO[@]}" "${CTR[@]}" images rm "${tmp_ref}" >/dev/null 2>&1 || true
}

mirror_one "${SRC_NODE_IMAGE}" "${DST_NODE_IMAGE}"
mirror_one "${SRC_NGINX_IMAGE}" "${DST_NGINX_IMAGE}"

echo "=== Done ==="
