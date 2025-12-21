#!/bin/bash
set -euo pipefail

# Build and push evcs-admin frontend image inside the cluster using Kaniko (no local Docker required).
# Recommended: do NOT use hostPath. Use git checkout in CI, or git clone inside the pod.
# This script uses: initContainer git clone -> emptyDir workspace -> Kaniko build/push.

EVCS_K8S_REGISTRY="${EVCS_K8S_REGISTRY:-192.168.20.235:5000}"
EVCS_IMAGE_TAG="${EVCS_IMAGE_TAG:-dev}"

# Build context mode:
# - git (default): initContainer clones repo into emptyDir
# - pvc: use a pre-populated PVC as /workspace (recommended when cluster cannot access GitHub)
EVCS_BUILD_CONTEXT_MODE="${EVCS_BUILD_CONTEXT_MODE:-git}"
EVCS_BUILD_PVC_NAME="${EVCS_BUILD_PVC_NAME:-evcs-build-workspace}"

# Git context (override when needed)
EVCS_GIT_URL="${EVCS_GIT_URL:-https://github.com/Big-Dao/evcs-mgr.git}"
EVCS_GIT_REF="${EVCS_GIT_REF:-main}"
EVCS_GIT_SUBDIR="${EVCS_GIT_SUBDIR:-evcs-admin}"

# Enable Kaniko cache to speed up rebuilds
KANIKO_CACHE_REPO="${KANIKO_CACHE_REPO:-${EVCS_K8S_REGISTRY}/evcs/kaniko-cache}"

JOB_NAME="build-admin-frontend"

echo "=== Building admin frontend image via Kaniko ==="
echo "Registry:  ${EVCS_K8S_REGISTRY}"
echo "Tag:       ${EVCS_IMAGE_TAG}"
echo "Mode:      ${EVCS_BUILD_CONTEXT_MODE}"
echo "Subdir:    ${EVCS_GIT_SUBDIR}"
if [ "${EVCS_BUILD_CONTEXT_MODE}" = "git" ]; then
  echo "Git URL:   ${EVCS_GIT_URL}"
  echo "Git REF:   ${EVCS_GIT_REF}"
else
  echo "PVC:       ${EVCS_BUILD_PVC_NAME} (must contain /workspace/${EVCS_GIT_SUBDIR})"
fi

echo "Cleaning up previous job (if any)..."
kubectl delete job -n evcs "${JOB_NAME}" --ignore-not-found

JOB_YAML=""
if [ "${EVCS_BUILD_CONTEXT_MODE}" = "git" ]; then
  JOB_YAML="$(cat <<YAML
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB_NAME}
  namespace: evcs
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      nodeSelector:
        role: core
      initContainers:
      - name: git-clone
        image: docker.m.daocloud.io/alpine/git:2.45.2
        command: [\"sh\", \"-lc\"]
        args:
        - |
          set -euo pipefail
          apk add --no-cache ca-certificates >/dev/null 2>&1 || true
          update-ca-certificates >/dev/null 2>&1 || true

          if [ \"${EVCS_GIT_SSL_NO_VERIFY:-false}\" = \"true\" ]; then
            export GIT_SSL_NO_VERIFY=true
            echo \"[WARN] EVCS_GIT_SSL_NO_VERIFY=true (temporary workaround)\"
          fi

          rm -rf /workspace/*
          git clone --depth=1 --branch=\"${EVCS_GIT_REF}\" \"${EVCS_GIT_URL}\" /workspace
        volumeMounts:
        - name: workspace
          mountPath: /workspace
      containers:
      - name: kaniko
        image: gcr.io/kaniko-project/executor:v1.23.2
        args:
        - --dockerfile=Dockerfile
        - --context=dir:///workspace/${EVCS_GIT_SUBDIR}
        - --build-arg=NODE_IMAGE=${EVCS_K8S_REGISTRY}/base/node:20-alpine
        - --build-arg=NGINX_IMAGE=${EVCS_K8S_REGISTRY}/base/nginx:alpine
        - --destination=${EVCS_K8S_REGISTRY}/evcs/admin-frontend:${EVCS_IMAGE_TAG}
        - --cache=true
        - --cache-repo=${KANIKO_CACHE_REPO}
        - --insecure
        - --skip-tls-verify
        - --insecure-registry=${EVCS_K8S_REGISTRY}
        volumeMounts:
        - name: workspace
          mountPath: /workspace
      volumes:
      - name: workspace
        emptyDir: {}
YAML
  )"
elif [ "${EVCS_BUILD_CONTEXT_MODE}" = "pvc" ]; then
  JOB_YAML="$(cat <<YAML
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB_NAME}
  namespace: evcs
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      nodeSelector:
        role: core
      containers:
      - name: kaniko
        image: gcr.io/kaniko-project/executor:v1.23.2
        args:
        - --dockerfile=Dockerfile
        - --context=dir:///workspace/${EVCS_GIT_SUBDIR}
        - --build-arg=NODE_IMAGE=${EVCS_K8S_REGISTRY}/base/node:20-alpine
        - --build-arg=NGINX_IMAGE=${EVCS_K8S_REGISTRY}/base/nginx:alpine
        - --destination=${EVCS_K8S_REGISTRY}/evcs/admin-frontend:${EVCS_IMAGE_TAG}
        - --cache=true
        - --cache-repo=${KANIKO_CACHE_REPO}
        - --insecure
        - --skip-tls-verify
        - --insecure-registry=${EVCS_K8S_REGISTRY}
        volumeMounts:
        - name: workspace
          mountPath: /workspace
      volumes:
      - name: workspace
        persistentVolumeClaim:
          claimName: ${EVCS_BUILD_PVC_NAME}
YAML
  )"
else
  echo "Error: unsupported EVCS_BUILD_CONTEXT_MODE='${EVCS_BUILD_CONTEXT_MODE}' (expected: git|pvc)" >&2
  exit 1
fi

echo "Applying Kaniko job..."
echo "${JOB_YAML}" | kubectl apply -f -

echo "Waiting for job completion..."
kubectl wait -n evcs --for=condition=complete job/${JOB_NAME} --timeout=20m

echo "=== Kaniko build logs (tail) ==="
kubectl logs -n evcs job/${JOB_NAME} --tail=200
