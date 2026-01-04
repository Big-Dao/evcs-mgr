#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="evcs"
TIMEOUT_SECONDS=180
GATEWAY_URL=""
API_SMOKE_URL=""
VERBOSE=false
INCLUDE_JOBS=false

usage() {
  cat <<'EOF'
Usage: k8s/verify-after-reboot.sh [options]

Purpose:
  Read-only verification that the EVCS k3s environment recovered after a host reboot.
  The script does NOT modify any resource.

Options:
  -n, --namespace <ns>      Kubernetes namespace (default: evcs)
  -t, --timeout <seconds>   Rollout timeout seconds (default: 180)
  -g, --gateway-url <url>   Optional: check gateway health via HTTP GET (e.g. http://<node-ip>:30080/actuator/health)
  --api-smoke-url <url>     Optional: check API route is loaded (e.g. http://<node-ip>:30080/api/auth/login). Fails on 404/5xx.
  --include-jobs            Also treat Job-owned pods as part of verification (default: off)
  -v, --verbose             Print extra kubectl outputs
  -h, --help                Show help

Exit codes:
  0  All checks passed
  1  One or more checks failed

Examples:
  k8s/verify-after-reboot.sh
  k8s/verify-after-reboot.sh -g http://192.168.20.10:30080/actuator/health
  k8s/verify-after-reboot.sh --api-smoke-url http://192.168.20.10:30080/api/auth/login
EOF
}

log() { printf '%s\n' "$*"; }
warn() { printf 'WARN: %s\n' "$*" >&2; }
err() { printf 'ERROR: %s\n' "$*" >&2; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || { err "missing command: $1"; exit 1; }
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -n|--namespace)
      NAMESPACE="$2"; shift 2;;
    -t|--timeout)
      TIMEOUT_SECONDS="$2"; shift 2;;
    -g|--gateway-url)
      GATEWAY_URL="$2"; shift 2;;
    --api-smoke-url)
      API_SMOKE_URL="$2"; shift 2;;
    -v|--verbose)
      VERBOSE=true; shift;;
    --include-jobs)
      INCLUDE_JOBS=true; shift;;
    -h|--help)
      usage; exit 0;;
    *)
      err "unknown argument: $1"; usage; exit 1;;
  esac
done

need_cmd kubectl

if [[ -n "$GATEWAY_URL" || -n "$API_SMOKE_URL" ]]; then
  need_cmd curl
fi

TIMEOUT="${TIMEOUT_SECONDS}s"
FAILED=0

section() {
  log ""
  log "=== $* ==="
}

run_kubectl() {
  if $VERBOSE; then
    kubectl "$@"
  else
    kubectl "$@" >/dev/null
  fi
}

check_kubectl_access() {
  section "Cluster access"
  if ! kubectl version --client >/dev/null 2>&1; then
    err "kubectl not working (client)"; return 1
  fi

  if ! kubectl get ns "$NAMESPACE" >/dev/null 2>&1; then
    err "namespace '$NAMESPACE' not found or cluster not reachable"; return 1
  fi

  log "kubectl OK, namespace '$NAMESPACE' exists"
  return 0
}

check_nodes_ready() {
  section "Node readiness"
  local ready_count
  ready_count=$(kubectl get nodes --no-headers 2>/dev/null | awk '{print $2}' | grep -c 'Ready' || true)
  if [[ "$ready_count" -lt 1 ]]; then
    err "no Ready nodes";
    kubectl get nodes -o wide || true
    return 1
  fi

  if $VERBOSE; then
    kubectl get nodes -o wide
  else
    log "Ready nodes: ${ready_count}"
  fi

  return 0
}

rollout_deploy() {
  local deploy="$1"
  if ! kubectl -n "$NAMESPACE" get deploy "$deploy" >/dev/null 2>&1; then
    err "missing deployment: $deploy"
    return 1
  fi

  if ! kubectl -n "$NAMESPACE" rollout status "deploy/${deploy}" --timeout="$TIMEOUT"; then
    err "rollout not ready: deploy/${deploy}"
    return 1
  fi

  return 0
}

