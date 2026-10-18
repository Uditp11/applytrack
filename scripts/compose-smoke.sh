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

wait_for "app health is UP" 90 sh -c "curl -fsS $APP_URL/actuator/health | grep -q '\"status\":\"UP\"'"

# Fictional sample data. The name carries the time so the script can run more than once.
company=$(post /api/companies "{\"name\":\"Smoke Test Logistics $(date +%s)\"}" | id_of)
posting=$(post /api/postings "{\"companyId\":$company,\"title\":\"Site Reliability Engineer\"}" | id_of)
application=$(post /api/applications "{\"postingId\":$posting}" | id_of)
post "/api/applications/$application/transitions" '{"status":"APPLIED"}' >/dev/null
post "/api/applications/$application/transitions" '{"status":"INTERVIEW"}' >/dev/null
echo "ok: created application $application and moved it to INTERVIEW"

curl -fsS "$APP_URL/api/stats" | grep -q '"responded":[1-9]' || fail "stats do not count the response"
echo "ok: /api/stats counts the response"

# Prometheus scrapes every 15 seconds, so give it time to see the new counter.
wait_for "Prometheus has scraped applytrack_status_transitions_total" 90 sh -c \
  "curl -fsS '$PROMETHEUS_URL/api/v1/query?query=applytrack_status_transitions_total' | grep -q '\"to\":\"INTERVIEW\"'"

wait_for "Grafana is up" 60 curl -fsS "$GRAFANA_URL/api/health"
curl -fsS -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/datasources/uid/prometheus" | grep -q '"type":"prometheus"' \
  || fail "Grafana has no provisioned Prometheus datasource"
echo "ok: Grafana datasource provisioned"
curl -fsS -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/dashboards/uid/applytrack" | grep -q '"title":"ApplyTrack"' \
  || fail "Grafana has no provisioned ApplyTrack dashboard"
echo "ok: Grafana dashboard provisioned"

echo "Smoke test passed"
