#!/usr/bin/env bash
set -euo pipefail

# ------------ config (env or flags) ------------
NAME="${NAME:-kc}"                 # container name
KC_HOST="${KC_HOST:-192.168.56.2}" # host/IP used for discovery URL output
KC_PORT="${KC_PORT:-9000}"         # host port of Keycloak
REALM="${REALM:-Test}"
CLIENT_ID="${CLIENT_ID:-cas}"
CAS_URL="${CAS_URL:-http://${KC_HOST}/cas}"   # your CAS base URL
CAS_HTTPS_URL="${CAS_HTTPS_URL:-https://${KC_HOST}/cas}"   # your CAS base URL
REDIRECT="${REDIRECT:-${CAS_HTTPS_URL%/}/login*}"     # redirect pattern

while getopts ":n:H:P:r:c:u:" opt; do
  case "$opt" in
    n) NAME="$OPTARG" ;;
    H) KC_HOST="$OPTARG" ;;
    P) KC_PORT="$OPTARG" ;;
    r) REALM="$OPTARG" ;;
    c) CLIENT_ID="$OPTARG" ;;
    u) CAS_URL="$OPTARG"; REDIRECT="${CAS_URL%/}/login*" ;;
    *) ;;
  esac
done

log() { printf '[kc-setup] %s\n' "$*" >&2; }

# sanity
if ! sudo docker ps --format '{{.Names}}' | grep -q "^${NAME}\$"; then
  log "Container ${NAME} is not running. Start it with ./kc-up.sh first."
  exit 1
fi

# Write an idempotent setup script into the container and execute it
sudo docker exec -i \
  -e PUBLIC_HOST="${KC_HOST}" \
  -e PUBLIC_PORT="${KC_PORT}" \
  -e REALM="${REALM}" \
  -e CLIENT_ID="${CLIENT_ID}" \
  -e REDIRECT="${REDIRECT}" \
  "${NAME}" bash -s <<'EOF'
set -euo pipefail

REL="/auth"
BASE="http://localhost:8080${REL}"

# Find kcadm for both legacy and Quarkus paths
if [ -x /opt/jboss/keycloak/bin/kcadm.sh ]; then
  KCADM="/opt/jboss/keycloak/bin/kcadm.sh"
elif [ -x /opt/keycloak/bin/kcadm.sh ]; then
  KCADM="/opt/keycloak/bin/kcadm.sh"
else
  echo "[inside] kcadm.sh not found" >&2
  exit 1
fi

: "${PUBLIC_HOST:?missing PUBLIC_HOST}"
: "${PUBLIC_PORT:?missing PUBLIC_PORT}"
: "${REALM:?missing REALM}"
: "${CLIENT_ID:?missing CLIENT_ID}"
: "${REDIRECT:?missing REDIRECT}"

echo "[inside] Logging in to master…"
"${KCADM}" config credentials --server "${BASE}" --realm master --user admin --password admin

# Realm (idempotent)
if ! "${KCADM}" get "realms/${REALM}" >/dev/null 2>&1; then
  echo "[inside] Creating realm ${REALM}…"
  "${KCADM}" create realms -s realm="${REALM}" -s enabled=true
else
  echo "[inside] Realm ${REALM} exists."
fi

# Client (idempotent)
if ! "${KCADM}" get clients -r "${REALM}" -q clientId="${CLIENT_ID}" --fields id | grep -q '"id"'; then
  echo "[inside] Creating client ${CLIENT_ID}…"
  "${KCADM}" create clients -r "${REALM}" \
    -s clientId="${CLIENT_ID}" \
    -s enabled=true \
    -s protocol=openid-connect \
    -s publicClient=false \
    -s standardFlowEnabled=true \
    -s implicitFlowEnabled=false \
    -s directAccessGrantsEnabled=false \
    -s clientAuthenticatorType=client-secret \
    -s 'webOrigins=["*"]' \
    -s "redirectUris=[\"${REDIRECT}\"]" \
    -s 'attributes."pkce.code.challenge.method"="S256"' \
    -s 'attributes."token.endpoint.auth.method"="client_secret_post"'
