#!/bin/bash
set -euo pipefail

# Tags and pushes local dev images (evcs/*:dev) to the private registry.
# Usage:
#   REGISTRY_HOST=192.168.20.235 REGISTRY_PORT=5000 ./scripts/registry/push-dev-images.sh

REGISTRY_HOST=${REGISTRY_HOST:-localhost}
REGISTRY_PORT=${REGISTRY_PORT:-5000}
REGISTRY="${REGISTRY_HOST}:${REGISTRY_PORT}"

IMAGES=(
  "evcs/auth-service:dev"
  "evcs/config-server:dev"
  "evcs/eureka:dev"
  "evcs/gateway:dev"
  "evcs/monitoring-service:dev"
  "evcs/order-service:dev"
  "evcs/payment-service:dev"
  "evcs/protocol-service:dev"
  "evcs/station-service:dev"
  "evcs/tenant-service:dev"
)

echo "Pushing images to registry: ${REGISTRY}"

if [[ "$REGISTRY_HOST" != "localhost" && "$REGISTRY_HOST" != "127.0.0.1" ]]; then
  echo "NOTE: Docker push to an HTTP (insecure) registry usually requires adding ${REGISTRY} to Docker insecure registries." >&2
fi

for img in "${IMAGES[@]}"; do
  if ! docker image inspect "$img" >/dev/null 2>&1; then
    echo "ERROR: local image not found: $img"
    exit 1
  fi

  target="${REGISTRY}/${img}"
  echo "Tagging $img -> $target"
  docker tag "$img" "$target"

  echo "Pushing $target"
  docker push "$target"
done

echo "Done."
