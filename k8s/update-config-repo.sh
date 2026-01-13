#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

NAMESPACE="${EVCS_NAMESPACE:-evcs}"
RESTART_CONFIG_SERVER=true
RESTART_DEPLOYMENTS=()

usage() {
  cat <<'EOF'
Usage: k8s/update-config-repo.sh [options]

Options:
  -n, --namespace <ns>        Kubernetes namespace (default: evcs)
  --no-restart-config         Do not restart config-server
  --restart <deployment>      Also rollout-restart a deployment (repeatable)
  -h, --help                  Show help

What it does:
  1) Rebuilds ConfigMap evcs-config-repo from the local ./config-repo directory
  2) Optionally restarts config-server and/or other deployments so changes take effect

Examples:
  k8s/update-config-repo.sh
  k8s/update-config-repo.sh -n evcs --restart payment-service
  k8s/update-config-repo.sh --no-restart-config
EOF
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "Error: missing command: $1" >&2; exit 1; }
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -n|--namespace)
      NAMESPACE="$2"; shift 2;;
    --no-restart-config)
      RESTART_CONFIG_SERVER=false; shift;;
    --restart)
      RESTART_DEPLOYMENTS+=("$2"); shift 2;;
    -h|--help)
      usage; exit 0;;
    *)
      echo "Error: unknown argument: $1" >&2
      usage
      exit 1;;
  esac
done

need_cmd kubectl

cd "${REPO_ROOT}"

if [[ ! -d "config-repo" ]]; then
  echo "Error: config-repo directory not found at ${REPO_ROOT}/config-repo" >&2
  exit 1
fi

echo "Namespace: ${NAMESPACE}"
echo "Updating ConfigMap: evcs-config-repo (from ./config-repo)"

kubectl -n "${NAMESPACE}" create configmap evcs-config-repo \
  --from-file=config-repo \
  --dry-run=client \
  -o yaml | kubectl -n "${NAMESPACE}" apply -f -

echo "ConfigMap evcs-config-repo updated."

if [[ "${RESTART_CONFIG_SERVER}" == "true" ]]; then
  echo "Restarting deployment/config-server to pick up mounted config..."
  kubectl -n "${NAMESPACE}" rollout restart deployment/config-server
fi

for dep in "${RESTART_DEPLOYMENTS[@]}"; do
  echo "Restarting deployment/${dep} to re-fetch config..."
  kubectl -n "${NAMESPACE}" rollout restart "deployment/${dep}"
done

echo "Done."
