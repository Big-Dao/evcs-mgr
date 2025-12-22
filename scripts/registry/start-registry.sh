#!/bin/bash
set -euo pipefail

# Starts a local private Docker Registry on this machine.
# Intended to run on Node C (192.168.20.235).

REGISTRY_NAME=${REGISTRY_NAME:-evcs-registry}
REGISTRY_PORT=${REGISTRY_PORT:-5000}
REGISTRY_DATA_DIR=${REGISTRY_DATA_DIR:-"$PWD/.local/registry-data"}

mkdir -p "$REGISTRY_DATA_DIR"

echo "Registry data dir: $REGISTRY_DATA_DIR"

echo "Starting registry container '$REGISTRY_NAME' on port $REGISTRY_PORT..."

# Remove existing container (if any)
if docker ps -a --format '{{.Names}}' | grep -qx "$REGISTRY_NAME"; then
  echo "Removing existing container '$REGISTRY_NAME'..."
  docker rm -f "$REGISTRY_NAME" >/dev/null
fi

docker run -d \
  --name "$REGISTRY_NAME" \
  --restart unless-stopped \
  -p "${REGISTRY_PORT}:5000" \
  -v "${REGISTRY_DATA_DIR}:/var/lib/registry" \
  registry:2

echo "Registry started."
echo "Health check: curl -s http://localhost:${REGISTRY_PORT}/v2/_catalog"
