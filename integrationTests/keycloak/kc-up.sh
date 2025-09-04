#!/usr/bin/env bash
set -euo pipefail

# --- config (can be overridden via env or flags) ---
NAME="${NAME:-kc}"
HOST="${HOST:-192.168.56.2}"     # host/IP you will curl from
PORT="${PORT:-9000}"             # host port to expose KC on
IMAGE="${IMAGE:-quay.io/keycloak/keycloak:24.0}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"
REL_PATH="${REL_PATH:-/auth}"    # keep /auth for CAS compatibility

# simple flags: -n name -H host -P port -i image -u admin -p password -r /path
while getopts ":n:H:P:i:u:p:r:" opt; do
  case "$opt" in
    n) NAME="$OPTARG" ;;
    H) HOST="$OPTARG" ;;
    P) PORT="$OPTARG" ;;
    i) IMAGE="$OPTARG" ;;
    u) ADMIN_USER="$OPTARG" ;;
    p) ADMIN_PASS="$OPTARG" ;;
    r) REL_PATH="$OPTARG" ;;
  esac
done

log() { printf '[kc-up] %s\n' "$*" >&2; }

# clean old container if present
if sudo docker ps -a --format '{{.Names}}' | grep -q "^${NAME}\$"; then
  log "Container ${NAME} already exists; removing…"
  sudo docker rm -f "${NAME}" >/dev/null
fi

log "Starting ${IMAGE} as ${NAME} on :${PORT} (relative path: ${REL_PATH})…"
sudo docker run -d --name "${NAME}" \
  -p "${PORT}:9000" \
  -e KEYCLOAK_ADMIN="${ADMIN_USER}" \
  -e KEYCLOAK_ADMIN_PASSWORD="${ADMIN_PASS}" \
  -e KC_HEALTH_ENABLED=true \
  "${IMAGE}" start-dev \
    --http-relative-path="${REL_PATH}" \
    --proxy=edge \
    --hostname-strict=false \
    --http-enabled=true >/dev/null

# wait until the discovery doc is reachable
DISCOVERY_URL="http://${HOST}:${PORT}${REL_PATH}/realms/master/.well-known/openid-configuration"
log "Waiting for Keycloak… (${DISCOVERY_URL})"
for i in $(seq 1 60); do
  if curl -fsS "${DISCOVERY_URL}" >/dev/null 2>&1; then
    log "Keycloak is up: http://${HOST}:${PORT}${REL_PATH}"
    exit 0
  fi
  sleep 1
done

log "Keycloak did not become ready in time. Recent logs:"
sudo docker logs --tail 200 "${NAME}" || true
exit 1
