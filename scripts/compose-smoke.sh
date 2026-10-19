#!/usr/bin/env bash
# Smoke test for the docker compose stack: the API checks from smoke.sh, then Prometheus has scraped
# the status change counter and Grafana has its datasource and dashboard.
# Usage: scripts/compose-smoke.sh   (override APP_URL, PROMETHEUS_URL, GRAFANA_URL if needed)
set -euo pipefail

export APP_URL=${APP_URL:-http://localhost:8080}
PROMETHEUS_URL=${PROMETHEUS_URL:-http://localhost:9090}
GRAFANA_URL=${GRAFANA_URL:-http://localhost:3000}
GRAFANA_AUTH="admin:${GRAFANA_ADMIN_PASSWORD:-admin}"

"$(dirname "$0")/smoke.sh"

# shellcheck source=lib.sh
source "$(dirname "$0")/lib.sh"

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

echo "Compose smoke test passed"