check_core_rollouts() {
  section "Core rollouts"
  local core_deployments=(postgres redis rabbitmq registry eureka config-server gateway)
  local d
  for d in "${core_deployments[@]}"; do
    if ! rollout_deploy "$d"; then
      FAILED=1
    fi
  done
}

check_all_deployments_ready() {
  section "All deployments readiness"
  # kubectl get deploy columns are: NAME READY UP-TO-DATE AVAILABLE AGE
  # Use spec.replicas vs status.availableReplicas for a reliable readiness check.
  local line name spec available ready
  local not_ready_lines=""
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    name=$(awk '{print $1}' <<<"$line")
    spec=$(awk '{print $2}' <<<"$line")
    available=$(awk '{print $3}' <<<"$line")
    ready=$(awk '{print $4}' <<<"$line")

    # spec.replicas may be empty when not set; default to 1 in most manifests.
    [[ -z "$spec" ]] && spec=1
    [[ -z "$available" ]] && available=0
    [[ -z "$ready" ]] && ready=0

    if [[ "$available" -lt "$spec" || "$ready" -lt "$spec" ]]; then
      not_ready_lines+="${name} (available ${available}/${spec}, ready ${ready}/${spec})"$'\n'
    fi
  done < <(kubectl -n "$NAMESPACE" get deploy -o jsonpath='{range .items[*]}{.metadata.name}{" "}{.spec.replicas}{" "}{.status.availableReplicas}{" "}{.status.readyReplicas}{"\n"}{end}' 2>/dev/null || true)

  if [[ -n "$not_ready_lines" ]]; then
    err "some deployments not ready:"
    printf '%s' "$not_ready_lines" >&2
    FAILED=1
    if $VERBOSE; then
      kubectl -n "$NAMESPACE" get deploy
    fi
  else
    log "all deployments have available/ready replicas"
  fi
}

check_problem_pods() {
  section "Pod status"
  local has_pods
  has_pods=$(kubectl -n "$NAMESPACE" get pods --no-headers 2>/dev/null | wc -l | tr -d ' ' || true)
  if [[ -z "$has_pods" || "$has_pods" -eq 0 ]]; then
    err "no pods found in namespace '$NAMESPACE'"; FAILED=1; return
  fi

  if $VERBOSE; then
    kubectl -n "$NAMESPACE" get pods -o wide
  else
    kubectl -n "$NAMESPACE" get pods
  fi

  # Only validate long-running workload pods by default.
  # - Include: ReplicaSet/StatefulSet/DaemonSet owned pods
  # - Exclude: Job owned pods (Completed/Failed build/probe jobs), unless --include-jobs
  local pod_line pod owner_kind status ready_field ready_ok
  local bad_pods=""
  while IFS= read -r pod_line; do
    [[ -z "$pod_line" ]] && continue
    pod=$(awk '{print $1}' <<<"$pod_line")
    status=$(awk '{print $3}' <<<"$pod_line")
    ready_field=$(awk '{print $2}' <<<"$pod_line")

    owner_kind=$(kubectl -n "$NAMESPACE" get pod "$pod" -o jsonpath='{.metadata.ownerReferences[0].kind}' 2>/dev/null || true)

    case "$owner_kind" in
      ReplicaSet|StatefulSet|DaemonSet)
        ;;
      Job)
        if ! $INCLUDE_JOBS; then
          continue
        fi
        ;;
      "")
        # Standalone pods are typically temporary probes; ignore by default.
        continue
        ;;
      *)
        continue
        ;;
    esac

    # Consider Completed as OK (common for Jobs when --include-jobs).
    if [[ "$status" == "Completed" ]]; then
      continue
    fi

    # Validate Running + Ready containers ratio.
    ready_ok=false
    if [[ "$status" == "Running" ]]; then
      # ready_field looks like 1/1
      if [[ "$ready_field" =~ ^([0-9]+)/([0-9]+)$ ]]; then
        if [[ "${BASH_REMATCH[1]}" == "${BASH_REMATCH[2]}" ]]; then
          ready_ok=true
        fi
      fi
    fi

    if ! $ready_ok; then
      bad_pods+="$pod"$'\n'
    fi
  done < <(kubectl -n "$NAMESPACE" get pods --no-headers 2>/dev/null || true)

  bad_pods=$(printf '%s' "$bad_pods" | sed '/^$/d' | sort -u || true)

  if [[ -n "$bad_pods" ]]; then
    err "found non-ready/problem pods:"; printf '%s\n' "$bad_pods" >&2
    FAILED=1

    local p
    for p in $bad_pods; do
      log ""
      log "--- describe pod/$p ---"
      kubectl -n "$NAMESPACE" describe pod "$p" || true

      # Print last 120 log lines from the first container
      local c
      c=$(kubectl -n "$NAMESPACE" get pod "$p" -o jsonpath='{.spec.containers[0].name}' 2>/dev/null || true)
      if [[ -n "$c" ]]; then
        log "--- logs pod/$p (container=$c, tail=120) ---"
        kubectl -n "$NAMESPACE" logs "$p" -c "$c" --tail=120 || true
      fi
    done
  else
    log "all pods are Running/Completed and Ready"
  fi
}

