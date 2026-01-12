#!/usr/bin/env bash
set -euo pipefail

REQUESTS=1000
CONCURRENCY=50

usage() {
  cat <<'EOF'
Usage: performance-tests/simple-baseline.sh [--requests N] [--concurrency N]

Runs a simple fixed-request baseline against local /actuator/health endpoints.
Outputs are written under repo-root tmp/.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --requests) REQUESTS="$2"; shift 2;;
    --concurrency) CONCURRENCY="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown arg: $1" >&2; usage; exit 1;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TMP_DIR="${REPO_ROOT}/tmp"
OUT_DIR="${TMP_DIR}/perf/baseline"
mkdir -p "${OUT_DIR}"

services=(
  "Order|http://127.0.0.1:8083/actuator/health"
  "Station|http://127.0.0.1:8082/actuator/health"
  "Gateway|http://127.0.0.1:8080/actuator/health"
)

percentile_value() {
  local sorted_file="$1"
  local pct="$2"
  local n
  n=$(wc -l <"${sorted_file}" | tr -d ' ')
  if [[ "${n}" -le 0 ]]; then
    echo 0
    return 0
  fi
  local idx
  idx=$(( (n*pct + 99) / 100 ))
  if [[ "${idx}" -lt 1 ]]; then idx=1; fi
  sed -n "${idx}p" "${sorted_file}" | head -n 1
}

json_escape() {
  python3 - "$1" <<'PY'
import json,sys
print(json.dumps(sys.argv[1]))
PY
}

echo "=== EVCS Simple Baseline (Linux) ==="
echo "Requests per service: ${REQUESTS}"
echo "Concurrency:         ${CONCURRENCY}"

timestamp="$(date +%Y%m%d-%H%M%S)"
json_out="${OUT_DIR}/simple-baseline-${timestamp}.json"

printf '[' >"${json_out}"
first=1

for row in "${services[@]}"; do
  IFS='|' read -r name url <<<"${row}"
  echo
  echo "--- ${name} ---"
  echo "URL: ${url}"

  raw_file="${OUT_DIR}/${name}-${timestamp}.raw"
  lat_file="${OUT_DIR}/${name}-${timestamp}.lat"
  : >"${raw_file}"
  : >"${lat_file}"

  t0=$(date +%s)
  # Each request prints: CODE,MS
  seq 1 "${REQUESTS}" | xargs -P "${CONCURRENCY}" -n 1 -I{} bash -c '
    url="$1"
    start=$(date +%s%3N)
    code=$(curl -sS -o /dev/null -w "%{http_code}" --max-time 5 "$url" || echo 000)
    end=$(date +%s%3N)
    ms=$((end-start))
    echo "${code},${ms}"
  ' _ "${url}" >>"${raw_file}"
  t1=$(date +%s)

  awk -F',' '$1==200 {print $2}' "${raw_file}" >"${lat_file}"

  total=$(wc -l <"${raw_file}" | tr -d ' ')
  ok=$(awk -F',' '$1==200 {c++} END{print c+0}' "${raw_file}")
  fail=$(( total - ok ))
  elapsed=$(( t1 - t0 ))
  if [[ "${elapsed}" -le 0 ]]; then elapsed=1; fi

  tps=$(awk -v ok="${ok}" -v e="${elapsed}" 'BEGIN{printf "%.2f", ok/e}')

  avg=0
  p50=0
  p90=0
  p99=0
  if [[ "${ok}" -gt 0 ]]; then
    sorted="${lat_file}.sorted"
    sort -n "${lat_file}" >"${sorted}"
    avg=$(awk '{s+=$1} END{printf "%.2f", (NR? s/NR : 0)}' "${sorted}")
    p50=$(percentile_value "${sorted}" 50)
    p90=$(percentile_value "${sorted}" 90)
    p99=$(percentile_value "${sorted}" 99)
  fi

  error_rate=$(awk -v f="${fail}" -v t="${total}" 'BEGIN{printf "%.2f", (t? (f/t*100) : 0)}')

  echo "Total: ${total}, OK: ${ok}, Fail: ${fail}, TPS: ${tps}, Avg(ms): ${avg}, P50/P90/P99(ms): ${p50}/${p90}/${p99}"

  if [[ ${first} -eq 0 ]]; then printf ',' >>"${json_out}"; fi
  first=0

  printf '{' >>"${json_out}"
  printf '"service":%s,' "$(json_escape "${name}")" >>"${json_out}"
  printf '"url":%s,' "$(json_escape "${url}")" >>"${json_out}"
  printf '"totalRequests":%d,' "${total}" >>"${json_out}"
  printf '"success":%d,' "${ok}" >>"${json_out}"
  printf '"errors":%d,' "${fail}" >>"${json_out}"
  printf '"errorRatePercent":%s,' "$(json_escape "${error_rate}")" >>"${json_out}"
  printf '"tps":%s,' "$(json_escape "${tps}")" >>"${json_out}"
  printf '"avgMs":%s,' "$(json_escape "${avg}")" >>"${json_out}"
  printf '"p50Ms":%s,' "$(json_escape "${p50}")" >>"${json_out}"
  printf '"p90Ms":%s,' "$(json_escape "${p90}")" >>"${json_out}"
  printf '"p99Ms":%s' "$(json_escape "${p99}")" >>"${json_out}"
  printf '}' >>"${json_out}"

done

printf ']
' >>"${json_out}"

echo
echo "Saved: ${json_out}"
