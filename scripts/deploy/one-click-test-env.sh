#!/bin/bash
set -euo pipefail

# One-click: local test environment (docker-compose.test.yml)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

SKIP_HEALTH="${SKIP_HEALTH:-false}"

cd "$PROJECT_ROOT"

./scripts/deploy/workflow.sh prepare-dirs
./scripts/deploy/workflow.sh test-env

if [[ "$SKIP_HEALTH" != "true" ]]; then
  ./scripts/deploy/workflow.sh health
fi
