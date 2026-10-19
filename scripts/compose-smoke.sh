#!/usr/bin/env bash
# Smoke test for the running stack (docker compose up): the API works, Prometheus scrapes the
# status change counter, and Grafana has its datasource and dashboard.
# Usage: scripts/compose-smoke.sh   (override APP_URL, PROMETHEUS_URL, GRAFANA_URL if needed)
set -euo pipefail

APP_URL=${APP_URL:-http://localhost:8080}
PROMETHEUS_URL=${PROMETHEUS_URL:-http://localhost:9090}
GRAFANA_URL=${GRAFANA_URL:-http://localhost:3000}
GRAFANA_AUTH="admin:${GRAFANA_ADMIN_PASSWORD:-admin}"

fail() { echo "FAIL: $*" >&2; exit 1; }

# fetch_contains <pattern> <curl arguments...>: true if the response body matches the pattern.
# The body goes into a variable first: piping curl into "grep -q" can fail when grep stops reading early.
fetch_contains() {
  local pattern=$1 body; shift
  body=$(curl -fsS "$@") || return 1
  grep -q "$pattern" <<<"$body"
}

# wait_for <description> <seconds> <command...>: retries the command once per second.
wait_for() {
  local what=$1 seconds=$2; shift 2
  for _ in $(seq 1 "$seconds"); do
    if "$@" >/dev/null 2>&1; then echo "ok: $what"; return 0; fi
    sleep 1
  done
  fail "$what (gave up after ${seconds}s)"
}

post() { curl -fsS -H 'Content-Type: application/json' -d "$2" "$APP_URL$1"; }
id_of() { sed -E 's/^\{"id":([0-9]+).*/\1/'; }

wait_for "app health is UP" 90 fetch_contains '"status":"UP"' "$APP_URL/actuator/health"

# Fictional sample data. The name carries the time so the script can run more than once.
company=$(post /api/companies "{\"name\":\"Smoke Test Logistics $(date +%s)\"}" | id_of)
posting=$(post /api/postings "{\"companyId\":$company,\"title\":\"Site Reliability Engineer\"}" | id_of)
application=$(post /api/applications "{\"postingId\":$posting}" | id_of)
post "/api/applications/$application/transitions" '{"status":"APPLIED"}' >/dev/null
post "/api/applications/$application/transitions" '{"status":"INTERVIEW"}' >/dev/null
echo "ok: created application $application and moved it to INTERVIEW"

fetch_contains '"responded":[1-9]' "$APP_URL/api/stats" || fail "stats do not count the response"
echo "ok: /api/stats counts the response"

# Prometheus scrapes every 15 seconds, so give it time. Every counter exists at 0 from startup, so ask for > 0.
wait_for "Prometheus has scraped the INTERVIEW move" 90 fetch_contains '"to":"INTERVIEW"' \
  --get --data-urlencode 'query=applytrack_status_transitions_total{to="INTERVIEW"} > 0' \
  "$PROMETHEUS_URL/api/v1/query"

wait_for "Grafana is up" 60 fetch_contains '"database"' "$GRAFANA_URL/api/health"
fetch_contains '"type":"prometheus"' -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/datasources/uid/prometheus" \
  || fail "Grafana has no provisioned Prometheus datasource"
echo "ok: Grafana datasource provisioned"
fetch_contains '"title":"ApplyTrack"' -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/dashboards/uid/applytrack" \
  || fail "Grafana has no provisioned ApplyTrack dashboard"
echo "ok: Grafana dashboard provisioned"

echo "Smoke test passed"
