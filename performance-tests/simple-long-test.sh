#!/usr/bin/env bash
set -euo pipefail

DURATION_MINUTES=30
CONCURRENT_REQUESTS=20

usage() {
  cat <<'EOF'
Usage: performance-tests/simple-long-test.sh [--minutes N] [--concurrent N]

Runs a long-duration low-impact curl-based load against local /actuator/health.
Intended to keep services busy while collecting JFR.

Outputs are written under repo-root tmp/.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --minutes) DURATION_MINUTES="$2"; shift 2;;
    --concurrent) CONCURRENT_REQUESTS="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown arg: $1" >&2; usage; exit 1;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TMP_DIR="${REPO_ROOT}/tmp"
OUT_DIR="${TMP_DIR}/perf/long-load"
mkdir -p "${OUT_DIR}" "${TMP_DIR}/jfr"

timestamp="$(date +%Y%m%d-%H%M%S)"
json_out="${OUT_DIR}/simple-long-${timestamp}.json"

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

start_epoch=$(date +%s)
end_epoch=$(( start_epoch + DURATION_MINUTES * 60 ))

iter=0
while (( $(date +%s) < end_epoch )); do
  iter=$((iter+1))

  for row in "${services[@]}"; do
    IFS='|' read -r name port <<<"${row}"
    url="http://127.0.0.1:${port}/actuator/health"

    # Fire N concurrent requests and count HTTP 200
    tmp_codes="${OUT_DIR}/${name}-${timestamp}.codes"
    : >"${tmp_codes}"

    seq 1 "${CONCURRENT_REQUESTS}" | xargs -P "${CONCURRENT_REQUESTS}" -n 1 -I{} bash -c '
      url="$1"
      code=$(curl -sS -o /dev/null -w "%{http_code}" --max-time 5 "$url" || echo 000)
      echo "$code"
    ' _ "${url}" >>"${tmp_codes}"

    s=$(grep -c '^200$' "${tmp_codes}" || true)
    t=$(wc -l <"${tmp_codes}" | tr -d ' ')
    e=$(( t - s ))

    ok["${name}"]=$(( ok["${name}"] + s ))
    err["${name}"]=$(( err["${name}"] + e ))
  done

  if (( iter % 10 == 0 )); then
    now=$(date +%s)
    elapsed=$(( now - start_epoch ))
    remaining=$(( end_epoch - now ))
    echo "[$(date +%H:%M:%S)] iter=${iter} elapsed=${elapsed}s remaining=${remaining}s"
    for row in "${services[@]}"; do
      IFS='|' read -r name _ <<<"${row}"
      total=$(( ok["${name}"] + err["${name}"] ))
      echo "  ${name}: ${total} reqs (${ok["${name}"]} ok, ${err["${name}"]} err)"
    done
    echo
  fi

  sleep 0.5

done

# Write summary JSON
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
echo
echo "Next: export JFR files to repo tmp/ (examples)"
echo "  docker cp evcs-order:/tmp/flight.jfr tmp/jfr/long-order.jfr"
echo "  docker cp evcs-station:/tmp/flight.jfr tmp/jfr/long-station.jfr"
echo "  docker cp evcs-gateway:/tmp/flight.jfr tmp/jfr/long-gateway.jfr"
