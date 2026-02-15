#!/usr/bin/env bash

set -e

COMPOSE_FILE="docker-compose.yml"

echo "=========================================="
echo "EVCS Manager - Stop Admin E2E Environment"
echo "=========================================="

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker command not found"
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "[ERROR] Docker is not available or not running"
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "[ERROR] docker compose is not available"
  exit 1
fi

DOWN_ARGS=("-f" "${COMPOSE_FILE}" "down" "--remove-orphans")
if [ "${E2E_CLEAN_VOLUMES:-0}" = "1" ]; then
  DOWN_ARGS+=("--volumes")
  echo "[INFO] Stopping stack and removing volumes"
else
  echo "[INFO] Stopping stack (keeping volumes)"
fi

docker compose "${DOWN_ARGS[@]}"

echo "[OK] Admin E2E environment stopped"
