# Helpers shared by the smoke scripts. Sourced, not run.

fail() { echo "FAIL: $*" >&2; exit 1; }

# fetch_contains <pattern> <curl arguments...>: true if the response body matches the pattern.
# The body goes into a variable first: piping curl into "grep -q" can fail when grep stops reading early.
fetch_contains() {
  local pattern=$1 body; shift
  body=$(curl -fsS --max-time "${CURL_MAX_TIME:-10}" "$@") || return 1
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
