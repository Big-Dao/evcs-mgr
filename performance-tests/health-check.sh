#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TMP_DIR="${REPO_ROOT}/tmp"
mkdir -p "${TMP_DIR}/perf"

# Local port assumptions: for k8s you typically port-forward services to these ports.
services=(
  "Eureka|8761|/actuator/health"
  "Config Server|8888|/actuator/health"
  "Gateway|8080|/actuator/health"
  "Auth|8081|/actuator/health"
  "Station|8082|/actuator/health"
  "Order|8083|/actuator/health"
  "Payment|8084|/actuator/health"
  "Protocol|8085|/actuator/health"
  "Tenant|8086|/actuator/health"
)

echo "=== EVCS Environment Health Check (Linux) ==="

ok=0
total=${#services[@]}

for row in "${services[@]}"; do
  IFS='|' read -r name port path <<<"${row}"
  url="http://127.0.0.1:${port}${path}"
  printf 'Checking %-12s (%s)... ' "${name}" "${url}"

  if out=$(curl -fsS --max-time 5 "${url}" 2>/dev/null); then
    if echo "${out}" | tr -d '\n\r ' | grep -q '"status":"UP"'; then
      echo "UP"
      ok=$((ok+1))
    else
      echo "NOT_UP"
    fi
  else
    echo "DOWN"
  fi
done

echo
echo "Result: ${ok}/${total} UP"

if [[ ${ok} -eq ${total} ]]; then
  echo "OK: all services healthy"
  exit 0
fi

if (( ok * 10 >= total * 8 )); then
  echo "WARN: most services healthy; retry later"
  exit 1
fi

echo "ERROR: multiple services not ready"
exit 1