else
  echo "[inside] Client ${CLIENT_ID} exists."
fi

# Client ID (UUID)
CID="$(
  "${KCADM}" get clients -r "${REALM}" -q clientId="${CLIENT_ID}" --fields id 2>/dev/null \
  | tr -d '\r' \
  | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]\+\)".*/\1/p' \
  | head -n1
)"
if [ -z "${CID}" ]; then
  echo "[inside][ERROR] Could not resolve client id for '${CLIENT_ID}'" >&2
  exit 1
fi
echo "[inside] Client UUID: ${CID}"

# Ensure 'groups' mapper exists exactly once (no awk/jq)
# Try both id-before-name and name-before-id orders on a single line
MID="$(
  "${KCADM}" get "clients/${CID}/protocol-mappers/models" -r "${REALM}" --fields id,name 2>/dev/null \
  | tr -d '\r\n' \
  | sed -n -e 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]\+\)","name"[[:space:]]*:[[:space:]]*"groups".*/\1/p' \
           -e 's/.*"name"[[:space:]]*:[[:space:]]*"groups","id"[[:space:]]*:[[:space:]]*"\([^"]\+\)".*/\1/p' \
  | head -n1
)"
if [ -n "${MID}" ]; then
  echo "[inside] Removing old groups mapper: ${MID}"
  "${KCADM}" delete "clients/${CID}/protocol-mappers/models/${MID}" -r "${REALM}" || true
fi
echo "[inside] Creating groups mapper…"
"${KCADM}" create "clients/${CID}/protocol-mappers/models" -r "${REALM}" \
  -s name=groups \
  -s protocol=openid-connect \
  -s protocolMapper=oidc-group-membership-mapper \
  -s 'config."full.path"=false' \
  -s 'config."access.token.claim"=true' \
  -s 'config."id.token.claim"=true' \
  -s 'config."userinfo.token.claim"=true' \
  -s 'config."claim.name"=groups'

# Group testers (idempotent) without awk
if ! "${KCADM}" get groups -r "${REALM}" | tr -d '\r' | grep -q '"name"[[:space:]]*:[[:space:]]*"testers"'; then
  echo "[inside] Creating group testers…"
  "${KCADM}" create groups -r "${REALM}" -s name=testers
fi

# Client secret
SECRET="$(
  "${KCADM}" get "clients/${CID}/client-secret" -r "${REALM}" | tr -d '\r' \
  | sed -n 's/.*"value"[[:space:]]*:[[:space:]]*"\([^"]\+\)".*/\1/p' \
  | head -n1
)"

printf 'DISCOVERY=http://%s:%s%s/realms/%s/.well-known/openid-configuration\n' "${PUBLIC_HOST}" "${PUBLIC_PORT}" "${REL}" "${REALM}" > /tmp/kc_out
printf 'CLIENT_ID=%s\n' "${CLIENT_ID}" >> /tmp/kc_out
printf 'CLIENT_SECRET=%s\n' "${SECRET}"   >> /tmp/kc_out

cat /tmp/kc_out
EOF

# Save outputs locally, too
sudo docker exec -i "${NAME}" bash -lc 'cat /tmp/kc_out' | tee kc_out.env >/dev/null

echo
echo "==> Wrote kc_out.env:"
cat kc_out.env
echo
echo "Use these in CAS:"
echo "  cas.authn.pac4j.oidc[0].discovery-uri=$(grep ^DISCOVERY= kc_out.env | cut -d= -f2-)"
echo "  cas.authn.pac4j.oidc[0].client-id=$(grep ^CLIENT_ID= kc_out.env | cut -d= -f2-)"
echo "  cas.authn.pac4j.oidc[0].secret=$(grep ^CLIENT_SECRET= kc_out.env | cut -d= -f2-)"
