#!/usr/bin/env bash
# Manages the "docs" and "app" dev services (not PostgreSQL, which is owned by
# docker-compose). Both already watch/reload themselves (zensical serve,
# Spring DevTools) so this only handles the one-time detached launch, health
# gating, and teardown. Usage:
#   dev-services.sh <start|stop|restart|status|logs> [docs|app|all]
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root_dir="$(cd "$script_dir/../.." && pwd)"

# Isolates state per checkout so multiple clones on the same host don't collide.
export IDP_STATE_DIR="/tmp/idp-dev-services-$(id -u)-$(printf '%s' "$root_dir" | sha256sum | cut -c1-12)"
mkdir -p -m 700 "$IDP_STATE_DIR"

declare -A PORT=([docs]=8000 [app]=8084)
declare -A URL_PATH=([docs]=/ [app]=/actuator/health)
declare -A READY_TIMEOUT=([docs]=120 [app]=300)
declare -A COMMAND=(
  [docs]="uv run --frozen --directory docs zensical serve --dev-addr 0.0.0.0:8000"
  [app]="$root_dir/mvnw -B -ntp spring-boot:run -Dspring-boot.run.profiles=local"
)

port_occupied() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# Spring Boot's default actuator health mapping is 200 only when UP (503 otherwise).
is_healthy() {
  local name="$1" url code
  url="http://127.0.0.1:${PORT[$name]}${URL_PATH[$name]}"
  code="$(curl --noproxy '*' -sS -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || true)"
  [ "$code" = "200" ]
}

# Each service is launched under job control so it gets its own process
# group (pgid == its pid): signalling "-$pid" reaches the whole group.
running_pid() {
  local pid
  pid="$(cat "$IDP_STATE_DIR/$1.pid" 2>/dev/null)" || return 1
  [ -n "$pid" ] && kill -0 "-$pid" 2>/dev/null && echo "$pid"
}

svc_status() {
  local name="$1" log="$IDP_STATE_DIR/$name.log"
  local url="http://127.0.0.1:${PORT[$name]}${URL_PATH[$name]}"
  if running_pid "$name" >/dev/null; then
    if is_healthy "$name"; then
      echo "$name: ready, $url; log: $log"
      return 0
    fi
    echo "$name: starting or unhealthy; log: $log"
    return 1
  fi
  if port_occupied "${PORT[$name]}"; then
    echo "$name: port ${PORT[$name]} is externally owned; left untouched"
  else
    echo "$name: stopped; log: $log"
  fi
  return 1
}

svc_start() {
  local name="$1" deadline
  (
    flock -x 200
    if running_pid "$name" >/dev/null || port_occupied "${PORT[$name]}"; then
      exit 0
    fi
    rm -f "$IDP_STATE_DIR/$name.pid"
    # Job control (not setsid, which forks a copy under bash job control and
    # would make $! point at the wrong, short-lived PID) puts this in its own
    # process group so it doesn't share ours. Close the lock fd on the
    # backgrounded command itself: it outlives this subshell and would
    # otherwise keep the flock held forever through its inherited copy.
    set -m
    bash -c "exec ${COMMAND[$name]}" >"$IDP_STATE_DIR/$name.log" 2>&1 200>&- &
    echo "$!" >"$IDP_STATE_DIR/$name.pid"
    disown
  ) 200>"$IDP_STATE_DIR/$name.lock"

  if ! running_pid "$name" >/dev/null; then
    if port_occupied "${PORT[$name]}"; then
      echo "$name: port ${PORT[$name]} already occupied; skipping managed launch"
      return 0
    fi
    echo "$name: failed to launch; log: $IDP_STATE_DIR/$name.log" >&2
    return 1
  fi

  deadline=$((SECONDS + READY_TIMEOUT[$name]))
  while [ "$SECONDS" -lt "$deadline" ]; do
    is_healthy "$name" && { svc_status "$name"; return $?; }
    running_pid "$name" >/dev/null || break
    sleep 0.25
  done
  echo "$name: failed to become ready; log: $IDP_STATE_DIR/$name.log" >&2
  tail -n 30 "$IDP_STATE_DIR/$name.log" >&2 2>/dev/null || true
  svc_stop "$name" >/dev/null 2>&1 || true
  return 1
}

svc_stop() {
  local name="$1" pid
  if pid="$(running_pid "$name")" && [ -n "$pid" ]; then
    kill -TERM "-$pid" 2>/dev/null || true
    for _ in $(seq 1 50); do
      kill -0 "-$pid" 2>/dev/null || break
      sleep 0.1
    done
    kill -0 "-$pid" 2>/dev/null && kill -KILL "-$pid" 2>/dev/null || true
    echo "$name: stopped managed process"
  fi
  rm -f "$IDP_STATE_DIR/$name.pid"
  if port_occupied "${PORT[$name]}"; then
    echo "$name: port ${PORT[$name]} still occupied; left untouched"
    return 1
  fi
  return 0
}

svc_logs() {
  local name="$1" log="$IDP_STATE_DIR/$name.log"
  echo "$name: $log"
  [ -f "$log" ] && tail -n 80 "$log"
  return 0
}

usage() {
  echo "Usage: $(basename "$0") <start|stop|restart|status|logs> [docs|app|all]" >&2
  exit 2
}

action="${1:-}"
target="${2:-all}"
case "$action" in start | stop | restart | status | logs) ;; *) usage ;; esac
case "$target" in docs | app | all) ;; *) usage ;; esac

names=(docs app)
[ "$target" != "all" ] && names=("$target")

status=0
for name in "${names[@]}"; do
  case "$action" in
    start) svc_start "$name" || status=1 ;;
    stop) svc_stop "$name" || status=1 ;;
    restart)
      svc_stop "$name" || true
      svc_start "$name" || status=1
      ;;
    status) svc_status "$name" || status=1 ;;
    logs) svc_logs "$name" || status=1 ;;
  esac
done
exit "$status"
