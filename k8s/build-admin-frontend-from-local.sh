#!/bin/bash
set -euo pipefail

# Build and push evcs-admin frontend image inside the cluster using Kaniko,
# while sending build context from the local machine (no git clone, no hostPath).
#
# Flow:
# 1) Create/ensure a PVC as build workspace
# 2) Create an uploader Pod mounting the PVC
# 3) Stream-tar local evcs-admin/ into the PVC via kubectl exec
# 4) Run a Kaniko Job mounting the same PVC to build & push image

EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"
EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG:-dev}"
EVCS_ADMIN_DOCKERFILE="${EVCS_ADMIN_DOCKERFILE:-Dockerfile}"

# Version identifiers for admin-frontend static assets
EVCS_GIT_COMMIT="${EVCS_GIT_COMMIT:-}"
EVCS_GIT_BRANCH="${EVCS_GIT_BRANCH:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
EVCS_ADMIN_CONTEXT_DIR="${EVCS_ADMIN_CONTEXT_DIR:-${REPO_ROOT}/evcs-admin}"

KANIKO_CACHE_REPO="${KANIKO_CACHE_REPO:-${EVCS_K8S_REGISTRY}/evcs/kaniko-cache}"

NAMESPACE="evcs"
PVC_NAME="evcs-build-workspace"
UPLOADER_POD="build-workspace-uploader"
JOB_NAME="build-admin-frontend"

cleanup() {
  kubectl delete pod -n "${NAMESPACE}" "${UPLOADER_POD}" --ignore-not-found >/dev/null 2>&1 || true
}
trap cleanup EXIT

if [ ! -d "${EVCS_ADMIN_CONTEXT_DIR}" ]; then
  echo "Error: EVCS_ADMIN_CONTEXT_DIR not found: ${EVCS_ADMIN_CONTEXT_DIR}" >&2
  exit 1
fi

if [ ! -f "${EVCS_ADMIN_CONTEXT_DIR}/${EVCS_ADMIN_DOCKERFILE}" ]; then
  echo "Error: Dockerfile not found: ${EVCS_ADMIN_CONTEXT_DIR}/${EVCS_ADMIN_DOCKERFILE}" >&2
  exit 1
fi

echo "=== Building admin frontend image via Kaniko (local context upload) ==="
echo "Registry:   ${EVCS_K8S_REGISTRY}"
echo "Tag:        ${EVCS_IMAGE_TAG}"
echo "Dockerfile: ${EVCS_ADMIN_DOCKERFILE}"
echo "Local dir:  ${EVCS_ADMIN_CONTEXT_DIR}"

if [ -z "${EVCS_GIT_COMMIT}" ]; then
  EVCS_GIT_COMMIT="$(cd "${REPO_ROOT}" && git rev-parse --short HEAD 2>/dev/null || true)"
fi
if [ -z "${EVCS_GIT_BRANCH}" ]; then
  EVCS_GIT_BRANCH="$(cd "${REPO_ROOT}" && git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
fi

if [ -n "${EVCS_GIT_COMMIT}" ] && [ -n "${EVCS_GIT_BRANCH}" ]; then
  echo "Git:        ${EVCS_GIT_BRANCH}@${EVCS_GIT_COMMIT}"
else
  echo "Git:        unknown (no .git in build context)"
fi

echo "Ensuring PVC ${PVC_NAME}..."
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
      storage: 2Gi
  storageClassName: local-path
YAML

echo "Starting uploader pod ${UPLOADER_POD}..."
cleanup
cat <<YAML | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: ${UPLOADER_POD}
  namespace: ${NAMESPACE}
  labels:
    app: build-workspace-uploader
spec:
  restartPolicy: Never
  nodeSelector:
    role: core
  volumes:
  - name: workspace
    persistentVolumeClaim:
      claimName: ${PVC_NAME}
  containers:
  - name: uploader
    image: ${EVCS_K8S_REGISTRY}/base/node:20-alpine
    imagePullPolicy: IfNotPresent
    command: ["sh", "-lc"]
    args:
    - |
      set -e
      sleep infinity
    volumeMounts:
    - name: workspace
      mountPath: /workspace
YAML

kubectl wait -n "${NAMESPACE}" --for=condition=Ready pod/${UPLOADER_POD} --timeout=180s

echo "Uploading local build context to PVC..."
(
  cd "${REPO_ROOT}"
  # Keep upload small; prebuilt mode doesn't need node_modules.
  tar -czf - \
    --exclude="$(basename "${EVCS_ADMIN_CONTEXT_DIR}")/node_modules" \
    "$(basename "${EVCS_ADMIN_CONTEXT_DIR}")"
) | kubectl exec -n "${NAMESPACE}" -i pod/${UPLOADER_POD} -c uploader -- sh -lc '
  set -euo pipefail
  rm -rf /workspace/*
  mkdir -p /workspace
  tar -xzf - -C /workspace
'

echo "Stopping uploader pod..."
kubectl delete pod -n "${NAMESPACE}" "${UPLOADER_POD}" --ignore-not-found

echo "Running Kaniko job ${JOB_NAME}..."
kubectl delete job -n "${NAMESPACE}" "${JOB_NAME}" --ignore-not-found

cat <<YAML | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB_NAME}
  namespace: ${NAMESPACE}
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      nodeSelector:
        role: core
      volumes:
      - name: workspace
        persistentVolumeClaim:
          claimName: ${PVC_NAME}
      containers:
      - name: kaniko
        image: gcr.io/kaniko-project/executor:v1.23.2
        args:
        - --dockerfile=${EVCS_ADMIN_DOCKERFILE}
        - --context=dir:///workspace/evcs-admin
        - --build-arg=NODE_IMAGE=${EVCS_K8S_REGISTRY}/base/node:20-alpine
        - --build-arg=NGINX_IMAGE=${EVCS_K8S_REGISTRY}/base/nginx:alpine
        - --build-arg=GIT_COMMIT=${EVCS_GIT_COMMIT:-unknown}
        - --build-arg=GIT_BRANCH=${EVCS_GIT_BRANCH:-unknown}
        - --destination=${EVCS_K8S_REGISTRY}/evcs/admin-frontend:${EVCS_IMAGE_TAG}
        - --cache=true
        - --cache-repo=${KANIKO_CACHE_REPO}
        - --insecure
        - --skip-tls-verify
        - --insecure-registry=${EVCS_K8S_REGISTRY}
        volumeMounts:
        - name: workspace
          mountPath: /workspace
YAML

kubectl wait -n "${NAMESPACE}" --for=condition=complete job/${JOB_NAME} --timeout=30m

echo "=== Kaniko build logs (tail) ==="
kubectl logs -n "${NAMESPACE}" job/${JOB_NAME} --tail=200

echo "=== Build complete ==="
echo "Image: ${EVCS_K8S_REGISTRY}/evcs/admin-frontend:${EVCS_IMAGE_TAG}"
