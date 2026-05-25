#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

red() { echo "❌ $*"; }
green() { echo "✅ $*"; }

section() { echo; echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; echo "▶ $1"; echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; }

# Check .env on host (values hidden)
if [[ -f "$ROOT/.env" ]]; then
  openai_set=$(grep -E '^OPENAI_API_KEY=.+' "$ROOT/.env" | wc -l | tr -d ' ')
  gemini_set=$(grep -E '^GEMINI_API_KEY=.+' "$ROOT/.env" | wc -l | tr -d ' ')
  if [[ "$openai_set" == "0" ]]; then red ".env: OPENAI_API_KEY is empty — paste your key and save the file"; exit 1; fi
  if [[ "$gemini_set" == "0" ]]; then red ".env: GEMINI_API_KEY is empty — paste your key and save the file"; exit 1; fi
  green ".env keys look set on disk"
else
  red "Missing $ROOT/.env"; exit 1
fi

section "Restart Docker (loads .env)"
cd "$ROOT"
docker compose down
docker compose up --build -d
for i in $(seq 1 20); do
  curl -sf "$BASE/v1/health" >/dev/null && break
  sleep 2
done
green "API healthy"

section "Verify keys inside container"
docker exec planbvalidator-api-1 sh -c '
  test -n "$OPENAI_API_KEY" && echo OPENAI=ok || echo OPENAI=missing
  test -n "$GEMINI_API_KEY" && echo GEMINI=ok || echo GEMINI=missing
' | while read -r line; do
  if [[ "$line" == *missing* ]]; then red "Container: $line — run docker compose from project root after saving .env"; exit 1; fi
  green "Container: $line"
done

section "POST /v1/analyze (full pipeline, research ON)"
curl -s -m 240 -X POST "$BASE/v1/analyze" \
  -H 'Content-Type: application/json' \
  -d @"$ROOT/scripts/e2e-analyze.json" \
  -o /tmp/planb-e2e.json -w "HTTP %{http_code} in %{time_total}s\n"

python3 <<'PY'
import json
with open("/tmp/planb-e2e.json") as f:
    r = json.load(f)

print("\n--- Summary ---")
print("requestId:", r.get("requestId"))
print("verdict:", r.get("overallVerdict"))
print("feasibility:", r.get("feasibilityScore"), "risk:", r.get("riskScore"))
print("confidence:", r.get("confidence"))
print("runwayMonths:", r.get("runwayMonths"))
print("opportunityCost:", r.get("opportunityCost", {}).get("score"), r.get("opportunityCost", {}).get("band"))
print("\n--- AI providers ---")
for k, v in sorted(r.get("aiProviders", {}).items()):
    print(f"  {k}: {v}")
print("\n--- Timings (ms) ---")
for k, v in sorted(r.get("timings", {}).items()):
    print(f"  {k}: {v}")
print("\n--- Research ---")
rc = r.get("researchContext") or {}
print("  corporate_salary_range:", (rc.get("corporate_salary_range") or "(none)")[:80])
print("  market_sentiment:", rc.get("market_sentiment"))
print("\n--- Narrative (first 120 chars) ---")
print(" ", (r.get("recommendationSummary") or "")[:120])
print("\n--- Data gaps ---")
for g in r.get("dataGaps", []):
    print(" ", g)

providers = r.get("aiProviders", {})
failed = [k for k, v in providers.items() if v in ("not_configured", "failed")]
if failed:
    print("\n⚠️  Some providers did not succeed:", ", ".join(failed))
    raise SystemExit(1)
if r.get("processingMs", 0) < 1000:
    print("\n⚠️  processingMs < 1s — likely fallbacks only, not real LLM calls")
    raise SystemExit(1)
print("\n✅ End-to-end analyze completed with live AI providers")
PY
