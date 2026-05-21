#!/usr/bin/env bash
set -euo pipefail

# This script runs INSIDE the Keycloak pod
# Usage: called by kc-group-k8s.sh

REL=/auth
BASE=http://localhost:8080${REL}
KCADM=/opt/keycloak/bin/kcadm.sh

# Use a writable location for kcadm config since some Keycloak images mount /opt as read-only
KCADM_CONFIG=/tmp/kcadm.config

: "${REALM:?missing REALM}"
: "${CLIENT_ID_STR:?missing CLIENT_ID_STR}"
: "${SCOPE_NAME:?missing SCOPE_NAME}"
: "${CLAIM_NAME:?missing CLAIM_NAME}"

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

# Ensure client-scope
echo "[inside] Ensuring client-scope ${SCOPE_NAME}..."
$KCADM create client-scopes -r "${REALM}" -s name="${SCOPE_NAME}" -s protocol=openid-connect --config "$KCADM_CONFIG" >/dev/null 2>&1 || true

# Helper to convert multi-line JSON to single line (strip whitespace)
to_one_line(){ tr -d '\r\n\t ' ; }

# Resolve scope id
echo "[inside] Resolving scope id for ${SCOPE_NAME}..."
SCOPE_LIST=$($KCADM get client-scopes -r "${REALM}" --fields id,name --config "$KCADM_CONFIG" 2>/dev/null)
SCOPE_ONE=$(printf '%s' "$SCOPE_LIST" | to_one_line)
SCOPE_ID=$(printf '%s' "$SCOPE_ONE" | sed -n 's/.*{"id":"\([^"]\+\)","name":"'"${SCOPE_NAME}"'".*/\1/p')
if [ -z "$SCOPE_ID" ]; then
  echo '[inside][ERROR] Could not resolve scope id' >&2
  exit 1
fi
echo "[inside] Scope ID: ${SCOPE_ID}"

# Resolve client id
echo "[inside] Resolving client id for ${CLIENT_ID_STR}..."
CLIENT_LIST=$($KCADM get clients -r "${REALM}" --fields id,clientId --config "$KCADM_CONFIG" 2>/dev/null)
CLIENT_ONE=$(printf '%s' "$CLIENT_LIST" | to_one_line)
CLIENT_ID=$(printf '%s' "$CLIENT_ONE" | sed -n 's/.*{"id":"\([^"]\+\)","clientId":"'"${CLIENT_ID_STR}"'".*/\1/p')
if [ -z "$CLIENT_ID" ]; then
  echo '[inside][ERROR] Could not resolve client id' >&2
  exit 1
fi
echo "[inside] Client ID: ${CLIENT_ID}"

# Ensure groups mapper on scope
echo "[inside] Ensuring 'Group Membership' mapper on scope ${SCOPE_NAME}..."
MAPPERS_JSON=$($KCADM get "client-scopes/${SCOPE_ID}/protocol-mappers/models" -r "${REALM}" --config "$KCADM_CONFIG" 2>/dev/null | to_one_line)
if ! printf '%s' "$MAPPERS_JSON" | grep -q '"name":"groups"'; then
  echo "[inside] Creating mapper..."
  $KCADM create "client-scopes/${SCOPE_ID}/protocol-mappers/models" -r "${REALM}" \
    -s name=groups \
    -s protocol=openid-connect \
    -s protocolMapper=oidc-group-membership-mapper \
    -s 'config."full.path"=false' \
    -s 'config."id.token.claim"=true' \
    -s 'config."access.token.claim"=true' \
    -s 'config."userinfo.token.claim"=true' \
    -s 'config."introspection.token.claim"=true' \
    -s "config.\"claim.name\"=${CLAIM_NAME}" \
    -s 'config."jsonType.label"=String' \
    --config "$KCADM_CONFIG"
else
  echo "[inside] Mapper already exists."
fi

# Attach scope as default to client if missing
echo "[inside] Attaching scope to client ${CLIENT_ID_STR} if not already attached..."
DEFAULTS_JSON=$($KCADM get "clients/${CLIENT_ID}/default-client-scopes" -r "${REALM}" --config "$KCADM_CONFIG" 2>/dev/null | to_one_line)
if ! printf '%s' "$DEFAULTS_JSON" | grep -q '"id":"'"${SCOPE_ID}"'"'; then
  echo "[inside] Attaching scope..."
  $KCADM update "clients/${CLIENT_ID}/optional-client-scopes/${SCOPE_ID}" -r "${REALM}" -n --config "$KCADM_CONFIG" || true
else
  echo "[inside] Scope already attached."
fi

echo '[inside] Done.'

