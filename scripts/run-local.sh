#!/usr/bin/env bash
# Run Plan B Validator on http://localhost:8080 without Docker.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -f .env ]]; then
  echo "Missing .env — copy from example:"
  echo "  cp .env.example .env"
  echo "Then add OPENAI_API_KEY and GEMINI_API_KEY."
  exit 1
fi

if lsof -nP -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port 8080 is already in use. Stop Docker or the other process first:"
  lsof -nP -iTCP:8080 -sTCP:LISTEN || true
  echo ""
  echo "  docker compose down    # if Docker is using 8080"
  exit 1
fi

# Load API keys (same as docker compose env_file)
set -a
# shellcheck disable=SC1091
source .env
set +a

# Prefer Java 21 (project target). Fall back to current JAVA_HOME / PATH.
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  if JAVA_21="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
    export JAVA_HOME="$JAVA_21"
  fi
fi

echo "Using JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(command -v java)")")}"
java -version
echo ""
echo "Starting API at http://localhost:8080 (Ctrl+C to stop)"
echo ""

exec ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
