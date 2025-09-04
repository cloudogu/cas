#!/usr/bin/env bash
set -euo pipefail

# Defaults
NAME="${NAME:-kc}"                       # container name
REALM="${REALM:-Test}"                   # realm
CLIENT_ID_STR="${CLIENT_ID_STR:-cas}"    # clientId (string, not UUID)
SCOPE_NAME="${SCOPE_NAME:-groups}"       # client scope name
CLAIM_NAME="${CLAIM_NAME:-groups}"       # claim name in tokens

usage() {
  cat <<'USAGE' >&2
Usage: kc-group.sh [-n container] [-r realm] [-c clientId] [-s scopeName] [-a claimName]
  -n  Keycloak container name (default: kc)
  -r  Realm (default: Test)
  -c  Client ID (string, default: cas)
  -s  Client Scope name (default: groups)
  -a  Claim name for mapper (default: groups)
USAGE
  exit 1
}

while getopts ":n:r:c:s:a:" opt; do
  case "$opt" in
    n) NAME="$OPTARG" ;;
    r) REALM="$OPTARG" ;;
    c) CLIENT_ID_STR="$OPTARG" ;;
    s) SCOPE_NAME="$OPTARG" ;;
    a) CLAIM_NAME="$OPTARG" ;;
    *) usage ;;
  esac
done

# Container running?
if ! sudo docker ps --format '{{.Names}}' | grep -qx "$NAME"; then
  echo "[kc-group] Container '$NAME' not running. Start it first." >&2
  exit 1
fi

sudo docker exec -iu 0 \
  -e REALM="$REALM" \
  -e CLIENT_ID_STR="$CLIENT_ID_STR" \
  -e SCOPE_NAME="$SCOPE_NAME" \
  -e CLAIM_NAME="$CLAIM_NAME" \
  "$NAME" bash -s <<'INSIDE'
set -euo pipefail

BASE="http://localhost:8080/auth"

# kcadm path
if [ -x /opt/keycloak/bin/kcadm.sh ]; then
  KCADM="/opt/keycloak/bin/kcadm.sh"
elif [ -x /opt/jboss/keycloak/bin/kcadm.sh ]; then
  KCADM="/opt/jboss/keycloak/bin/kcadm.sh"
else
  echo "[inside] kcadm.sh not found" >&2
  exit 1
fi

: "${REALM:?missing REALM}"
: "${CLIENT_ID_STR:?missing CLIENT_ID_STR}"
: "${SCOPE_NAME:?missing SCOPE_NAME}"
: "${CLAIM_NAME:?missing CLAIM_NAME}"

echo "[inside] Login (master)…"
"$KCADM" config credentials --server "$BASE" --realm master --user admin --password admin

# Ensure realm exists
"$KCADM" get "realms/${REALM}" >/dev/null 2>&1 || "$KCADM" create realms -s realm="${REALM}" -s enabled=true

echo "[inside] Ensure client-scope '${SCOPE_NAME}'…"
"$KCADM" create client-scopes -r "$REALM" -s name="$SCOPE_NAME" -s protocol=openid-connect >/dev/null 2>&1 || true

# ---- Robust ID resolvers (no jq/awk) ----
# Make a one-line JSON string to pattern-match '..."name":"<name>"...' and grab the preceding id.
to_one_line() { tr -d '\r\n\t ' ; }

SCOPE_LIST="$("$KCADM" get client-scopes -r "$REALM" --fields id,name 2>/dev/null)"
SCOPE_ONE="$(printf '%s' "$SCOPE_LIST" | to_one_line)"
SCOPE_ID="$(printf '%s' "$SCOPE_ONE" | sed -n 's/.*{"id":"\([^"]\+\)","name":"'"$SCOPE_NAME"'".*/\1/p')"
if [ -z "$SCOPE_ID" ]; then
  echo "[inside][ERROR] Could not resolve scope id for '$SCOPE_NAME'"; exit 1
fi
echo "[inside] SCOPE_ID($SCOPE_NAME): $SCOPE_ID"

CLIENT_LIST="$("$KCADM" get clients -r "$REALM" --fields id,clientId 2>/dev/null)"
CLIENT_ONE="$(printf '%s' "$CLIENT_LIST" | to_one_line)"
CLIENT_ID="$(printf '%s' "$CLIENT_ONE" | sed -n 's/.*{"id":"\([^"]\+\)","clientId":"'"$CLIENT_ID_STR"'".*/\1/p')"
if [ -z "$CLIENT_ID" ]; then
  echo "[inside][ERROR] Could not resolve client id for '$CLIENT_ID_STR'"; exit 1
fi
echo "[inside] CLIENT_ID($CLIENT_ID_STR): $CLIENT_ID"

# ---- Ensure 'Group Membership' mapper on the scope ----
MAPPERS_JSON="$("$KCADM" get "client-scopes/${SCOPE_ID}/protocol-mappers/models" -r "$REALM" 2>/dev/null | to_one_line)"
if ! printf '%s' "$MAPPERS_JSON" | grep -q '"name":"groups"'; then
  echo "[inside] Creating 'Group Membership' mapper on scope '${SCOPE_NAME}'…"
  "$KCADM" create "client-scopes/${SCOPE_ID}/protocol-mappers/models" -r "$REALM" \
    -s name=groups \
    -s protocol=openid-connect \
    -s protocolMapper=oidc-group-membership-mapper \
    -s 'config."full.path"=false' \
    -s 'config."id.token.claim"=true' \
    -s 'config."access.token.claim"=true' \
    -s 'config."userinfo.token.claim"=true' \
    -s 'config."introspection.token.claim"=true' \
    -s "config.\"claim.name\"=${CLAIM_NAME}" \
    -s 'config."jsonType.label"=String'
else
  echo "[inside] Mapper 'groups' already present on scope '${SCOPE_NAME}'."
fi

# ---- Attach scope as default to the client ----
DEFAULTS_JSON="$("$KCADM" get "clients/${CLIENT_ID}/default-client-scopes" -r "$REALM" 2>/dev/null | to_one_line)"
if ! printf '%s' "$DEFAULTS_JSON" | grep -q "\"id\":\"${SCOPE_ID}\""; then
  echo "[inside] Attaching scope '${SCOPE_NAME}' to client '${CLIENT_ID_STR}' as default…"
  "$KCADM" update "clients/${CLIENT_ID}/optional-client-scopes/${SCOPE_ID}" -r "$REALM" -n
else
  echo "[inside] Scope '${SCOPE_NAME}' already attached as default."
fi

# ---- Verification ----
echo
echo "[inside][VERIFY] Client-scopes (name → id):"
"$KCADM" get client-scopes -r "$REALM" --fields id,name | sed 's/^/  /'

echo
echo "[inside][VERIFY] Mappers on scope '${SCOPE_NAME}':"
"$KCADM" get "client-scopes/${SCOPE_ID}/protocol-mappers/models" -r "$REALM" --fields id,name,protocolMapper | sed 's/^/  /'

echo
echo "[inside][VERIFY] Default client-scopes for client '${CLIENT_ID_STR}':"
"$KCADM" get "clients/${CLIENT_ID}/default-client-scopes" -r "$REALM" --fields id,name | sed 's/^/  /'

echo
echo "[inside] Done."
INSIDE

echo "[kc-group] Success."