check_service_endpoints() {
  section "Service endpoints"
  local services=(evcs-postgres evcs-redis evcs-rabbitmq evcs-config evcs-eureka evcs-gateway)
  local s
  for s in "${services[@]}"; do
    if ! kubectl -n "$NAMESPACE" get svc "$s" >/dev/null 2>&1; then
      warn "service not found: $s";
      continue
    fi

    # If endpoints has no subsets, it's usually not ready
    local subsets
    subsets=$(kubectl -n "$NAMESPACE" get endpoints "$s" -o jsonpath='{.subsets}' 2>/dev/null || true)
    if [[ -z "$subsets" || "$subsets" == "[]" ]]; then
      err "service has no endpoints: $s"
      FAILED=1
    else
      log "endpoints OK: $s"
    fi
  done
}

check_gateway_health() {
  if [[ -z "$GATEWAY_URL" ]]; then
    return 0
  fi

  section "Gateway HTTP health"
  if ! curl -fsS --max-time 5 "$GATEWAY_URL" >/dev/null; then
    err "gateway health check failed: $GATEWAY_URL"
    FAILED=1
  else
    log "gateway health OK: $GATEWAY_URL"
  fi
}

derive_api_smoke_url_from_gateway_health() {
  if [[ -n "$API_SMOKE_URL" || -z "$GATEWAY_URL" ]]; then
    return 0
  fi

  # Common case: http://<node-ip>:30080/actuator/health
  local base
  base="$GATEWAY_URL"
  base="${base%/actuator/health}"
  base="${base%/actuator/health/}"

  if [[ "$base" == "$GATEWAY_URL" ]]; then
    # Can't confidently derive; leave empty.
    return 0
  fi

  API_SMOKE_URL="${base}/api/auth/login"
}

check_api_smoke() {
  derive_api_smoke_url_from_gateway_health
  if [[ -z "$API_SMOKE_URL" ]]; then
    return 0
  fi

  section "API smoke test"
  # Intention: ensure gateway has loaded routes from Config Server.
  # - 404 indicates route definition missing (common when config import failed).
  # - 5xx indicates backend not healthy.
  local code
  code=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 8 \
    -X POST "$API_SMOKE_URL" \
    -H 'Content-Type: application/json' \
    -d '{}' || echo "000")

  if [[ "$code" == "000" ]]; then
    err "api smoke request failed: $API_SMOKE_URL"
    FAILED=1
    return 0
  fi

  if [[ "$code" == "404" ]]; then
    err "api returned 404 (routes likely not loaded): $API_SMOKE_URL"
    FAILED=1
    return 0
  fi

  if [[ "$code" =~ ^5 ]]; then
    err "api returned ${code} (backend unhealthy): $API_SMOKE_URL"
    FAILED=1
    return 0
  fi

  log "api smoke OK (http ${code}): $API_SMOKE_URL"
}

main() {
  if ! check_kubectl_access; then FAILED=1; fi
  if ! check_nodes_ready; then FAILED=1; fi

  check_core_rollouts
  check_all_deployments_ready
  check_problem_pods
  check_service_endpoints
  check_gateway_health
  check_api_smoke

  section "Summary"
  if [[ "$FAILED" -eq 0 ]]; then
    log "PASS"
    exit 0
  fi

  err "FAIL"
  exit 1
}

main
