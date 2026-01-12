#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://192.168.20.235:30080"

usage() {
  cat <<'EOF'
Usage: performance-tests/quick-verify.sh [--base-url <url>]

This is a lightweight sanity check to verify JMeter can reach the target.
Outputs are written under repo-root tmp/.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url) BASE_URL="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown arg: $1" >&2; usage; exit 1;;
  esac
done

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "ERROR: missing command: $1" >&2; exit 1; }
}

parse_url() {
  local url="$1"
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
}

require_cmd python3
require_cmd jmeter
require_cmd curl

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TMP_DIR="${REPO_ROOT}/tmp"
mkdir -p "${TMP_DIR}/perf/jmeter"

mapfile -t parsed < <(parse_url "${BASE_URL}")
HOST_NAME="${parsed[0]}"
PORT="${parsed[1]}"
PROTOCOL="${parsed[2]}"

TS="$(date +%Y%m%d-%H%M%S)"
JMX="${TMP_DIR}/perf/jmeter/quick-verify-${TS}.jmx"
JTL="${TMP_DIR}/perf/jmeter/quick-verify-${TS}.jtl"

cat >"${JMX}" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Quick Verify" enabled="true">
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">${BASE_URL}</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" testname="HTTP Defaults" enabled="true">
        <stringProp name="HTTPSampler.domain">${HOST_NAME}</stringProp>
        <stringProp name="HTTPSampler.port">${PORT}</stringProp>
        <stringProp name="HTTPSampler.protocol">${PROTOCOL}</stringProp>
        <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
        <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
      </ConfigTestElement>
      <hashTree/>

      <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="Headers" enabled="true">
        <collectionProp name="HeaderManager.headers">
          <elementProp name="" elementType="Header">
            <stringProp name="Header.name">Content-Type</stringProp>
            <stringProp name="Header.value">application/json</stringProp>
          </elementProp>
          <elementProp name="" elementType="Header">
            <stringProp name="Header.name">X-Tenant-Id</stringProp>
            <stringProp name="Header.value">1</stringProp>
          </elementProp>
        </collectionProp>
      </HeaderManager>
      <hashTree/>

      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Verify" enabled="true">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">5</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">5</stringProp>
        <stringProp name="ThreadGroup.ramp_time">5</stringProp>
        <boolProp name="ThreadGroup.scheduler">false</boolProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET /actuator/health" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments"/>
          </elementProp>
          <stringProp name="HTTPSampler.path">/actuator/health</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
        </HTTPSamplerProxy>
        <hashTree/>
      </hashTree>

      <ResultCollector guiclass="SummaryReport" testclass="ResultCollector" testname="Summary" enabled="true">
        <boolProp name="ResultCollector.error_logging">false</boolProp>
        <objProp>
          <name>saveConfig</name>
          <value class="SampleSaveConfiguration">
            <time>true</time>
            <latency>true</latency>
            <timestamp>true</timestamp>
            <success>true</success>
            <label>true</label>
            <code>true</code>
            <message>true</message>
          </value>
        </objProp>
        <stringProp name="filename">${JTL}</stringProp>
      </ResultCollector>
      <hashTree/>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
EOF

echo "=== JMeter Quick Verify (Linux) ==="
echo "Target: ${BASE_URL}"
echo "Plan:   ${JMX}"
echo "JTL:    ${JTL}"

echo
jmeter -n -t "${JMX}" -l "${JTL}" -Jjmeter.save.saveservice.output_format=csv

echo
# Best-effort parse: CSV lines usually start with epochMillis
TOTAL=$(grep -E '^[0-9]+,' "${JTL}" 2>/dev/null | wc -l | tr -d ' ')
OK=$(grep -E '^[0-9]+,' "${JTL}" 2>/dev/null | awk -F',' 'NF>=4 && $4==200 {c++} END{print c+0}')
FAIL=$((TOTAL-OK))

echo "Total:   ${TOTAL}"
echo "Success: ${OK}"
echo "Fail:    ${FAIL}"

if [[ ${TOTAL} -gt 0 && ${FAIL} -eq 0 ]]; then
  echo "OK: environment reachable via JMeter"
  exit 0
fi

echo "WARN: quick verify had failures; check gateway health or network"
exit 1
