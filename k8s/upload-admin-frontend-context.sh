#!/bin/bash
set -euo pipefail

# Upload evcs-admin build context into a PVC inside the cluster.
# This avoids hostPath mounts and avoids requiring the cluster to access GitHub.
# Intended usage from a CI runner or any machine with kubectl access.

PVC_NAME="${EVCS_BUILD_PVC_NAME:-evcs-build-workspace}"
PVC_SIZE="${EVCS_BUILD_PVC_SIZE:-2Gi}"
NAMESPACE="${EVCS_NAMESPACE:-evcs}"

EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"

LOCAL_CONTEXT_DIR="${EVCS_ADMIN_CONTEXT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../evcs-admin" && pwd)}"
REMOTE_SUBDIR="${EVCS_WORKSPACE_SUBDIR:-evcs-admin}"

UPLOADER_POD="${EVCS_WORKSPACE_UPLOADER_POD:-build-workspace-uploader}"

if [ ! -d "${LOCAL_CONTEXT_DIR}" ]; then
  echo "Error: local context dir not found: ${LOCAL_CONTEXT_DIR}" >&2
  exit 1
fi

echo "=== Preparing build workspace PVC ==="
echo "Namespace: ${NAMESPACE}"
echo "PVC:       ${PVC_NAME} (${PVC_SIZE})"
echo "Local:     ${LOCAL_CONTEXT_DIR}"
echo "Remote:    /workspace/${REMOTE_SUBDIR}"

kubectl get ns "${NAMESPACE}" >/dev/null 2>&1 || kubectl create ns "${NAMESPACE}"

# Create PVC if missing
if ! kubectl get pvc -n "${NAMESPACE}" "${PVC_NAME}" >/dev/null 2>&1; then
  cat <<YAML | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: ${PVC_NAME}
  namespace: ${NAMESPACE}
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: ${PVC_SIZE}
  storageClassName: local-path
YAML
fi

echo "=== Starting uploader pod ==="
# Recreate uploader pod to ensure clean state
kubectl delete pod -n "${NAMESPACE}" "${UPLOADER_POD}" --ignore-not-found

cat <<YAML | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: ${UPLOADER_POD}
  namespace: ${NAMESPACE}
spec:
  restartPolicy: Never
  nodeSelector:
    role: core
  containers:
  - name: uploader
    image: ${EVCS_K8S_REGISTRY}/base/node:20-alpine
    imagePullPolicy: IfNotPresent
    command: ["sh", "-lc"]
    args: ["sleep 3600"]
    volumeMounts:
    - name: workspace
      mountPath: /workspace
  volumes:
  - name: workspace
    persistentVolumeClaim:
      claimName: ${PVC_NAME}
YAML

kubectl wait -n "${NAMESPACE}" --for=condition=Ready pod/${UPLOADER_POD} --timeout=120s

echo "=== Uploading context via kubectl cp ==="
# Ensure target dir exists and is empty
kubectl exec -n "${NAMESPACE}" "${UPLOADER_POD}" -- sh -lc "rm -rf /workspace/${REMOTE_SUBDIR} && mkdir -p /workspace/${REMOTE_SUBDIR}"

# Copy local context contents into PVC
kubectl cp "${LOCAL_CONTEXT_DIR}/." "${NAMESPACE}/${UPLOADER_POD}:/workspace/${REMOTE_SUBDIR}"

echo "=== Uploaded. Cleaning uploader pod ==="
kubectl delete pod -n "${NAMESPACE}" "${UPLOADER_POD}" --ignore-not-found

echo "OK: context available in PVC ${PVC_NAME} at /workspace/${REMOTE_SUBDIR}"
