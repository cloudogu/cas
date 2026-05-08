#!/usr/bin/env bash
set -euo pipefail
# Idempotent Keycloak client-scope and group-mapper setup executed inside the Keycloak pod.
# Usage: KC_NAMESPACE=ecosystem REALM=Test CLIENT_ID_STR=cas SCOPE_NAME=groups CLAIM_NAME=groups ./kc-group-k8s.sh
NAMESPACE=${KC_NAMESPACE:-ecosystem}
REALM=${REALM:-Test}
CLIENT_ID_STR=${CLIENT_ID_STR:-cas}
SCOPE_NAME=${SCOPE_NAME:-groups}
CLAIM_NAME=${CLAIM_NAME:-groups}
log(){ printf '[kc-group-k8s] %s\n' "$*" >&2; }
POD=$(kubectl -n "$NAMESPACE" get pod -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}')
[ -n "$POD" ] || { log "No Keycloak pod found in namespace ${NAMESPACE}"; exit 1; }
log "Executing group/scope setup inside pod ${POD} (realm=${REALM}, client=${CLIENT_ID_STR})"
# Copy the setup script into the pod and execute it
kubectl -n "$NAMESPACE" cp "$(dirname "$0")/kc-group-pod.sh" "$POD":/tmp/kc-group-pod.sh
# Execute the setup script inside the pod with required environment variables
kubectl -n "$NAMESPACE" exec "$POD" -- bash -c "
  export REALM='${REALM}'
  export CLIENT_ID_STR='${CLIENT_ID_STR}'
  export SCOPE_NAME='${SCOPE_NAME}'
  export CLAIM_NAME='${CLAIM_NAME}'
  bash /tmp/kc-group-pod.sh
"
log "Group/scope setup finished"
