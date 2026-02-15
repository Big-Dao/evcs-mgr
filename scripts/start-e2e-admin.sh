#!/usr/bin/env bash

set -e

COMPOSE_FILE="docker-compose.yml"
TIMEOUT_SECONDS="${E2E_HEALTH_TIMEOUT_SECONDS:-600}"
INTERVAL_SECONDS=5

HEALTH_URLS=(
  "http://localhost:8080/actuator/health"
  "http://localhost:8081/actuator/health"
  "http://localhost:8082/actuator/health"
  "http://localhost:8083/actuator/health"
  "http://localhost:8084/actuator/health"
  "http://localhost:8085/actuator/health"
  "http://localhost:3000/"
)

echo "=========================================="
echo "EVCS Manager - Start Admin E2E Environment"
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

BUILD_FLAG=""
if [ -n "${CI:-}" ] || [ "${E2E_BUILD:-0}" = "1" ]; then
  BUILD_FLAG="--build"
fi

echo "[INFO] Bringing up stack with ${COMPOSE_FILE} (${BUILD_FLAG:-no-build})"
docker compose -f "${COMPOSE_FILE}" up -d ${BUILD_FLAG}

wait_for_url() {
  local url="$1"
  local elapsed=0

  while [ "${elapsed}" -lt "${TIMEOUT_SECONDS}" ]; do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      echo "[OK] ${url}"
      return 0
    fi

    sleep "${INTERVAL_SECONDS}"
    elapsed=$((elapsed + INTERVAL_SECONDS))
  done

  echo "[FAIL] ${url} (timeout: ${TIMEOUT_SECONDS}s)"
  return 1
}

echo "[INFO] Waiting for service health endpoints..."
FAILED_URLS=()
for url in "${HEALTH_URLS[@]}"; do
  if ! wait_for_url "${url}"; then
    FAILED_URLS+=("${url}")
  fi
done

if [ "${#FAILED_URLS[@]}" -gt 0 ]; then
  echo ""
  echo "=========================================="
  echo "E2E environment start FAILED"
  echo "=========================================="
  echo "Failed URLs:"
  for url in "${FAILED_URLS[@]}"; do
    echo "  - ${url}"
  done
  echo ""
  echo "Current compose status:"
  docker compose -f "${COMPOSE_FILE}" ps
  exit 1
fi

echo ""
echo "=========================================="
echo "E2E environment is READY"
echo "=========================================="
echo "Checked URLs:"
for url in "${HEALTH_URLS[@]}"; do
  echo "  - ${url}"
done