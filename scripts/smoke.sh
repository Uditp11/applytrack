#!/usr/bin/env bash
# API smoke test against a running ApplyTrack: health, the main flow, an illegal move, statistics.
# Used for docker compose (through compose-smoke.sh) and for the kind cluster in CI.
# Usage: scripts/smoke.sh   (set APP_URL if the app is not on http://localhost:8080)
# SMOKE_WAIT_SECONDS: how long to wait for health (default 90; a scaled-to-zero app needs longer),
# CURL_MAX_TIME: the time limit of each request in seconds (default 10).
set -euo pipefail

APP_URL=${APP_URL:-http://localhost:8080}

# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

post() { curl -fsS --max-time "${CURL_MAX_TIME:-10}" -H 'Content-Type: application/json' -d "$2" "$APP_URL$1"; }
id_of() { sed -E 's/^\{"id":([0-9]+).*/\1/'; }

wait_for "app health is UP" "${SMOKE_WAIT_SECONDS:-90}" fetch_contains '"status":"UP"' "$APP_URL/actuator/health"
fetch_contains '"status":"UP"' "$APP_URL/actuator/health/readiness" || fail "readiness is not UP"
echo "ok: readiness is UP"

# Fictional sample data. The name carries the time so the script can run more than once.
company=$(post /api/companies "{\"name\":\"Smoke Test Logistics $(date +%s%N)\"}" | id_of)
posting=$(post /api/postings "{\"companyId\":$company,\"title\":\"Site Reliability Engineer\"}" | id_of)
application=$(post /api/applications "{\"postingId\":$posting}" | id_of)
post "/api/applications/$application/transitions" '{"status":"APPLIED"}' >/dev/null
post "/api/applications/$application/transitions" '{"status":"INTERVIEW"}' >/dev/null
echo "ok: created application $application and moved it to INTERVIEW"

refused=$(curl -sS --max-time "${CURL_MAX_TIME:-10}" -w '\n%{http_code}' -H 'Content-Type: application/json' \
  -d '{"status":"DRAFT"}' "$APP_URL/api/applications/$application/transitions")
[ "${refused##*$'\n'}" = 409 ] || fail "moving INTERVIEW back to DRAFT did not return 409: $refused"
grep -q '"title":"Illegal status transition"' <<<"$refused" || fail "the 409 is not an illegal transition: $refused"
echo "ok: an illegal move is refused with 409"

fetch_contains '"responded":[1-9]' "$APP_URL/api/stats" || fail "stats do not count the response"
echo "ok: /api/stats counts the response"

echo "API smoke test passed"
