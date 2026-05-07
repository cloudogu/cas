#!/usr/bin/env bash
set -euo pipefail
# Idempotent Keycloak user creation executed inside the Keycloak pod.
# Usage: KC_NAMESPACE=ecosystem REALM=Test GROUP=testers USERNAME=tester EMAIL=test@example.com PASSWORD=test ./kc-add-user-k8s.sh
NAMESPACE=${KC_NAMESPACE:-ecosystem}
REALM=${REALM:-Test}
GROUP=${GROUP:-testers}
USERNAME=${USERNAME:?}
EMAIL=${EMAIL:?}
PASSWORD=${PASSWORD:?}
FIRST=${FIRST:-Test}
LAST=${LAST:-User}
log(){ printf '[kc-add-user-k8s] %s\n' "$*" >&2; }
POD=$(kubectl -n "$NAMESPACE" get pod -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}')
[ -n "$POD" ] || { log "No Keycloak pod found in namespace ${NAMESPACE}"; exit 1; }
log "Executing user creation inside pod ${POD} (realm=${REALM}, user=${USERNAME}, group=${GROUP})"
# Copy the setup script into the pod and execute it
kubectl -n "$NAMESPACE" cp "$(dirname "$0")/kc-add-user-pod.sh" "$POD":/tmp/kc-add-user-pod.sh
# Execute the user creation script inside the pod with required environment variables
kubectl -n "$NAMESPACE" exec "$POD" -- bash -c "
  export REALM='${REALM}'
  export GROUP='${GROUP}'
  export USERNAME='${USERNAME}'
  export EMAIL='${EMAIL}'
  export PASSWORD='${PASSWORD}'
  export FIRST='${FIRST}'
  export LAST='${LAST}'
  bash /tmp/kc-add-user-pod.sh
"
log "User creation finished"
