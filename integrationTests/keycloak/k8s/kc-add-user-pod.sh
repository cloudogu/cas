#!/usr/bin/env bash
set -euo pipefail

# This script runs INSIDE the Keycloak pod
# Usage: called by kc-add-user-k8s.sh

REL=/auth
BASE=http://localhost:8080${REL}
KCADM=/opt/keycloak/bin/kcadm.sh

# Use a writable location for kcadm config since some Keycloak images mount /opt as read-only
KCADM_CONFIG=/tmp/kcadm.config

: "${REALM:?missing REALM}"
: "${GROUP:?missing GROUP}"
: "${USERNAME:?missing USERNAME}"
: "${EMAIL:?missing EMAIL}"
: "${PASSWORD:?missing PASSWORD}"
: "${FIRST:?missing FIRST}"
: "${LAST:?missing LAST}"

echo '[inside] Using kcadm config:' "$KCADM_CONFIG"

echo '[inside] Login to master...'
$KCADM config credentials --server "$BASE" --realm master --user admin --password admin --config "$KCADM_CONFIG"

# Ensure realm
if ! $KCADM get realms/"${REALM}" --config "$KCADM_CONFIG" >/dev/null 2>&1; then
  echo "[inside] Creating realm ${REALM}..."
  $KCADM create realms -s realm="${REALM}" -s enabled=true --config "$KCADM_CONFIG"
else
  echo "[inside] Realm ${REALM} exists."
fi

# Ensure group (capture create output when creating so we can extract the id reliably)
GROUP_ID=""
if ! $KCADM get groups -r "${REALM}" --config "$KCADM_CONFIG" 2>/dev/null | tr -d '\r\n' | grep -q "\"name\"[[:space:]]*:[[:space:]]*\"${GROUP}\""; then
  echo "[inside] Creating group ${GROUP}..."
  CREATE_OUT=$($KCADM create groups -r "${REALM}" -s name="${GROUP}" --config "$KCADM_CONFIG" 2>/dev/null || true)
  # try to extract id from create output which may contain "Created new group with id '...'", JSON, or plain id
  GROUP_ID=$(printf '%s' "$CREATE_OUT" | tr -d '\r\n' | sed -n -e "s/.*Created new group with id '\\([^'\\]\\+\\)'.*/\\1/p" -e "s/.*\"id\"[[:space:]]*:[[:space:]]*\"\\([^\"]\\+\\)\".*/\\1/p" -e "s/.*\\b\\([0-9a-fA-F-]\{36\}\\)\\b.*/\\1/p" | head -n1)
fi

# If GROUP_ID still empty, fallback to GET search parsing
if [ -z "$GROUP_ID" ]; then
  GROUP_JSON=$($KCADM get "groups?search=${GROUP}" -r "${REALM}" --config "$KCADM_CONFIG" 2>/dev/null || true)
  GROUP_ID=$(printf '%s' "$GROUP_JSON" | tr -d '\r\n' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^\"]\+\)".*/\1/p' | head -n1)
fi

if [ -z "$GROUP_ID" ]; then
  echo '[inside][ERROR] Could not resolve group id' >&2
  exit 1
fi

echo "[inside] Group UUID: ${GROUP_ID}"

# Ensure user exists
if ! $KCADM get users -r "${REALM}" -q username="${USERNAME}" --fields id --config "$KCADM_CONFIG" 2>/dev/null | grep -q '"id"'; then
  echo "[inside] Creating user ${USERNAME}..."
  $KCADM create users -r "${REALM}" -s username="${USERNAME}" -s enabled=true -s email="${EMAIL}" -s emailVerified=true -s firstName="${FIRST}" -s lastName="${LAST}" --config "$KCADM_CONFIG"
else
  echo "[inside] User ${USERNAME} exists."
fi

# Resolve user id
USER_JSON=$($KCADM get users -r "${REALM}" -q username="${USERNAME}" --fields id --config "$KCADM_CONFIG" 2>/dev/null)
USER_ID=$(printf '%s' "$USER_JSON" | tr -d '\r\n' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]\+\)".*/\1/p; q')
if [ -z "$USER_ID" ]; then
  echo '[inside][ERROR] Could not resolve user id' >&2
  exit 1
fi

echo '[inside] User UUID: '"$USER_ID"

# Set password
echo '[inside] Setting password for user '"$USERNAME"'...'
$KCADM set-password -r "${REALM}" --userid "$USER_ID" --new-password "${PASSWORD}" --temporary=false --config "$KCADM_CONFIG" || true

# Add user to group
if ! $KCADM get "users/${USER_ID}/groups" -r "${REALM}" --config "$KCADM_CONFIG" 2>/dev/null | grep -q '"id"'; then
  echo "[inside] Adding user ${USERNAME} to group ${GROUP}..."
  $KCADM update "users/${USER_ID}/groups/${GROUP_ID}" -r "${REALM}" -n --config "$KCADM_CONFIG"
else
  echo "[inside] User ${USERNAME} already in group ${GROUP}."
fi

echo '[inside] Done.'

