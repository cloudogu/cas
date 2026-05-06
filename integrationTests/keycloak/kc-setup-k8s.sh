#!/usr/bin/env bash
set -euo pipefail

# Idempotent Keycloak realm & client setup executed inside the Keycloak pod.
# Usage: KC_NAMESPACE=ecosystem REALM=Cloudogu CLIENT_ID=cas CLIENT_REDIRECT="https://..." ./kc-setup-k8s.sh

NAMESPACE=${KC_NAMESPACE:-ecosystem}
REALM=${REALM:-Test}
CLIENT_ID=${CLIENT_ID:-cas}
CLIENT_REDIRECT=${CLIENT_REDIRECT:-http://127.0.0.1/cas/*}
TIMEOUT=${TIMEOUT:-600}

log(){ printf '[kc-setup-k8s] %s\n' "$*" >&2; }

POD=$(kubectl -n "$NAMESPACE" get pod -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}')
[ -n "$POD" ] || { log "No Keycloak pod found in namespace ${NAMESPACE}"; exit 1; }

log "Executing setup inside pod ${POD} (realm=${REALM}, client=${CLIENT_ID})"

kubectl -n "$NAMESPACE" exec "$POD" -- bash -c "set -euo pipefail
REL=/auth
BASE=http://localhost:8080\$REL
KCADM=/opt/keycloak/bin/kcadm.sh

echo '[inside] Creating kcadm config directory...'
mkdir -p /opt/keycloak/.keycloak

echo '[inside] Login to master...'
\$KCADM config credentials --server \"\$BASE\" --realm master --user admin --password admin

# Realm
if ! \$KCADM get realms/${REALM} >/dev/null 2>&1; then
  echo '[inside] Creating realm ${REALM}...'
  \$KCADM create realms -s realm=\"${REALM}\" -s enabled=true
else
  echo '[inside] Realm ${REALM} exists.'
fi

# disable ssl requirement
echo '[inside] Disabling ssl...'
\$KCADM update realms/${REALM} -s sslRequired=NONE || true

# Client
if ! \$KCADM get clients -r ${REALM} -q clientId=${CLIENT_ID} --fields id | grep -q '"id"'; then
  echo '[inside] Creating client ${CLIENT_ID}...'
  \$KCADM create clients -r ${REALM} -s clientId=\"${CLIENT_ID}\" -s enabled=true -s protocol=openid-connect -s publicClient=false -s standardFlowEnabled=true -s implicitFlowEnabled=false -s directAccessGrantsEnabled=false -s clientAuthenticatorType=client-secret -s 'webOrigins=["*"]' -s "redirectUris=[\"${CLIENT_REDIRECT}\"]" -s 'attributes."token.endpoint.auth.method"="client_secret_basic"'
else
  echo '[inside] Client ${CLIENT_ID} exists.'
fi

# Client UUID
CID=\$(\$KCADM get clients -r ${REALM} -q clientId=${CLIENT_ID} --fields id 2>/dev/null | tr -d '\\r' | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\\([^\\"]\\+\\)".*/\\1/p' | head -n1)
if [ -z \"\$CID\" ]; then
  echo '[inside][ERROR] Could not resolve client id' >&2
  exit 1
fi

echo '[inside] Client UUID: ' \$CID

# Ensure groups mapper like in old script
MID=\$(\$KCADM get "clients/\${CID}/protocol-mappers/models" -r ${REALM} --fields id,name 2>/dev/null | tr -d '\\r\n' | sed -n -e 's/.*"id"[[:space:]]*:[[:space:]]*"\\([^\\"]\\+\\)","name"[[:space:]]*:[[:space:]]*"groups".*/\\1/p' -e 's/.*"name"[[:space:]]*:[[:space:]]*"groups","id"[[:space:]]*:[[:space:]]*"\\([^\\"]\\+\\)".*/\\1/p' | head -n1)
if [ -n \"\$MID\" ]; then
  echo '[inside] Removing old groups mapper: '\$MID
  \$KCADM delete "clients/\${CID}/protocol-mappers/models/\${MID}" -r ${REALM} || true
fi
\$KCADM create "clients/\${CID}/protocol-mappers/models" -r ${REALM} -s name=groups -s protocol=openid-connect -s protocolMapper=oidc-group-membership-mapper -s 'config."full.path"=false' -s 'config."access.token.claim"=true' -s 'config."id.token.claim"=true' -s 'config."userinfo.token.claim"=true' -s 'config."claim.name"=groups' -s 'config."jsonType.label"=String'

# Secret extraction and writing to /tmp/kc_out
SECRET=\$(\$KCADM get "clients/\${CID}/client-secret" -r ${REALM} | tr -d '\\r' | sed -n 's/.*"value"[[:space:]]*:[[:space:]]*"\\([^\\"]\\+\\)".*/\\1/p' | head -n1)
printf 'DISCOVERY=http://%s:%s%s/realms/%s/.well-known/openid-configuration\n' \"localhost\" \"8080\" \"/auth\" \"${REALM}\" > /tmp/kc_out
printf 'CLIENT_ID=%s\n' \"${CLIENT_ID}\" >> /tmp/kc_out
printf 'CLIENT_SECRET=%s\n' \"\$SECRET\" >> /tmp/kc_out
cat /tmp/kc_out
"

# copy out the result
kubectl -n "$NAMESPACE" exec -i "$POD" -- cat /tmp/kc_out > kc_out.env

log "Wrote kc_out.env (contains discovery uri, client id and client secret)."

