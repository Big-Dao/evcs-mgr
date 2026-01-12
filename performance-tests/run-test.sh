#!/usr/bin/env bash
set -euo pipefail

SCENARIO="all"
BASE_URL="http://192.168.20.235:30080"
DURATION=600
CONNECT_TIMEOUT_MS=5000
RESPONSE_TIMEOUT_MS=30000
NO_PROMPT=false

usage() {
  cat <<'EOF'
Usage: performance-tests/run-test.sh [options]

Options:
  --scenario <all|scenario1|scenario2|scenario3>
  --base-url <url>              (default: http://192.168.20.235:30080)
  --duration <seconds>          (default: 600)
  --connect-timeout-ms <ms>     (default: 5000)
  --response-timeout-ms <ms>    (default: 30000)
  --no-prompt                   (do not wait for confirmation)
  -h, --help

Outputs:
  - Results (.jtl) and HTML report are written under repo-root tmp/
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --scenario) SCENARIO="$2"; shift 2;;
    --base-url) BASE_URL="$2"; shift 2;;
    --duration) DURATION="$2"; shift 2;;
    --connect-timeout-ms) CONNECT_TIMEOUT_MS="$2"; shift 2;;
    --response-timeout-ms) RESPONSE_TIMEOUT_MS="$2"; shift 2;;
    --no-prompt) NO_PROMPT=true; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown arg: $1" >&2; usage; exit 1;;
  esac
done

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "ERROR: missing command: $1" >&2; exit 1; }
}

parse_url() {
  local url="$1"
  if command -v python3 >/dev/null 2>&1; then
    python3 - "$url" <<'PY'
import sys
from urllib.parse import urlparse
u = urlparse(sys.argv[1])
if not u.scheme or not u.hostname:
  raise SystemExit(2)
port = u.port or (443 if u.scheme == 'https' else 80)
print(u.hostname)
print(port)
print(u.scheme)
PY
  else
    echo "ERROR: python3 is required to parse --base-url" >&2
    exit 2
  fi
}

require_cmd jmeter

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TMP_DIR="${REPO_ROOT}/tmp"
mkdir -p "${TMP_DIR}/perf/jmeter"

TEST_PLAN="${SCRIPT_DIR}/jvm-tuning-test.jmx"
if [[ ! -f "${TEST_PLAN}" ]]; then
  echo "ERROR: test plan not found: ${TEST_PLAN}" >&2
  exit 1
fi

mapfile -t parsed < <(parse_url "${BASE_URL}")
HOST_NAME="${parsed[0]}"
PORT="${parsed[1]}"
PROTOCOL="${parsed[2]}"

TS="$(date +%Y%m%d-%H%M%S)"
RESULT_JTL="${TMP_DIR}/perf/jmeter/results-${TS}.jtl"
REPORT_DIR="${TMP_DIR}/perf/jmeter/report-${TS}"
ARTIFACT_DIR="${TMP_DIR}/perf/jmeter/artifacts-${TS}"

# Scenario tuning (must match jvm-tuning-test.jmx properties)
DURATION_S1=1
DURATION_S2=1
DURATION_S3=1
RAMP_S1=1
RAMP_S2=1
RAMP_S3=1

DURATION_S3_DEFAULT=$(( DURATION / 2 ))
if (( DURATION_S3_DEFAULT < 60 )); then DURATION_S3_DEFAULT=60; fi
RAMP_DEFAULT=$(( DURATION / 6 ))
if (( RAMP_DEFAULT < 1 )); then RAMP_DEFAULT=1; fi
if (( RAMP_DEFAULT > 60 )); then RAMP_DEFAULT=60; fi

THREADS_S1=0
THREADS_S2=0
THREADS_S3=0

case "${SCENARIO}" in
  scenario1)
    DURATION_S1="${DURATION}"
    RAMP_S1="${RAMP_DEFAULT}"
    THREADS_S1=100
    ;;
  scenario2)
    DURATION_S2="${DURATION}"
    RAMP_S2="${RAMP_DEFAULT}"
    THREADS_S2=200
    ;;
  scenario3)
    DURATION_S3="${DURATION}"
    RAMP_S3="${RAMP_DEFAULT}"
    THREADS_S3=500
    ;;
  all|*)
    DURATION_S1="${DURATION}"
    DURATION_S2="${DURATION}"
    DURATION_S3="${DURATION_S3_DEFAULT}"
    RAMP_S1=60
    RAMP_S2=60
    RAMP_S3=60
    THREADS_S1=100
    THREADS_S2=200
    THREADS_S3=500
    ;;
esac

echo "=== EVCS JMeter Performance Test (Linux) ==="
echo "Test plan:        ${TEST_PLAN}"
echo "Base URL:         ${BASE_URL}"
echo "Scenario:         ${SCENARIO}"
echo "Duration:         ${DURATION}s"
echo "Connect timeout:  ${CONNECT_TIMEOUT_MS}ms"
echo "Response timeout: ${RESPONSE_TIMEOUT_MS}ms"
echo "Results (.jtl):    ${RESULT_JTL}"
echo "Report (HTML):     ${REPORT_DIR}/index.html"

if [[ "${NO_PROMPT}" != "true" ]]; then
  read -r -p "Press Enter to start (Ctrl+C to cancel)... " _
fi

mkdir -p "${REPORT_DIR}" "${ARTIFACT_DIR}"

CMD=(
  -n
  -t "${TEST_PLAN}"
  -l "${RESULT_JTL}"
  -e
  -o "${REPORT_DIR}"
  "-JBASE_URL=${BASE_URL}"
  "-JHOST=${HOST_NAME}"
  "-JPORT=${PORT}"
  "-JPROTOCOL=${PROTOCOL}"
  "-JCONNECT_TIMEOUT_MS=${CONNECT_TIMEOUT_MS}"
  "-JRESPONSE_TIMEOUT_MS=${RESPONSE_TIMEOUT_MS}"
  "-Jartifact_dir=${ARTIFACT_DIR}"
  "-Jduration_s1=${DURATION_S1}" "-Jduration_s2=${DURATION_S2}" "-Jduration_s3=${DURATION_S3}"
  "-Jramp_s1=${RAMP_S1}" "-Jramp_s2=${RAMP_S2}" "-Jramp_s3=${RAMP_S3}"
  "-Jthreads_s1=${THREADS_S1}" "-Jthreads_s2=${THREADS_S2}" "-Jthreads_s3=${THREADS_S3}"
)

echo
printf 'Command: jmeter'; printf ' %q' "${CMD[@]}"; echo

t0=$(date +%s)
set +e
jmeter "${CMD[@]}"
rc=$?
set -e
t1=$(date +%s)

echo
if [[ $rc -eq 0 ]]; then
  echo "OK: completed in $((t1-t0))s"
else
  echo "ERROR: jmeter exited with code ${rc}" >&2
fi

echo "Results: ${RESULT_JTL}"
echo "Report:  ${REPORT_DIR}/index.html"
exit $rc
