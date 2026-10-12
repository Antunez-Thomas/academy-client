#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$repo_root"

echo "[agent-check] validating diff"
git diff --check

echo "[agent-check] scanning for credential patterns"
if rg -n --hidden \
  --glob '!.git/**' \
  --glob '!build/**' \
  --glob '!.gradle/**' \
  --glob '!**/README.md' \
  --glob '!docs/**' \
  --glob '!shared/src/commonTest/**' \
  -e 'BEGIN (RSA|OPENSSH|EC|PRIVATE) KEY' \
  -e '(WORDPRESS|BUNNY|ACADEMY)_[A-Z0-9_]*(PASSWORD|SECRET|TOKEN|KEY)[[:space:]]*=' \
  -e 'Authorization:[[:space:]]*Bearer[[:space:]]+[A-Za-z0-9._-]{20,}' \
  .; then
  echo '[agent-check] possible credential material found' >&2
  exit 1
fi

echo "[agent-check] running shared Android-host tests"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/tmp/academy-gradle-agent}"
./gradlew :shared:testAndroidHostTest --no-daemon --console=plain

echo '[agent-check] passed'
