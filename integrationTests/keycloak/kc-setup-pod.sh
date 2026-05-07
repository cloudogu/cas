#!/usr/bin/env bash
set -euo pipefail

# This script runs INSIDE the Keycloak pod
# Usage: called by kc-setup-k8s.sh

REL=/auth
BASE=http://localhost:8080${REL}
KCADM=/opt/keycloak/bin/kcadm.sh

# Use a writable location for kcadm config since some Keycloak images mount /opt as read-only
KCADM_CONFIG=/tmp/kcadm.config

echo '[inside] Using kcadm config:' "$KCADM_CONFIG"

echo '[inside] Login to master...'
$KCADM config credentials --server "$BASE" --realm master --user admin --password admin --config "$KCADM_CONFIG"

# Realm (from environment)
if ! $KCADM get realms/"${REALM}" --config "$KCADM_CONFIG" >/dev/null 2>&1; then
  echo "[inside] Creating realm ${REALM}..."
  $KCADM create realms -s realm="${REALM}" -s enabled=true --config "$KCADM_CONFIG"
else
  echo "[inside] Realm ${REALM} exists."
fi

# disable ssl requirement
echo '[inside] Disabling ssl...'
$KCADM update realms/"${REALM}" -s sslRequired=NONE --config "$KCADM_CONFIG" || true

# Client
if ! $KCADM get clients -r "${REALM}" -q clientId="${CLIENT_ID}" --fields id --config "$KCADM_CONFIG" | grep -q '"id"'; then
  echo "[inside] Creating client ${CLIENT_ID}..."
  $KCADM create clients -r "${REALM}" -s clientId="${CLIENT_ID}" -s enabled=true -s protocol=openid-connect -s publicClient=false -s standardFlowEnabled=true -s implicitFlowEnabled=false -s directAccessGrantsEnabled=false -s clientAuthenticatorType=client-secret -s 'webOrigins=["*"]' -s "redirectUris=[\"${CLIENT_REDIRECT}\"]" -s 'attributes."token.endpoint.auth.method"="client_secret_basic"' --config "$KCADM_CONFIG"
else
  echo "[inside] Client ${CLIENT_ID} exists."
fi

# Client UUID
CID=$($KCADM get clients -r "${REALM}" -q clientId="${CLIENT_ID}" --fields id --config "$KCADM_CONFIG" 2>/dev/null | tr -d '\r' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^\"]\+\)".*/\1/p' | head -n1)
if [ -z "$CID" ]; then
  echo '[inside][ERROR] Could not resolve client id' >&2
  exit 1
fi

echo '[inside] Client UUID: '"$CID"

# Ensure groups mapper like in old script
MID=$($KCADM get "clients/${CID}/protocol-mappers/models" -r "${REALM}" --fields id,name --config "$KCADM_CONFIG" 2>/dev/null | tr -d '\r\n' | sed -n -e 's/.*"id"[[:space:]]*:[[:space:]]*"\([^\"]\+\)","name"[[:space:]]*:[[:space:]]*"groups".*/\1/p' -e 's/.*"name"[[:space:]]*:[[:space:]]*"groups","id"[[:space:]]*:[[:space:]]*"\([^\"]\+\)".*/\1/p' | head -n1)
if [ -n "$MID" ]; then
  echo '[inside] Removing old groups mapper: '"$MID"
  $KCADM delete "clients/${CID}/protocol-mappers/models/${MID}" -r "${REALM}" --config "$KCADM_CONFIG" || true
fi
$KCADM create "clients/${CID}/protocol-mappers/models" -r "${REALM}" -s name=groups -s protocol=openid-connect -s protocolMapper=oidc-group-membership-mapper -s 'config."full.path"=false' -s 'config."access.token.claim"=true' -s 'config."id.token.claim"=true' -s 'config."userinfo.token.claim"=true' -s 'config."claim.name"=groups' -s 'config."jsonType.label"=String' --config "$KCADM_CONFIG"

# Secret extraction and writing to /tmp/kc_out
SECRET=$($KCADM get "clients/${CID}/client-secret" -r "${REALM}" --config "$KCADM_CONFIG" | tr -d '\r' | sed -n 's/.*"value"[[:space:]]*:[[:space:]]*"\([^\"]\+\)".*/\1/p' | head -n1)
printf 'DISCOVERY=http://%s:%s%s/realms/%s/.well-known/openid-configuration\n' "localhost" "8080" "/auth" "${REALM}" > /tmp/kc_out
printf 'CLIENT_ID=%s\n' "${CLIENT_ID}" >> /tmp/kc_out
printf 'CLIENT_SECRET=%s\n' "$SECRET" >> /tmp/kc_out
cat /tmp/kc_out

