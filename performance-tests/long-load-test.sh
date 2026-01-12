#!/usr/bin/env bash
set -euo pipefail

# Long load test runner (Linux).
# For reliable, low-overhead long running load (for JFR collection), reuse simple-long-test.sh.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/simple-long-test.sh" "$@"
