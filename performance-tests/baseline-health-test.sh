#!/usr/bin/env bash
set -euo pipefail

DURATION=60
CONCURRENT=50

usage() {
  cat <<'EOF'
Usage: performance-tests/baseline-health-test.sh [--duration-sec N] [--concurrent N]

Duration-based baseline against local /actuator/health endpoints.
This is intentionally simple (counts only). For detailed percentiles use JMeter: performance-tests/run-test.sh

Outputs are written under repo-root tmp/.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --duration-sec) DURATION="$2"; shift 2;;
    --concurrent) CONCURRENT="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown arg: $1" >&2; usage; exit 1;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TMP_DIR="${REPO_ROOT}/tmp"
OUT_DIR="${TMP_DIR}/perf/baseline"
mkdir -p "${OUT_DIR}"

timestamp="$(date +%Y%m%d-%H%M%S)"
json_out="${OUT_DIR}/baseline-health-${timestamp}.json"

services=(
  "Order|8083"
  "Station|8082"
  "Gateway|8080"
)

declare -A ok
declare -A err
for row in "${services[@]}"; do
  IFS='|' read -r name _ <<<"${row}"
  ok["${name}"]=0
  err["${name}"]=0
done

start=$(date +%s)
end=$(( start + DURATION ))
iter=0

echo "=== EVCS Baseline Health (Linux) ==="
echo "Duration:    ${DURATION}s"
echo "Concurrent:  ${CONCURRENT} per service"

while (( $(date +%s) < end )); do
  iter=$((iter+1))
  for row in "${services[@]}"; do
    IFS='|' read -r name port <<<"${row}"
    url="http://127.0.0.1:${port}/actuator/health"

    tmp_codes="${OUT_DIR}/${name}-${timestamp}.codes"
    : >"${tmp_codes}"

    seq 1 "${CONCURRENT}" | xargs -P "${CONCURRENT}" -n 1 -I{} bash -c '
      url="$1"
      code=$(curl -sS -o /dev/null -w "%{http_code}" --max-time 2 "$url" || echo 000)
      echo "$code"
    ' _ "${url}" >>"${tmp_codes}"

    s=$(grep -c '^200$' "${tmp_codes}" || true)
    t=$(wc -l <"${tmp_codes}" | tr -d ' ')
    e=$(( t - s ))

    ok["${name}"]=$(( ok["${name}"] + s ))
    err["${name}"]=$(( err["${name}"] + e ))
  done

  sleep 0.2

done

{
  echo '['
  first=1
  for row in "${services[@]}"; do
    IFS='|' read -r name _ <<<"${row}"
    total=$(( ok["${name}"] + err["${name}"] ))
    if [[ ${first} -eq 0 ]]; then echo ','; fi
    first=0
    if (( total > 0 )); then
      error_rate=$(awk -v e="${err["${name}"]}" -v t="${total}" 'BEGIN{printf "%.2f", (e/t*100)}')
    else
      error_rate="0.00"
    fi
    printf '{"service":"%s","totalRequests":%d,"success":%d,"errors":%d,"errorRatePercent":"%s"}' \
      "${name}" "${total}" "${ok["${name}"]}" "${err["${name}"]}" "${error_rate}"
  done
  echo
  echo ']'
} >"${json_out}"

echo
echo "Saved: ${json_out}"
