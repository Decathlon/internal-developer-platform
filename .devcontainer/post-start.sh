#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$(realpath "$0")")/.."
services=(bash .devcontainer/scripts/dev-services.sh)
result=0

"${services[@]}" start docs || result=1

wait_for() {
  for attempt in $(seq 1 30); do
    if "$@" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

if ! wait_for docker info; then
  echo "Docker daemon did not become ready; app startup skipped" >&2
  result=1
elif ! docker compose up -d postgres; then
  echo "PostgreSQL container failed to start; app startup skipped" >&2
  result=1
elif ! wait_for pg_isready -h localhost -p 5437 -U idpcore -d idpcore; then
  echo "PostgreSQL did not become ready on localhost:5437; app startup skipped" >&2
  result=1
else
  "${services[@]}" start app || result=1
fi

"${services[@]}" status all || result=1
exit "$result"
