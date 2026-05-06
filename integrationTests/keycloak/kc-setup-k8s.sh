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

# Copy the setup script into the pod and execute it
kubectl -n "$NAMESPACE" cp "$(dirname "$0")/kc-setup-pod.sh" "$POD":/tmp/kc-setup-pod.sh

# Execute the setup script inside the pod with required environment variables
kubectl -n "$NAMESPACE" exec "$POD" -- bash -c "
  export REALM='${REALM}'
  export CLIENT_ID='${CLIENT_ID}'
  export CLIENT_REDIRECT='${CLIENT_REDIRECT}'
  bash /tmp/kc-setup-pod.sh
"

# copy out the result
kubectl -n "$NAMESPACE" exec -i "$POD" -- cat /tmp/kc_out > kc_out.env

log "Wrote kc_out.env (contains discovery uri, client id and client secret)."

